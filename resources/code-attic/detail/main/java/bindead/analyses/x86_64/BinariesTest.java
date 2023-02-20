package bindead.analyses.x86_64;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import bindead.analyses.ReconstructionCallString;
import bindead.analyses.UnreachableCodeRemover;
import java.io.IOException;
import java.util.Set;
import org.junit.Test;
import rreil.abstractsyntax.RReilAddr;
import rreil.cfa.Cfa;
import rreil.cfa.util.CfaHelpers;
import rreil.cfa.CompositeCfa;

/**
 * Some tests with 64 bit binaries.
 * TODO: fix and cleanup or remove
 */
public class BinariesTest {
  private final static String rootPath = BinariesTest.class.getResource("/binary/x86-64/").getPath();

  @Test public void disassembleIntArray000 () throws IOException {
    String file = rootPath + "array-int";
    CompositeCfa forest = ReconstructionCallString.runElf(file).getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
  }

  @Test public void mcVetoInstructionAliasingExample () throws IOException {
    String file = rootPath + "instraliasing.o";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
    Cfa main = forest.lookupCfa("main").get();
    Set<RReilAddr> programPoints = CfaHelpers.getAddressableVertices(main).keySet();
    assertThat(programPoints, hasItem(RReilAddr.valueOf(0x3d)));
  }

  @Test public void matrix () throws IOException {
    String file = rootPath + "matrix64";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
  }

  @Test public void array () throws IOException {
    String file = rootPath + "array";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
  }

  @Test public void unreachableCodeShiftLeft () throws IOException {
    String file = rootPath + "unreachable-code-shift-left";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    @SuppressWarnings({"unchecked", "rawtypes"})
    UnreachableCodeRemover<?> remover = new UnreachableCodeRemover(analysis);
    remover.run();
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
  }
}
