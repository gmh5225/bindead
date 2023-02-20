package bindead.analysis.x86_32;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import bindead.analyses.ReconstructionCallString;

import java.io.IOException;
import javalx.data.Interval;
import org.junit.Test;
import rreil.cfa.CompositeCfa;

/**
 * Tests the reconstruction of jump tables (switch statements).
 */
public class JumpTablesTest {
  private final static String rootPath = JumpTablesTest.class.getResource("/binary/x86-32/").getPath();

  @Test public void testNonJumpTableStaticSwitches () throws IOException {
    String file = rootPath + "switches-static-non-jump-table";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(3));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(699)));
  }

  @Test public void testStaticSwitchesWithDefault_O0 () throws IOException {
    String file = rootPath + "switches-static-default-O0";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(666)));
  }

  @Test public void testStaticSwitchesInLoop_O0 () throws IOException {
    String file = rootPath + "switches-static-in-loop-O0";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(699)));
  }

  @Test public void testStaticSwitchesDuffsDevice_O0 () throws IOException {
    String file = rootPath + "switches-static-duffs-device-O0";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
// TODO:
//    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(699)));
  }

  @Test public void testStaticSwitches_O0 () throws IOException {
    String file = rootPath + "switches-static-O0";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(55)));
  }

  @Test public void testStaticSwitches_O1 () throws IOException {
    String file = rootPath + "switches-static-O1";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(55)));
  }

  @Test public void testDynamicSwitches_O0 () throws IOException {
    String file = rootPath + "switches-dynamic-O0";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(11, 666)));
  }

  @Test public void testDynamicSwitches_O1 () throws IOException {
    String file = rootPath + "switches-dynamic-O1";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(11, 666)));
  }

  @Test public void testDynamicSwitches_O2 () throws IOException {
    String file = rootPath + "switches-dynamic-O2";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(11, 666)));
  }

  @Test public void testDynamicNestedSwitches_O0 () throws IOException {
    String file = rootPath + "switches-dynamic-nested-O0";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
// TODO:
//    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(11, 666)));
  }

  @Test public void testDynamicNegativeSwitches_O0 () throws IOException {
    String file = rootPath + "switches-dynamic-negative-O0";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(-55, 666)));
  }

}
