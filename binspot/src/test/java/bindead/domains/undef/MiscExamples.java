package bindead.domains.undef;

import static bindead.TestsHelper.lines;

import org.junit.Test;

import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.debug.DebugHelper;

public class MiscExamples {
  private final static AnalysisFactory undefAnalyzer = new AnalysisFactory().enableDomains("Undef");

  /**
   * When a loop counter variable has an affine relation with a flag and it gets widened it could lead to
   * no threshold being applied for the widened variable. This happens because flags are not widened and
   * thus thresholds that would narrow after widening are marked as redundant where they in reality are not.
   * This only happens if the redundancy is tested by applying the negated test
   * and catching unreachable. We there test on the flag only and will assume the test was redundant, i.e. did
   * not change the domain.
   * Hence, we now test the redundancy by looking if applying a threshold had an effect on the domain.
   *
   * --------------------------------------
   * After improving the redundant affine to perform a transitive closure on test application there is a new problem.
   * Because of the equality relation between the flag and the loop variable and because the flag is not widened, we
   * narrow too much after widening. This in turn means that the loop counter is not stable yet and needs further
   * widening. Unfortunately on the second widening, we cannot use the threshold anymore and lose precision.
   */
  @Test public void undefFlagWidening1 () {
    // i = 0
    // while (i <= 10)
    //   i++
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov i, 0",
        "loop:",
        "  assert i = [0, 10]",
        // LES is a new variable that exists only in the loop
        // and thus when joining on the backjump to the loop header LES
        // will cause Undef to introduce a flag for it.
        "  cmples LES, 10, i",
        "  brc LES, exit:",
        "  add i, i, 1",
        "  br loop:", // on the join the Undef flag has an equality with the loop counter: e = i
        "exit:",
        "assert i = 10",
        "halt ");
    Analysis<?> analysis = undefAnalyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * When a loop counter variable has an affine relation with a flag and it gets widened it could lead to
   * no threshold being applied for the widened variable. This happens because flags are not widened and
   * thus thresholds that would narrow after widening are marked as redundant where they in reality are not.
   * This only happens if the redundancy is tested by applying the negated test
   * and catching unreachable. We there test on the flag only and will assume the test was redundant, i.e. did
   * not change the domain.
   * Hence, we now test the redundancy by looking if applying a threshold had an effect on the domain.
   *
   * --------------------------------------
   * After improving the redundant affine to perform a transitive closure on test application there is a new problem.
   * Because of the equality relation between the flag and the loop variable and because the flag is not widened, we
   * narrow too much after widening. This in turn means that the loop counter is not stable yet and needs further
   * widening. Unfortunately on the second widening, we cannot use the threshold anymore and lose precision.
   *
   * --------------------------------------
   * This example adds more equalities inside the loop that get inlined by the affine and thus can lead to an
   * imprecise addition when assigning the inlined term. Is now fixed by performing either the original
   * or the inlined assigned in affine depending on the resulting terms.
   */
  @Test public void undefFlagWidening2 () {
    // i = 0
    // while (i <= 10)
    //   i++
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov pc, 0",
        //        "mov tmp, 0", // tmp variable must be introduced inside the loop and not exist on loop entry
        "mov i, 0",
        "loop:",
        "  assert i = [0, 10]",
        // LES is a new variable that exists only in the loop
        // and thus when joining on the backjump to the loop header LES
        // will cause Undef to introduce a flag for it.
        "  cmples LES, 10, i",
        "  brc LES, exit:",
        "  add i, i, 1",
        "  sub tmp, 1, pc",
        "  add pc, pc, tmp",
        "  br loop:", // on the join the Undef flag has an equality with the loop counter: e = i
        "exit:",
        "assert i = 10",
        "halt ");
    DebugHelper.analysisKnobs.enableCommon();
    DebugHelper.analysisKnobs.printMemVarOnly();
    DebugHelper.analysisKnobs.printWidening();
    Analysis<?> analysis = undefAnalyzer.runAnalysis(assembly, DebugHelper.printers.domainDumpBoth());
    TestsHelper.evaluateAssertions(analysis);
  }

  /**
   * When copying a variable over that is associated with a flag that already exists in this
   * we would get a variable support set failure because of trying to reintroduce an already existing variable.
   * Now such variables get renamed before being copied over.
   */
  @Test public void undefcopyAndPasteBug () throws Throwable {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov eax, 4",
        "prim (root) = malloc(eax)",
        "whiletrue:",
        "  mov key, rnd",
        "  mul key, key, key",
        "  innerLoop:",
        "    load node, root",
        "    cmpeq Z, 0, node",
        "    brc Z, endInnerLoop:",
        "    add nodeKeyAdr, node, 4",
        "    load nodeKey, nodeKeyAdr",
        "    cmpleu Z, nodeKey, key",
        "    brc Z, endInnerLoop:",
        "    load root, node",
        "    br innerLoop:",
        "  endInnerLoop:",
        "br whiletrue:"
      );
    undefAnalyzer.runAnalysis(assembly);
  }

}
