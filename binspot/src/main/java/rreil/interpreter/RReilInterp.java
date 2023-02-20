package rreil.interpreter;

import org.apache.commons.lang.NotImplementedException;

import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import rreil.lang.Rhs.LinRval;
import rreil.lang.SignednessHint;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.RReil.Assertion;
import rreil.lang.RReil.Assign;
import rreil.lang.RReil.Branch;
import rreil.lang.RReil.BranchToNative;
import rreil.lang.RReil.BranchToRReil;
import rreil.lang.RReil.Flop;
import rreil.lang.RReil.Load;
import rreil.lang.RReil.Native;
import rreil.lang.RReil.Nop;
import rreil.lang.RReil.PrimOp;
import rreil.lang.RReil.Store;
import rreil.lang.RReil.Throw;
import rreil.lang.Rhs.Address;
import rreil.lang.Rhs.Bin;
import rreil.lang.Rhs.Cmp;
import rreil.lang.Rhs.Convert;
import rreil.lang.Rhs.LinBin;
import rreil.lang.Rhs.LinScale;
import rreil.lang.Rhs.RangeRhs;
import rreil.lang.Rhs.Rlit;
import rreil.lang.Rhs.Rvar;
import rreil.lang.Rhs.SignExtend;
import rreil.lang.util.RReilVisitor;
import rreil.lang.util.RhsFactory;
import rreil.lang.util.RhsVisitor;

/**
 *
 * @author mb0
 */
public final class RReilInterp implements RReilVisitor<Void, InterpCtx> {
  private static final Eval eval = new Eval(); // Be aware that {Eval} mutates its state!

  public RReilInterp (final Rvar pc) {}

  public void run (RReil insn, final InterpCtx ctx) {
    insn.accept(this, ctx);
  }

  @Override public Void visit (Assign stmt, InterpCtx ctx) {
    eval.signed = SignednessHint.DontCare;
    Lhs lhs = stmt.getLhs();
    BigInt value = stmt.getRhs().accept(eval, ctx);
    ctx.getRegisters().set(lhs, value);
    return null;
  }

  @Override public Void visit (Load stmt, InterpCtx ctx) {
    BigInt address = stmt.getReadAddress().accept(eval, ctx);
    BigInt value = ctx.getMemory().load(stmt.lhsSize(), address.longValue());
    ctx.getRegisters().set(stmt.getLhs(), value);
    return null;
  }

  @Override public Void visit (Store stmt, InterpCtx ctx) {
    final BigInt address = stmt.getWriteAddress().accept(eval, ctx);
    // ctx.store(stmt.sizeOfValue(), address.longValue(),
    // stmt.getLhsVariable().accept(eval, ctx));
    ctx.store(stmt.rhsSize(), address.longValue(), stmt.getRhs().accept(eval, ctx));

    // throw new UnsupportedOperationException("Not supported yet.");
    return null;
  }

  @Override public Void visit (Branch stmt, InterpCtx ctx) {
    // throw new UnsupportedOperationException("Not supported yet.");
    Rvar programPc = RhsFactory.getInstance().variable(8, 0, MemVar.getVarOrFresh("$pc"));
    ctx.getRegisters().set(programPc, stmt.getTarget().accept(eval, ctx).mul(2));
    return null;
  }

  @Override public Void visit (BranchToNative stmt, InterpCtx ctx) {
    if (stmt.getCond().accept(eval, ctx).isOne()) {
      Rvar programPc = RhsFactory.getInstance().variable(8, 0, MemVar.getVarOrFresh("$pc"));
      ctx.getRegisters().set(programPc, stmt.getTarget().accept(eval, ctx));
    }
    return null;
  }

  @Override public Void visit (BranchToRReil stmt, InterpCtx data) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override public Void visit (PrimOp stmt, InterpCtx data) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override public Void visit (Native stmt, InterpCtx data) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override public Void visit (Nop stmt, InterpCtx ctx) {
    return null;
  }

  @Override public Void visit (Assertion stmt, InterpCtx ctx) {
    return null;
  }

  private static final class Eval implements RhsVisitor<BigInt, InterpCtx> {
    private SignednessHint signed;

    @Override public BigInt visit (Bin expr, InterpCtx ctx) {
      signed = expr.getOp().signedness();
      final BigInt left = expr.getLeft().accept(this, ctx);
      final BigInt right = expr.getRight().accept(this, ctx);
      switch (expr.getOp()) {
        case And:
          return left.and(right);
        case Divu:
          return left.div(right);
        case Divs:
          return left.div(right);
        case Mod:
          return left.mod(right);
        case Or:
          return left.or(right);
        case Mul:
          return left.mul(right);
        case Shl:
          return left.shl(right);
        case Shr:
        case Shrs:
          return left.shr(right);
        case Xor:
          return left.xor(right);
        default:
          assert false : "Unhandled binary operation";
          return null;
      }
    }
    
    @Override public BigInt visit (LinBin expr, InterpCtx ctx) {
      signed = expr.getOp().signedness();
      final BigInt left = expr.getLeft().accept(this, ctx);
      final BigInt right = expr.getRight().accept(this, ctx);
      switch (expr.getOp()) {
        case Add:
          return left.add(right);
        case Sub:
          return left.sub(right);
        default:
          assert false : "Unhandled binary operation";
          return null;
      }
    }
    
    @Override public BigInt visit (LinRval expr, InterpCtx ctx) {
      return expr.getRval().accept(this, ctx);
    }

    @Override public BigInt visit (LinScale expr, InterpCtx ctx) {
      signed = SignednessHint.DontCare;
      final BigInt opnd = expr.getOpnd().accept(this, ctx);
      return opnd.mul(expr.getConst());
    }

    @Override public BigInt visit (Cmp expr, InterpCtx ctx) {
      signed = expr.getOp().signedness();
      BigInt left = expr.getLeft().accept(this, ctx);
      BigInt right = expr.getRight().accept(this, ctx);
      int cmp = left.compareTo(right);
      switch (expr.getOp()) {
        case Cmpeq:
          return cmp == 0 ? Bound.ONE : Bound.ZERO;
        case Cmpneq:
          return cmp != 0 ? Bound.ONE : Bound.ZERO;
        case Cmples:
        case Cmpleu:
          return cmp <= 0 ? Bound.ONE : Bound.ZERO;
        case Cmplts:
        case Cmpltu:
          return cmp < 0 ? Bound.ONE : Bound.ZERO;
        default:
          assert false : "Unhandled comparision operation";
          return null;
      }
    }

    @Override public BigInt visit (SignExtend expr, InterpCtx ctx) {
      signed = SignednessHint.ForceSigned;
      return expr.getRhs().accept(this, ctx);
    }

    @Override public BigInt visit (Convert expr, InterpCtx ctx) {
      signed = SignednessHint.ForceUnsigned;
      return expr.getRhs().accept(this, ctx);
    }

    @Override public BigInt visit (Rvar variable, InterpCtx ctx) {
      return signed == SignednessHint.ForceSigned
          ? ctx.getRegisters().getSigned(variable) : ctx.getRegisters().get(variable);
    }

    @Override public BigInt visit (Rlit literal, InterpCtx ctx) {
      if (signed == null)
        return ctx.getRegisters().get(literal);
      switch (signed) {
        case ForceSigned:
          return ctx.getRegisters().getSigned(literal);
        case ForceUnsigned:
          return ctx.getRegisters().getUnsigned(literal);
        default:
          return ctx.getRegisters().get(literal);
      }
    }

    @Override public BigInt visit (RangeRhs range, InterpCtx ctx) {
      throw new RReilMachineException("Range values not allowed");
    }

    @Override public BigInt visit (Address expr, InterpCtx data) {
      throw new UnsupportedOperationException("Not supported yet.");
    }
  }

  @Override
  public Void visit(Throw stmt, InterpCtx data) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Void visit(Flop stmt, InterpCtx data) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
