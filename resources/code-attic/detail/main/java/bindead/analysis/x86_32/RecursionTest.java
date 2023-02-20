package bindead.analysis.x86_32;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import bindead.analyses.ReconstructionCallString;

import java.io.IOException;
import javalx.data.BigInt;
import javalx.data.Interval;
import org.junit.Test;
import rreil.cfa.CompositeCfa;

/**
 */
public class RecursionTest {
  private final static String rootPath = ArrayTest.class.getResource("/binary/x86-32/").getPath();

  @Test public void factorial10 () throws IOException {
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(rootPath + "factorial-10");
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(Interval.valueOf(3628800)));
  }

  @Test public void factorial500 () throws IOException {
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(rootPath + "factorial-500");
    CompositeCfa forest = analysis.getCompositeCfa();
    assertThat(forest.cfaForest().size(), is(2));
    // TODO: actually the result should be in the C-int range because of wrap-arounds
    // but the wrapping domain is not yet implemented. As soon as it is this test should fail!
    Interval result = Interval.valueOf(BigInt.valueOf("12201368259911100687012387854230469262535743428031928421924135" +
        "8838584537315388199760549644750220328186301361647714820358416337872207817720048078520515932928547790" +
        "7571939330603772960859086270429174547882424912726344305670173270769461062802310452644218878789465754" +
        "7771498634943677810376442740338273653974713864778784954384895955375379904232410612713269843277457155" +
        "4630997720278101456108118837370953101635632443298702956389662891165897476957208792692887128178007026" +
        "5174507768410719624390394322536422605234945850129918571501248706961568141625359056693423813008856249" +
        "2468915641267756544818865065938479517753608940057452389403357984763639449053130623237490664450488246" +
        "6507594673586207463792518420045936969298102226397195259719094521782333175693458150855233282076282002" +
        "3402626907898342451712006207714640979456116127629145951237229913340169552363850942885592018727433795" +
        "1730145863575708283557801587354327688886801203998823847021514676054454076635359841744304801289383138" +
        "9688163948746965881750450692636533817505547812864000000000000000000000000000000000000000000000000000" +
        "0000000000000000000000000000000000000000000000000000000000000000000000000"));
    assertThat(analysis.queryIntervalOfRegisterAtExit("eax").get(), is(result));
  }
}
