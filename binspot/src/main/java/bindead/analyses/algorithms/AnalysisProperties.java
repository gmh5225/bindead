package bindead.analyses.algorithms;

import bindead.data.properties.BoolProperty;
import bindead.data.properties.DebugProperties;

public class AnalysisProperties extends DebugProperties {
  private static final String keyFmt = "analysis.%s.debug.%s";
  public static final String NAME = "ANALYSIS";
  public static final AnalysisProperties INSTANCE = new AnalysisProperties();
  public final BoolProperty debugNativeCode;
  public final BoolProperty debugRReilCode;
  public final BoolProperty debugWarnings;
  public final BoolProperty debugTests;
  public final BoolProperty disassembleBlockWise;
  public final BoolProperty useGDSLDisassembler;
  public final BoolProperty skipDisassembleErrors;
  public final BoolProperty ignoreNonExistentJumpTargets;

  /**
   * Do not perform a depth-first search but always take the lowest address
   * in the worklist to process it next. Simple heuristic to process both if-else branches
   * and whole loops before continuing with following code. It leads to less iterations as
   * the dependencies of a program point are processed before processing the program point itself.
   * However, this simple heuristic is broken whenever calls jump to higher/later addresses.
   */
  public final BoolProperty processAddressesInOrder;

  private AnalysisProperties () {
    super(NAME);
    debugNativeCode = new BoolProperty(String.format(keyFmt, NAME.toLowerCase(), "debugNativeCode"));
    debugRReilCode = new BoolProperty(String.format(keyFmt, NAME.toLowerCase(), "debugRReilCode"));
    debugWarnings = new BoolProperty(String.format(keyFmt, NAME.toLowerCase(), "debugWarnings"));
    debugTests = new BoolProperty(String.format(keyFmt, NAME.toLowerCase(), "debugTests"));
    useGDSLDisassembler = new BoolProperty(String.format(keyFmt, NAME.toLowerCase(), "useGDSLDisassembler"));
    disassembleBlockWise = new BoolProperty(String.format(keyFmt, NAME.toLowerCase(), "disassembleBlockWise"));
    skipDisassembleErrors = new BoolProperty(String.format(keyFmt, NAME.toLowerCase(), "skipDisassembleErrors"));
    ignoreNonExistentJumpTargets =
      new BoolProperty(String.format(keyFmt, NAME.toLowerCase(), "ignoreNonExistentJumpTargets"));
    processAddressesInOrder = new BoolProperty(String.format(keyFmt, NAME.toLowerCase(), "processAddressesInOrder"));
    processAddressesInOrder.setValue(true); // on by default
  }
}