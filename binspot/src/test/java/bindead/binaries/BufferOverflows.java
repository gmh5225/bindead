package bindead.binaries;

import static bindead.TestsHelper.assertHasWarnings;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.AnalysisFactory.AnalysisDebugHooks;
import bindead.debug.DebugHelper;
import binparse.Binary;

/**
 * Test the overwriting of the return address and how the analysis handles it.
 *
 * @author Bogdan Mihaila
 */
public class BufferOverflows {
  private final static AnalysisFactory analyzer = new AnalysisFactory().disableDomains("Heap", "Undef");

//  private static AnalysisDebugHooks debugger = null;

  private static final AnalysisDebugHooks debugger =
    DebugHelper.combine(DebugHelper.printers.instructionEffect(),
        DebugHelper.printers.domainDumpBoth());

  @BeforeClass public static void init () {
    DebugHelper.analysisKnobs.useGDSLasFrontendAndOptimize();
//    DebugHelper.analysisKnobs.useGDSLasFrontend();
  }

  @AfterClass public static void teardown () {
    DebugHelper.analysisKnobs.useLegacyFrontend();
  }

  /**
   * Silence any debug output that was enabled by previous tests.
   */
  @Before public void silence () {
    DebugHelper.analysisKnobs.disableAll();
  }

  @Before public void enableDebugger () {
    DebugHelper.analysisKnobs.enableCommon();
    DebugHelper.analysisKnobs.printWidening();
    DebugHelper.analysisKnobs.printMemVarOnly();

    DebugHelper.analysisKnobs.printCodeListing();
    DebugHelper.analysisKnobs.printSummary();
  }

  @Test public void returnAddressOverwritten () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("return-address-overwrite");
//    Bindead.main(new String[]{"-sym", "-dyn", binary.getFile().get().getCanonicalPath()});
    Analysis<?> analysis =
      analyzer.runAnalysis(binary, DebugHelper.combine(debugger, DebugHelper.breakpoints.triggerBeforeInsn("08048401.02")));
    assertHasWarnings(4, analysis);
  }

}
