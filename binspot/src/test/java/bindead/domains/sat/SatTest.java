package bindead.domains.sat;

import static bindead.debug.DebugHelper.logln;
import static javalx.data.Option.some;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import javalx.data.Option;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;

import org.junit.Ignore;
import org.junit.Test;

import rreil.lang.RReilAddr;
import rreil.lang.util.Type;
import bindead.abstractsyntax.finite.Finite;
import bindead.abstractsyntax.finite.Finite.Lhs;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.algorithms.data.CallString;
import bindead.data.Linear;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.VarPair;
import bindead.domains.intervals.Intervals;
import bindead.domains.wrapping.Wrapping;

public class SatTest {
  private final static FiniteFactory fin = FiniteFactory.getInstance();

  private final static AnalysisFactory analyzer = new AnalysisFactory(
    "Root Fields Predicates(F) PointsTo SAT Wrapping Affine Intervals");

  public SatTest () {
//    DebugHelper.analysisKnobs.printLogging();
  }

  // TODO hsi check why these are not registered in SAT domain
  @Test(timeout = 10000) public void canonicalThreeVariablesTest () {
    String assembly = "mov.1 x1, [1, 1]\n"
      + "mov.1 x2, [1, 1]\n"
      + "add.1 x3, x1, x2\n"
      + "halt\n";
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    System.out.println(analysis.getState(CallString.root(),
        RReilAddr.valueOf(3)));
    assertThat(analysis.query(RReilAddr.valueOf(3), "x2", 1, 0),
        is(some(Interval.of(1, 1))));
    assertThat(analysis.query(RReilAddr.valueOf(3), "x1", 1, 0),
        is(some(Interval.of(1, 1))));
    assertThat(analysis.query(RReilAddr.valueOf(3), "x3", 1, 0),
        is(some(Interval.of(2, 2))));
  }

  Option<BigInt> justZero = Option.some(Bound.ZERO);
  Option<BigInt> justOne = Option.some(Bound.ONE);
  Option<BigInt> any = Option.none();

  @SuppressWarnings({"unchecked", "rawtypes"}) Sat<?> mkSatHierarchy () {
    Wrapping<Intervals> w = new Wrapping<Intervals>(new Intervals());
    return new Sat(w);
  }

  @Test(timeout = 10000) public void introduceVariables () {
    Sat<?> sd = mkSatHierarchy();
    logln("domain" + sd);
    sd = mkVar(sd, justZero);
    sd = mkVar(sd, justOne);
    sd = mkVar(sd, any);
    logln("finally " + sd);
  }

  @Test(timeout = 10000) public void assumeEquality () {
    Sat<?> sd = mkSatHierarchy();
    logln("domain" + sd);
    NumVar v1 = NumVar.fresh();
    NumVar v2 = NumVar.fresh();
    NumVar v3 = NumVar.fresh();
    sd = mkVar(sd, v1, justZero);
    sd = mkVar(sd, v2, justOne);
    sd = mkVar(sd, v3, any);
    int size = 1;
    Linear leftExpr = Linear.linear(v1);
    Linear rightExpr = Linear.linear(v3);
    Finite.Test t = fin.equalTo(size, leftExpr, rightExpr);
    logln("before test " + sd);
    sd = sd.eval(t);
    logln("finally " + sd);
  }

  @Test(timeout = 10000) public void assign () {
    Sat<?> sd = mkSatHierarchy();
    logln("domain" + sd);
    NumVar v1 = NumVar.fresh();
    NumVar v2 = NumVar.fresh();
    NumVar v3 = NumVar.fresh();
    sd = mkVar(sd, v1, justZero);
    sd = mkVar(sd, v2, justOne);
    sd = mkVar(sd, v3, any);
    Lhs lhs = fin.variable(1, v3);
    Finite.Rhs rhs = new Finite.FiniteRangeRhs(1, Interval.of(1));
    Finite.Assign t = fin.assign(lhs, rhs);
    logln("before test " + sd);
    sd = sd.eval(t);
    logln("finally " + sd);
  }

  @Test(timeout = 10000) public void fold () {
    Sat<?> sd = mkSatHierarchy();
    logln("domain" + sd);
    NumVar v1 = NumVar.fresh();
    NumVar v2 = NumVar.fresh();
    NumVar v3 = NumVar.fresh();
    NumVar v4 = NumVar.fresh();
    sd = mkVar(sd, v1, justZero);
    sd = mkVar(sd, v2, justOne);
    sd = mkVar(sd, v3, any);
    sd = mkVar(sd, v4, justOne);
    ListVarPair vps = new ListVarPair();
    vps.add(v1, v3);
    vps.add(v2, v4);
    logln("before fold " + sd);
    sd = sd.foldNG(vps);
    logln("finally " + sd);
  }

  @Ignore // this is bit-rotted, debug when needed
  @Test(timeout = 10000) public void foldAndExpand () {
    Sat<?> sd = mkSatHierarchy();
    logln("domain" + sd);
    NumVar v1 = NumVar.fresh();
    NumVar v2 = NumVar.fresh();
    NumVar v3 = NumVar.fresh();
    NumVar v4 = NumVar.fresh();
    sd = mkVar(sd, v1, justZero);
    sd = mkVar(sd, v2, justOne);
    sd = mkVar(sd, v3, any);
    sd = mkVar(sd, v4, justOne);
    ListVarPair vps = new ListVarPair();
    vps.add(new VarPair(v1, v3));
    vps.add(new VarPair(v2, v4));
    logln("before fold " + sd);
    sd = sd.foldNG(vps);
    logln("folded: " + sd);
    sd.expandNG(vps);
    logln("finally " + sd);
  }

  private static Sat<?> mkVar (Sat<?> sd, NumVar var, Option<BigInt> val) {
    return sd.introduce(var, Type.Bool, val);
  }

  private static Sat<?> mkVar (Sat<?> sd, Option<BigInt> val) {
    return mkVar(sd, NumVar.fresh(), val);
  }

}
