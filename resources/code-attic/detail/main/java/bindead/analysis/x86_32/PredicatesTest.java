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
public class PredicatesTest {
  private final static String rootPath = PredicatesTest.class.getResource("/binary/x86-32/").getPath();

  @Test public void predicates001 () throws IOException {
    String file = rootPath + "predicates-001";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(0, 9)));
    assertThat(forest.cfaForest().size(), is(1));
  }

  @Test public void predicates002 () throws IOException {
    String file = rootPath + "predicates-002";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(0, 10)));
    assertThat(forest.cfaForest().size(), is(1));
  }

  @Test public void predicates003 () throws IOException {
    String file = rootPath + "predicates-003";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(-1, 1)));
    assertThat(forest.cfaForest().size(), is(1));
  }

  @Test public void predicates004 () throws IOException {
    String file = rootPath + "predicates-004";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(-2, 2)));
    assertThat(forest.cfaForest().size(), is(1));
  }

  @Test public void predicates005 () throws IOException {
    String file = rootPath + "predicates-005";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(-4, 4)));
    assertThat(forest.cfaForest().size(), is(1));
  }
}
