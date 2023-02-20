package bindead.analysis.x86_32;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import bindead.analyses.ReconstructionCallString;

import java.io.IOException;
import javalx.data.Interval;
import org.junit.Test;
import rreil.cfa.CompositeCfa;

/**
 * Tests the reading and writing of global variables.
 */
public class GlobalVarsTest {
  private final static String rootPath = GlobalVarsTest.class.getResource("/binary/x86-32/").getPath();

  /**
   * Test the reading of a value defined as a global variable.
   */
  @Test public void testGlobalVarsRead001 () throws IOException {
    String file = rootPath + "global-vars-read-001";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(3)));
  }

  /**
   * Test the reading of a multi-byte value defined as a global variable. Should test the endianness handling.
   */
  @Test public void testGlobalVarsRead002 () throws IOException {
    String file = rootPath + "global-vars-read-002";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(0xFFAA)));
  }

  /**
   * Test the reading of an uninitialized value defined as a global variable.
   */
  @Test public void testGlobalVarsRead003 () throws IOException {
    String file = rootPath + "global-vars-read-003";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(0)));
  }

  /**
   * Test the reading and writing of a value defined as a global variable.
   */
  @Test public void testGlobalVarsWrite001 () throws IOException {
    String file = rootPath + "global-vars-write-001";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(5)));
  }

  /**
   * Test the reading and possible overwriting of a value defined as a global variable. The value is only overwritten
   * on one of the program's possible paths.
   */
  @Test public void testGlobalVarsWrite002 () throws IOException {
    String file = rootPath + "global-vars-write-002";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.top()));
  }

  /**
   * Test the reading of an overwritten value defined as a global variable. The value is written by two different values
   * depending on the path the program takes.
   */
  @Test public void testGlobalVarsWrite003 () throws IOException {
    String file = rootPath + "global-vars-write-003";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(1, 2)));
  }

  /**
   * Test the reading of an overwritten value defined as a global variable. The value is written by the same value on
   * two different program paths.
   */
  @Test public void testGlobalVarsWrite004 () throws IOException {
    String file = rootPath + "global-vars-write-004";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(1)));
  }

}
