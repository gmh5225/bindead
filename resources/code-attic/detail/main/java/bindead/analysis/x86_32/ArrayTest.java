package bindead.analysis.x86_32;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import bindead.analyses.AnalysisCtx;
import bindead.analyses.ReconstructionCallString;
import bindead.analyses.liveness.LivenessAnalysis;
import bindead.analyses.liveness.LivenessAnalysisCtx;
import java.io.IOException;
import org.junit.Test;
import rreil.cfa.Cfa;

/**
 */
public class ArrayTest {
  private final static String rootPath = ArrayTest.class.getResource("/binary/x86-32/").getPath();

  @Test public void disassembleIntArray_O0 () throws IOException {
    String file = rootPath + "array-sub-int.O0";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    assertCfasReconstructed(analysis, 2);
  }

  @Test public void disassembleIntArray_O1 () throws IOException {
    String file = rootPath + "array-sub-int.O1";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    AnalysisCtx<?> ctx = analysis.getAnalysisContexts();
    ctx.dumpWarnings(System.out);
    assertCfasReconstructed(analysis, 2);
  }

  @Test public void disassembleArraySum_O1 () throws IOException {
    String file = rootPath + "array-sum-int";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    AnalysisCtx<?> ctx = analysis.getAnalysisContexts();
    ctx.dumpWarnings(System.out);
    assertCfasReconstructed(analysis, 2);
  }

  @Test public void disassembleUnsignedIntArray_00 () throws IOException {
    String file = rootPath + "array-sub-unsigned-int.O0";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    assertCfasReconstructed(analysis, 2);
  }

  @Test public void disassembleUnsignedIntArray_01 () throws IOException {
    String file = rootPath + "array-sub-unsigned-int.O1";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    assertCfasReconstructed(analysis, 2);
  }

  @Test public void disassembleAndDeadCodeElimintaionIntArrayO1 () throws IOException {
    String file = rootPath + "array-sub-int.O1";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    Cfa main = analysis.getCompositeCfa().lookupCfa("main").get();
    LivenessAnalysis liveness = new LivenessAnalysis(analysis.getPlatform());
    LivenessAnalysisCtx bla = liveness.runWithResults(main);
    Cfa main_ = bla.getCfa(main.getEntryAddress());
    assertCfasReconstructed(analysis, 2);
  }

  @Test public void disassembleAndDeadCodeElimintaionIsqrt () throws IOException {
    String file = rootPath + "isqrt";
    ReconstructionCallString<?> analysis = ReconstructionCallString.runElf(file);
    Cfa main = analysis.getCompositeCfa().lookupCfa("main").get();
    LivenessAnalysis liveness = new LivenessAnalysis(analysis.getPlatform());
    LivenessAnalysisCtx bla = liveness.runWithResults(main);
    Cfa main_ = bla.getCfa(main.getEntryAddress());
    assertCfasReconstructed(analysis, 1);
  }

  private static void assertCfasReconstructed (ReconstructionCallString<?> analysis, int value) {
    assertThat(analysis.getCompositeCfa().cfaForest().size(), is(value));
  }
}
