package bindead.domains.heap;

import static bindead.TestsHelper.assertHasWarnings;
import static bindead.TestsHelper.assertNoWarnings;
import static bindead.TestsHelper.lines;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.AnalysisFactory.AnalysisDebugHooks;
import bindead.debug.DebugHelper;
import bindead.domains.apron.NativeLibsLoading;
import bindead.domains.pointsto.PointsToProperties;

@Ignore
public class HeapAllocatedMemory {

  private static AnalysisFactory analyzer;
  static {
    if (NativeLibsLoading.haveApronNativeLibraries()) {
      analyzer =
        new AnalysisFactory(
          //          "SegMem Processor Stack Data Heap Fields Predicates(F) PointsTo "
          //    + "Wrapping ThresholdsWidening Affine Congruences Apron(Octagons)");
          "SegMem Processor Null Heap -FieldsDisjunction Fields -SupportSet -Disjunction -Undef SupportSet Predicates(F) -SupportSet PointsTo SupportSet "
            + "Wrapping -ThresholdsWidening -Affine Apron(Octagons) -Intervals");
    }
  }

  private static AnalysisDebugHooks debugger = DebugHelper.printers.domainDump();

  /**
   * Silence any debug output that was enabled by previous tests.
   */
  @Before public void silence () {
    DebugHelper.analysisKnobs.disableAll();
  }

  private static final boolean verbose = !true;

  private Analysis<?> runWithOutput (String assembly) {
    Analysis<?> analysis;
    silence();
    DebugHelper.analysisKnobs.enableCommon();
    DebugHelper.analysisKnobs.printNumVarOnly();
    DebugHelper.analysisKnobs.printCompactDomain();
    if (verbose) {
      PointsToProperties.INSTANCE.debugOther.setValue(true);
      DebugHelper.analysisKnobs.printWidening();
      DebugHelper.analysisKnobs.printSubsetOrEqual();
      analysis = analyzer.runAnalysis(assembly, DebugHelper.printers.domainDump());
    } else {
      PointsToProperties.INSTANCE.debugOther.setValue(false);
      //DebugHelper.analysisKnobs.printWidening();
      //DebugHelper.analysisKnobs.printSubsetOrEqual();
      analysis = analyzer.runAnalysis(assembly);
      System.out.println(analysis);
      //assert analysis != null;
    }
    TestsHelper.evaluateAssertions(analysis);
    return analysis;
  }


  @Test public void doublePointer () {
    // if (?) {
    // x = 4;
    // p = malloc(x);
    // *p = p;
    // } else {
    // p = NULL;
    // }
    // if (p != NULL) {
    // x = *p;
    // y = *x; // without Undef domain the pointer should be TOP so this deref emits a warning
    // }
    // exit_label:
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "cmpeq Z, 0, rnd",
        "brc Z, else:",
        "mov x, 4",
        "prim (p) = malloc(x)",
        "store p, p",
        "br endif1:",
        "else:",
        "mov p, 0",
        "endif1:",
        "cmpeq Z, 0, p",
        "brc Z, endif2:",
        "load x, p",
        "load y, x",
        // exactly one warning is emitted (for the second load operation).
        // (except when the Undef domain or a Disjunction domain is used.)
        "assert #warnings = 0",
        "endif2:",
        "mov bla,x",
        "prim coredump()",
        "halt"
      );
    Analysis<?> analysis = runWithOutput(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Test public void doublePointerSimple () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 0",
        "load x, x",
        "assert #warnings = 1",
        "halt"
      );
    DebugHelper.analysisKnobs.enableCommon();
    Analysis<?> analysis = analyzer.runAnalysis(assembly, debugger);
    TestsHelper.evaluateAssertions(analysis); // exactly one warning is emitted (for the second load operation).
    assertHasWarnings(1, analysis);
  }

  @Test public void testMalloc () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 16",
        "prim (ebx) = malloc(eax)",
        "prim coredump()",
        "halt"
      );
    Analysis<?> analysis = runWithOutput(assembly);
    assertNoWarnings(analysis);
  }

  @Test public void testMaybeMalloc () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov ebx,ebx",
        "cmpeq Z, 0, rnd",
        "brc Z, label:",
        "mov eax, 16",
        "prim (ebx) = malloc(eax)",
        "label:",
        "load ebx,ebx",
        "mov eax,ebx",
        "halt"
      );
    DebugHelper.analysisKnobs.enableCommon();
    DebugHelper.analysisKnobs.printCompactDomain();
    PointsToProperties.INSTANCE.debugOther.setValue(true);
    Analysis<?> analysis = analyzer.runAnalysis(assembly, DebugHelper.printers.domainDump());
    // when the Disjunction domain is not used, loss of precision may trigger another false positive
    assertHasWarnings(1, analysis);
  }



  @Test(timeout = 10000) public void testLoopMalloc () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 16",
        "label:",
        "prim (ebx) = malloc(eax)",
        "mov rnd,?",
        "cmpeq Z, 0, rnd",
        "brc Z, label:",
        "mov eax,ebx",
        "prim coredump()",
        "halt"
      );
    runWithOutput(assembly);
  }

  @Test public void testListMallocInfinite () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 16",
        "mov ebx,0",
        "label:",
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "br label:",
        "halt"
      );
    runWithOutput(assembly);
  }

  @Test public void testListMalloc () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov ebx,0",
        "mov eax, 16",
        "label:",
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "cmpeq Z, 0, rnd",
        "brc Z, label:",
        "mov eax,ebx",
        "halt"
      );
    runWithOutput(assembly);
  }


  @Test public void testListMallocWithContent () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov ebx,0",
        "mov eax, 16",
        "mov counter, 42",
        // first
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        // second
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        // loop
        "label:",
        "prim coredump()",
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        "br label:"
      );
    runWithOutput(assembly);
  }

  @Test public void testListMallocWithContentFromCounter () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov edx,0",
        "mov ecx,0",
        "mov ebx,0",
        "mov eax, 16",
        "mov counter, 42",
        // first
        "mov edx, ecx",
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 42",
        "add counter, counter , 1",
        // second
        "mov edx, ecx",
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "load counter,counterfield",
        "add counter, counter , 1",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        // loop
        "label:",
        //"prim coredump()",
        "mov edx, ecx",
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "load counter,counterfield",
        "add counter, counter , 1",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "mov Z,?",
        "brc Z,label:",
        "prim coredump()",
        "halt"
      );
    runWithOutput(assembly);
  }

  @Test//(timeout = 100000)
  public void testListWithCounter () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov r1,17",
        "mov r2,17",
        "mov r3,17",
        "mov r4,17",
        "mov ebx,0",
        "mov counter, 42",
        //
        "mov eax, 16",
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        //
        "label:",
        //
        "mov eax, 16",
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        "mov rnd, ?",
        "cmpeq Z, 0, rnd",
        "brc Z, label:",
        "mov r1,ebx",
        "load r2,r1",
        "prim coredump()",
        "load r3,r2",
        "load r4,r3",
        "halt"
      );
    runWithOutput(assembly);
  }

  @Test public void testNonzeroListWithCounter () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov ebx,0",
        "mov counter, 42",
        // at least one iteration
        "mov eax, 16",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        // iterate
        /*  "mov ecx,ebx",
          "prim (ebx) = malloc(eax)",
          "store ebx,ecx",
          "add counterfield, ebx, 4",
          "store counterfield, counter",
          "add counter, counter , 1",*/
        // iterate
        "label:",
        "prim coredump()",
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        "mov rnd,?",
        "cmpeq Z, 0, rnd",
        "brc Z, label:",
        "mov eax,ebx",
        "halt"
      );
    runWithOutput(assembly);
  }

  @Test//(timeout = 100000)
  public void testFiniteListWithCounter () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov ebx,0",
        "mov counter, 42",
        // at least one iteration
        "mov eax, 16",
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",    // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",        // iterate
        "prim foldRegions()",
        "prim coredump()",
        "load ebx,ebx",
        "prim foldRegions()",
        "halt"
      );
    runWithOutput(assembly);
  }

  @Test//(timeout = 100000)
  public void testOcti () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x,[12,19]",
        "mov y,[2,5]",
        "halt"
      );
    runWithOutput(assembly);
  }

  public static void main (String[] args) {
    try {
      new HeapAllocatedMemory().makeTree();
    } catch (Throwable ex) {
      Logger.getLogger(HeapAllocatedMemory.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  //@Ignore // takes quite long
  @Test//(timeout = 20000)
  public void testFiniteSummarizedList () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 4",
        // iterate
        "prim (ebx) = malloc(eax)",
        "store ebx,42",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        // fold
        "mov ecx, 0",
        "prim foldRegions()",
        "prim coredump()",
        // read values
        "load ebx,ebx",
        "prim coredump()",
        // read values
        "load ebx,ebx",
        "prim coredump()",
        // read values
        "load ebx,ebx",
        "prim coredump()",
        "halt"
      );
    runWithOutput(assembly);
  }

  // Note that this testcase requires single-step connectors.
  // Need to disable transitive connectors
  @Test//(timeout = 20000)
  public void testContentsOfFiniteSummarizedList () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 16",
        "mov ebx,0",
        // at least one iteration
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 39",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 40",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 41",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 42",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 43",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 44",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 45",
        // fold
        "prim foldRegions()",
        "halt",
        //"prim coredump()",
        // read values
        "add counterfield, ebx, 4",
        "load v45, counterfield",
        "assert v45=45",
        "load ebx,ebx",
        //"prim coredump()",
        // read values
        "add counterfield, ebx, 4",
        "load v44, counterfield",
        "assert v44=44",
        "load ebx,ebx",
        //"prim coredump()",
        // read values
        "add counterfield, ebx, 4",
        "load v43, counterfield",
        "assert v43=43",
        "load ebx,ebx",
        //"prim coredump()",
        // read values
        "add counterfield, ebx, 4",
        "load v42, counterfield",
        "assert v42=42",
        "load ebx,ebx",
        //"prim coredump()",
        // read values
        "add counterfield, ebx, 4",
        "load v41, counterfield",
        "assert v41=41",
        "load ebx,ebx",
        //"prim coredump()",
        "halt"
      );
    runWithOutput(assembly);
  }

  @Test public void testContentsOfFiniteSortedSummarizedList () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 16",
        "mov ebx,0",
        // at least one iteration
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 30",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 33",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 35",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 41",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 42",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 44",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 45",
        // fold
        "prim foldRegions()",
        //"halt",
        //"prim coredump()",
        // read values
        "add counterfield, ebx, 4",
        "load v45, counterfield",
        "assert v45=45",
        "load ebx,ebx",
        //"prim coredump()",
        // read values
        "add counterfield, ebx, 4",
        "load v44, counterfield",
        "assert v44=44",
        "load ebx,ebx",
        //"prim coredump()",
        // read values
        "add counterfield, ebx, 4",
        "load v43, counterfield",
        "assert v43=43",
        "load ebx,ebx",
        //"prim coredump()",
        // read values
        "add counterfield, ebx, 4",
        "load v42, counterfield",
        "assert v42=42",
        "load ebx,ebx",
        //"prim coredump()",
        // read values
        "add counterfield, ebx, 4",
        "load v41, counterfield",
        "assert v41=41",
        "load ebx,ebx",
        //"prim coredump()",
        "halt"
      );
    runWithOutput(assembly);
  }

  @Test(timeout = 20000) public void testReverseFiniteListWithCounter () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov ebx,0",
        "mov counter, 42",
        // at least one iteration
        "mov eax, 16",
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        // iterate
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, counter",
        "add counter, counter , 1",
        // iterate
        "prim foldRegions()",
        "add counter, counter , 1",
        "label:",
        //"add counter, counter , 1",
        //"br label:",
        "mov ecx, 0",
        // reverse (ebx to ecx)
        "load h,ebx",
        "store ebx, ecx",
        "mov ecx,ebx",
        "mov ebx, h",
        // reverse (ebx to ecx)
        "load h,ebx",
        "store ebx, ecx",
        "mov ecx,ebx",
        "mov ebx, h",
        // reverse (ebx to ecx)
        "load h,ebx",
        "store ebx, ecx",
        "mov ecx,ebx",
        "mov ebx, h",
        // reverse (ebx to ecx)
        /* "load h,ebx",
         "store ebx, ecx",
         "mov ecx,ebx",
         "mov ebx, h",*/
        // reverse (ebx to ecx)
        "load h,ebx",
        "store ebx, ecx",
        "mov ecx,ebx",
        "mov ebx, h",
        "halt"
      );
    runWithOutput(assembly);
  }

  @Test//(timeout = 10000)
  public void testListMallocAccessHead () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 32",
        "mov ebx,0",
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "label:",
        "mov ecx,ebx",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "cmpeq Z, 0, rnd",
        "brc Z, label:",
        "mov edx,ebx",
        "prim coredump()",
        "load ebx,ebx",
        "load ebx,ebx",
        "halt"
      );
    runWithOutput(assembly);
  }

  @Test public void testAccessHeadWrongly () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 32",
        "prim (ebx) = malloc(eax)",
        "load ebx,ebx",
        "load ebx,ebx",
        "halt"
      );
    runWithOutput(assembly);
  }

  @Test public void testSimpleListStructure () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 64",
        "loop:",
        "cmpeq Z, 0, rnd",
        "brc Z, end:",
        "prim (c1) = malloc(eax)",
        "prim (c2) = malloc(eax)",
        "prim (c3) = malloc(eax)",
        "prim (c4) = malloc(eax)",
        "store c1, c2",
        "store c2, c3",
        "store c3, c4",
        "br loop:",
        "end:",
        "mov a1,c1",
        "load a1,a1",
        "load a1,a1",
        "load a1,a1",
        //"prim coredump()",
        "halt"
      );
    runWithOutput(assembly);
  }


  // segfaults with Polyhedra
  @Test public void testJoin () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 16",
        "cmpeq Z, 0, rnd",
        "brc Z, label:",
        "prim (ebx) = malloc(eax)",
        "br join:",
        "label:",
        "prim (ebx) = malloc(eax)",
        "join:",
        "mov eax,ebx",
        "halt"
      );
    Analysis<?> analysis = runWithOutput(assembly);
    assertNoWarnings(analysis);
  }

  @Test public void testFold () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 16",
        "prim (ebx) = malloc(eax)",
        "prim (ebx) = malloc(eax)",
        "prim (ebx) = malloc(eax)",
        "halt"
      );
    DebugHelper.analysisKnobs.printCompactDomain();
    Analysis<?> analysis = analyzer.runAnalysis(assembly, DebugHelper.printers.domainDump());
    assertNoWarnings(analysis);
  }

  @Test public void testFoldLoop () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 16",
        "label:",
        "prim (ebx) = malloc(eax)",
        "prim (ebx) = malloc(eax)",
        "prim (ebx) = malloc(eax)",
        "br label:",
        "halt"
      );
    runWithOutput(assembly);
  }

  @Test public void testFoldFinite () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 16",
        "prim (ecx) = malloc(eax)",
        "store ecx,0",
        "add counterfield, ecx, 4",
        "store counterfield, 42",
        "prim (ebx) = malloc(eax)",
        "store ebx,ecx",
        "add counterfield, ebx, 4",
        "store counterfield, 43",
        // fold!
        "prim foldRegions()",
        "load ebx,ebx",
        "load ebx,ebx",
        "halt"
      );
    runWithOutput(assembly);
  }

  @Test public void testSmallFoldFinite () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov r2Copy, 0",
        "mov r1Copy, 0",
        "mov eax, 4",
        "prim (r1) = malloc(eax)",
        "store r1,0",
        "prim (r2) = malloc(eax)",
        "store r2,r1",
        "prim (r3) = malloc(eax)",
        "store r3,r2",
        // fold!
        "prim foldRegions()",
        "load r2Copy,r3",
        "load r1Copy,r2Copy",
        "load r1cont, r1Copy",
        "halt"
      );
    Analysis<?> analysis = runWithOutput(assembly);
    assertHasWarnings(0, analysis);
  }

  @Test public void testFoldInconsistency () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov r2Copy, 0",
        "mov r1Copy, 0",
        "mov r0Copy, 0",
        "mov eax, 4",
        "prim (r1) = malloc(eax)",
        "store r1,0",
        "prim (r2) = malloc(eax)",
        "store r2,r1",
        "prim (r3) = malloc(eax)",
        "store r3,r2",
        // fold!
        "prim foldRegions()",
        "load r2Copy,r3",
        "load r1Copy,r2",
        "load r0Copy,r1",
        "halt"
      );
    Analysis<?> analysis = runWithOutput(assembly);
    assertHasWarnings(0, analysis);
  }


  @Test public void testTwoFold () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov r1Copy, 0",
        "mov r1Cont, 0",
        "mov eax, 4",
        "prim (r1) = malloc(eax)",
        "store r1,42",
        "prim (r2) = malloc(eax)",
        "store r2,r1",
        "prim foldRegions()",
        "load r1Copy,r2",
        "load r1Cont, r1Copy",
        "assert r1Cont=42",
        "halt"
      );
    Analysis<?> analysis = runWithOutput(assembly);
    TestsHelper.evaluateAssertions(analysis); // exactly one warning is emitted (for the second load operation).
  }

  // applying edgenodes should restrict r1Cont to 42
  @Test public void testThreeFold () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov r3Copy, 0",
        "mov r1Copy, 0",
        "mov r1Cont, 0",
        "mov eax, 4",
        "prim (r1) = malloc(eax)",
        "store r1,42",
        "prim (r2) = malloc(eax)",
        "store r2,r1",
        "prim (r3) = malloc(eax)",
        "store r3,666",
        // fold!
        "prim foldRegions()",
        "load r3Copy,r3",
        "load r1Copy,r2",
        "load r1Cont, r1Copy",
        "assert r1Cont=42",
        "halt"
      );
    Analysis<?> analysis = runWithOutput(assembly);
    TestsHelper.evaluateAssertions(analysis); // exactly one warning is emitted (for the second load operation).
  }


  @Test public void testExpand () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 16",
        "prim (ebx) = malloc(eax)",
        "prim (ecx) = malloc(eax)",
        "prim (ecx) = malloc(eax)",
        "store ebx, ebx",
        "halt"
      );
    DebugHelper.analysisKnobs.printCompactDomain();
    DebugHelper.analysisKnobs.enableCommon();
    Analysis<?> analysis = analyzer.runAnalysis(assembly, DebugHelper.printers.domainDump());
    assertNoWarnings(analysis);
  }


  // TODO hsi: I will debug self pointers later
  @Test public void testFoldWithSelfPointers () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov size, 16",
        "prim (a) = malloc(size)",
        "store a,a",
        "prim (b) = malloc(size)",
        "store b,b",
        "prim foldRegions()",
        "load fromB,b",
        "halt"
      );
    Analysis<?> analysis = runWithOutput(assembly);
    assertNoWarnings(analysis);
  }

  @Test public void testFoldWithCrossPointers () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 16",
        "prim (ebx) = malloc(eax)",
        "prim (ecx) = malloc(eax)",
        "store ebx,ecx",
        "store ecx,ebx",
        "prim (edx) = malloc(eax)",
        "halt"
      );
    DebugHelper.analysisKnobs.enableCommon();
    Analysis<?> analysis = analyzer.runAnalysis(assembly, DebugHelper.printers.domainDump());
    assertNoWarnings(analysis);
  }

  @Test public void testMallocAndWrite () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 16",
        "prim (ebx) = malloc(eax)",
        "store ebx, ebx",
        "halt"
      );
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertNoWarnings(analysis);
  }

  @Ignore("need proper implementation of free()") @Test public void testMallocAndFree () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 16",
        "prim (ebx) = malloc(eax)",
        "prim free (ebx)",
        "mov ebx,ebx",
        "halt"
      );
    runWithOutput(assembly);
  }

  @Test public void testPrimopIntroducesRegister () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 16",
        "prim (ebx) = malloc(eax)",
        "mov eax, ebx",
        "halt"
      );
    DebugHelper.analysisKnobs.enableCommon();
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertNoWarnings(analysis);
  }

  /*
  typedef struct Node {
    struct Node * leftChild;
    int key;
    struct Node * rightChild;
  } Node;

  void insert(Node ** root, int key) {
    Node * node;
    while((node = *root))
      if (key < node->key)
        root = &node->leftChild;
      else
        root = &node->rightChild;
    node = (Node*) malloc(sizeof *node);
    node->leftChild = NULL;
    node->key = key;
    node->rightChild = NULL;
    *root = node;
  }

  main() {
    while(1) {
      key = rnd();
      insert(root, key);
    }
  }
  */
  // XXX fails with Undef!
  @Test public void testInsertInTree () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        // Node ** root = malloc(4);
        "mov eax, 4",
        "prim (root) = malloc(eax)",
        // while(1) do
        "while1:",
        // key = rnd();
        "mov key, rnd",
        "mul key, key, key",
        // Node * node;
        "innerLoop:",
        // node = *root;
        "load node, root",
        // if !node goto endInnerLoop:
        "cmpeq Z, 0, node",
        "brc Z, endInnerLoop:",
        //   if (key >= node->key) goto innerLoopElse:
        "add nodeKeyAdr, node, 4",
        "load nodeKey, nodeKeyAdr",
        "cmpleu Z,nodeKey, key",
        "brc Z, innerLoopElse:",
        //     root = &node->leftChild;
        "load root, node",
        //   goto innerLoop:
        "br innerLoop:",
        "innerLoopElse:",
        //     root = &node->rightChild;
        // XXX HSI todo
        //   goto innerLoop:
        // XXX HSI todo
        "endInnerLoop:",
        // node = (Node*) malloc(sizeof *node);
        // XXX HSI todo
        // node->leftChild = NULL;
        // XXX HSI todo
        // node->key = key;
        // XXX HSI todo
        // node->rightChild = NULL;
        // XXX HSI todo
        // *root = node;
        // XXX HSI todo
        // od.
        "br while1:",
        // Ok.
        "mov root, root",
        "halt"
      );
    runWithOutput(assembly);
  }

  /*
   * struct node {
   *   int data;
   *   struct node* next;
   * };
   *
   * function to insert a new_node in a list. Note that this
   * function expects a pointer to head_ref as this can modify the
   * head of the input linked list (similar to push())
   *
   * main() {
   *   head_ref = malloc();
   *   loop:
   *   int v = rnd();
   *   node * new_node = malloc();
   *   *new_node = v;
   *   if (*head_ref == NULL)
   *     goto elsebranch
   *   if(*head_ref)->data >= new_node->data)
   *     goto elsebranch;
   *   struct node* current = *head_ref;
   *   while (current->next!=NULL && current->next->data < new_node->data) {
   *     current = current->next;
   *   }
   *   new_node->next = current->next;
   *   current->next = new_node;
   *   goto loop:
   *   elsebranch:
   *   new_node->next = *head_ref;
   *   *head_ref = new_node;
   *   goto loop:
   * }
   *
   * void sortedInsert(struct node** head_ref, struct node* new_node) {
   * }
   */
  @Ignore// does not terminate in reasonable time on most machines, thus not enabled by default
  @Test public void testInsertInList () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 64",
        //   head_ref = malloc();
        "prim (head_ref) = malloc(eax)",
        "store head_ref, 0",
        "loop:",
        //   int v = rnd();
        "mov v, rnd",
        //   node * new_node = malloc();
        "prim (new_node) = malloc(eax)",
        //   *new_node = v;
        "store new_node,v",
        //   if (*head_ref == NULL)
        "load accu,head_ref",
        "cmpeq Z, 0, accu",
        //     goto elsebranch
        "brc Z, elsebranch:",
        //   if(*head_ref)->data >= new_node->data)
        "add accu,accu,4",
        "load accu, accu",
        "add accu2, new_node,4",
        "load accu2, accu2",
        "cmpleu Z,accu2,accu",
        //     goto elsebranch;
        "brc Z, elsebranch:",
        //   struct node* current = *head_ref;
        "load current, head_ref",
        "innerloop:",
        //   if (current->next==NULL)
        "load accu, current",
        "cmpeq Z, 0, accu",
        //     goto endinnerloop:
        "brc Z, endinnerloop:",
        //   if (current->next->data >= new_node->data)
        "load accu, current",
        "load accu, accu",
        "add accu2, new_node, 4",
        "load accu2, accu2",
        "cmpleu Z, accu2, accu",
        //     goto endinnerloop:
        "brc Z, endinnerloop:",
        //  current = current->next;
        "load current, current",
        //  goto innerloop:
        "br innerloop:",
        "endinnerloop:",
        //   new_node->next = current->next;
        "load accu, current",
        "store new_node,accu",
        //   current->next = new_node;
        "store current, new_node",
        //   goto loop:
        "br loop:",
        "elsebranch:",
        //   new_node->next = *head_ref;
        "load accu, head_ref",
        "store new_node, accu",
        //   *head_ref = new_node;
        "store head_ref, new_node",
        //   goto loop:
        "br loop:",
        "halt"
      );
    runWithOutput(assembly);
  }

  @Test public void makeTree () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov valtwo,12",
        makeNode("l11", "0", 1, "0"),
        makeNode("l12", "0", 3, "0"),
        makeNode("l1", "l11", 2, "l12"),
        makeNode("l21", "0", 5, "0"),
        makeNode("l22", "0", 7, "0"),
        makeNode("l2", "l21", 6, "l22"),
        makeNode("root", "l1", 4, "l2"),
        "mov l11,0",
        "mov l12,0",
        "mov l21,0",
        "mov l22,0",
        "mov l1,0",
        "mov l2,0",
        // "mov root,0",
        //"prim coredump()",
        "prim foldRegions()",
        //"prim coredump()",
        //"halt",
        "load l,root",
        //"prim coredump()",
        "load l,l",
        //"prim coredump()",
        "add  l,l,4",
        "load tipl,l",
        "assert tipl=1",
        //"prim coredump()",
        "add r,root,8",
        "load r,r",
        //"prim coredump()",
        "add r,r,8",
        "load r,r",
        //"prim coredump()",
        "add r,r,4",
        "load tipr,r",
        "assert tipr=7",
        //"prim foldRegions()",
        //"prim coredump()",
        "halt"
      );
    runWithOutput(assembly);
  }

  // Requires working implementation of transitive connectors.
  // But when it finally works, it will prove my thesis right :)
  @Test public void accessTree () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov valtwo,12",
        makeNode("l11", "0", 1, "0"),
        makeNode("l12", "0", 3, "0"),
        makeNode("l1", "l11", 2, "l12"),
        makeNode("l2111", "0", 5, "0"),
        makeNode("l211", "l2111", 6, "0"),
        makeNode("l21", "l211", 8, "0"),
        makeNode("l2", "l21", 12, "0"),
        makeNode("root", "l1", 4, "l2"),
        "mov l11,0",
        "mov l12,0",
        "mov l2111,0",
        "mov l211,0",
        "mov l21,0",
        "mov l1,0",
        "mov l2,0",
        "prim foldRegions()",
        //"halt",
        //"prim coredump()",
        "add r,root,8",  // r = &node4+8
        "load r,r",      // r = &node12
        //"prim coredump()",
        "load r,r",      // r = &node8
        "load r,r",      // r = &node6
        "load r,r",      // r = &node5
        "add contentPtr,r,4",
        "load contentIs5,contentPtr",
        "assert contentIs5=[5,6]",
        //"prim coredump()",
        //"load r,r",
        //"load nonexisting,r",
        //"prim coredump()",
        "halt"
      );
    Analysis<?> analysis = runWithOutput(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  String makeNode (String to, String c1, int value, String c2) {
    return lines("prim (accu) = malloc(valtwo)",
        "mov " + to + ",accu",
        "store accu," + c1,
        "add accu,accu,4",
        "store accu," + value,
        "add accu,accu,4",
        "store accu," + c2);
  }
}
