package bindead.abstractsyntax;

import static bindead.TestsHelper.evaluateAssertion;
import static bindead.TestsHelper.evaluateAssertions;
import static bindead.TestsHelper.lines;

import java.util.SortedMap;

import org.junit.Before;
import org.junit.Test;

import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.algorithms.data.CallString;
import bindead.debug.DebugHelper;
import bindead.domainnetwork.interfaces.RootDomain;

/**
 * Test the parsing and building of instructions and the execution on domain states.
 */
public class InstructionsBuilder {
  /**
   * Silence any debug output that was enabled by previous tests.
   */
  @Before public void silence () {
    DebugHelper.analysisKnobs.disableAll();
  }

  @Test public void simpleAssignment () {
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, 0",
        "assert x = 0",
        "halt");
    Analysis<?> analysis = TestsHelper.evaluateAssertions(assembly);
    evaluateAssertions(analysis);
    RootDomain<?> state = getStateAtEnd(analysis).eval("mov.b x, 2");
    evaluateAssertion(state, "assert.b x = 2");
  }

  /**
   * Return the domain state at the end of the analysis, i.e. the last instruction in the instruction stream.
   * Note that at the moment the "last instruction" is really just the instruction with the highest address and
   * not the one that is the last to be executed in the control flow.
   */
  private static <D extends RootDomain<D>> RootDomain<D> getStateAtEnd (Analysis<D> analysis) {
    SortedMap<RReilAddr, RReil> instructions = analysis.getRReilCode().getInstructions();
    return analysis.getState(CallString.root(), instructions.lastKey()).get();
  }
}
