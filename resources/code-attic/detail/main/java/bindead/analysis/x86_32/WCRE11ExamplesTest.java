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
public class WCRE11ExamplesTest {
  private final static String rootPath = WCRE11ExamplesTest.class.getResource("/binary/x86-32/").getPath();

  @Test public void m () throws IOException {
    String file = rootPath + "m";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
  }

  @Test public void isqrt () throws IOException {
    String file = rootPath + "isqrt";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.upFrom(2)));
  }

  @Test public void matrix () throws IOException {
    String file = rootPath + "matrix";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
  }

  @Test public void array () throws IOException {
    String file = rootPath + "array-sum-100";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
  }

}
