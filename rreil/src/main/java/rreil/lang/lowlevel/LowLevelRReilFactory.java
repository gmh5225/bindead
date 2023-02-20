package rreil.lang.lowlevel;

import java.util.List;

import javalx.numeric.Interval;
import rreil.disassembler.Instruction;
import rreil.disassembler.OperandTree.Node;
import rreil.disassembler.OperandTree.NodeBuilder;
import rreil.disassembler.OperandTree.Type;
import rreil.lang.RReilAddr;

/**
 * This class provides some helper functions for working with RREIL instructions.
 */
public class LowLevelRReilFactory {
  private static LowLevelRReilFactory INSTANCE = new LowLevelRReilFactory();

  private LowLevelRReilFactory () {
  }

  /** The mnemonic of the RREIL instruction ADD. */
  public static final String $Add = "add";
  /** The mnemonic of the RREIL instruction AND. */
  public static final String $And = "and";
  /** The mnemonic of the RREIL instruction BRC (branch-conditional). */
  public static final String $Brc = "if";
  /** The mnemonic of the RREIL instruction CALL. */
  public static final String $Call = "call";
  /** The mnemonic of the RREIL return instruction RETURN */
  public static final String $Return = "return";
  /** The mnemonic of the RREIL instruction CMPEQ. */
  public static final String $Cmpeq = "cmpeq";
  /** The mnemonic of the RREIL instruction CMPNEQ. */
  public static final String $Cmpneq = "cmpneq";
  /** The mnemonic of the RREIL instruction CMPLES (signed-less-than). */
  public static final String $Cmples = "cmples";
  /** The mnemonic of the RREIL instruction CMPLEU (unsigned-less-than). */
  public static final String $Cmpleu = "cmpleu";
  /** The mnemonic of the RREIL instruction CMPLTS (signed-strict-less-than). */
  public static final String $Cmplts = "cmplts";
  /** The mnemonic of the RREIL instruction CMPLTU (unsigned-strict-less-than). */
  public static final String $Cmpltu = "cmpltu";
  /** The mnemonic of the RREIL instruction DIV. */
  public static final String $Divu = "divu";
  /** The mnemonic of the RREIL instruction DIVS. */
  public static final String $Divs = "divs";
  /** The mnemonic of the RREIL instruction LOAD (load-memory). */
  public static final String $Load = "load";
  /** The mnemonic of the RREIL instruction MOD. */
  public static final String $Mod = "mod";
  /** The mnemonic of the RREIL instruction MOV (move). */
  public static final String $Mov = "mov";
  /** The mnemonic of the RREIL instruction ASSERT. */
  public static final String $Assert = "assert";
  /** The mnemonic of the RREIL instruction SIGN-EXTEND. */
  public static final String $SignExtend = "sign-extend";
  /** The mnemonic of the RREIL instruction CONVERT. */
  public static final String $Convert = "convert";
  /** The mnemonic of the RREIL instruction MUL. */
  public static final String $Mul = "mul";
  /** The mnemonic of the RREIL instruction NOP. */
  public static final String $Nop = "nop";
  /** The mnemonic of the RREIL instruction OR. */
  public static final String $Or = "or";
  /** The mnemonic of the RREIL instruction SHL (shift-logically-left). */
  public static final String $Shl = "shl";
  /** The mnemonic of the RREIL instruction SHR (shift-logically-right). */
  public static final String $Shru = "shru";
  /** The mnemonic of the RREIL instruction SHRS (shift-right-signed). */
  public static final String $Shrs = "shrs";
  /** The mnemonic of the RREIL instruction STORE (store-memory). */
  public static final String $Store = "store";
  /** The mnemonic of the RREIL instruction SUB. */
  public static final String $Sub = "sub";
  /** The mnemonic of the RREIL instruction XOR. */
  public static final String $Xor = "xor";
  /** "Virtual" mnemonic for primitive operations */
  public static final String $PrimOp = "primop#";
  /** "Virtual" mnemonic for native instructions. This instruction get emitted if no translator for a native
   * instruction was found.
   */
  public static final String $Native = "native#";

  public static LowLevelRReilFactory getInstance () {
    return INSTANCE;
  }

  private static LowLevelRReil make (RReilAddr offset, String mnemonic, LowLevelRReilOpnd... operands) {
    return new LowLevelRReil(offset, mnemonic, operands);
  }

  private static LowLevelRReilOpnd makeOperand (OperandSize size, Type ty, Object value) {
    return makeOperand(size.getBits(), ty, value);
  }

  private static LowLevelRReilOpnd makeOperand (Number size, Type ty, Object value) {
    NodeBuilder root = new NodeBuilder().type(Type.Size).data(size);
    Node child = new NodeBuilder().type(ty).data(value).build();
    return new LowLevelRReilOpnd(root.link(child).build());
  }

  public LowLevelRReilOpnd variable (OperandSize size, String name) {
    return makeOperand(size, Type.Sym, name);
  }

  public LowLevelRReilOpnd variable (Number size, String name) {
    return makeOperand(size, Type.Sym, name);
  }

  public LowLevelRReilOpnd flag (String name) {
    return variable(1, name);
  }

  public LowLevelRReilOpnd immediate (Number size, Number value) {
    return makeOperand(size, Type.Immi, value);
  }

  public LowLevelRReilOpnd immediate (OperandSize size, Number value) {
    return makeOperand(size, Type.Immi, value);
  }

  public LowLevelRReilOpnd range (Number size, Interval range) {
    return makeOperand(size, Type.Immr, range);
  }

  public LowLevelRReilOpnd range (OperandSize size, Interval range) {
    return makeOperand(size, Type.Immr, range);
  }

  public LowLevelRReilOpnd immediateSizeFixup (int size, LowLevelRReilOpnd a) {
    if (a.child().getType() == Type.Immi)
      if (size != a.size())
        return a.withSize(size);
    return a;
  }

  private static void assertEqualSize (LowLevelRReilOpnd a, LowLevelRReilOpnd b) {
    if (a.size() != b.size())
      throw new IllegalArgumentException("Error: Incompatible operand sizes");
  }

  private static void assertEqualSize (LowLevelRReilOpnd lhs, LowLevelRReilOpnd a, LowLevelRReilOpnd b) {
    if (!(lhs.size() == a.size() && lhs.size() == b.size()))
      throw new IllegalArgumentException("Error: Incompatible operand sizes");
  }

  private static void assertEqualSize (LowLevelRReilOpnd a, OperandSize sz) {
    if (!(a.size() == sz.getBits()))
      throw new IllegalArgumentException("Error: Incompatible operand sizes");
  }
  public LowLevelRReil UNDEF (RReilAddr offset, LowLevelRReilOpnd dst) {
    return MOV(offset, dst, range(dst.size(), Interval.unsignedTop(dst.size())));
  }

  /* == RREIL instruction smart constructors == */
  public LowLevelRReil ADD (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(dst, src1, src2);
    return make(offset, $Add, dst, src1, src2);
  }

  public LowLevelRReil DIVU (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(dst, src1, src2);
    return make(offset, $Divu, dst, src1, src2);
  }

  public LowLevelRReil DIVS (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(dst, src1, src2);
    return make(offset, $Divs, dst, src1, src2);
  }

  public LowLevelRReil MUL (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(dst, src1, src2);
    return make(offset, $Mul, dst, src1, src2);
  }

  public LowLevelRReil SUB (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(dst, src1, src2);
    return make(offset, $Sub, dst, src1, src2);
  }

  public LowLevelRReil LOAD (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1) {
    return make(offset, $Load, dst, src1);
  }

  public LowLevelRReil STORE (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1) {
    return make(offset, $Store, dst, src1);
  }

  public LowLevelRReil CMPEQ (RReilAddr offset, LowLevelRReilOpnd flag, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(src1, src2);
    assertEqualSize(flag, OperandSize.BIT);
    return make(offset, $Cmpeq, flag, src1, src2);
  }

  public LowLevelRReil CMPNEQ (RReilAddr offset, LowLevelRReilOpnd flag, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(src1, src2);
    assertEqualSize(flag, OperandSize.BIT);
    return make(offset, $Cmpneq, flag, src1, src2);
  }

  public LowLevelRReil CMPLES (RReilAddr offset, LowLevelRReilOpnd flag, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(src1, src2);
    assertEqualSize(flag, OperandSize.BIT);
    return make(offset, $Cmples, flag, src1, src2);
  }

  public LowLevelRReil CMPLEU (RReilAddr offset, LowLevelRReilOpnd flag, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(src1, src2);
    assertEqualSize(flag, OperandSize.BIT);
    return make(offset, $Cmpleu, flag, src1, src2);
  }

  public LowLevelRReil CMPLTS (RReilAddr offset, LowLevelRReilOpnd flag, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(src1, src2);
    assertEqualSize(flag, OperandSize.BIT);
    return make(offset, $Cmplts, flag, src1, src2);
  }

  public LowLevelRReil CMPLTU (RReilAddr offset, LowLevelRReilOpnd flag, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(src1, src2);
    assertEqualSize(flag, OperandSize.BIT);
    return make(offset, $Cmpltu, flag, src1, src2);
  }

  public LowLevelRReil AND (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(dst, src1, src2);
    return make(offset, $And, dst, src1, src2);
  }

  public LowLevelRReil XOR (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(dst, src1, src2);
    return make(offset, $Xor, dst, src1, src2);
  }

  public LowLevelRReil OR (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(dst, src1, src2);
    return make(offset, $Or, dst, src1, src2);
  }

  public LowLevelRReil SHRU (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(dst, src1, src2);
    return make(offset, $Shru, dst, src1, src2);
  }

  public LowLevelRReil SHRS (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(dst, src1, src2);
    return make(offset, $Shrs, dst, src1, src2);
  }

  public LowLevelRReil SHL (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(dst, src1, src2);
    return make(offset, $Shl, dst, src1, src2);
  }

  public LowLevelRReil MOD (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2) {
    assertEqualSize(dst, src1, src2);
    return make(offset, $Mod, dst, src1, src2);
  }

  public LowLevelRReil MOV (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src) {
    assertEqualSize(dst, src);
    return make(offset, $Mov, dst, src);
  }

  public LowLevelRReil ASSERT (RReilAddr offset, LowLevelRReilOpnd lhs, LowLevelRReilOpnd rhs) {
    assertEqualSize(lhs, rhs);
    return make(offset, $Assert, lhs, rhs);
  }

  public LowLevelRReil SIGNEXTEND (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src) {
    // TODO: Maybe allow only upcasts
    return make(offset, $SignExtend, dst, src);
  }

  public LowLevelRReil CONVERT (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src) {
    return make(offset, $Convert, dst, src);
  }

  public LowLevelRReil NOP (RReilAddr offset) {
    return make(offset, $Nop);
  }

  public LowLevelRReil IFGOTO (RReilAddr offset, LowLevelRReilOpnd cond, LowLevelRReilOpnd dst) {
    assertEqualSize(cond, OperandSize.BIT);
    return make(offset, $Brc, cond, dst);
  }

  public LowLevelRReil CALL (RReilAddr offset, LowLevelRReilOpnd dst) {
    return make(offset, $Call, immediate(OperandSize.BIT, 1), dst);
  }

  public LowLevelRReil RETURN (RReilAddr offset, LowLevelRReilOpnd dst) {
    return make(offset, $Return, immediate(OperandSize.BIT, 1), dst);
  }

  public LowLevelRReil PRIMOP (RReilAddr offset, String primop, List<LowLevelRReilOpnd> outArgs, List<LowLevelRReilOpnd> inArgs) {
    return new PrimitiveInstruction(offset, $PrimOp + primop, inArgs, outArgs);
  }

  public LowLevelRReil NATIVE (RReilAddr offset, Instruction nativeInsn) {
    LowLevelRReilOpnd[] rreilOperands = new LowLevelRReilOpnd[nativeInsn.operands().size()];
    for (int i = 0; i < nativeInsn.operands().size(); i++) {
      rreilOperands[i] = new LowLevelRReilOpnd(nativeInsn.operands().get(i).getRoot());
    }
    return make(offset, $Native + nativeInsn.mnemonic(), rreilOperands);
  }

  /* == Derived instructions (short-cuts) == */
  public LowLevelRReil NOT (RReilAddr offset, LowLevelRReilOpnd dst, LowLevelRReilOpnd src) {
    assertEqualSize(dst, src);
    return XOR(offset, dst, src, TranslationHelpers.getMaxImmediate(src.size()));
  }

  public LowLevelRReil GOTONATIVE (RReilAddr offset, LowLevelRReilOpnd dst) {
    return IFGOTO(offset, immediate(OperandSize.BIT, 1), dst);
  }

  public LowLevelRReil GOTORREIL (RReilAddr address, long base, long offset) {
    return IFGOTORREIL(address, immediate(1, 1), base, offset);
  }

  public LowLevelRReil IFGOTORREIL (RReilAddr address, LowLevelRReilOpnd cond, long base, long offset) {
    int size = 32;
    NodeBuilder subAddressBuilder = new NodeBuilder().type(Type.Op).data(LowLevelRReilOpnd.$SubaddressOperatorSymbol);
    Node rreilBase = new NodeBuilder().type(Type.Immi).data(base).build();
    Node rreilOffset = new NodeBuilder().type(Type.Immi).data(offset).build();
    NodeBuilder rootBuilder = new NodeBuilder().type(Type.Size).data(size);
    Node subAddress = subAddressBuilder.link(rreilBase).link(rreilOffset).build();
    return IFGOTO(address, cond, new LowLevelRReilOpnd(rootBuilder.link(subAddress).build()));
  }

}
