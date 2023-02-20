package bindead.domainnetwork.interfaces;

import javalx.data.Option;
import bindead.domainnetwork.channels.WarningMessage;
import bindead.domainnetwork.channels.WarningsContainer;
import bindead.environment.AnalysisEnvironment;

/**
 * Manage the context of the current analysis. Used to inject data by the fixpoint
 * into the domains that might be useful for some but not used by all domains. As this collects all such
 * data we do not need to add extra parameters to the transfer functions of the domains where such data
 * is needed.
 *
 * @author Bogdan Mihaila
 */
public class AnalysisCtx {
  private final Option<ProgramPoint> location;
  private final AnalysisEnvironment environment;
  private final WarningsContainer warnings;

  public AnalysisCtx (AnalysisEnvironment environment) {
    this(ProgramPoint.nowhere, environment, new WarningsContainer());
  }

  public AnalysisCtx (Option<ProgramPoint> location, AnalysisEnvironment environment, WarningsContainer warnings) {
    this.location = location;
    this.environment = environment;
    this.warnings = warnings;
  }

  /**
   * Start with an unknown platform, location but with a warnings channel set.
   * Useful for initialization of domains in tests
   * where most of the context is not needed but a warnings channel is.
   */
  public static AnalysisCtx unknown () {
    return new AnalysisCtx(null);
  }

  /**
   * Return this context but with the location information removed.
   * Useful when performing a transfer function that should not depend on the location.
   */
  public AnalysisCtx withoutLocation () {
    return new AnalysisCtx(ProgramPoint.nowhere, environment, warnings);
  }

  /**
   * @return The current location the fixpoint analysis is at if it is known.
   */
  public Option<ProgramPoint> getLocation () {
    return location;
  }

  /**
   * Get the platform and other environment specific settings objects.
   */
  public AnalysisEnvironment getEnvironment () {
    return environment;
  }

  /**
   * Access the channel that is used to post warnings during domain operations.
   *
   * As this channel is mutable and can be reseted at any time and usually will be,
   * the only reliable way to access the warnings for a state later on
   * is to collect them outside of this channel after each operation that may produce warnings.
   * This especially means that this method is not returning anything useful after an analysis
   * but the analysis itself must be queried for the warning hat occurred.
   */
  public WarningsContainer getWarningsChannel () {
    return warnings;
  }

  /**
   * Post a new warnings message on the warnings channel.
   */
  public void addWarning (WarningMessage warning) {
    warnings.addWarning(warning);
  }

  @Override public String toString () {
    return "ctx:\n" + getLocation() + "\n" + getWarningsChannel();
  }
}
