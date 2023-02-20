package bindead.analysis.x86_32;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import bindead.analyses.ReconstructionCallString;

import java.io.IOException;
import javalx.data.Interval;
import org.junit.Test;
import rreil.cfa.CompositeCfa;

/**
 */
public class McVetoTest {
  private final static String rootPath = McVetoTest.class.getResource("/binary/x86-32/").getPath();

  @Test public void barber () throws IOException {
    String file = rootPath + "barber-O0";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(3));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(0, 1)));
  }

  @Test public void clobberCase4 () throws IOException {
    String file = rootPath + "clobber--CASE-4-O0";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
  }

  @Test public void clobberCase4_1 () throws IOException {
    String file = rootPath + "clobber--CASE-4_1";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
  }

  /**
   * Tests the McVeto instruction aliasing example where the parameter passing to function foo happens on the stack.
   */
  @Test public void instructionAliasingExample001 () throws IOException {
    String file = rootPath + "mcveto-instraliasing-stack";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(0, 1)));
  }

  /**
   * Tests the McVeto instruction aliasing example where the parameter passing to function foo happens through a global var.
   */
  @Test public void instructionAliasingExample002 () throws IOException {
    String file = rootPath + "mcveto-instraliasing";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(0, 1)));
  }

}