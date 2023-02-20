package bindead.binaries;

import static bindead.TestsHelper.assertHasWarnings;
import static bindead.TestsHelper.lines;

import java.io.IOException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.AnalysisFactory.AnalysisDebugHooks;
import bindead.debug.DebugHelper;
import bindead.domains.affine.Affine;
import bindead.domains.apron.Apron;
import bindead.domains.intervals.IntervalSets;
import bindead.domains.intervals.Intervals;
import bindead.domains.predicates.finite.Predicates;
import bindead.domains.widening.oldthresholds.ThresholdsWidening;
import bindead.exceptions.AnalysisException;
import binparse.Binary;

/**
 * Test the sendmail crack address example and variations of it.
 *
 * @author Bogdan Mihaila
 */
public class SendmailCrackAddr {
  // NOTE: using the Undef domain unfortunately decreases precision as it introduces more
  // variables in Affine and the reduction is less precise
  private final static AnalysisFactory analyzer = new AnalysisFactory().disableDomains("Heap", "Undef");

  private static String[] interestingDomains = {Predicates.NAME, ThresholdsWidening.NAME, bindead.domains.widening.thresholds.ThresholdsWidening.NAME,
    Affine.NAME, Intervals.NAME, IntervalSets.NAME, Apron.NAME};
  private static AnalysisDebugHooks debugger = null;
//  private static AnalysisDebugHooks debugger = DebugHelper.printers.domainDumpFiltered(interestingDomains);
//  private static final AnalysisDebugHooks debugger =
//    DebugHelper.combine(DebugHelper.printers.instructionEffect(),
//        DebugHelper.printers.domainDumpBoth());

  @BeforeClass
  public static void init () {
//  AnalysisProperties.INSTANCE.useGDSLDisassembler.setValue(true);
//    AnalysisProperties.INSTANCE.disassembleBlockWise.setValue(true);
  }

  /**
   * Silence any debug output that was enabled by previous tests.
   */
  @Before public void silence () {
    DebugHelper.analysisKnobs.disableAll();
  }

//  @Before
  public void enableDebugger () {
    DebugHelper.analysisKnobs.enableCommon();
    DebugHelper.analysisKnobs.printWidening();
    DebugHelper.analysisKnobs.printMemVarOnly();
  }

  @Test public void crackaddrGoodOversimplifiedNoData () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("crackaddr-good-oversimplified-nodata");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  /**
   * Results in an exception as the return address is overwritten and we cannot return to main safely.
   * Warnings are similar to above but writes to local buffer go over the buffer bounds and write to the whole stack.
   */
  @Test(expected = AnalysisException.class) public void crackaddrBadOversimplifiedNoData () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("crackaddr-bad-oversimplified-nodata");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(14, analysis); // currently not reached because of the exception
  }

  /**
   * Warnings are:
   * - read from global data segment with non-constant offset. That is the reading of the string literal.
   * - write to local buffer inside loop with non-constant offset but still bounded to buffer, thus benign
   * - 2 writes to local buffer with non-constant offset after the loop. This should be more precise. TODO: Investigate!
   */
  @Test public void crackaddrGoodOversimplified () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("crackaddr-good-oversimplified");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  /**
   * Results in an exception as the return address is overwritten and we cannot return to main safely.
   * Warnings are similar to above but writes to local buffer go over the buffer bounds and write to the whole stack.
   */
  @Test(expected = AnalysisException.class) public void crackaddrBadOversimplified () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("crackaddr-bad-oversimplified");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(14, analysis); // currently not reached because of the exception
  }

  @Test public void crackaddrGoodOversimplifiedLonger () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("crackaddr-good-oversimplified-longer-input");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  @Test(expected = AnalysisException.class) public void crackaddrBadOversimplifiedLonger () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("crackaddr-bad-oversimplified-longer-input");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(14, analysis);  // currently not reached because of the exception
  }

  @Test public void crackaddrGoodNoLength () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("crackaddr-good-nolength");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  @Test(expected = AnalysisException.class) public void crackaddrBadNoLength () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("crackaddr-bad-nolength");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(14, analysis); // currently not reached because of the exception
  }

  @Test public void crackaddrGood () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("crackaddr-good");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  @Test(expected = AnalysisException.class) public void crackaddrBad () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("crackaddr-bad");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(14, analysis); // currently not reached because of the exception
  }

  /**
   * Does not work because of a not happening threshold substitution because of the affine domain
   * not storing variables equalities but constants for constant variables.
   */
  @Test public void crackaddrGoodPointersWithData () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("crackaddr-good-pointers-with-data");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  @Test(expected = AnalysisException.class) public void crackaddrBadPointersWithData () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("crackaddr-bad-pointers-with-data");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(16, analysis); // currently not reached because of the exception
  }

  @Test public void crackaddrGoodPointers () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("crackaddr-good-pointers");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(4, analysis);
  }

  @Test(expected = AnalysisException.class) public void crackaddrBadPointers () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("crackaddr-bad-pointers");
    Analysis<?> analysis = analyzer.runAnalysis(binary, debugger);
    assertHasWarnings(16, analysis); // currently not reached because of the exception
  }

  @Test public void nonTerminationExampleFromCrackaddr () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "0x080483ec.00: br 0x08048410 // Jump to main",

        // copy_it function
        "0x080483ed.00: nop",
//        "0x080483ed.00: mov t0, ebp",
//        "0x080483ed.01: sub esp, esp, 4",
//        "0x080483ed.02: store esp, t0",
        "0x080483ee.00: mov ebp, esp",
//        "0x080483f0.00: sub t0, esp, 16",
//        "0x080483f0.01: cmpltu CF, esp, 16",
//        "0x080483f0.02: cmpleu BE, esp, 16",
//        "0x080483f0.03: cmplts LT, esp, 16",
//        "0x080483f0.04: cmples LE, esp, 16",
//        "0x080483f0.05: cmpeq ZF, esp, 16",
//        "0x080483f0.06: cmplts SF, t0, 0",
//        "0x080483f0.07: xor.1 OF, LT, SF",
        "0x080483f0.08: mov esp, t0",
        "0x080483f3.00: add t0, -4, ebp",
        "0x080483f3.01: store t0, 0",
        "0x080483fa.00: add t0, -8, ebp",       // this is unused but for some reason needed for non-termination
        "0x080483fa.01: store t0, 0",
        "0x08048401.00: br 0x08048407.00 // Jump loop test",

        // "loop_body:",
        "0x08048403.00: add t0, -4, ebp",       // increments the variable
        "0x08048403.01: load t1, t0",
        "0x08048403.02: add t2, t1, 1",
//        "0x08048403.03: sub t3, -1, 1",
//        "0x08048403.04: cmpltu CF, t3, t1",
//        "0x08048403.05: cmpeq ZF, t2, 0",
//        "0x08048403.06: cmplts SF, t2, 0",
//        "0x08048403.07: xor t4, t2, t1",
//        "0x08048403.08: xor t5, t2, 1",
//        "0x08048403.09: and t6, t4, t5",
//        "0x08048403.0a: cmplts OF, t6, 0",
//        "0x08048403.0b: or.1 BE, CF, ZF",
//        "0x08048403.0c: xor.1 LT, SF, OF",
//        "0x08048403.0d: or.1 LE, LT, ZF",
        "0x08048403.0e: store t0, t2",

        // "loop_test:",
        "0x08048407.00: add t0, -4, ebp",
        "0x08048407.01: load t1, t0",
        "0x08048407.02: mov eax, t1",
//        "0x0804840a.00: add t0, 12, ebp",
//        "0x0804840a.01: load t1, t0",
//        "0x0804840a.02: sub t2, eax, t1",
//        "0x0804840a.03: cmpltu CF, eax, t1",
        "0x0804840a.00: cmpltu CF, eax, unknown_value",
//        "0x0804840a.04: cmpleu BE, eax, t1",
//        "0x0804840a.05: cmplts LT, eax, t1",
//        "0x0804840a.06: cmples LE, eax, t1",
//        "0x0804840a.07: cmpeq ZF, eax, t1",
//        "0x0804840a.08: cmplts SF, t2, 0",
//        "0x0804840a.09: xor.1 OF, LT, SF",
        "0x0804840d.00: brc CF, 0x08048403.00 // jump loop entry",
        "halt",

        // main
        "0x08048410.00: mov esp, sp",   // initialize the stack pointer
//        "0x08048411.00: mov t0, ebp",
//        "0x08048411.01: sub esp, esp, 4",
//        "0x08048411.02: store esp, t0",
//        "0x08048412.00: mov ebp, esp",
//        "0x08048414.00: sub t0, esp, 24",
//        "0x08048414.01: cmpltu CF, esp, 24",
//        "0x08048414.02: cmpleu BE, esp, 24",
//        "0x08048414.03: cmplts LT, esp, 24",
//        "0x08048414.04: cmples LE, esp, 24",
//        "0x08048414.05: cmpeq ZF, esp, 24",
//        "0x08048414.06: cmplts SF, t0, 0",
//        "0x08048414.07: xor.1 OF, LT, SF",
//        "0x08048414.08: mov esp, t0",
//        "0x08048417.00: add t0, -4, ebp",
//        "0x08048417.01: load t1, t0",
//        "0x08048417.02: mov eax, t1",
//        "0x0804841a.00: add t0, 4, esp",
//        "0x0804841a.01: store t0, eax",
//        "0x0804841e.00: add t0, -8, ebp",
//        "0x0804841e.01: load t1, t0",
//        "0x0804841e.02: mov eax, t1",
//        "0x08048421.00: store esp, eax",
//        "0x08048424.00: sub esp, esp, 4",
//        "0x08048424.01: store esp, 134513705",
//        "0x08048424.02: br 134513645 // Call copy_it"
        "0x08048424.00: br 0x080483ed.00 // Call copy_it"
      );
    Analysis<?> analysis = analyzer.runAnalysis(assembly, debugger);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Test public void crackaddrRReil () {
    //  #define BUFFERSIZE 200
    //  #define TRUE 1
    //  #define FALSE 0
    //  int copy_it (char *input, unsigned int length) {
    //      char c, localbuf[BUFFERSIZE];
    //      unsigned int upperlimit = BUFFERSIZE - 10;
    //      unsigned int quotation = roundquote = FALSE;
    //      unsigned int inputIndex = outputIndex = 0;
    //      while (inputIndex < length) {
    //          c = input[inputIndex++];
    //          if ((c == '<') && (!quotation)) {
    //              quotation = TRUE; upperlimit--;
    //          }
    //          if ((c == '>') && (quotation)) {
    //              quotation = FALSE; upperlimit++;
    //          }
    //          if ((c == '(') && (!quotation) && !roundquote) {
    //              roundquote = TRUE; upperlimit--; // decrementation was missing in bug
    //          }
    //          if ((c == ')') && (!quotation) && roundquote) {
    //              roundquote = FALSE; upperlimit++;
    //          }
    //          // If there is sufficient space in the buffer, write the character.
    //          if (outputIndex < upperlimit) {
    //              localbuf[outputIndex] = c;
    //              outputIndex++;
    //          }
    //      }
    //      if (roundquote) {
    //          localbuf[outputIndex] = ')';
    //          outputIndex++;
    //      }
    //      if (quotation) {
    //          localbuf[outputIndex] = '>';
    //          outputIndex++;
    //        }
    //  }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov BUFFERSIZE, 200",
        "mov TRUE, 1",
        "mov FALSE, 0",
        "mov input, 0xaaaaaa",          // some absolute address
        "sub sp, sp, BUFFERSIZE",              // make space for local arrays
        "mov buf, sp",                  // local buffer
        "mov length, ?",
        "sub ulimit, BUFFERSIZE, 10",
        "mov q, FALSE",
        "mov rq, FALSE",
        "mov iIndex, 0",
        "mov oIndex, 0",
        "mov c, 0",

        "loop:",
        "  cmplts LTS, iIndex, length",
        "  brc LTS, loop_body:",
        "  br loop_exit:",
        "  loop_body:",
        // read from input array
        "  add tmp, input, iIndex",
        "  load c, tmp",
        "  add iIndex, iIndex, 1",


        "cmpeq EQ, q, FALSE",
        "brc EQ, q_false_1:",
        "br q_false_1_end:",
        "  q_false_1:",
        "  mov q, TRUE",
        "  sub ulimit, ulimit, 1",
        "  q_false_1_end:",

        "cmpeq EQ, q, TRUE",
        "brc EQ, q_true_1:",
        "br q_true_1_end:",
        "  q_true_1:",
        "  mov q, FALSE",
        "  add ulimit, ulimit, 1",
        "  q_true_1_end:",

        // copy the char to the local buffer
        "  cmpltu LTU, oIndex, ulimit",
        "  brc LTU, copy_char:",
        "  br copy_char_end:",
        "    copy_char:",
        "    add tmp, buf, oIndex",
        "    store tmp, c",
        "    add oIndex, oIndex, 1",
        "    copy_char_end:",

        "  br loop:",
        "loop_exit:",

        "cmpeq EQ, rq, TRUE",
        "brc EQ, rq_true:",
        "br rq_true_end:",
        "  rq_true:",
        "  mov c, 0xba", // TODO: find the value for ")"
        "  add tmp, buf, oIndex",
        "  store tmp, c",
        "  rq_true_end:",

        "cmpeq EQ, q, TRUE",
        "brc EQ, q_true:",
        "br q_true_end:",
        "  q_true:",
        "  mov c, 0xba", // TODO: find the value for ">"
        "  add tmp, buf, oIndex",
        "  store tmp, c",
        "  q_true_end:",

        "halt"
      );
    Analysis<?> analysis = analyzer.runAnalysis(assembly, debugger);
    TestsHelper.evaluateAssertions(analysis);
  }

}
