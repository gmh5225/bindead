package bindead.domains.sat;

import static bindead.debug.DebugHelper.logln;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import satDomain.Clause;
import satDomain.NativeInverter;
import satDomain.SatDomain;

public class Tests {
  final static int x1 = 1;
  final static int x2 = 2;
  final static int x3 = 3;
  final static int x4 = 4;
  final static int x5 = 5;
  final static int x6 = 6;
  final static int y1 = 7;
  final static int y2 = 8;
  final static int y3 = 9;
  final static int y4 = 10;
  final static int y5 = 11;
  final static int y6 = 12;

  @SuppressWarnings("unused") private static String showModel (int[] model) {
    return new Clause(model).toString();
  }

  Clause cls;
  Clause cls2;

  @Test public void clauseTestGet () {
    if (cls.getClause()[2] != -3)
      fail("third component should be -3 but is " + cls.getClause()[2]);
  }

  @Test public void clauseTestLength () {
    if (cls.getClause().length != 7)
      fail("Clause.length");
  }

  @Test public void clauseTestSubsetSat () {
    final SatDomain d1 = new SatDomain();
    final int v1 = d1.freshTrueVar();
    final SatDomain d2 = new SatDomain();
    final int v2 = d2.freshTopVar();
    assert v1 == v2;
    assert d1.isSubset(d2);
  }

  @Before public void setup2 () {
    cls = new Clause(new int[] {1, 2, -3, 4, -5, 6, 7});
    cls2 = new Clause(new int[] {6, -3, 4, 2, -5, 1, 7});
  }

  /**
   * See if the native lib is present on this machine.
   */
  public static boolean checkForPreconditions () {
    try {
      NativeInverter.loadNativeDependencies();
    } catch (UnsatisfiedLinkError e) {
      return false;
    }
    return true;
  }

  @Test public void testNativeInverter () {
//    DebugHelper.analysisKnobs.printLogging();
    // ignore Test if the preconditions are not satisfied
    assumeTrue(checkForPreconditions());

    final SatDomain sd = new SatDomain();
    final int x1 = sd.freshTopVar();
    final int x2 = sd.freshTopVar();
    final int x3 = sd.freshTopVar();
    sd.assumeEquality(x1, x2);
    sd.assumeEquality(x2, x3);
    logln(sd);
    final List<Integer> renamings = new ArrayList<Integer>();
    renamings.add(0);
    renamings.add(1);
    renamings.add(2);
    sd.project(renamings, true);
    logln(sd);
  }

  /*
   * @Test public void testProject() { CNF cnf; int lastVar = y6; CNFSolver
   * mu; final PropFormula f = P.and(new PropFormula[] { // this is the mu
   * formula from // "Existential Quantification as Incremental SAT"
   * P.not(P.and(P.lit(x2), P.lit(y2))), P.not(P.and(P.lit(y2), P.lit(y1))),
   * P.imp(P.and(P.lit(x4), P.lit(x6)), P.lit(y1)), P.iff(P.lit(x3),
   * P.lit(y4)), P.iff(P.lit(x4), P.lit(y3)), P.iff(P.lit(x5), P.lit(y6)),
   * P.iff(P.lit(x6), P.lit(y5)), // this is the zeta formula P.or(P.and(new
   * int[] { x1, -x2, x3, -x4, x5, -x6 }), P.and(new int[] { x1, -x2, -x3, x4,
   * -x5, x6 })) }); // System.out.println("formula: "+f); cnf = new
   * CNF(lastVar); f.addConstraintsTo(cnf); lastVar = cnf.getLastVar(); mu =
   * cnf.createSolver(); final int[] onto = new int[lastVar + 1]; final int[]
   * remove = { y1, y2, y3, y4, y5, y6 }; int idx = 0; for (int i = 0; i <
   * remove.length; i++) onto[remove[i]] = ++idx; //
   * System.out.println("system before projection: " + cnf.showDimacs());
   * final CNF res = new CNF(cnf); res.project(onto); //
   * System.out.println("projection:\n" + res.showDimacs()); // this is the
   * CNF result from the paper, with the fourth clause being // corrected
   * final int[][] goalCNF = { { -2, 6 }, { -3, -6 }, { 5, 6 }, { 3, -5 }, {
   * 4, -6 }, { 1, 6 }, { -1, -2 }, { -4, 6 } }; // this is my manual result
   * // int[][] goalCNF = { { -3, -4 }, { 4, 3 }, { -5, -6 }, { 6, 5 }, // {
   * -4, 6 }, { -6, 4 }, // { -1, -2 }, { 1, 4 }}; final CNF goal = new
   * CNF(6); for (final int[] element : goalCNF) goal.addClause(new
   * Clause(element)); final boolean meImpOth = res.entails(goal); final
   * boolean othImpMe = goal.entails(res); if (meImpOth && othImpMe) return;
   * try { final CNFSolver goals = goal.createSolver(); int[] model =
   * goals.findModel(); while (model != null) { final int[] value = new
   * int[model.length]; for (int i = 0; i < model.length; i++) value[i] =
   * model[i] > 0 ? model[i] + 6 : model[i] - 6; final VecInt assumption = new
   * VecInt(value); final boolean isSat = mu.checkSat(assumption); if (!isSat)
   * // System.out.println("assumptions used: " // + mu.unsatExplanation());
   * fail("vector " + showModel(model) + " does not hold in original system");
   * // System.out.print(showModel(model)); final int[] invModel = new
   * int[model.length]; for (int i = 0; i < model.length; i++) invModel[i] =
   * -model[i]; goals.addClause(new Clause(invModel)); model =
   * goals.findModel(); } } catch (final UnexpectedComplexity e) {
   * fail("timeout"); } if (!meImpOth && !othImpMe)
   * fail("no subset relationship holds"); if (!meImpOth)
   * fail("result has more models than goal"); if (!othImpMe)
   * fail("goal has more models than res"); }
   */
  /*
   * @Test public void testProjectionTop() { final CNF cnf1 = new CNF(2);
   * final int[] onto = new int[] { 0, 1, 2 }; cnf1.addClause(new Clause(new
   * int[] { 1 })); cnf1.addClause(new Clause(new int[] { -1 }));
   * cnf1.project(onto); cnf1.makeNonInverted("testProjectionTop"); assert
   * cnf1.getClauses().hasZeroClause(); }
   */
  /*
   * @Test public void testProjectNone() { final CNF cnf1 = new CNF(1); //
   * true final int[] onto = new int[] { 0, 0 }; cnf1.project(onto); if
   * (cnf1.getLastVar() > 0)
   * fail("this should contain 0 variables, but lastVar is " +
   * cnf1.getLastVar() + " in CNF " + cnf1); }
   */
  /*
   * @Test public void testProjectTrue() { final CNF cnf3 = new CNF(3); //
   * true final int[] onto = new int[] { 0, 1, 2, 3 }; cnf3.project(onto);
   * cnf3.makeNonInverted("testProjectTrue"); assert
   * cnf3.getClauses().hasNoClauses(); }
   */
  /*
   * @Test public void testProjectTrueNotBottom() { final CNF cnf1 = new
   * CNF(1); // true final int[] onto = new int[] { 0, 1 };
   * cnf1.project(onto); if (cnf1.isBottom())
   * fail("this should not be bottom"); }
   */
  // the test above would work without a hack in the constructor of
  // ProjectWorker that explicitly checks for satisfiability if the following
  // test would work
  @Test public void testSolver () {
    final ISolver s = SolverFactory.newDefault();
    final int resVars = s.newVar(6);
    assert resVars == 6;
    try {
      s.addClause(new VecInt(new int[] {-1, -3}));
      s.addClause(new VecInt(new int[] {-2, -4}));
      s.addClause(new VecInt(new int[] {1}));
      s.addClause(new VecInt(new int[] {3}));
      final IConstr r = s.addAtMost(new VecInt(new int[] {5, 6}), 1);
      int[] model = s.findModel();
      Assert.assertNull(model);
      s.removeConstr(r);
      model = s.findModel();
      Assert.assertNull(model);
    } catch (final ContradictionException e) {
      return;
    } catch (final TimeoutException e) {
      return;
    }
  }
}
