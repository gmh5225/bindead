package rreil.assembler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import javalx.numeric.BigInt;
import rreil.assembler.parser.ASTCallNat;
import rreil.assembler.parser.ASTFunArgs;
import rreil.assembler.parser.ASTIdent;
import rreil.assembler.parser.ASTLabel;
import rreil.assembler.parser.ASTLabelDeref;
import rreil.assembler.parser.ASTModule;
import rreil.assembler.parser.ASTNatDef;
import rreil.assembler.parser.ASTNatLabel;
import rreil.assembler.parser.ASTNatSizeParam;
import rreil.assembler.parser.ASTNatSizeParams;
import rreil.assembler.parser.ASTOpt;
import rreil.assembler.parser.ASTOptID;
import rreil.assembler.parser.ASTOptVal;
import rreil.assembler.parser.ASTSize;
import rreil.assembler.parser.ASTVariable;
import rreil.assembler.parser.ParseException;
import rreil.assembler.parser.VarSize;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;

/**
 * Parses a single function from the given module<br/>
 * Layout:
 *
 * <pre>
 * def ((out_args)*) = fun (<A (, B)*>)? ((in_args)*) {
 *      (stmts)*
 * }[...]EOF
 * </pre>
 *
 * (in_args and out_args may intersect. out_args start with pure out, followed by in/out<br/>
 *
 * Thereby it does two things:
 * <ul>
 * <li>replace arguments names with the given ones</li>
 * <li>instantiate the size template variables with the given sizes ones</li>
 * </ul>
 *
 * Therefore it parses input and output arguments. During the further parsing process (mainly handled by
 * {@link BasicParser}) it checks every detected variable and replaces it if it matches an {@link #varsToReplace}.
 * Local variables are given an unique name.<br/>
 * TODO:
 * - Options
 * - Check for usage (differ between read/write??) of ins/outs
 * - Real unique names for locals!!!
 */
public class SingleFunctionParser extends AbstractParseTreeTranslator {
  public static final String FUNCTION_LOCALS_PREFIX = "__local__";

  private final List<SizedVariable> inputVars;
  private final List<SizedVariable> outputVars;
  /** Given sizes for template instantiation */
  private final Map<String, Integer> sizeParams;

  private LabelCollector labelCollector = null;

  private String name = "<noname_native>";
  /** {templateId, Argument(name, size, type)} */
  private final Map<String, Argument> varsToReplace = new HashMap<String, Argument>();
  private final Map<String, Integer> sizeTemplateParams = new HashMap<String, Integer>();


  public SingleFunctionParser (List<SizedVariable> inputVars, List<SizedVariable> outputVars,
      Map<String, Integer> sizeParams) {
    this.inputVars = inputVars;
    this.outputVars = outputVars;
    if (sizeParams == null) {
      this.sizeParams = Collections.emptyMap();
    } else {
      this.sizeParams = new HashMap<String, Integer>();
      for (Entry<String, Integer> entry : sizeParams.entrySet()) {
        assert entry.getValue() > 0 : "Sizes must be >= 0!";
        this.sizeParams.put(entry.getKey(), entry.getValue());
      }
    }
  }

  public SortedMap<RReilAddr, RReil> instantiateFunctionTemplate (ASTModule module)
      throws ParseException {
    labelCollector = new LabelCollector();
    module.jjtAccept(labelCollector, null);
    module.jjtAccept(this, null);
    return instructions;
  }

  @Override public Object visit (ASTNatDef node, Object data) throws ParseException {
    // Handle size parameters
    ASTNatSizeParams sizeParamsNode = (ASTNatSizeParams) node.jjtGetChild(2);
    for (int i = 0; i < sizeParamsNode.jjtGetNumChildren(); i++) {
      // Grab template variable
      ASTNatSizeParam sizeParamNode = (ASTNatSizeParam) sizeParamsNode.jjtGetChild(i);
      String paramImage = (String) sizeParamNode.jjtGetValue();

      // Get given sizes
      Integer templateSize = sizeParams.get(paramImage);
      if (templateSize == null) {
        throw new ParseException("No template size set for '" + paramImage + "'!");
      }
      sizeTemplateParams.put(paramImage, templateSize);
    }

    // Handle arguments
    // IN first
    ASTFunArgs inArgs = (ASTFunArgs) node.jjtGetChild(3);
    for (int i = 0; i < inArgs.jjtGetNumChildren(); i += 2) {        // <-- += 2!!!
      int parsedSize = size((ASTSize) inArgs.jjtGetChild(i + 1));
      ASTVariable variable = (ASTVariable) inArgs.jjtGetChild(i);
      String templateId = identifier((ASTIdent) variable.jjtGetChild(0));

      // Set replacement
      int inIndex = i / 2;
      if (inIndex >= inputVars.size()) {
        throw new ParseException(
          "Error while instantiating template function: Expecting more input parameters then available!");
      }
      SizedVariable mappedVar = inputVars.get(inIndex);
      if (parsedSize != mappedVar.getSize()) {
        throw new ParseException("Function definitions parameter size does not match given variable size!");
      }

      Argument arg = varsToReplace.get(templateId);
      if (arg != null) {
        throw new ParseException("Double occurence of input parameter '" + templateId + "'!");
      }
      Argument inArg = new Argument(mappedVar, Argument.Type.IN);
      varsToReplace.put(templateId, inArg);
    }

    // OUT
    ASTFunArgs outArgs = (ASTFunArgs) node.jjtGetChild(0);
    for (int i = 0; i < outArgs.jjtGetNumChildren(); i += 2) {        // <-- += 2!!!
      int parsedSize = size((ASTSize) outArgs.jjtGetChild(i + 1));
      ASTVariable variable = (ASTVariable) outArgs.jjtGetChild(i);
      String templateId = identifier((ASTIdent) variable.jjtGetChild(0));

      // Set replacement
      int outIndex = i / 2;
      if (outIndex >= outputVars.size()) {
        throw new ParseException(
          "Error while instantiating template function: Expecting more output parameters then available!");
      }
      SizedVariable mappedVar = outputVars.get(outIndex);
      if (parsedSize != mappedVar.getSize()) {
        throw new ParseException("Function definitions parameter size does not match given variable size!");
      }

      Argument inArg = varsToReplace.remove(templateId);
      if (inArg != null) {
        if (inArg.getType() == Argument.Type.OUT) {
          throw new ParseException("Double occurence of output parameter '" + templateId + "'!");
        }

        if (!mappedVar.getSize().equals(inArg.getSize())) {
          throw new ParseException("Size of argument '" + mappedVar.getName() + "' differs between in ("
            + inArg.getSize() + ") and out (" + mappedVar.getSize() + ")!");
        }
        varsToReplace.put(templateId, new Argument(inArg, Argument.Type.INOUT));
      } else {
        varsToReplace.put(templateId, new Argument(mappedVar, Argument.Type.OUT));
      }
    }
    this.name = ((ASTNatLabel) node.jjtGetChild(1)).jjtGetValue().toString();


    // Process all child statements
    for (int i = 4; i < node.jjtGetNumChildren(); i++) {
      node.jjtGetChild(i).jjtAccept(this, data);
    }
    return null;
  }

  @Override public Object visit (ASTNatSizeParams node, Object data) throws ParseException {
    return node.childrenAccept(this, data);
  }

  @Override public Object visit (ASTNatSizeParam node, Object data) throws ParseException {
    return null;
  }

  @Override protected Rhs.Rvar variable (ASTVariable node, Integer size) throws ParseException {
    String templateId = identifier((ASTIdent) node.jjtGetChild(0));

    // Adjust names - locals should be unique, others are replaced
    String instantiatedId = checkAndAdjustName(templateId);
    return variableWithId(instantiatedId, node, size);
  }

  @Override protected Integer size (ASTSize node) throws ParseException {
    VarSize size = (VarSize) node.jjtGetValue();

    // Insert template size if necessary
    if (!size.isSet()) {
      // size = templateFieldSize;
      // TODO: Options?
      throw new ParseException("In function definitions all sizes must be set explicitly!");
    } else if (size.isTemlateVar()) {
      String image = size.getToken().image;
      Integer templateSize = sizeTemplateParams.get(image);
      if (templateSize == null) {
        throw new ParseException("No template parameter '" + image + "' defined!");
      }
      return templateSize;
    }
    return size.asInteger();
  }

  private String checkAndAdjustName (String templateId) {
    String result;
    if (varsToReplace.containsKey(templateId)) {
      result = varsToReplace.get(templateId).getName();
    } else {
      // TemplateId is not to be replaced, so it has to a local!
      result = FUNCTION_LOCALS_PREFIX + templateId;
      // TODO: make real unique!
    }
    return result;
  }


  /**
   * Handled in by ASTFunDef/not needed
   */
  @Override public Object visit (ASTFunArgs node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTNatLabel node, Object data) throws ParseException {
    return null;
  }

  /**
   * Not allowed inside native function definitions:
   */
  @Override public Object visit (ASTOpt node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTLabelDeref node, Object data) throws ParseException {
    return label((ASTLabel) node.jjtGetChild(0), (Integer) data);
  }

  private Rhs.Rlit label (ASTLabel node, Integer size) {
    String name = (String) node.jjtGetValue();
    BigInt labelAddress = BigInt.of(labelCollector.getLabelAddress(name).base());
    return rreil.literal(size, labelAddress);
  }

  @Override public Object visit (ASTOptID node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTOptVal node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTCallNat node, Object data) throws ParseException {
    return null;
  }


  public String getName () {
    return name;
  }

  public static class SizedVariable {
    final String a;
    final Integer b;

    public SizedVariable (String a, Integer b) {
      this.a = a;
      this.b = b;
      assert b > 0 : "Size must be >= 0!";
    }

    public String getName () {
      return a;
    }

    public Integer getSize () {
      return b;
    }
  }

  public static class Argument extends SizedVariable {
    public enum Type {
      IN,
      OUT,
      INOUT;
    }

    private final Type type;

    private boolean isWrittenTo = false;
    private boolean isReadFrom = false;

    public Argument (String name, Integer size, Type type) {
      super(name, size);
      this.type = type;
    }

    public Argument (SizedVariable var, Type type) {
      super(var.getName(), var.getSize());
      this.type = type;
    }

    public Type getType () {
      return type;
    }

    public boolean isWrittenTo () {
      return isWrittenTo;
    }

    public boolean isReadFrom () {
      return isReadFrom;
    }

    public boolean isUsed () {
      return isWrittenTo || isReadFrom;
    }

    public void setReadFrom () throws ParseException {
      switch (type) {
      case OUT:
        throw new ParseException("OUT parameter " + getName() + " is read from!");

      case IN:
      case INOUT:
        isReadFrom = true;
        break;
      }
    }

    public void setWrittenTo () throws ParseException {
      switch (type) {
      case IN:
        throw new ParseException("IN parameter " + getName() + " is read from!");

      case INOUT:
      case OUT:
        isWrittenTo = true;
        break;
      }
    }
  }
}
