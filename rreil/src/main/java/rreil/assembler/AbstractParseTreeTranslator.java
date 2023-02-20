package rreil.assembler;

import static javalx.data.Option.none;
import static javalx.data.Option.some;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javalx.data.Option;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;
import rreil.assembler.parser.ASTAdd;
import rreil.assembler.parser.ASTAnd;
import rreil.assembler.parser.ASTAssert;
import rreil.assembler.parser.ASTAssertionComparison;
import rreil.assembler.parser.ASTAssertionComparisonOperator;
import rreil.assembler.parser.ASTAssertionReachable;
import rreil.assembler.parser.ASTAssertionUnreachable;
import rreil.assembler.parser.ASTAssertionWarnings;
import rreil.assembler.parser.ASTBlock;
import rreil.assembler.parser.ASTBlockEnd;
import rreil.assembler.parser.ASTBlockStart;
import rreil.assembler.parser.ASTBr;
import rreil.assembler.parser.ASTBrc;
import rreil.assembler.parser.ASTBrci;
import rreil.assembler.parser.ASTCall;
import rreil.assembler.parser.ASTCallPrim;
import rreil.assembler.parser.ASTCmpeq;
import rreil.assembler.parser.ASTCmples;
import rreil.assembler.parser.ASTCmpleu;
import rreil.assembler.parser.ASTCmplts;
import rreil.assembler.parser.ASTCmpltu;
import rreil.assembler.parser.ASTCmpneq;
import rreil.assembler.parser.ASTConvert;
import rreil.assembler.parser.ASTDiv;
import rreil.assembler.parser.ASTDivs;
import rreil.assembler.parser.ASTFunArgs;
import rreil.assembler.parser.ASTHalt;
import rreil.assembler.parser.ASTIdent;
import rreil.assembler.parser.ASTIfElse;
import rreil.assembler.parser.ASTInsn;
import rreil.assembler.parser.ASTInteger;
import rreil.assembler.parser.ASTIntegerOrNull;
import rreil.assembler.parser.ASTInterval;
import rreil.assembler.parser.ASTIntervalBound;
import rreil.assembler.parser.ASTIntervalSet;
import rreil.assembler.parser.ASTJumpTargetAddress;
import rreil.assembler.parser.ASTLabel;
import rreil.assembler.parser.ASTLdm;
import rreil.assembler.parser.ASTMod;
import rreil.assembler.parser.ASTModule;
import rreil.assembler.parser.ASTMov;
import rreil.assembler.parser.ASTMul;
import rreil.assembler.parser.ASTNop;
import rreil.assembler.parser.ASTOr;
import rreil.assembler.parser.ASTPrimLabel;
import rreil.assembler.parser.ASTPtr;
import rreil.assembler.parser.ASTRReilAddress;
import rreil.assembler.parser.ASTReturn;
import rreil.assembler.parser.ASTRvalue;
import rreil.assembler.parser.ASTShl;
import rreil.assembler.parser.ASTShr;
import rreil.assembler.parser.ASTShrs;
import rreil.assembler.parser.ASTSignExtend;
import rreil.assembler.parser.ASTSize;
import rreil.assembler.parser.ASTStm;
import rreil.assembler.parser.ASTStmt;
import rreil.assembler.parser.ASTSub;
import rreil.assembler.parser.ASTTest;
import rreil.assembler.parser.ASTVariable;
import rreil.assembler.parser.ASTXor;
import rreil.assembler.parser.Node;
import rreil.assembler.parser.ParseException;
import rreil.assembler.parser.SimpleNode;
import rreil.assembler.parser.VarSize;
import rreil.lang.AssertionOp;
import rreil.lang.BinOp;
import rreil.lang.ComparisonOp;
import rreil.lang.Lhs;
import rreil.lang.LinBinOp;
import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.RReil.Branch.BranchTypeHint;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Address;
import rreil.lang.util.RReilFactory;

/**
 * Implements all the basic parsing stuff like module, statements and all the basic instructions.
 */
public abstract class AbstractParseTreeTranslator extends ParseTreeVisitorSkeleton {
  protected static final RReilFactory rreil = new RReilFactory();
  protected final SortedMap<RReilAddr, RReil> instructions = new TreeMap<RReilAddr, RReil>();
  protected final List<PointerInfo> pointers = new ArrayList<PointerInfo>();
  protected RReilAddr currentInstructionAddress = RReilAddr.ZERO;


  public static final int SIZE_NOT_SET = 0;
  public static final int SIZE_TEMPLATE_PARAM = -1;


  @Override public Object visit (ASTModule node, Object data) throws ParseException {
    return node.childrenAccept(this, data);
  }

  @Override public Object visit (ASTPtr node, Object data) throws ParseException {
    String id = identifier((ASTIdent) node.jjtGetChild(0));
    Integer size = size((ASTSize) node.jjtGetChild(1));
    Option<BigInt> address = integerOrNull((ASTIntegerOrNull) node.jjtGetChild(2));
    pointers.add(new PointerInfo(size, id, address));
    return null;
  }

  @Override public Object visit (ASTStmt node, Object data) throws ParseException {
    if (node.jjtGetNumChildren() == 2) {
      RReilAddr instructionAddress = (RReilAddr) node.jjtGetChild(0).jjtAccept(this, data);
      node.jjtGetChild(1).jjtAccept(this, instructionAddress);
    } else {
      node.childrenAccept(this, data);
    }
    return null;
  }

  @Override public Object visit (ASTInsn node, Object data) throws ParseException {
    if (data != null && data instanceof RReilAddr)
      currentInstructionAddress = (RReilAddr) data;
    RReil instruction = (RReil) node.jjtGetChild(0).jjtAccept(this, null);
    if (instruction == null)
      throw new IllegalStateException("No instruction was produced in translator for " + node);
    instructions.put(currentInstructionAddress, instruction);
    RReilAddr nextInstructionAddress = currentInstructionAddress.nextBase();
    if (instructions.containsKey(nextInstructionAddress)) {
      RReil oldInstruction = instructions.get(nextInstructionAddress);
      String message = "Address " + nextInstructionAddress.toStringWithHexPrefix()
        + " already assigned for instruction: " + oldInstruction;
      throw new IllegalStateException(message);
    }
    currentInstructionAddress = nextInstructionAddress;
    return null;
  }

  @Override public Object visit (ASTRReilAddress node, Object data) throws ParseException {
    String addressLiteral = (String) node.jjtGetValue();
    RReilAddr address = RReilAddr.valueOf(addressLiteral);
    return address;
  }

  @Override public Object visit (ASTLabel node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTAdd node, Object data) throws ParseException {
    return visitBinop(node, LinBinOp.Add);
  }

  @Override public Object visit (ASTSub node, Object data) throws ParseException {
    return visitBinop(node, LinBinOp.Sub);
  }

  @Override public Object visit (ASTMul node, Object data) throws ParseException {
    return visitBinop(node, BinOp.Mul);
  }

  @Override public Object visit (ASTDiv node, Object data) throws ParseException {
    return visitBinop(node, BinOp.Divu);
  }

  @Override public Object visit (ASTDivs node, Object data) throws ParseException {
    return visitBinop(node, BinOp.Divs);
  }

  @Override public Object visit (ASTMod node, Object data) throws ParseException {
    return visitBinop(node, BinOp.Mod);
  }

  @Override public Object visit (ASTShl node, Object data) throws ParseException {
    return visitBinop(node, BinOp.Shl);
  }

  @Override public Object visit (ASTShr node, Object data) throws ParseException {
    return visitBinop(node, BinOp.Shr);
  }

  @Override public Object visit (ASTShrs node, Object data) throws ParseException {
    return visitBinop(node, BinOp.Shrs);
  }

  @Override public Object visit (ASTXor node, Object data) throws ParseException {
    return visitBinop(node, BinOp.Xor);
  }

  @Override public Object visit (ASTOr node, Object data) throws ParseException {
    return visitBinop(node, BinOp.Or);
  }

  @Override public Object visit (ASTAnd node, Object data) throws ParseException {
    return visitBinop(node, BinOp.And);
  }

  private Object visitBinop (SimpleNode node, BinOp operator) throws ParseException {
    Node parent = node.jjtGetParent();
    Integer size = size((ASTSize) parent.jjtGetChild(1));
    Rhs.Rvar lhs = lhsOf((ASTVariable) parent.jjtGetChild(2), size);
    Rhs.Rval left = rhsRval((ASTRvalue) parent.jjtGetChild(3), size);
    Rhs.Rval right = rhsRval((ASTRvalue) parent.jjtGetChild(4), size);
    return rreil.assign(currentInstructionAddress, lhs.asLhs(), rreil.binary(left, operator, right));
  }
  
  private Object visitBinop (SimpleNode node, LinBinOp operator) throws ParseException {
    Node parent = node.jjtGetParent();
    Integer size = size((ASTSize) parent.jjtGetChild(1));
    Rhs.Rvar lhs = lhsOf((ASTVariable) parent.jjtGetChild(2), size);
    Rhs.Rval left = rhsRval((ASTRvalue) parent.jjtGetChild(3), size);
    Rhs.Rval right = rhsRval((ASTRvalue) parent.jjtGetChild(4), size);
    return rreil.assign(currentInstructionAddress, lhs.asLhs(), rreil.binary(left, operator, right));
  }

  @Override public Object visit (ASTCmpneq node, Object data) throws ParseException {
    return visitComparison(node, ComparisonOp.Cmpneq);
  }

  @Override public Object visit (ASTCmpeq node, Object data) throws ParseException {
    return visitComparison(node, ComparisonOp.Cmpeq);
  }

  @Override public Object visit (ASTCmples node, Object data) throws ParseException {
    return visitComparison(node, ComparisonOp.Cmples);
  }

  @Override public Object visit (ASTCmpleu node, Object data) throws ParseException {
    return visitComparison(node, ComparisonOp.Cmpleu);
  }

  @Override public Object visit (ASTCmplts node, Object data) throws ParseException {
    return visitComparison(node, ComparisonOp.Cmplts);
  }

  @Override public Object visit (ASTCmpltu node, Object data) throws ParseException {
    return visitComparison(node, ComparisonOp.Cmpltu);
  }

  private Object visitComparison (SimpleNode node, ComparisonOp operator) throws ParseException {
    Node parent = node.jjtGetParent();
    Integer size = size((ASTSize) parent.jjtGetChild(1));
    Rhs.Rvar lhs = lhsOf((ASTVariable) parent.jjtGetChild(2), 1);
    Rhs.Rval left = rhsRval((ASTRvalue) parent.jjtGetChild(3), size);
    Rhs.Rval right = rhsRval((ASTRvalue) parent.jjtGetChild(4), size);
    return rreil.assign(currentInstructionAddress, lhs.asLhs(), rreil.comparision(left, operator, right));
  }

  @Override public Object visit (ASTMov node, Object data) throws ParseException {
    ASTInsn parent = (ASTInsn) node.jjtGetParent();
    Integer size = size((ASTSize) parent.jjtGetChild(1));
    Rhs.Rvar lhs = lhsOf((ASTVariable) parent.jjtGetChild(2), size);
    Rhs rhs = rhsOf((ASTRvalue) parent.jjtGetChild(3), size);
    return rreil.assign(currentInstructionAddress, lhs.asLhs(), rhs);
  }

  @Override public Object visit (ASTAssert node, Object data) throws ParseException {
    return node.jjtGetChild(0).jjtAccept(this, data);
  }

  @Override public Object visit (ASTAssertionReachable node, Object data) throws ParseException {
    return rreil.assertionReachable(currentInstructionAddress);
  }

  @Override public Object visit (ASTAssertionUnreachable node, Object data) throws ParseException {
    return rreil.assertionUnreachable(currentInstructionAddress);
  }

  @Override public Object visit (ASTAssertionWarnings node, Object data) throws ParseException {
    BigInteger numberOfExprectedWarnings = (BigInteger) ((ASTInteger) node.jjtGetChild(0)).jjtGetValue();
    return rreil.assertionWarnings(currentInstructionAddress, numberOfExprectedWarnings.intValue());
  }

  @Override public Object visit (ASTAssertionComparison node, Object data) throws ParseException {
    Integer size = size((ASTSize) node.jjtGetChild(0));
    Rhs lhs = rhsOf((ASTRvalue) node.jjtGetChild(1), size);
    String operator = (String) ((SimpleNode) node.jjtGetChild(2)).jjtGetValue();
    Rhs rhs = rhsOf((ASTRvalue) node.jjtGetChild(3), size);
    return rreil.assertion(currentInstructionAddress, lhs, AssertionOp.from(operator), rhs, size);
  }

  @Override public Object visit (ASTAssertionComparisonOperator node, Object data) throws ParseException {
    throw new UnsupportedOperationException(); // is handled above
  }

  @Override public Object visit (ASTSignExtend node, Object data) throws ParseException {
    ASTInsn parent = (ASTInsn) node.jjtGetParent();
    Integer sizeOfLhs = size((ASTSize) parent.jjtGetChild(1));
    Integer sizeOfRhs = size((ASTSize) parent.jjtGetChild(2));
    Rhs.Rvar lhs = lhsOf((ASTVariable) parent.jjtGetChild(3), sizeOfLhs);
    Rhs.Rval rhs = rhsRval((ASTRvalue) parent.jjtGetChild(4), sizeOfRhs);
    return rreil.assign(currentInstructionAddress, lhs.asLhs(), rreil.castSx(rhs));
  }

  @Override public Object visit (ASTConvert node, Object data) throws ParseException {
    ASTInsn parent = (ASTInsn) node.jjtGetParent();
    Integer sizeOfLhs = size((ASTSize) parent.jjtGetChild(1));
    Integer sizeOfRhs = size((ASTSize) parent.jjtGetChild(2));
    Rhs.Rvar lhs = lhsOf((ASTVariable) parent.jjtGetChild(3), sizeOfLhs);
    Rhs.Rval rhs = rhsRval((ASTRvalue) parent.jjtGetChild(4), sizeOfRhs);
    return rreil.assign(currentInstructionAddress, lhs.asLhs(), rreil.castZx(rhs));
  }

  @Override public Object visit (ASTLdm node, Object data) throws ParseException {
    ASTInsn parent = (ASTInsn) node.jjtGetParent();
    Integer sizeOfValue = size((ASTSize) parent.jjtGetChild(1));
    Integer sizeOfAddress = size((ASTSize) parent.jjtGetChild(2));
    Rhs.Rvar lhs = lhsOf((ASTVariable) parent.jjtGetChild(3), sizeOfValue);
    Rhs.Rval address = rhsRval((ASTRvalue) parent.jjtGetChild(4), sizeOfAddress);
    return rreil.load(currentInstructionAddress, lhs.asLhs(), address);
  }

  @Override public Object visit (ASTStm node, Object data) throws ParseException {
    ASTInsn parent = (ASTInsn) node.jjtGetParent();
    Integer sizeOfAddress = size((ASTSize) parent.jjtGetChild(1));
    Integer sizeOfValue = size((ASTSize) parent.jjtGetChild(2));
    Rhs.Rval address = rhsRval((ASTRvalue) parent.jjtGetChild(3), sizeOfAddress);
    Rhs.Rval value = rhsRval((ASTRvalue) parent.jjtGetChild(4), sizeOfValue);
    return rreil.store(currentInstructionAddress, address, value);
  }

  @Override public Object visit (ASTBrci node, Object data) throws ParseException {
    ASTInsn parent = (ASTInsn) node.jjtGetParent();
    Integer size = size((ASTSize) parent.jjtGetChild(1));
    Rhs.Rval cond = rhsRval((ASTRvalue) parent.jjtGetChild(2), 1);
    Rhs.Address target = (Address) rhsRval((ASTRvalue) parent.jjtGetChild(3), size);
    return rreil.branch(currentInstructionAddress, cond, target);
  }

  @Override public Object visit (ASTBrc node, Object data) throws ParseException {
    ASTInsn parent = (ASTInsn) node.jjtGetParent();
    Integer size = size((ASTSize) parent.jjtGetChild(1));
    Rhs.Rval cond = rhsRval((ASTRvalue) parent.jjtGetChild(2), 1);
    Rhs.Rval target = rhsRval((ASTRvalue) parent.jjtGetChild(3), size);
    return rreil.branchNative(currentInstructionAddress, cond, target);
  }

  @Override public Object visit (ASTBr node, Object data) throws ParseException {
    ASTInsn parent = (ASTInsn) node.jjtGetParent();
    Integer size = size((ASTSize) parent.jjtGetChild(1));
    Rhs.Rval target = rhsRval((ASTRvalue) parent.jjtGetChild(2), size);
    return rreil.branchNative(currentInstructionAddress, rreil.literal(1, Bound.ONE), target);
  }

  @Override public Object visit (ASTCall node, Object data) throws ParseException {
    ASTInsn parent = (ASTInsn) node.jjtGetParent();
    Integer size = size((ASTSize) parent.jjtGetChild(1));
    Rhs.Rval target = rhsRval((ASTRvalue) parent.jjtGetChild(2), size);
    return rreil.branchNative(currentInstructionAddress, target, BranchTypeHint.Call);
  }

  @Override public Object visit (ASTReturn node, Object data) throws ParseException {
    ASTInsn parent = (ASTInsn) node.jjtGetParent();
    Integer size = size((ASTSize) parent.jjtGetChild(1));
    Rhs.Rval target = rhsRval((ASTRvalue) parent.jjtGetChild(2), size);
    return rreil.branchNative(currentInstructionAddress, target, BranchTypeHint.Return);
  }

  @Override public Object visit (ASTHalt node, Object data) throws ParseException {
    // Shortcut for: "prim () = halt()"
    return rreil.primOp(currentInstructionAddress, "halt", Collections.<Lhs>emptyList(),
        Collections.<Rhs.Rval>emptyList());
  }

  @Override public Object visit (ASTCallPrim node, Object data) throws ParseException {
    ASTFunArgs outArgs = null;
    ASTPrimLabel label = null;
    ASTFunArgs inArgs = null;

    // Handle different cases
    switch (node.jjtGetNumChildren()) {
    case 1:
      // "prim" PrimLabel()
      label = (ASTPrimLabel) node.jjtGetChild(0);
      break;

    case 2:
      // "prim" "(" Argument()+ ")" "=" PrimLabel() ODER "prim" PrimLabel() "(" Argument()+ ")"
      if (node.jjtGetChild(1) instanceof ASTPrimLabel) {
        outArgs = (ASTFunArgs) node.jjtGetChild(0);
        label = (ASTPrimLabel) node.jjtGetChild(1);
      } else {
        label = (ASTPrimLabel) node.jjtGetChild(0);
        inArgs = (ASTFunArgs) node.jjtGetChild(1);
      }
      break;

    case 3:
      // "prim" "(" Argument()+ ")" "=" PrimLabel() "(" Argument()+ ")"
      outArgs = (ASTFunArgs) node.jjtGetChild(0);
      label = (ASTPrimLabel) node.jjtGetChild(1);
      inArgs = (ASTFunArgs) node.jjtGetChild(2);
      break;

    default:
      throw new ParseException("Unable to parse ASTCallPrim: unexpected number of children!");
    }

    List<Lhs> outOpnds = outArgs != null ? argumentsLhs(outArgs) : Collections.<Lhs>emptyList();
    List<Rhs.Rval> inOpnds = inArgs != null ? arguments(inArgs) : Collections.<Rhs.Rval>emptyList();
    String labelStr = (String) label.jjtGetValue();
    return rreil.primOp(currentInstructionAddress, labelStr, outOpnds, inOpnds);
  }

  @Override public Object visit (ASTFunArgs node, Object data) throws ParseException {
    return null;
  }

  protected List<Lhs> argumentsLhs (ASTFunArgs args) throws ParseException {
    List<Lhs> result = new LinkedList<Lhs>();
    for (Rhs.Rval rhs : arguments(args)) {
      result.add(((Rhs.Rvar) rhs).asLhs());
    }
    return result;
  }

  protected List<Rhs.Rval> arguments (ASTFunArgs args) throws ParseException {
    List<Rhs.Rval> opnds = new ArrayList<Rhs.Rval>(args.jjtGetNumChildren());
    for (int i = 0; i < args.jjtGetNumChildren(); i += 2) {
      int size = size((ASTSize) args.jjtGetChild(i + 1));
      Rhs.Rvar opnd = variable((ASTVariable) args.jjtGetChild(i), size);
      opnds.add(opnd);
    }
    return opnds;
  }

  @Override public Object visit (ASTPrimLabel node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTNop node, Object data) throws ParseException {
    return rreil.nop(currentInstructionAddress);
  }

  @Override public Object visit (ASTRvalue node, Object data) throws ParseException {
    return node.childrenAccept(this, data);
  }

  @Override public Object visit (ASTVariable node, Object data) throws ParseException {
    return variable(node, (Integer) data);
  }

  @Override public Object visit (ASTInterval node, Object data) throws ParseException {
    Bound lo = (Bound) node.jjtGetChild(0).jjtAccept(this, true);
    Bound up = (Bound) node.jjtGetChild(1).jjtAccept(this, false);
    return rreil.range((Integer) data, Interval.of(lo, up));
  }

  @Override public Object visit (ASTIntervalBound node, Object data) throws ParseException {
    if (node.jjtGetNumChildren() > 0) {
      BigInt value = integer((ASTInteger) node.jjtGetChild(0));
      return value;
    }
    Boolean isLowerBound = (Boolean) data;
    return isLowerBound ? Bound.NEGINF : Bound.POSINF;
  }

  @Override public Object visit (ASTIntervalSet node, Object data) throws ParseException {
    throw new UnsupportedOperationException("No IntervalSets allowed, yet!");
    // XXX bm: the Range datatype does not understand IntervalSets yet so it cannot be used.
//    Token token = (Token) node.jjtGetValue();
//    try {
//      IntervalSet intervalSet = IntervalSet.valueOf(token.image);
//      return rreil.range((Integer) data, intervalSet);
//    } catch (Throwable err) {
//      throw new ParseException("Error while parsing IntervalSet:\n" + err.getMessage());
//    }
  }

  @Override public Object visit (ASTInteger node, Object data) throws ParseException {
    return literal(node, (Integer) data);
  }

  @Override public Object visit (ASTJumpTargetAddress node, Object data) throws ParseException {
    RReilAddr address = (RReilAddr) node.jjtGetChild(0).jjtAccept(this, null);
    return rreil.rreilAddress((Integer) data, address);
  }

  @Override public Object visit (ASTSize node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTIntegerOrNull node, Object data) throws ParseException {
    return literalOrArbitary(node, (Integer) data);
  }

  @Override public Object visit (ASTIdent node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (SimpleNode node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTIfElse node, Object data) throws ParseException {
    throw new UnsupportedOperationException();
  }

  @Override public Object visit (ASTTest node, Object data) throws ParseException {
    throw new UnsupportedOperationException();
  }

  @Override public Object visit (ASTBlock node, Object data) throws ParseException {
    throw new UnsupportedOperationException();
  }

  @Override public Object visit (ASTBlockStart node, Object data) throws ParseException {
    throw new UnsupportedOperationException();
  }

  @Override public Object visit (ASTBlockEnd node, Object data) throws ParseException {
    throw new UnsupportedOperationException();
  }


  protected Rhs test (ASTTest node) throws ParseException {
    if (node.jjtGetNumChildren() == 1) {
      // Number()
      return literal((ASTInteger) node.jjtGetChild(0), 0);      // TODO: check sizes!
    } else if (node.jjtGetNumChildren() == 2) {
      // Variable() Size()
      int size = size((ASTSize) node.jjtGetChild(1));
      return variable((ASTVariable) node.jjtGetChild(0), size);
    } else
      throw new ParseException("ASTTest node may only have 1 or 2 children!");
  }

  private static BigInt integer (ASTInteger node) {
    return BigInt.of((BigInteger) node.jjtGetValue());
  }

  private static Option<BigInt> integerOrNull (ASTIntegerOrNull node) {
    if (node.jjtGetNumChildren() < 1)
      return none();
    BigInt integer = integer((ASTInteger) node.jjtGetChild(0));
    return some(integer);
  }

  protected Integer size (ASTSize node) throws ParseException {
    VarSize size = (VarSize) node.jjtGetValue();
    if (!size.isSet() || size.isTemlateVar()) {
      throw new ParseException("Size not defined!");
    }
    return size.asInteger();
  }

  protected static String identifier (ASTIdent node) {
    return (String) node.jjtGetValue();
  }

  private Rhs.Rvar lhsOf (ASTVariable node, Integer size) throws ParseException {
    return variable(node, size);
  }

  private Rhs rhsOf (ASTRvalue node, Integer size) throws ParseException {
    return (Rhs) node.jjtGetChild(0).jjtAccept(this, size);
  }

  private Rhs.Rval rhsRval (ASTRvalue node, Integer size) throws ParseException {
    return (Rhs.Rval) node.jjtGetChild(0).jjtAccept(this, size);
  }

  protected Rhs.Rvar variable (ASTVariable node, Integer size) throws ParseException {
    String id = identifier((ASTIdent) node.jjtGetChild(0));
    return variableWithId(id, node, size);
  }

  protected Rhs.Rvar variableWithId (String id, ASTVariable node, Integer size) {
    int offset = 0;
    if (isVariableWithOffsetAnnotation(node))
      offset = integer((ASTInteger) node.jjtGetChild(1)).intValue();
    return rreil.variable(size, offset, MemVar.getVarOrFresh(id));
  }

  private static Rhs.Rval literal (ASTInteger node, Integer size) {
    return rreil.literal(size, integer(node));
  }

  private static Rhs literalOrArbitary (ASTIntegerOrNull node, Integer size) {
    final BigInteger value = (BigInteger) node.jjtGetValue();
    if (value == null)
      return rreil.range(size, Interval.unsignedTop(size));
    return rreil.literal(size, BigInt.of(value));
  }

  private static boolean isVariableWithOffsetAnnotation (ASTVariable node) {
    return node.jjtGetNumChildren() > 1;
  }
}
