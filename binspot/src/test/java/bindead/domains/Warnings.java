package bindead.domains;

import static bindead.TestsHelper.assertHasWarnings;
import static bindead.TestsHelper.assertNoWarnings;
import static bindead.TestsHelper.evaluateAssertions;
import static bindead.TestsHelper.lines;

import java.io.IOException;

import org.junit.Test;

import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;

public class Warnings {
  private final static AnalysisFactory analyzer = new AnalysisFactory().enableDomains("Null").disableDomains("Data");

  @Test public void test001 () throws IOException {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 2",
        "add y, x, sp",
        "load.b.d z, x",
        "exit:",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertHasWarnings(1, analysis);
  }

  @Test public void test002 () throws IOException {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [1, 2]",
        "add y, x, sp",
        "store.d.b y, 10",
        "exit:",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertHasWarnings(1, analysis);
  }

  @Test public void test003 () throws IOException {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [1, 2]",
        "sub y, x, sp",
        "store.d.b y, 10",
        "exit:",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertHasWarnings(1, analysis);
  }

  @Test public void test004 () throws IOException {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [1, 2]",
        "add y, x, 1",
        "exit:",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    assertNoWarnings(analysis);
  }

  @Test public void indirectBranch () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov r1, 10",
        "sub r1, r1, 1",
        "cmpltu NZF, 0, r1",
        "mov r2, target:",
        "brc NZF, r2",
        "add r3, r1, 1",
        "target:",
        "add r4, r1, 1",
        "halt");
     Analysis<?> analysis = analyzer.runAnalysis(assembly);
     assertNoWarnings(analysis);
  }

  @Test public void test001Inline () throws IOException {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, 2",
        "add y, x, sp",
        "load.b.d z, x",
        "exit:",
        "assert #warnings = 1",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    evaluateAssertions(analysis);
  }

  @Test public void test002Inline () throws IOException {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [1, 2]",
        "add y, x, sp",
        "store.d.b y, 10",
        "exit:",
        "assert #warnings = 1",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    evaluateAssertions(analysis);
  }

  @Test public void test003Inline () throws IOException {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [1, 2]",
        "sub y, x, sp",
        "store.d.b y, 10",
        "exit:",
        "assert #warnings = 1",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    evaluateAssertions(analysis);
  }

  @Test public void test004Inline () throws IOException {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [1, 2]",
        "add y, x, 1",
        "exit:",
        "assert #warnings = 0",
        "halt");
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    evaluateAssertions(analysis);
  }

  @Test public void indirectBranchInline () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov r1, 10",
        "sub r1, r1, 1",
        "cmpltu NZF, 0, r1",
        "mov r2, target:",
        "brc NZF, r2",
        "add r3, r1, 1",
        "target:",
        "add r4, r1, 1",
        "assert #warnings = 0",
        "halt");
     Analysis<?> analysis = analyzer.runAnalysis(assembly);
     evaluateAssertions(analysis);
  }

}
