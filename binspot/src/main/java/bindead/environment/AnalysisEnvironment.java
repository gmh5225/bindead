package bindead.environment;

import bindead.analyses.callback.Callbacks;
import bindead.analyses.systems.SystemModel;
import bindead.environment.abi.ABI;
import bindead.environment.platform.Platform;

/**
 * The model of the environment an analysis is carried out in, consisting of:
 * <ul>
 * <li> {@link Platform}</li>
 * <li> {@link ABI}</li>
 * <li> {@link SystemModel}</li>
 * <li> {@link Callbacks}</li>
 * </ul>
 */
public class AnalysisEnvironment {
  private final Platform platform;
  private final SystemModel systemModel;
  private final Callbacks callbacks;

  public AnalysisEnvironment (Platform platform) {
    this(platform, null);
  }

  public AnalysisEnvironment (Platform platform, Callbacks callbacks) {
    this.platform = platform;
    this.systemModel = null;
    this.callbacks = callbacks;
  }

  public AnalysisEnvironment (SystemModel systemModel) {
    this(systemModel, null);
  }

  public AnalysisEnvironment (SystemModel systemModel, Callbacks callbacks) {
    this.platform = systemModel.getPlatform();
    this.systemModel = systemModel;
    this.callbacks = callbacks;
  }

  public Platform getPlatform () {
    return platform;
  }

  public SystemModel getSystemModel () {
    return systemModel;
  }

  public Callbacks getCallbacks () {
    return callbacks;
  }

  public ABI getABI () {
    return getSystemModel() == null ? null : getSystemModel().getABI();
  }
}
