package bindead.analyses.systems;

import java.util.HashMap;
import java.util.Map;

import bindead.analyses.systems.SystemModel.ESystemType;
import bindead.environment.abi.ABI;
import bindead.environment.abi.RReilABI;
import bindead.environment.abi.X32SysVAbi;
import bindead.environment.abi.X64SysVAbi;
import bindead.environment.platform.Platform;
import bindead.environment.platform.PlatformsRegistry;
import binparse.AbstractBinary;
import binparse.Binary;
import binparse.rreil.RReilBinary;

public class SystemModelRegistry {
  private static final Map<ESystemType, Ctor> map = new HashMap<ESystemType, Ctor>();
  static {
    map.put(ESystemType.LINUX, new LinuxCtor());
  }

  private static SystemModel getModel (ESystemType type, Binary binary) {
    Ctor systemCtor = map.get(type);
    Platform platform = PlatformsRegistry.get(binary);
    return systemCtor.buildFor(platform, binary);
  }

  public static GenericSystemModel getLinuxModel (Binary file) {
    if (file.getArchitectureName().equals(RReilBinary.rreilArch))
      return SystemModelRegistry.getRReilModel(file);
    else
      return (LinuxX86Model) getModel(ESystemType.LINUX, file);
  }

  public static GenericSystemModel getRReilModel (Binary binary) {
    Platform platform = PlatformsRegistry.get(binary);
    String platformName = platform.getName();
    assert platformName.equals(RReilBinary.rreilArch);
    return new RReilSystemModel(new RReilABI(platform));
  }

  private interface Ctor {
    SystemModel buildFor (Platform platform, Binary binary);
  }

  private static class LinuxCtor implements Ctor {
    @Override public SystemModel buildFor (Platform platform, Binary binary) {
      String platformName = platform.getName();
      ABI abi;
      if (platformName.toLowerCase().startsWith(AbstractBinary.x86_32)) {
        abi = new X32SysVAbi(platform);
      } else if (platformName.toLowerCase().startsWith(AbstractBinary.x86_64)) {
        abi = new X64SysVAbi(platform);
      } else {
        throw new IllegalArgumentException("No linux model for platform: " + platformName);
      }
      return new LinuxX86Model(abi, binary);
    }
  }
}
