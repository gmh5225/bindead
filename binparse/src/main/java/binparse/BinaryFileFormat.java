package binparse;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javalx.exceptions.UnimplementedException;
import binparse.elf.ElfBinary;
import binparse.pe.PEBinary;
import binparse.rreil.RReilBinary;

/**
 * The various types of formats for executable code that are used on different platforms.
 */
public enum BinaryFileFormat {
  ELF,          // UNIX, LINUX
  PE,           // Windows
  RREIL,        // RREIL assembler file
  MACHO,        // MacOS
  TRACE;        // Trace of an execution of a binary

  /**
   * Instantiate the right binary wrapper format for a file given by its path.
   *
   * @param fileName A path to a binary executable file
   * @return A binary wrapper for the executable
   * @throws IOException on any read/write errors or if the file is not found
   */
  public static Binary getBinary (String fileName) throws IOException {
    BinaryFileFormat format = getFormatOf(fileName);
    switch (format) {
    case RREIL:
      return new RReilBinary(fileName);
    case ELF:
      return new ElfBinary(fileName);
    case PE:
      return new PEBinary(fileName);
    case MACHO:
      throw new UnimplementedException("Support for Mach-O binaries not yet implemented.");
    case TRACE:
      throw new UnimplementedException("Support for traces not yet implemented."); // TODO: implement!
    default:
      assert false;
    }
    assert false;
    return null;
  }

  /**
   * Return the format of the binary given by a file path.
   * @throws IOException on any read/write errors or if the file is not found
   */
  public static BinaryFileFormat getFormatOf (String file) throws IOException {
    if (getFileExtension(file).equals("rreil"))
      return RREIL;

    InputStream stream = new BufferedInputStream(new FileInputStream(file));
    // If we can't read ahead safely, just give up on guessing
    if (!stream.markSupported()) {
      stream.close();
      return null;
    }
    stream.mark(4);
    int c1 = stream.read();
    int c2 = stream.read();
    int c3 = stream.read();
    int c4 = stream.read();
    stream.reset();
    stream.close();

    // 7F 45 4C 46      - ELF magic first 4 bytes "<DEL>ELF"
    if (c1 == 0x7F && c2 == 0x45 && c3 == 0x4C && c4 == 0x46)
      return ELF;
    // 4D 5A            - PE magic first 2 bytes "MZ"
    if (c1 == 0x4D && c2 == 0x5A)
      return PE;
    return null;
  }

  private static String getFileExtension (String file) {
    String separator = System.getProperty("file.separator");
    String filename;
    // remove the path up to the filename
    int lastSeparatorIndex = file.lastIndexOf(separator);
    if (lastSeparatorIndex == -1)
      filename = file;
    else
      filename = file.substring(lastSeparatorIndex + 1);
    // remove the extension
    int extensionIndex = filename.lastIndexOf(".");
    if (extensionIndex == -1)
      return filename;
    return filename.substring(extensionIndex + 1);
  }

}
