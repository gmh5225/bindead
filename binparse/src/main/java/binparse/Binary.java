package binparse;

import java.io.File;
import java.util.List;

import javalx.data.Option;

/**
 * Simple interface abstracting access to binary object files.
 */
public interface Binary {

  /**
   * @return The file object for this binary if there is one.
   */
  public Option<File> getFile ();

  /**
   * @return The name of this binary object.
   */
  public String getFileName ();

  /**
   * @return The type of binary file that this file represents, e.g. ELF, PE ...
   */
  public BinaryFileFormat getFileFormat ();

  /**
   * @return The type of the file, e.g. executable, shared library ...
   */
  public BinaryType getType ();

  /**
   * @return An identifier for the CPU architecture type that this binary is meant for.
   */
  public String getArchitectureName ();

  /**
   * @return The size in bits of the platform this binary should be executed on (e.g. 8, 16, 32, 64).
   */
  public int getArchitectureSize ();

  /**
   * @return The endianness of the data segments in this binary.
   */
  public Endianness getEndianness ();

  /**
   * @return The entry point when executing the binary.
   */
  public long getEntryAddress ();

  /**
   * Convenience method to retrieve the main function from the binary if there exists one.
   * @return The main function or none if there does not exist one.
   */
  public Option<Symbol> getMainFunction ();

  /**
   * @return If this file contains debug information or not.
   */
  public boolean hasDebugInformation ();

  /**
   * @return All segments in this binary.
   */
  public List<Segment> getSegments ();

  /**
   * @param name A name to find a segment for.
   * @return The segment with the given name if there exists one.
   */
  public Option<Segment> getSegment (String name);

  /**
   * @param address An address to find a segment for.
   * @return The segment that contains the given address. The segment does not necessarily have to start at this address.
   */
  public Option<Segment> getSegment (long address);

  /**
   * @param symbol A symbol for which to find the segment it is part of.
   * @return The segment that contains the given symbol.
   */
  public Segment getSegment (Symbol symbol);

  /**
   * @return The symbols that are part of this binary and are exported/visible. Note that some binaries can be "stripped"
   * --i.e. the symbol table has been removed -- in which case they do not export any static symbol.
   * REFACTOR: how should we name these? static/dynamic (binutils/objdump)? imported/exported (radare) or provided/needed
   */
  public List<Symbol> getExportedSymbols ();

  /**
   * @return The symbols that are needed from other binaries. This file needs to be linked against other binaries that
   * will provide/export these symbols.
   * REFACTOR: how should we name these? static/dynamic (binutils/objdump)? imported/exported (radare) or provided/needed
   */
  public List<Symbol> getImportedSymbols ();

  /**
   * @param name A name to find a symbol for.
   * @return A symbol with the given name if there exists one. Note that there can be more symbols with the same name. To
   * make sure you get a unique symbol use {@link #getSymbol(long)} to retrieve symbols by their addresses.
   */
  public Option<Symbol> getSymbol (String name);

  /**
   * @param address An address to find a symbol for.
   * @return The symbol for the given address if there exists one.
   */
  public Option<Symbol> getSymbol (long address);

}
