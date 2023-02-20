package rreil.lang.util;

import static rreil.lang.lowlevel.LowLevelRReilFactory.$Add;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$And;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Brc;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Call;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Cmpeq;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Cmples;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Cmpleu;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Cmplts;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Cmpltu;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Cmpneq;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Convert;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Divs;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Divu;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Load;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Mod;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Mov;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Mul;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Nop;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Or;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Return;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Shl;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Shrs;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Shru;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$SignExtend;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Store;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Sub;
import static rreil.lang.lowlevel.LowLevelRReilFactory.$Xor;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javalx.numeric.BigInt;
import javalx.numeric.Interval;
import rreil.disassembler.OperandTree.Node;
import rreil.disassembler.OperandTree.Type;
import rreil.lang.BinOp;
import rreil.lang.ComparisonOp;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.RReil.Branch.BranchTypeHint;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Address;
import rreil.lang.Rhs.Rlit;
import rreil.lang.Rhs.Rval;
import rreil.lang.Rhs.Rvar;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.PrimitiveInstruction;

/** Translates the RREIL instruction tree to the RREIL concrete syntax. */
public class LowLevelToRReilTranslator {
  static final RReilFactory rreil = new RReilFactory();
  private static final Map<String, Cons> $ = new HashMap<String, Cons>();

  static {
    $.put($Add, Cons.binary(BinOp.Add));
    $.put($And, Cons.binary(BinOp.And));
    $.put($Divu, Cons.binary(BinOp.Divu));
    $.put($Divs, Cons.binary(BinOp.Divs));
    $.put($Mod, Cons.binary(BinOp.Mod));
    $.put($Mul, Cons.binary(BinOp.Mul));
    $.put($Or, Cons.binary(BinOp.Or));
    $.put($Shl, Cons.binary(BinOp.Shl));
    $.put($Shru, Cons.binary(BinOp.Shr));
    $.put($Shrs, Cons.binary(BinOp.Shrs));
    $.put($Sub, Cons.binary(BinOp.Sub));
    $.put($Xor, Cons.binary(BinOp.Xor));
    $.put($Cmpeq, Cons.comparison(ComparisonOp.Cmpeq));
    $.put($Cmpneq, Cons.comparison(ComparisonOp.Cmpneq));
    $.put($Cmples, Cons.comparison(ComparisonOp.Cmples));
    $.put($Cmpleu, Cons.comparison(ComparisonOp.Cmpleu));
    $.put($Cmplts, Cons.comparison(ComparisonOp.Cmplts));
    $.put($Cmpltu, Cons.comparison(ComparisonOp.Cmpltu));
    $.put($Mov, Cons.mov());
    $.put($SignExtend, Cons.movsx());
    $.put($Convert, Cons.movzx());
    $.put($Nop, Cons.nop());
    $.put($Call, Cons.branch(BranchTypeHint.Call));
    $.put($Return, Cons.branch(BranchTypeHint.Return));
    $.put($Brc, Cons.branch());
    $.put($Load, Cons.load());
    $.put($Store, Cons.store());
  }

  public static RReil translate (LowLevelRReil insn) {
    final Cons cons = $.get(insn.mnemonic());
    if (cons != null)
      return cons.translate(insn);
    if (insn.mnemonic().startsWith(LowLevelRReilFactory.$PrimOp)) {
      PrimitiveInstruction prim = (PrimitiveInstruction) insn;
      String name = insn.mnemonic().substring(LowLevelRReilFactory.$PrimOp.length());
      List<Lhs> outArgs = new LinkedList<Lhs>();
      for (LowLevelRReilOpnd op : prim.getOutArgs())
        outArgs.add(translateRvar(op).asLhs());
      List<Rhs.Rval> inArgs = new LinkedList<Rhs.Rval>();
      for (LowLevelRReilOpnd op : prim.getInArgs())
        inArgs.add(translateRval(op));
      return rreil.primOp(insn.address(), name, outArgs, inArgs);
    } else if (insn.mnemonic().startsWith(LowLevelRReilFactory.$Native)) {
      String name = insn.mnemonic().substring(LowLevelRReilFactory.$Native.length());
      // pass the opcode as a constant operand to the RReil instruction
      // FIXME: this is not endianness aware so the opcode might be translated to a wrong number
      Rlit opnd = new Rhs.Rlit(1, BigInt.of(new BigInteger(insn.opcode())));
      return rreil.nativeInsn(insn.address(), name, opnd);
    }
    throw new IllegalArgumentException("Illegal RREIL instruction: " + insn);
  }

  private static Rhs.Rval translateAddr (LowLevelRReilOpnd LowLevelRReilOpnd) {
    final int size = LowLevelRReilOpnd.size();
    final Node opnd = LowLevelRReilOpnd.child();
    switch (opnd.getType()) {
    case Immi:
      return rreil.literal(size, BigInt.of(((Number) opnd.getData()).longValue()));
    case Sym:
      return rreil.variable(size, 0, MemVar.getVarOrFresh((String) opnd.getData()));
    case Op: {
      return translateSymbolWithOffset(opnd, size);
    }
    default:
      throw new IllegalArgumentException("Invalid RREIL operand type: " + opnd.getType());
    }
  }

  private static Rhs.Address translateRaddr (LowLevelRReilOpnd LowLevelRReilOpnd) {
    final int size = LowLevelRReilOpnd.size();
    final Node opnd = LowLevelRReilOpnd.child();
    switch (opnd.getType()) {
    case Op: {
      if (!LowLevelRReilOpnd.$SubaddressOperatorSymbol.equals(opnd.getData()))
        throw new IllegalArgumentException("Invalid RREIL operand: " + (String) opnd.getData());
      final Number base = (Number) opnd.child(0).getData();
      final Number offs = (Number) opnd.child(1).getData();
      return rreil.rreilAddress(size, RReilAddr.valueOf(base.longValue(), offs.intValue()));
    }
    case Mem:
    case Immf:
    case Size:
    default:
      throw new IllegalArgumentException("Invalid RREIL operand type: " + opnd.getType());
    }
  }

  private static boolean isIntraRReilAddress (final LowLevelRReilOpnd LowLevelRReilOpnd) {
    final Node opnd = LowLevelRReilOpnd.child();
    if (opnd.getType() == Type.Op)
      return LowLevelRReilOpnd.$SubaddressOperatorSymbol.equals(opnd.getData());
    return false;
  }

  private static Rhs.Rvar translateSymbolWithOffset (final Node opnd, int size) {
    final String op = (String) opnd.getData();
    if (LowLevelRReilOpnd.$OffsetOperatorSymbol.equals(op)) {
      final Node baseNode = opnd.child(0);
      final Node offsNode = opnd.child(1);
      final MemVar x = MemVar.getVarOrFresh((String) baseNode.getData());
      final int offs = ((Number) offsNode.getData()).intValue();
      return rreil.variable(size, offs, x);
    }
    throw new IllegalArgumentException("Invalid RREIL operand: " + op);
  }

  public static Rhs.Rvar translateRvar (LowLevelRReilOpnd LowLevelRReilOpnd) {
    return (Rvar) translateRval(LowLevelRReilOpnd);
  }

  public static Rhs.Rval translateRval (LowLevelRReilOpnd LowLevelRReilOpnd) {
    final int size = LowLevelRReilOpnd.size();
    final Node opnd = LowLevelRReilOpnd.child();
    switch (opnd.getType()) {
    case Immi:
      return rreil.literal(size, BigInt.of(((Number) opnd.getData()).longValue()));
    case Sym:
      return rreil.variable(size, 0, MemVar.getVarOrFresh((String) opnd.getData()));
    case Op:
      return translateSymbolWithOffset(opnd, size);
    case Mem:
    case Immf:
    case Size:
    default:
      throw new IllegalArgumentException("Invalid RREIL operand type: " + opnd.getType());
    }
  }

  public static Rhs translateRhs (LowLevelRReilOpnd LowLevelRReilOpnd) {
    final int size = LowLevelRReilOpnd.size();
    final Node opnd = LowLevelRReilOpnd.child();
    switch (opnd.getType()) {
    case Immi:
      return rreil.literal(size, BigInt.of(((Number) opnd.getData()).longValue()));
    case Immr:
      return rreil.range(size, (Interval) opnd.getData());
    case Sym:
      return rreil.variable(size, 0, MemVar.getVarOrFresh((String) opnd.getData()));
    case Op:
      return translateSymbolWithOffset(opnd, size);
    case Mem:
    case Immf:
    case Size:
    default:
      throw new IllegalArgumentException("Invalid RREIL operand type: " + opnd.getType());
    }
  }

  private static abstract class Cons {
    public Cons () {
      // TODO Auto-generated constructor stub
    }

    abstract RReil translate (LowLevelRReil insn);

    static Cons binary (final BinOp op) {
      return new Cons() {
        @Override RReil translate (LowLevelRReil insn) {
          final Rhs.Rval left = translateRval(insn.rreilOperand(1));
          final Rhs.Rval right = translateRval(insn.rreilOperand(2));
          final Rhs.Rvar lhs = translateRvar(insn.rreilOperand(0));
          return rreil.assign(insn.address(), lhs.asLhs(), rreil.binary(left, op, right));
        }
      };
    }

    static Cons comparison (final ComparisonOp op) {
      return new Cons() {
        @Override RReil translate (LowLevelRReil insn) {
          final Rhs.Rval left = translateRval(insn.rreilOperand(1));
          final Rhs.Rval right = translateRval(insn.rreilOperand(2));
          final Rhs.Rvar lhs = translateRvar(insn.rreilOperand(0));
          return rreil.assign(insn.address(), lhs.asLhs(), rreil.comparision(left, op, right));
        }
      };
    }

    static Cons load () {
      return new Cons() {
        @Override RReil translate (LowLevelRReil insn) {
          final Rhs.Rval source = translateRval(insn.rreilOperand(1));
          final Rhs.Rval target = translateRval(insn.rreilOperand(0));
          return rreil.load(insn.address(), ((Rvar) target).asLhs(), source);
        }
      };
    }

    static Cons store () {
      return new Cons() {
        @Override RReil translate (LowLevelRReil insn) {
          final Rhs.Rval source = translateRval(insn.rreilOperand(1));
          final Rhs.Rval target = translateRval(insn.rreilOperand(0));
          return rreil.store(insn.address(), target, source);
        }
      };
    }

    static Cons mov () {
      return new Cons() {
        @Override RReil translate (LowLevelRReil insn) {
          final Rval lhs = translateRval(insn.rreilOperand(0));
          final Rhs rhs = translateRhs(insn.rreilOperand(1));
          return rreil.assign(insn.address(), ((Rvar) lhs).asLhs(), rhs);
        }
      };
    }

    static Cons movsx () {
      return new Cons() {
        @Override RReil translate (LowLevelRReil insn) {
          final Rval lhs = translateRval(insn.rreilOperand(0));
          final Rval rhs = translateRval(insn.rreilOperand(1));
          return rreil.assign(insn.address(), ((Rvar) lhs).asLhs(), rreil.castSx(rhs));
        }
      };
    }

    static Cons movzx () {
      return new Cons() {
        @Override RReil translate (LowLevelRReil insn) {
          final Rval lhs = translateRval(insn.rreilOperand(0));
          final Rval rhs = translateRval(insn.rreilOperand(1));
          return rreil.assign(insn.address(), ((Rvar) lhs).asLhs(), rreil.castZx(rhs));
        }
      };
    }

    static Cons branch (final BranchTypeHint hint) {
      return new Cons() {
        @Override RReil translate (LowLevelRReil insn) {
          final Rval target = translateRval(insn.rreilOperand(1));
          return rreil.branchNative(insn.address(), target, hint);
        }
      };
    }

   static Cons branch () {
      return new Cons() {
        @Override RReil translate (LowLevelRReil insn) {
          if (isIntraRReilAddress(insn.rreilOperand(1))) {
            final Rval cond = translateRval(insn.rreilOperand(0));
            final Address addr = translateRaddr(insn.rreilOperand(1));
            return rreil.branch(insn.address(), cond, addr);
          } else {
            final Rval cond = translateRval(insn.rreilOperand(0));
            final Rval addr = translateAddr(insn.rreilOperand(1));
            if (cond instanceof Rlit) {
              Rlit lit = (Rlit) cond;
              if (lit.getValue().isZero()) {
                return rreil.nop(insn.address());
              } else {
                return rreil.branchNative(insn.address(), addr, BranchTypeHint.Jump);
              }
            }
            return rreil.branchNative(insn.address(), cond, addr);
          }
        }
      };
    }

    static Cons nop () {
      return new Cons() {
        @Override RReil translate (LowLevelRReil insn) {
          return rreil.nop(insn.address());
        }
      };
    }
  }
}
