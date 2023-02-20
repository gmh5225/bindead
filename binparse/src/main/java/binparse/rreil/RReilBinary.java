package binparse.rreil;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.FiniteRange;
import rreil.assembler.CompiledAssembler;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import binparse.AbstractBinary;
import binparse.BinaryFileFormat;
import binparse.BinaryType;
import binparse.Endianness;
import binparse.Permission;
import binparse.Segment;
import binparse.SegmentImpl;
import binparse.Symbol;

/**
 * Wrapper / Adapter class providing the {@link binparse.Binary} interface for RREIL assembler source code files.
 *
 * @author Bogdan Mihaila
 */
public class RReilBinary extends AbstractBinary {
  public static final String rreilArch = "rreil-vm";
  private static final String no_file_backing = "<rreil-assembler-text>";
  private final File sourceFile;
  private final CompiledAssembler compilationUnit;

  public RReilBinary (CompiledAssembler compilationUnit) {
    this.compilationUnit = compilationUnit;
    this.sourceFile = null;
  }

  public RReilBinary (String fileName) throws FileNotFoundException {
    sourceFile = new File(fileName);
    Scanner scanner = new Scanner(sourceFile);
    String fileContent = scanner.useDelimiter("\\A").next();
    scanner.close();
    compilationUnit = CompiledAssembler.from(fileContent);
  }

  public static RReilBinary fromString (String assemblerSourceCode) {
    return new RReilBinary(CompiledAssembler.from(assemblerSourceCode));
  }

  @Override public Option<File> getFile () {
    if (sourceFile != null)
      return Option.some(sourceFile);
    else
      return Option.none();
  }

  @Override public String getFileName () {
    return sourceFile != null ? sourceFile.getName() : no_file_backing;
  }

  @Override public String getArchitectureName () {
    return rreilArch;
  }

  @Override public int getArchitectureSize () {
    return compilationUnit.getDefaultSize();
  }

  @Override public Endianness getEndianness () {
    return Endianness.LITTLE;
  }

  @Override public BinaryType getType () {
    return BinaryType.Executable;
  }

  @Override public BinaryFileFormat getFileFormat () {
    return BinaryFileFormat.RREIL;
  }

  @Override public long getEntryAddress () {
    RReilAddr entry = compilationUnit.entryAddress();
    assert entry.offset() == 0 : "invalid compilation unit entry address";
    return entry.base();
  }

  @Override public boolean hasDebugInformation () {
    return false;
  }

  @Override public Option<Symbol> getMainFunction () {
    return getSymbol("main");
  }

  @Override public String toString () {
    return sourceFile != null ? sourceFile.getPath() : no_file_backing;
  }

  /**
   * Initialize an artificial segment for the instructions as the analysis needs to allocate the segment
   * data and look for for offsets into this data when disassembling instructions.
   */
  @Override protected void initSegments () {
    List<Segment> segments = new ArrayList<Segment>();
    String name = ".text"; // similar to ELF instructions reside in the .text segment
    long firstAddress = compilationUnit.minInstructionAddress().base();
    long lastAddress = compilationUnit.maxInstructionAddress().base();
    assert firstAddress <= lastAddress;
    long size = lastAddress - firstAddress + 1;
    byte[] data = new byte[0];
    Set<Permission> permissions = Permission.$ReadExecute;
    SegmentImpl segment = new SegmentImpl(getFileName(), name, firstAddress, size, data, getEndianness(), permissions);
    segments.add(segment);
    segmentNames.put(name, segment);
    segmentAddresses = segmentAddresses.bind(FiniteRange.of(firstAddress, lastAddress + 1), segment);
    this.segments = segments;
  }

  @Override protected void initExportedSymbols () {
    List<Symbol> symbols = new ArrayList<Symbol>();
    for (P2<RReilAddr, String> entry : compilationUnit.getLabels()) {
      String name = entry._2();
      long address = entry._1().base();
      Symbol symbol = new RReilSymbol(name, address, getArchitectureSize());
      symbols.add(symbol);
      symbolNames.put(name, symbol);
      // if we have more symbols with different RREIL addresses that share the same base address, we will have to give up
      if (symbolAddresses.containsKey(address))
        throw new IllegalArgumentException("Two symbols have the same base address: " + address +
          ". Symbol names are " + symbolAddresses.get(address).getNameOrAddress() + " and " + symbol.getNameOrAddress());
      symbolAddresses.put(address, symbol);
    }
    exportedSymbols = symbols;
  }

  @Override protected void initImportedSymbols () {
    importedSymbols = Collections.emptyList();
  }

  /**
   * Return the instructions that are in the RREIL assembly file.
   */
  public SortedMap<RReilAddr, RReil> getInstructions () {
    return compilationUnit.getInstructions();
  }
}
