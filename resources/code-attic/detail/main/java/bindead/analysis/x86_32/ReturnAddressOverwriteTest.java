package bindead.analysis.x86_32;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import bindead.analyses.ReconstructionCallString;
import bindead.analysis.util.CallString;
import bindead.domainnetwork.channels.Message;
import java.io.IOException;
import java.util.List;
import javalx.data.products.P3;
import javalx.digraph.Digraph.Vertex;
import org.junit.Test;
import rreil.cfa.Cfa;

/**
 * Tests the generation of warnings when the return address has been overwritten.
 */
public class ReturnAddressOverwriteTest {
  private final static String rootPath = ArrayTest.class.getResource("/binary/x86-32/").getPath();

  @Test public void directOverwrite () throws IOException {
    String file = rootPath + "overwrite-return-address-002";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    Cfa cfa = analysis.getCompositeCfa().lookupCfa("victim").get();
    List<P3<CallString, Vertex, Message>> warnings = analysis.getAnalysisContexts(cfa).getWarnings();
    assertThat(warnings.size(), is(1));
    assertThat(analysis.getCompositeCfa().cfaForest().size(), is(2));
    analysis.getAnalysisContexts(cfa).dumpWarnings(System.out);
  }

}
