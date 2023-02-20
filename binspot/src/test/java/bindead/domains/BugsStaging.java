package bindead.domains;

import static bindead.TestsHelper.lines;
import static bindead.data.Linear.linear;
import static bindead.data.Linear.num;
import static bindead.data.Linear.term;
import static bindead.debug.DebugHelper.logln;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import javalx.data.Option;
import javalx.numeric.BigInt;
import javalx.numeric.Interval;

import org.junit.Before;
import org.junit.Test;

import rreil.lang.util.Type;
import bindead.FiniteDomainHelper;
import bindead.FiniteDomainHelper.RichFiniteDomain;
import bindead.TestsHelper;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.AnalysisFactory.AnalysisDebugHooks;
import bindead.analyses.DomainFactory;
import bindead.data.NumVar;
import bindead.debug.DebugHelper;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domains.affine.Affine;
import bindead.domains.apron.Apron;
import bindead.domains.apron.ApronIntervals;
import bindead.domains.apron.ApronOctagons;
import bindead.domains.apron.ApronPolyhedra;
import bindead.domains.apron.NativeLibsLoading;
import bindead.domains.congruences.Congruences;
import bindead.domains.intervals.Intervals;
import bindead.domains.predicates.finite.Predicates;
import bindead.domains.widening.oldthresholds.ThresholdsWidening;

/**
 * A place to gather unit tests for newly discovered bugs until they will get fixed.
 * Please move the unit test after fixing the bug to a separate file where it fits thematically.
 */
@SuppressWarnings("rawtypes")
public class BugsStaging {
  private final static AnalysisFactory analyzer = new AnalysisFactory(); // default domain stack

  @SuppressWarnings("unused")
  private static String[] interestingDomains =
  {Predicates.NAME, ThresholdsWidening.NAME, Affine.NAME, Congruences.NAME, Intervals.NAME};
//  private static AnalysisDebugHooks debugger = DebugHelper.printers.domainDumpFiltered(interestingDomains);
  @SuppressWarnings("unused")
  private static AnalysisDebugHooks debugger = DebugHelper.combine(
      DebugHelper.printers.instructionEffect(),
      DebugHelper.printers.domainDump());

  /**
   * Silence any debug output that was enabled by previous tests.
   */
  @Before public void silence () {
    DebugHelper.analysisKnobs.disableAll();
  }

  public void evaluateAssertionsDebug (String assembly) {
//    PredicatesProperties.INSTANCE.debugBinaryOperations.setValue(true);
//    FixpointAnalysisProperties.INSTANCE.debugBinaryOperations.setValue(true);
//    FixpointAnalysisProperties.INSTANCE.debugSubsetOrEqual.setValue(true);
    DebugHelper.analysisKnobs.enableCommon();
    DebugHelper.analysisKnobs.printWidening();
    DebugHelper.analysisKnobs.printMemVarOnly();
    Analysis<?> analysis = analyzer.runAnalysis(assembly, DebugHelper.printers.domainDumpBoth());
    TestsHelper.evaluateAssertions(analysis);
  }

//@Ignore// FIXME: investigate why the redundant affine loses precision here
  @Test public void Stripestest012 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov ebx, sp",
        "mov esi, 6",
        "mov ecx, 1",
        "mov edx, 1",
        "loop:",
        "  mul t0, ecx, 4",
        "  add t1, t0, ebx",
        "  load t2, t1",
        "  add t3, eax, t2",
        "  assert t1 = [4, 20]",
        "  mov eax, t3",
        "  mul t0, ecx, 4",
        "  add t1, t0, ebx",
        "  add t2, t1, -4",
        "  assert t2 = [0, 16]",
        "  load t3, t2",
        "  add t4, eax, t3",
        "  mov eax, t4",
        "  add t0, edx, 1",
        "  mov edx, t0",
        "  mov ecx, edx",
        "  cmpeq eq, edx, esi",
        "  xor.1 neq, eq, 1",
        "  brc neq, loop:",
        "exit: halt");
    evaluateAssertionsDebug(assembly);
  }

//@Ignore// FIXME: investigate why the redundant affine loses precision here
  @Test public void Stripestest013a () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov ebx, sp",
        "mov esi, 6",
        "mov ecx, 1",
        "mov edx, 1",
        "loop:",
        "  mul t0, ecx, 4",
        "  add t1, t0, ebx",
        "  load t2, t1",
        "  add t3, eax, t2",
        "  assert t1 = [4, 20]",
        "  mov eax, t3",
        "  add t0, edx, 1",
        "  mov edx, t0",
        "  mov ecx, edx",
        "  cmpeq eq, edx, esi",
        "  xor.1 neq, eq, 1",
        "  brc neq, loop:",
        "exit: halt");
    evaluateAssertionsDebug(assembly);
  }

//@Ignore// FIXME: investigate why the redundant affine loses precision here
  @Test public void Stripestest013b () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov ebx, sp",
        "mov esi, 6",
        "mov ecx, 1",
        "mov edx, 1",
        "loop:",
        "  mul t0, ecx, 4",
        "  add t1, t0, ebx",
        "  load t2, t1",
        "  add t3, eax, t2",
        "  assert t1 = [4, 20]",
        "  mov eax, t3",
        "  add t0, edx, 1",
        "  sub t1, 4294967295, edx",
        "  mov edx, t0",
        "  mov ecx, edx",
        "  cmpeq eq, edx, esi",
        "  xor.1 neq, eq, 1",
        "  brc neq, loop:",
        "exit: halt");
    evaluateAssertionsDebug(assembly);
  }

  /**
   * Shows that if the affine keeps no equalities for variables that are constant
   * we cannot use the affine to perform substitutions on widening thresholds.
   * Thus on projection of one variable we lose the threshold completely although
   * it would be desirable to keep it by performing a substitution with other variables.
   */
  @Test public void CollectedWideningExamplesThresholdSubstitutionWithConstantValues () {
    // x = 0;
    // while (x <= 10) {        ** y = [0, 11]
    //    tmp = x;
    //    x = ?
    //    x = tmp
    //    tmp = ?
    //    x = x + 1;
    // }                        ** x = 11, tmp = 11
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 0",
        "mov tmp1, 0",
        "mov tmp2, 0",
        "loop:",
        "  assert x = [0, 11]",
        "  cmpltu LT, 10, x",
        "  brc LT, exit:",
        "  mov tmp1, x",
        // this no-op is needed to "move" the threshold to tmp2
        // thus relying from now on only on affine equalities for threshold substitution
        "  mov tmp2, tmp1",
        "  mov x, ?",
        "  mov x, tmp1",
        "  mov tmp1, ?",        // here the threshold on 'tmp' should be substituted to x if there would be an affine equality
        "  add x, x, 1",
        "  br loop:",
        "exit:",
        "assert x = 11",
        "halt");
    evaluateAssertionsDebug(assembly);
  }

  @Test public void CollectedWideningExamplesThresholdSubstitutionWithConstantValuesSimpler () {
    // x = 0;
    // tmp = 0
    // do {
    //    tmp = ?
    //    x = x + 1;
    //    tmp = x;
    // } while (tmp <= 10);        ** y = [0, 11]
    //                         ** x = 11, tmp = 11
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 0",
        "mov tmp, 0",
        "loop:",
        "  assert x = [0, 11]",
        "  mov tmp, x",
        "  cmpltu LT, 10, tmp",
        "  brc LT, exit:",
        "  mov tmp, ?",         // here the threshold on 'tmp' should be substituted to x
        "  add x, x, 1",
        "  br loop:",
        "exit:",
        "assert x = 11",
        "assert tmp = 11",
        "halt");
    evaluateAssertionsDebug(assembly);
  }

  /**
   * Taken from binsearch binary. Trying to find out where the widening goes wrong
   * and widens the counter to the left, thus losing precision.
   */
  @Test public void BinsearchWidening() {
    String assembly = lines(
        "0x08048300.00: mov.32 t0, ebx",
        "0x08048300.01: sub.32 esp, esp, 4",
        "0x08048300.02: store.32.32 esp, t0",
        "0x08048301.00: mov.32 ecx, 9",
        "0x08048306.00: sub.32 t0, esp, 48",
        "0x08048306.01: cmpltu.32 CF, esp, 48",
        "0x08048306.02: cmpleu.32 BE, esp, 48",
        "0x08048306.03: cmplts.32 LT, esp, 48",
        "0x08048306.04: cmples.32 LE, esp, 48",
        "0x08048306.05: cmpeq.32 ZF, esp, 48",
        "0x08048306.06: cmplts.32 SF, t0, 0",
        "0x08048306.07: xor.1 OF, LT, SF",
        "0x08048306.08: mov.32 esp, t0",
        "0x08048309.00: xor.32 t0, edx, edx",
        "0x08048309.01: mov.1 CF, 0",
        "0x08048309.02: mov.1 OF, 0",
        "0x08048309.03: cmpeq.32 ZF, t0, 0",
        "0x08048309.04: cmplts.32 SF, t0, 0",
        "0x08048309.05: mov.1 BE, ZF",
        "0x08048309.06: mov.1 LT, SF",
        "0x08048309.07: or.1 LE, SF, ZF",
        "0x08048309.08: mov.32 edx, t0",
        "0x0804830b.00: add.32 t0, 56, esp",
        "0x0804830b.01: load.32.32 t1, t0",
        "0x0804830b.02: mov.32 ebx, t1",
        "0x0804830f.00: br.32 134513439 // Jump",
        "0x08048318.00: add.32 t0, -1, eax",
        "0x08048318.01: mov.32 ecx, t0",
        "0x0804831b.00: sub.32 t0, edx, ecx",
        "0x0804831b.01: cmpltu.32 CF, edx, ecx",
        "0x0804831b.02: cmpleu.32 BE, edx, ecx",
        "0x0804831b.03: cmplts.32 LT, edx, ecx",
        "0x0804831b.04: cmples.32 LE, edx, ecx",
        "0x0804831b.05: cmpeq.32 ZF, edx, ecx",
        "0x0804831b.06: cmplts.32 SF, t0, 0",
        "0x0804831b.07: xor.1 OF, LT, SF",
        "0x0804831d.00: xor.1 t0, LE, 1",
        "0x0804831d.01: brc.32 t0, 134513459",
        "0x0804831f.00: add.32 t0, ecx, edx",
        "0x0804831f.01: mov.32 eax, t0",
        "0x08048322.00: convert.32.32 t1, 1",
        "0x08048322.01: and.32 t1, t1, 31",
        "0x08048322.02: mov.32 t0, eax",
        "0x08048322.03: cmpeq.32 t2, t1, 0",
        "0x08048322.04: brci.32 t2, 0x08048322.0e:",
        "0x08048322.05: sub.32 t4, t1, 1",
        "0x08048322.06: shr.32 t4, eax, t4",
        "0x08048322.07: and.32 t4, t4, 1",
        "0x08048322.08: mov.1 CF, t4",
        "0x08048322.09: shrs.32 t0, eax, t1",
        "0x08048322.0a: cmpeq.32 t3, t1, 1",
        "0x08048322.0b: brci.32 t3, 0x08048322.0d:",
        "0x08048322.0c: mov.1 OF, [0, 1]",
        "0x08048322.0d: brci.32 1, 0x08048322.0e:",
        "0x08048322.0e: mov.1 OF, 0",
        "0x08048322.0f: mov.1 AF, [0, 1]",
        "0x08048322.10: cmpeq.32 ZF, t0, 0",
        "0x08048322.11: cmplts.32 SF, t0, 0",
        "0x08048322.12: or.1 BE, CF, ZF",
        "0x08048322.13: xor.1 LT, SF, OF",
        "0x08048322.14: or.1 LE, LT, ZF",
        "0x08048322.15: mov.32 eax, t0",
        "0x08048324.00: mul.32 t0, 4, eax",
        "0x08048324.01: add.32 t1, 8, t0",
        "0x08048324.02: add.32 t2, t1, esp",
        "0x08048324.03: load.32.32 t3, t2",
        "0x08048324.04: sub.32 t4, ebx, t3",
        "0x08048324.05: cmpltu.32 CF, ebx, t3",
        "0x08048324.06: cmpleu.32 BE, ebx, t3",
        "0x08048324.07: cmplts.32 LT, ebx, t3",
        "0x08048324.08: cmples.32 LE, ebx, t3",
        "0x08048324.09: cmpeq.32 ZF, ebx, t3",
        "0x08048324.0a: cmplts.32 SF, t4, 0",
        "0x08048324.0b: xor.1 OF, LT, SF",
        "0x08048328.00: brc.32 ZF, 134513466",
        "0x0804832a.00: brc.32 LT, 134513432",
        "0x0804832c.00: add.32 t0, 1, eax",
        "0x0804832c.01: mov.32 edx, t0",
        "0x0804832f.00: sub.32 t0, edx, ecx",
        "0x0804832f.01: cmpltu.32 CF, edx, ecx",
        "0x0804832f.02: cmpleu.32 BE, edx, ecx",
        "0x0804832f.03: cmplts.32 LT, edx, ecx",
        "0x0804832f.04: cmples.32 LE, edx, ecx",
        "0x0804832f.05: cmpeq.32 ZF, edx, ecx",
        "0x0804832f.06: cmplts.32 SF, t0, 0",
        "0x0804832f.07: xor.1 OF, LT, SF",
        "0x08048331.00: brc.32 LE, 134513439",
        "0x08048333.00: xor.32 t0, eax, eax",
        "0x08048333.01: mov.1 CF, 0",
        "0x08048333.02: mov.1 OF, 0",
        "0x08048333.03: cmpeq.32 ZF, t0, 0",
        "0x08048333.04: cmplts.32 SF, t0, 0",
        "0x08048333.05: mov.1 BE, ZF",
        "0x08048333.06: mov.1 LT, SF",
        "0x08048333.07: or.1 LE, SF, ZF",
        "0x08048333.08: mov.32 eax, t0",
        "0x08048335.00: add.32 t0, esp, 48",
        "0x08048335.01: sub.32 t1, -1, 48",
        "0x08048335.02: cmpltu.32 CF, t1, esp",
        "0x08048335.03: cmpeq.32 ZF, t0, 0",
        "0x08048335.04: cmplts.32 SF, t0, 0",
        "0x08048335.05: xor.32 t2, t0, esp",
        "0x08048335.06: xor.32 t3, t0, 48",
        "0x08048335.07: and.32 t4, t2, t3",
        "0x08048335.08: cmplts.32 OF, t4, 0",
        "0x08048335.09: or.1 BE, CF, ZF",
        "0x08048335.0a: xor.1 LT, SF, OF",
        "0x08048335.0b: or.1 LE, LT, ZF",
        "0x08048335.0c: mov.32 esp, t0",
        "0x08048338.00: load.32.32 t0, esp",
        "0x08048338.01: add.32 esp, esp, 4",
        "0x08048338.02: mov.32 ebx, t0",
        "0x08048339.00: load.32.32 t0, esp",
        "0x08048339.01: add.32 esp, esp, 4",
        "0x08048339.02: br.32 t0 // Return",
        "0x0804833a: mov.32 eax, 1",
        "0x0804833f.00: br.32 134513461 // Jump"
      );
    evaluateAssertionsDebug(assembly);
  }


  private static final NumVar x1 = NumVar.fresh("x1");

  public void apronPrecondition () {
    // ignore Tests if the preconditions (native library installed) are not satisfied
    assumeTrue(NativeLibsLoading.haveApronNativeLibraries());
  }

  @Test public void apronTestWithWrapping () throws Exception {
    apronPrecondition();
    logln("With intervals:");
    FiniteDomain top1 = DomainFactory.parseFiniteDomain(
        "Wrapping Affine Apron(Intervals)");
    RichFiniteDomain d1 = applyTestWithWrapping(top1);
    d1.assertValueIs(x1, 0);
    logln("With polyhedra:");
    FiniteDomain top2 = DomainFactory.parseFiniteDomain(
        "Wrapping Affine Apron(Polyhedra)");
    RichFiniteDomain d2 = applyTestWithWrapping(top2);
    d2.assertValueIs(x1, 0);
    logln("With octagons:");
    FiniteDomain top3 = DomainFactory.parseFiniteDomain(
        "Wrapping Affine Apron(Octagons)");
    RichFiniteDomain d3 = applyTestWithWrapping(top3);
    d3.assertValueIs(x1, 0); // here it is [0, 1] as octagons are too inprecise for the test
  }

  private static RichFiniteDomain applyTestWithWrapping (FiniteDomain top) {
    RichFiniteDomain d = FiniteDomainHelper.for32bitVars(top);
    d = d.introduce(x1);
    d = d.assign(x1, Interval.of(0, 1));
    logln("d:\n" + d);
    d.assertValueIs(x1, Interval.of(0, 1));
    RichFiniteDomain d1 = d.lessOrEqualTo(10, x1, 9);
    logln("testing with wrapping 10 * x1 <= 9:\n" + d1);
    return d1;
  }

  @Test public void apronTestWithoutWrapping () throws Exception {
    apronPrecondition();
    logln("With intervals:");
    Apron top1 = new ApronIntervals();
    Apron d1 = applyTestWithoutWrapping(top1);
    assertThat(d1.queryRange(x1).convexHull(), is(Interval.of(0, 0)));
    logln("With polyhedra:");
    Apron top2 = new ApronPolyhedra();
    Apron d2 = applyTestWithoutWrapping(top2);
    assertThat(d2.queryRange(x1).convexHull(), is(Interval.of(0, 0)));
    logln("With octagons:");
    Apron top3 = new ApronOctagons();
    Apron d3 = applyTestWithoutWrapping(top3);
    assertThat(d3.queryRange(x1).convexHull(), is(Interval.of(0, 0)));  // here it is [0, 1] as octagons are too inprecise for the test
  }

  private static Apron applyTestWithoutWrapping (Apron top) {
    ZenoFactory zeno = ZenoFactory.getInstance();
    Apron d = top.introduce(x1, Type.Zeno, Option.<BigInt>none());
    d = d.eval(zeno.assign(zeno.variable(x1), zeno.range(Interval.of(0, 1))));
    logln("d:\n" + d);
    assertThat(d.queryRange(x1).convexHull(), is(Interval.of(0, 1)));
    Apron d1 = d.eval(zeno.comparison(linear(num(-9), term(num(10), x1)), ZenoTestOp.LessThanOrEqualToZero));
    logln("testing without wrapping 10 * x1 <= 9:\n" + d1);
    return d1;
  }

}
