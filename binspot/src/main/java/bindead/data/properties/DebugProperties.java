package bindead.data.properties;


public class DebugProperties {
  static final String keyFmtDebug = "domains.%s.debug.%s";
  static final String keyFmtOption = "domains.%s.option.%s";
  public final BoolProperty debugSummary;
  public final BoolProperty debugAssignments;
  public final BoolProperty debugTests;
  public final BoolProperty debugBinaryOperations;
  public final BoolProperty debugWidening;
  public final BoolProperty debugSubsetOrEqual;
  public final BoolProperty debugQueries;
  public final BoolProperty debugOther;
  public final BoolProperty checkState;

  public DebugProperties (String name) {
    debugSummary = makeDebugProperty(name, "summary");
    debugAssignments = makeDebugProperty(name, "assignments");
    debugTests = makeDebugProperty(name, "tests");
    debugBinaryOperations = makeDebugProperty(name, "binaryoperations");
    debugSubsetOrEqual = makeDebugProperty(name, "subsetorequal");
    debugWidening = makeDebugProperty(name, "widening");
    debugQueries = makeDebugProperty(name, "queries");
    debugOther = makeDebugProperty(name, "other");
    checkState = makeDebugProperty(name, "checkstate");
  }

  /**
   * Sets all debug properties--i.e. the ones prefixed with "debug"--to {@code true}.
   */
  public void enableAllDebugSwitches () {
    debugAssignments.setValue(true);
    debugTests.setValue(true);
    debugBinaryOperations.setValue(true);
    debugWidening.setValue(true);
    debugSubsetOrEqual.setValue(true);
    debugQueries.setValue(true);
    debugOther.setValue(true);
  }

  /**
   * Sets all debug properties--i.e. the ones prefixed with "debug"--to {@code false}.
   */
  public void disableAllDebugSwitches () {
    debugAssignments.setValue(false);
    debugTests.setValue(false);
    debugBinaryOperations.setValue(false);
    debugWidening.setValue(false);
    debugSubsetOrEqual.setValue(false);
    debugQueries.setValue(false);
    debugOther.setValue(false);
  }

  protected static BoolProperty makeDebugProperty (String name, String assignments) {
    return new BoolProperty(String.format(keyFmtDebug, name.toLowerCase(), assignments));
  }

  protected static BoolProperty makeOptionProperty (String name, String assignments) {
    return new BoolProperty(String.format(keyFmtOption, name.toLowerCase(), assignments));
  }

}