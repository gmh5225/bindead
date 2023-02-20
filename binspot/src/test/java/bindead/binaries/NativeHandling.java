package bindead.binaries;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.debug.DebugHelper;
import binparse.Binary;

public class NativeHandling {
  private final static AnalysisFactory analyzer = new AnalysisFactory();

  /**
   * Silence any debug output that was enabled by previous tests.
   */
  @Before public void silence () {
    DebugHelper.analysisKnobs.disableAll();
  }

  @Ignore // FIXME: syscall 43 not implemented yet
  @Test public void Syscall () throws IOException {
    DebugHelper.analysisKnobs.enableCommon();
    Binary binary = TestsHelper.get32bitExamplesBinary("Syscall");
    String[] vars = {"ebx", "t0", "eax", "esp", "t1"};
    Analysis<?> analysis = analyzer.runAnalysis(binary, DebugHelper.printers.variablesWatcher(null, null, vars), null);
  }
}
