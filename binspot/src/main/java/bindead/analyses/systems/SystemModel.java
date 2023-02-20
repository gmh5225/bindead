package bindead.analyses.systems;

import bindead.environment.abi.ABI;
import bindead.environment.platform.Platform;

public abstract class SystemModel implements INativeHandler, INativeFunctionHandler {
  enum ESystemType {
    LINUX, RREIL;
  }

  private final ESystemType type;
  protected final ABI abi;

  protected SystemModel (ESystemType type, ABI abi) {
    this.type = type;
    this.abi = abi;
  }


  public ABI getABI () {
    return abi;
  }

  public Platform getPlatform () {
    return abi.getPlatform();
  }

  public ESystemType getSystemType () {
    return type;
  }
}
