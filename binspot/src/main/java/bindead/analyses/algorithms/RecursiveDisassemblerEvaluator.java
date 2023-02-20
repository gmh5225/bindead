package bindead.analyses.algorithms;

import javalx.data.products.P2;
import javalx.numeric.BigInt;
import rreil.RReilGrammarException;
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
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.Address;
import rreil.lang.Rhs.LinRval;
import rreil.lang.Rhs.Rlit;
import rreil.lang.Rhs.Rval;
import rreil.lang.Rhs.SimpleExpression;
import rreil.lang.util.RReilVisitor;
import bindead.analyses.algorithms.data.Flows;
import bindead.analyses.warnings.UnresolvedJumpTarget;
import bindead.analyses.warnings.WarningsMap;
import bindead.domainnetwork.channels.WarningMessage;
import bindead.domainnetwork.channels.WarningsContainer;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.exceptions.Unreachable;

/**
 * A simple evaluator for instructions. It is used to dispatch on the different instruction types
 * and return the targets for statically known branches.
 *
 * @param <D> The type of the abstract domain used in the analysis. This is unused here as we do not perform any
 *          analysis but it has to be passed through to some components that are reused here.
 *
 * @author Bogdan Mihaila
 */
public class RecursiveDisassemblerEvaluator<D extends RootDomain<D>> implements
    RReilVisitor<Flows<D>, P2<ProgramPoint, RReilAddr>> {
  private WarningsMap warnings;

  public RecursiveDisassemblerEvaluator (WarningsMap warnings) {
    this.warnings = warnings;
  }

  @Override public Flows<D> visit (Assign stmt, P2<ProgramPoint, RReilAddr> ctx) {
    return fallThroughSuccessor(ctx);
  }

  @Override public Flows<D> visit (Load stmt, P2<ProgramPoint, RReilAddr> ctx) {
    return fallThroughSuccessor(ctx);
  }

  @Override public Flows<D> visit (Store stmt, P2<ProgramPoint, RReilAddr> ctx) {
    return fallThroughSuccessor(ctx);
  }

  @Override public Flows<D> visit (Nop stmt, P2<ProgramPoint, RReilAddr> ctx) {
    return fallThroughSuccessor(ctx);
  }

  @Override public Flows<D> visit (BranchToRReil stmt, P2<ProgramPoint, RReilAddr> ctx) {
    // it does not matter so much for RReil code if we can evaluate the branch condition
    // statically, as we care more about the native control flow
    // So we can just take both paths here. They are anyways both known statically.
//    Flows<D> flow = new Flows<D>();
//    flow.addNext(ctx._2(), null);
//    Address target = stmt.getTarget();
//    RReilAddr rreilTarget = target.getAddress();
//    flow.addJump(rreilTarget, null);
//    return flow;
    /**
     * Todo: New RReil grammar
     */
    throw new RReilGrammarException(); 
  }

  @Override public Flows<D> visit (BranchToNative stmt, P2<ProgramPoint, RReilAddr> ctx) {
    Flows<D> flow = new Flows<D>();
    
    SimpleExpression se = stmt.getCond();
    Rval condition;
    if(se instanceof LinRval)
      condition = ((LinRval)se).getRval();
    else {
      /**
       * Todo: New RReil grammar
       */
      throw new RReilGrammarException();
    }
    
    // Try walking the "not-taken" branch
    try {
      if (condition instanceof Rlit) { // static evaluation of test if possible
        BigInt conditionValue = ((Rlit) condition).getValue();
        if (!conditionValue.isZero())
          throw new Unreachable(); // dead branch
        flow.addNext(ctx._2(), null);
      }
    } catch (Unreachable _) {
    }
    // Try walking the "taken" branch
    try {
      if (condition instanceof Rlit) { // static evaluation of test possible as an optimization
        BigInt conditionValue = ((Rlit) condition).getValue();
        if (!conditionValue.isOne())
          throw new Unreachable(); // dead branch
//        Rval target = stmt.getTarget();
//        if (target instanceof Rlit) {
//          RReilAddr singleTarget = RReilAddr.valueOf(((Rlit) target).getValue());
//          flow.addJump(singleTarget, null);
//        }
        /**
         * Todo: New RReil grammar
         */
        throw new RReilGrammarException(); 
      }
    } catch (Unreachable _) {
    }
    if (flow.isEmpty()) { // we could not evaluate the test statically, so assume that both branches are possible
      flow.addNext(ctx._2(), null);
//      Rval target = stmt.getTarget();
//      if (target instanceof Rlit) {
//        RReilAddr singleTarget = RReilAddr.valueOf(((Rlit) target).getValue());
//        flow.addJump(singleTarget, null);
//      } else {
//        addWarning(ctx._1(), new UnresolvedJumpTarget(stmt));
//      }
      /**
       * Todo: New RReil grammar
       */
      throw new RReilGrammarException(); 
    }
    return flow;
  }

  @Override public Flows<D> visit (Branch stmt, P2<ProgramPoint, RReilAddr> ctx) {
    Flows<D> flow = new Flows<D>();
//    Rval target = stmt.getTarget();
//    if (target instanceof Rlit) {
//      RReilAddr singleTarget = RReilAddr.valueOf(((Rlit) target).getValue());
//      switch (stmt.getBranchType()) {
//      case Call:
//        flow.addCall(singleTarget, null);
//        // NOTE: this assumes that each call returns
//        flow.addNext(ctx._2(), null);
//        break;
//      case Return:
//        flow.addReturn(singleTarget, null);
//        break;
//      case Jump:
//        flow.addJump(singleTarget, null);
//        break;
//      }
//    }
//    if (flow.isEmpty()) {
//      addWarning(ctx._1(), new UnresolvedJumpTarget(stmt));
//      flow.addHalt(null);
//      // TODO: one could continue here with the fall-through instruction but that is definitely wrong!
//      // it would also require recovering from disassembler errors as the fall-through bytes might not
//      // be a correct instruction
//    }
//    return flow;
    /**
     * Todo: New RReil grammar
     */
    throw new RReilGrammarException(); 
  }

  @Override public Flows<D> visit (PrimOp primOp, P2<ProgramPoint, RReilAddr> ctx) {
    if (primOp.is("halt", 0, 0))
      return Flows.halt(null);
    else
      return fallThroughSuccessor(ctx);
  }

  @Override public Flows<D> visit (Native stmt, P2<ProgramPoint, RReilAddr> ctx) {
    return fallThroughSuccessor(ctx);
  }

  @Override public Flows<D> visit (Assertion stmt, P2<ProgramPoint, RReilAddr> ctx) {
    return fallThroughSuccessor(ctx);
  }

  @Override public Flows<D> visit (Throw stmt, P2<ProgramPoint, RReilAddr> ctx) {
    return fallThroughSuccessor(ctx);
  }

  @Override public Flows<D> visit (Flop stmt, P2<ProgramPoint, RReilAddr> ctx) {
    return fallThroughSuccessor(ctx);
  }

  private Flows<D> fallThroughSuccessor (P2<ProgramPoint, RReilAddr> ctx) {
    RReilAddr nextInstructionaddress = ctx._2();
    return Flows.next(nextInstructionaddress, null);
  }

  private void addWarning (ProgramPoint location, WarningMessage warning) {
    WarningsContainer warningsHere = warnings.get(location);
    warningsHere.addWarning(warning);
    warnings.put(location, 0, warningsHere);
  }

}
