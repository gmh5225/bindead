package bindead.analyses;

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
import rreil.lang.util.RReilVisitor;
import bindead.domainnetwork.interfaces.RootDomain;

public class RReilRunner {
  /**
   * Convenience method to manipulate a domain state by using a RREIL
   * instruction given in RREIL assembler syntax. Note though that only
   * instructions without control flow will be executed, i.e. assignments,
   * loads, stores and primOps.
   *
   * @param state
   *          The state to be operated on
   * @param instructions
   *          A list of non-branching RREIL assembler instructions to be
   *          executed on the state
   * @return The resulting state after executing the instructions
   */
  public static <D extends RootDomain<D>> D eval (D state,
      String... instructions) {
    RReilVisitor<D, D> dispatcher = new Evaluator<D, D>() {

      @Override public D visit (Assign stmt, D state) {
        return state.eval(stmt);
      }

      @Override public D visit (Load stmt, D state) {
        return state.eval(stmt);
      }

      @Override public D visit (Store stmt, D state) {
        return state.eval(stmt);
      }

      @Override public D visit (PrimOp stmt, D state) {
        return state.eval(stmt);
      }

      @Override public D visit (Nop stmt, D state) {
        return state;
      }

    };
    D resultState = state;
    for (String instruction : instructions) {
      RReil rreilInstruction = RReil.from(instruction);
      resultState = rreilInstruction.accept(dispatcher, resultState);
    }
    return resultState;
  }

  private static class Evaluator<R, T> implements RReilVisitor<R, T> {

    public Evaluator () {
    }

    protected R defaultVisit (RReil insn, T data) {
      throw new UnsupportedOperationException(
        "Instruction not applicable to this eval on the state " + insn);
    }

    @Override public R visit (Assign stmt, T data) {
      return defaultVisit(stmt, data);
    }

    @Override public R visit (Load stmt, T data) {
      return defaultVisit(stmt, data);
    }

    @Override public R visit (Store stmt, T data) {
      return defaultVisit(stmt, data);
    }

    @Override public R visit (BranchToNative stmt, T data) {
      return defaultVisit(stmt, data);
    }

    @Override public R visit (BranchToRReil stmt, T data) {
      return defaultVisit(stmt, data);
    }

    @Override public R visit (Nop stmt, T data) {
      return defaultVisit(stmt, data);
    }

    @Override public R visit (Assertion stmt, T data) {
      return defaultVisit(stmt, data);
    }

    @Override public R visit (Branch stmt, T data) {
      return defaultVisit(stmt, data);
    }

    @Override public R visit (PrimOp stmt, T data) {
      return defaultVisit(stmt, data);
    }

    @Override public R visit (Native stmt, T data) {
      return defaultVisit(stmt, data);
    }

    @Override public R visit (Throw stmt, T data) {
      return defaultVisit(stmt, data);
    }

    @Override public R visit (Flop stmt, T data) {
      return defaultVisit(stmt, data);
    }

  }
}
