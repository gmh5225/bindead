package bindead.binaries;

import static bindead.TestsHelper.lines;

import java.io.IOException;

import org.junit.Test;

import bindead.TestsHelper;
import bindead.analyses.AnalysisFactory;
import binparse.Binary;

/**
 * Test the analysis of RREIL assembler files.
 *
 * @author Bogdan Mihaila
 */
public class RReilBinaries {
  private final static AnalysisFactory analyzer = new AnalysisFactory();

  @Test public void testRReilExample () {
    //  int i = 0;
    //  int j = 0;
    //  while (true) {
    //    if (i <= 50)
    //      j++;
    //    else
    //      j--;
    //    if (j < 0)
    //      break;
    //    i++;
    //  }
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov i, 0",
        "mov j, 0",
        "loop:",
        "  cmples LES, i, 50",
        "  brc LES, else:",
        "    sub j, j, 1",
        "    br if_end:",
        "  else:",
        "    add j, j, 1",
        "  if_end:",
        "  cmplts LTS, j, 0",
        "  brc LTS, exit:",
        "  add i, i, 1",
        "  br loop:",
        "exit:",
        "halt"
      );
    analyzer.runAnalysis(assembly);
  }

  @Test public void testRReilExampleFromFile () throws IOException {
    Binary binary = TestsHelper.get32bitExamplesBinary("RReilExample.rreil");
    analyzer.runAnalysis(binary);
  }
}
