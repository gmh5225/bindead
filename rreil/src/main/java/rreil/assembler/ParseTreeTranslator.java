package rreil.assembler;

import java.util.HashMap;
import java.util.Map;

import javalx.numeric.BigInt;
import javalx.persistentcollections.BiMap;
import rreil.assembler.parser.ASTCallNat;
import rreil.assembler.parser.ASTFunArgs;
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
import rreil.assembler.parser.ParseException;
import rreil.assembler.parser.VarSize;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.Rlit;

/**
 * Translates the parsed AST to a list of instructions with labels resolved.
 */
public class ParseTreeTranslator extends AbstractParseTreeTranslator {
  private LabelCollector labelCollector;
  // Options that can be set in the RREIL code
  private final Map<String, String> options = new HashMap<String, String>();
  private static final String OPT_DEFAULT_SIZE = "DEFAULT_SIZE";

  /**
   * Translates a parsed RREIL code module into an RREIL instructions module.
   */
  public CompiledAssembler translate (ASTModule module) throws ParseException {
    labelCollector = new LabelCollector();
    module.jjtAccept(labelCollector, null);
    module.jjtAccept(this, null);
    BiMap<RReilAddr, String> labels = BiMap.from(labelCollector.getLabels()).inverted();
    int defaultSize = 32;
    String defaultSizeOption = options.get(OPT_DEFAULT_SIZE);
    if (defaultSizeOption != null)
      defaultSize = parseSizeOption(defaultSizeOption);
    return new CompiledAssembler(instructions, pointers, labels, defaultSize);
  }

  @Override public Object visit (ASTOpt node, Object data) throws ParseException {
    ASTOptID optId = (ASTOptID) node.jjtGetChild(0);
    String id = optId.jjtGetValue().toString();
    ASTOptVal optValue = (ASTOptVal) node.jjtGetChild(1);
    Object value = optValue.jjtGetValue();
    options.put(id, value.toString());
    return null;
  }

  @Override public Object visit (ASTOptID node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTOptVal node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTLabelDeref node, Object data) throws ParseException {
    return label((ASTLabel) node.jjtGetChild(0), (Integer) data);
  }

  @Override public Object visit (ASTCallNat node, Object data) throws ParseException {
    throw new UnsupportedOperationException("Natives not supported!");
  }

  @Override public Object visit (ASTNatDef node, Object data) throws ParseException {
    throw new UnsupportedOperationException("Native definitions not supported!");
  }

  @Override public Object visit (ASTNatSizeParams node, Object data) throws ParseException {
    throw new UnsupportedOperationException("Native definitions not supported!");
  }

  @Override public Object visit (ASTNatSizeParam node, Object data) throws ParseException {
    throw new UnsupportedOperationException("Native definitions not supported!");
  }

  @Override public Object visit (ASTNatLabel node, Object data) throws ParseException {
    throw new UnsupportedOperationException("Natives not supported!");
  }

  @Override public Object visit (ASTFunArgs node, Object data) throws ParseException {
    throw new UnsupportedOperationException("Natives not supported!");
  }

  @Override protected Integer size (ASTSize node) throws ParseException {
    VarSize size = (VarSize) node.jjtGetValue();
    if (!size.isSet()) {
      // Fill in OPT_DEFAULT_SIZE
      Object optSize = options.get(OPT_DEFAULT_SIZE);
      if (optSize == null) {
        throw new ParseException("No size defined, while option DEFAULT_SIZE is not set, either! Near: "
          + currentInstructionAddress);
      }
      Integer defaultSize = parseSizeOption(optSize.toString());
      return defaultSize;
    } else if (size.isTemlateVar()) {
      throw new ParseException("No size templates allowed outside function definitions!");
    }
    return size.asInteger();
  }

  private static Integer parseSizeOption (String sizeStr) throws ParseException {
    if (sizeStr.isEmpty())
      throw new ParseException("Option DEFAULT_SIZE filled with undefined content: '" + sizeStr + "'!");
    if (sizeStr.length() != 1)
      return Integer.valueOf(sizeStr);
    switch (sizeStr.charAt(0)) {
    case '1':
      return 1;
    case 'b':
      return 8;
    case 'w':
      return 16;
    case 'd':
      return 32;
    case 'q':
      return 64;
    case 'o':
      return 128;
    case 'z':
      return 256;
    default:
      return Integer.valueOf(sizeStr);
    }
  }

  private Rlit label (ASTLabel node, Integer size) throws ParseException {
    String name = (String) node.jjtGetValue();
    RReilAddr rreilAddr = labelCollector.getLabelAddress(name);
    if (rreilAddr == null)
      throw new ParseException("Undefined label: " + name);
    BigInt labelAddress = BigInt.of(rreilAddr.base());
    return rreil.literal(size, labelAddress);
  }

}
