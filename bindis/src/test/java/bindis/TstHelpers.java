package bindis;

import java.io.IOException;

import org.eclipse.cdt.utils.coff.Coff;
import org.eclipse.cdt.utils.coff.PE;

import binparse.Binary;
import binparse.elf.ElfBinary;


/**
 *
 * @author mb0
 */
public final class TstHelpers {
  public static byte[] pack (int... opcodes) {
    byte[] bytes = new byte[opcodes.length];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) opcodes[i];
    }
    return bytes;
  }

  public static byte[] loadCodeSegmentElf (String filePath) throws IOException {
    Binary elf = new ElfBinary(filePath);
    return elf.getSegment(".text").get().getData();
  }

  public static byte[] loadCodeSegmentPE (String filePath) throws IOException {
    final PE pe = new PE(filePath);
    final Coff.SectionHeader sh[] = pe.getSectionHeaders();
    return sh[0].getRawData();
  }
}
