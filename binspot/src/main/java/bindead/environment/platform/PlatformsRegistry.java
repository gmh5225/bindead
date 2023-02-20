package bindead.environment.platform;

import java.util.HashMap;
import java.util.Map;

import bindead.analyses.algorithms.AnalysisProperties;
import binparse.AbstractBinary;
import binparse.Binary;
import binparse.rreil.RReilBinary;

/**
 * Collection of available platform configurations.
 *
 * @author Bogdan Mihaila
 */
public class PlatformsRegistry {
  private static Map<String, Platform> legacyMap = new HashMap<String, Platform>();
  private static Map<String, Platform> gdslMap = new HashMap<String, Platform>();
  static {
    legacyMap.put(AbstractBinary.x86_32, LegacyX86_32Platform.instance());
    legacyMap.put(AbstractBinary.x86_64, LegacyX86_64Platform.instance());
    // newer native disassembly library GDSL
    try {
      gdslMap.put(AbstractBinary.x86_32, GdslX86_32Platform.instance());
      gdslMap.put(AbstractBinary.x86_64, GdslX86_64Platform.instance());
    } catch (UnsatisfiedLinkError | Exception e) { // might not work if the native libraries are not present
      // TODO: find a better way to see if we have the GDSL frontend available
    }
  }

  /**
   * Retrieve a suitable platform configuration for the given binary or {@code null} if none fits.
   */
  public static Platform get (Binary file) {
    Platform platform = null;
    if (file.getArchitectureName().equals(RReilBinary.rreilArch)) {
      // special case for RREIL assembler files wrapped as a binary
      platform = new RReilPlatform(file.getArchitectureSize(), ((RReilBinary) file).getInstructions());
    } else if (AnalysisProperties.INSTANCE.useGDSLDisassembler.isTrue()) {
      if (gdslMap.isEmpty())
        throw new IllegalStateException("GDSL requested as disassembler frontend but no "
          + "GDSL frontends found in library path or could not load/initialize any.");
      platform = gdslMap.get(file.getArchitectureName());
    } else {
      platform = legacyMap.get(file.getArchitectureName());
    }
    if (platform == null)
      throw new IllegalArgumentException("Could not instantiate a platform for " +
        file.getArchitectureName() + "for the binay: " + file.getFileName());

    return platform;
  }

}
