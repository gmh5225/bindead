package bindead.analysis.x86_32;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import bindead.analyses.ReconstructionCallString;

import java.io.IOException;
import java.util.Set;
import org.junit.Test;
import rreil.abstractsyntax.RReil.Return;
import rreil.abstractsyntax.RReilAddr;
import rreil.cfa.Cfa;
import rreil.cfa.util.CfaHelpers;
import rreil.cfa.CompositeCfa;

/**
 */
public class ObfuscationTest {
  private final static String rootPath = ObfuscationTest.class.getResource("/binary/x86-32/").getPath();

  @Test public void disassembleXorInstructionAliasing () throws IOException {
    String file = rootPath + "instraliasing-simple.o";
    CompositeCfa forest = ReconstructionCallString.runElf(file).getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(1));
    Cfa main = forest.lookupCfa("main").get();
    Set<RReilAddr> programPoints = CfaHelpers.getAddressableVertices(main).keySet();
    assertThat(programPoints, hasItem(RReilAddr.valueOf(0x8)));
    Set<Return> returns = CfaHelpers.getReturnInstructions(main);
    assertThat(returns.size(), is(2));
    Set<Long> returnsAddresses = CfaHelpers.getBaseAddressesFor(returns);
    assertThat(returnsAddresses, hasItem(0x1fL));
    assertThat(returnsAddresses, hasItem(0x20L));
  }

  @Test public void disassembleReturnObfuscation () throws IOException {
    String file = rootPath + "return-obfuscation.o";
    CompositeCfa forest = ReconstructionCallString.runElf(file).getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
    Cfa main = forest.lookupCfa("main").get();
    Set<RReilAddr> programPoints = CfaHelpers.getAddressableVertices(main).keySet();
    assertThat(programPoints, hasItem(RReilAddr.valueOf(8)));
    Set<Return> returns = CfaHelpers.getReturnInstructions(main);
    assertThat(returns.size(), is(1));
    assertThat(returns.iterator().next().getRReilAddress().base(), is(0x15L));
  }

}
