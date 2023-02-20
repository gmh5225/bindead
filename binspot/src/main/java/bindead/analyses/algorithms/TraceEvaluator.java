package bindead.analyses.algorithms;

import javalx.data.products.P3;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import rreil.RReilGrammarException;
import rreil.lang.RReil;
import rreil.lang.RReil.Branch;
import rreil.lang.RReil.BranchToNative;
import rreil.lang.RReil.Native;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.LinRval;
import rreil.lang.Rhs.RangeRhs;
import rreil.lang.Rhs.Rval;
import rreil.lang.Rhs.Rvar;
import rreil.lang.Rhs.SimpleExpression;
import rreil.lang.util.RhsFactory;
import bindead.analyses.algorithms.data.Flows;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.AnalysisEnvironment;

class TraceEvaluator<D extends RootDomain<D>> extends FixpointAnalysisEvaluator<D> {
  private final TraceIterator trace;

  public TraceEvaluator (TraceIterator trace) {
    // FIXME: see if we need the canary for analyzing traces
    super(0xdead);
    this.trace = trace;
  }

  private D assignValueToRegister (String registerName, Rhs value, D domain) {
    AnalysisEnvironment env = domain.getContext().getEnvironment();
    Rvar register = env.getPlatform().getRegisterAsVariable(registerName);
    RReil.Assign setTarget = new RReil.Assign(RReilAddr.ZERO, register, value);
    domain = domain.eval(setTarget);
    return domain;
  }

  // set the values of the possibly influenced registers to top
  // TODO: extract this to a native handler (here it is only implemented for x86-64)
  @Override public Flows<D> visit (Native stmt, P3<D, ProgramPoint, RReilAddr> ctx) {
    D domainState = ctx._1();
    RhsFactory rhsFactory = RhsFactory.getInstance();
    RangeRhs top = rhsFactory.arbitrary(64);
    String[] registers = {"rax", "rbx", "rcx", "rdx"};
    for (String register : registers) {
      domainState = assignValueToRegister(register, top, domainState);
    }
    return Flows.next(ctx._3(), domainState);
  }

  @Override public Flows<D> visit (BranchToNative stmt, P3<D, ProgramPoint, RReilAddr> ctx) {
//    D domainState = ctx._1();
//    RReilAddr insnAddress = stmt.getRReilAddress();
//    Rval target = stmt.getTarget();
//
//    SimpleExpression se = stmt.getCond();
//    Rval condition;
//    if(se instanceof LinRval)
//      condition = ((LinRval)se).getRVal();
//    else {
//      /**
//       * Todo: New RReil grammar
//       */
//      throw new RReilGrammarException();
//    }
//
//    domainState = evalTrace(insnAddress, target, condition, domainState);
//    // TODO: inline the code from super() and perform only the branch operation that was taken
//    P3<D, ProgramPoint, RReilAddr> updatedCtx = P3.tuple3(domainState, ctx._2(), ctx._3());
//    return super.visit(stmt, updatedCtx);
    /**
     * Todo: New RReil grammar
     */
    throw new RReilGrammarException(); 
  }

  @Override public Flows<D> visit (Branch stmt, P3<D, ProgramPoint, RReilAddr> ctx) {
//    D domainState = ctx._1();
//    RReilAddr insnAddress = stmt.getRReilAddress();
//    Rval target = stmt.getTarget();
//    domainState = evalTrace(insnAddress, target, null, domainState);
//    P3<D, ProgramPoint, RReilAddr> updatedCtx = P3.tuple3(domainState, ctx._2(), ctx._3());
//    return super.visit(stmt, updatedCtx);
    /**
     * Todo: New RReil grammar
     */
    throw new RReilGrammarException(); 
  }

  private D evalTrace (RReilAddr insnAddress, Rval target, Rval condition, D domainState) {
    if (trace.hasNext() && trace.nextBranchIsAt(insnAddress)) {
      if (target instanceof Rvar)
        domainState = setJumpTarget((Rvar) target, domainState);
      if (condition != null && condition instanceof Rvar)
        domainState = setConditionFlag((Rvar) condition, domainState);
      trace.advance();
    }
    return domainState;
  }

  private D setJumpTarget (Rvar target, D domainState) {
    RReilAddr targetAddress = trace.getBranchTarget();
    BigInt value = BigInt.of(targetAddress.base());
    return setValue(domainState, target, value);
  }

  private D setConditionFlag (Rvar condition, D domainState) {
    BigInt value = trace.getBranchConditionEvaluation() ? Bound.ONE : Bound.ZERO;
    return setValue(domainState, condition, value);
  }

  private D setValue (D state, Rvar variable, BigInt value) {
    // using an assignment removes any relational information or congruence, etc.
    // RhsFactory rhsFactory = RhsFactory.getInstance();
    // Rlit targetValue = rhsFactory.literal(variable.getSize(), value);
    // RReil.Assign setTarget = new RReil.Assign(null, RReilAddr.ZERO, variable, value);

    // TODO: as resolve is not usable here and we are forced to expose interfaces from Zeno
    // introduce a new REIL test of the form "var == value" to be able to set variables to values
//    FiniteFactory finite = FiniteFactory.getInstance();
//    Rlin varAsLinear = state.resolve(variable);
//    if (varAsLinear == null)
//      DomainStateException.VariableSupportSetException.raise();
//    Test setValue = finite.equalTo(variable.getSize(), varAsLinear.getLinearTerm(), Linear.linear(value));
//    return state.eval(setValue);
    throw new UnimplementedException("Implement the TODO above when needed.");
  }

}