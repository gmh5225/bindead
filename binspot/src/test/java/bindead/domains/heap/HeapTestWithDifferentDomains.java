package bindead.domains.heap;

import static bindead.TestsHelper.lines;

import org.junit.Ignore;
import org.junit.Test;

import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.debug.DebugHelper;
import bindead.domains.apron.NativeLibsLoading;

@Ignore // takes too much time. Reenable later on
public class HeapTestWithDifferentDomains {

  private static String domainString (Boolean useUndef, Boolean useApron, Boolean useOcts) {
    assert !useApron || NativeLibsLoading.haveApronNativeLibraries() : "this test requires the Apron libraries";
    String aprDomain = useOcts ? "Octagons" : "Polyhedra";
    String apronLeaf = "Apron(" + aprDomain + ")";
    String leaf = useApron ? apronLeaf : "Intervals";
    String pre = "SegMem Processor Stack Data Heap Fields SupportSet ";
    String post = "SupportSet Predicates(F) SupportSet PointsTo SupportSet " +
      "Wrapping ThresholdsWidening Affine Congruences " + leaf;
    String undef = useUndef ? "Undef " : "";
    return pre + undef + post;

  }

  static final String assembly = lines(
      "option DEFAULT_SIZE = 32",
      "mov eax, 64",
      "loop:",
      "cmpeq Z, 0, rnd",
      "brc Z, end:",
      "prim (c1) = malloc(eax)",
      "prim (c2) = malloc(eax)",
      "prim (c3) = malloc(eax)",
      "store c1, c2",
      "store c2, c3",
      "br loop:",
      "end:",
      "mov a1,c1",
      "load a1,a1",
      "load a1,a1",
      "halt"
    );

  /**
   * Enable debug output.
   */
//  @Before
  public void debug () {
    //PointsToProperties.INSTANCE.debugOther.setValue(true);
//     DebugHelper.analysisKnobs.enableCommon();
    DebugHelper.analysisKnobs.printLogging();
    DebugHelper.analysisKnobs.printWidening();
    // DebugHelper.analysisKnobs.printSubsetOrEqual();
    DebugHelper.analysisKnobs.printCompactDomain();
//     DebugHelper.analysisKnobs.printFullDomain();
  }

  /**
   * Silence any debug output that was enabled by previous tests.
   */
//  @Before
  public void silence () {
    DebugHelper.analysisKnobs.disableAll();
  }

  @Test(timeout = 20000) public void testIntervalsWithoutUndef () throws Throwable {
    runWithOutput(false, false, false);
  }

  @Test(timeout = 20000) public void testIntervalsWithUndef () throws Throwable {
    runWithOutput(true, false, false);
  }

  // Polyhedra segfaults
  @Test(timeout = 20000) public void testApronPolyWithoutUndef () throws Throwable {
    runWithOutput(false, true, false);
  }

  // Polyhedra segfaults
  @Test(timeout = 20000) public void testApronPolyWithUndef () throws Throwable {
    runWithOutput(true, true, false);
  }
  @Test(timeout = 20000) public void testApronOctWithoutUndef () throws Throwable {
    runWithOutput(false, true, true);
  }

  @Test(timeout = 20000) public void testApronOctWithUndef () throws Throwable {
    runWithOutput(true, true, true);
  }

  private static void runWithOutput (boolean useUndef, boolean useApron, boolean useOct) {
    AnalysisFactory analyzer = new AnalysisFactory(domainString(useUndef, useApron, useOct));
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    DebugHelper.logln(analysis.getWarnings());
  }

}
