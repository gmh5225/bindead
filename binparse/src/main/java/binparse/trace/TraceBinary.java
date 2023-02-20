package binparse.trace;

import java.io.File;
import java.util.ArrayList;

import javalx.data.Option;
import javalx.numeric.FiniteRange;
import binparse.AbstractBinary;
import binparse.Binary;
import binparse.BinaryFileFormat;
import binparse.BinaryType;
import binparse.Endianness;
import binparse.Segment;
import binparse.Symbol;

/**
 * Wrapper / Adapter class providing the {@link binparse.Binary} interface for trace files.
 */
public class TraceBinary extends AbstractBinary {
  private final Binary tracedBinary;
  private final TraceDump traceDump;

  public TraceBinary (String tracesPrefix, Binary tracedBinary) {
    this.tracedBinary = tracedBinary;
    traceDump = new TraceDump(tracesPrefix, tracedBinary);
  }

  @Override public Option<File> getFile () {
    return Option.none();
  }

  @Override public String getFileName () {
    return tracedBinary.getFileName();
  }

  @Override public String getArchitectureName () {
    return tracedBinary.getArchitectureName();
  }

  @Override public int getArchitectureSize () {
    return tracedBinary.getArchitectureSize();
  }

  @Override public Endianness getEndianness () {
    return tracedBinary.getEndianness();
  }

  @Override public BinaryType getType () {
    return BinaryType.TraceDump;
  }

  @Override public BinaryFileFormat getFileFormat () {
    return BinaryFileFormat.TRACE;
  }

  @Override public long getEntryAddress () {
    return tracedBinary.getEntryAddress();
  }

  @Override public boolean hasDebugInformation () {
    return tracedBinary.hasDebugInformation();
  }

  @Override public Option<Symbol> getMainFunction () {
    return tracedBinary.getMainFunction();
  }

  public TraceDump getTraceDump () {
    return traceDump;
  }

  public Binary getTracedBinary () {
    return tracedBinary;
  }

  @Override protected void initSegments () {
    segments = new ArrayList<Segment>();
    for (Segment segment : traceDump.getSegments()) {
      segments.add(segment);
      if (segment.getName().isSome())
        segmentNames.put(segment.getName().get(), segment);
      long address = segment.getAddress();
      segmentAddresses = segmentAddresses.bind(FiniteRange.of(address, address + segment.getSize() - 1), segment);
    }
  }

  @Override protected void initExportedSymbols () {
    exportedSymbols = tracedBinary.getExportedSymbols();
    for (Symbol symbol : exportedSymbols) {
      symbolAddresses.put(symbol.getAddress(), symbol);
      symbolNames.put(symbol.getName().getOrNull(), symbol);
    }
  }

  @Override protected void initImportedSymbols () {
    importedSymbols = tracedBinary.getImportedSymbols();
    for (Symbol symbol : importedSymbols) {
      symbolAddresses.put(symbol.getAddress(), symbol);
      symbolNames.put(symbol.getName().getOrNull(), symbol);
    }
  }

}
