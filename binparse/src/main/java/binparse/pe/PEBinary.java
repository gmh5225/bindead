package binparse.pe;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javalx.data.Option;
import javalx.numeric.FiniteRange;

import org.eclipse.cdt.utils.coff.Coff;
import org.eclipse.cdt.utils.coff.Coff.SectionHeader;
import org.eclipse.cdt.utils.coff.PE;

import binparse.AbstractBinary;
import binparse.Binary;
import binparse.BinaryFactory;
import binparse.BinaryFileFormat;
import binparse.BinaryType;
import binparse.Endianness;
import binparse.Permission;
import binparse.Segment;
import binparse.SegmentImpl;
import binparse.Symbol;
import binparse.UncheckedIOException;

/**
 * Wrapper / Adapter class providing the {@link binparse.Binary} interface for {@code PE} files.
 */
public class PEBinary extends AbstractBinary {
  private final PE pe;
  private final static Map<String, String> cpuTranslation = new HashMap<String, String>();
  private final static Map<String, Integer> architectureSizeTranslation = new HashMap<String, Integer>();
  static {
    cpuTranslation.put("x86", x86_32);
    cpuTranslation.put("amd64", x86_64);
    architectureSizeTranslation.put(x86_32, 32);
    architectureSizeTranslation.put(x86_64, 64);
  }

  private final static BinaryFactory factory = new BinaryFactory() {
    @Override public Binary getBinary (String path) throws IOException {
      return new PEBinary(path);
    }
  };

  public PEBinary (PE file) {
    this.pe = file;
  }

  public PEBinary (String file) throws IOException {
    this(new PE(file));
  }

  public static BinaryFactory getFactory () {
    return factory;
  }

  @Override public Option<File> getFile () {
    return Option.some(new File(pe.getFilename()));
  }

  @Override public String getFileName () {
    return new File(pe.getFilename()).getName();
  }

  @Override public String getArchitectureName () {
    try {
      return cpuTranslation.get(pe.getAttribute().getCPU());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override public Endianness getEndianness () {
    Endianness endianness;
    try {
      endianness = pe.getAttribute().isLittleEndian() ? Endianness.LITTLE : Endianness.BIG;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return endianness;
  }

  @Override public BinaryType getType () {
    try {
      return BinaryType.values()[pe.getAttribute().getType() - 1];
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override public BinaryFileFormat getFileFormat () {
    return BinaryFileFormat.PE;
  }

  @Override public long getEntryAddress () {
    return pe.getOptionalHeader().entry + (long) pe.getNTOptionalHeader().ImageBase;
  }

  @Override public Option<Symbol> getMainFunction () {
    return getSymbol("_main");
  }

  @Override public boolean hasDebugInformation () {
    try {
      return pe.getAttribute().hasDebug();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override public int getArchitectureSize () {
    return architectureSizeTranslation.get(getArchitectureName());
  }

  @Override protected void initSegments () {
    int IMAGE_SCN_MEM_EXECUTE = 0x20000000;
    int IMAGE_SCN_MEM_READ = 0x40000000;
    int IMAGE_SCN_MEM_WRITE = 0x80000000;
    List<Segment> segments = new ArrayList<Segment>();
    try {
      for (SectionHeader peSection : pe.getSectionHeaders()) {
        String name = new String(peSection.s_name).trim(); // 8-bytes array padded with nulls needs trimming
        long address = peSection.s_vaddr + pe.getNTOptionalHeader().ImageBase;
        long size = peSection.s_size;
        byte[] data = peSection.getRawData();
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        if ((peSection.s_flags & IMAGE_SCN_MEM_READ) != 0)
          permissions.add(Permission.Read);
        if ((peSection.s_flags & IMAGE_SCN_MEM_EXECUTE) != 0)
          permissions.add(Permission.Execute);
        if ((peSection.s_flags & IMAGE_SCN_MEM_WRITE) != 0)
          permissions.add(Permission.Write);
        SegmentImpl segment = new SegmentImpl(getFileName(), name, address, size, data, getEndianness(), permissions);
        segments.add(segment);
        segmentNames.put(name, segment);
        segmentAddresses = segmentAddresses.bind(FiniteRange.of(address, address + segment.getSize() - 1), segment);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    this.segments = segments;
  }

  @Override protected void initExportedSymbols () {
    int STORAGE_CLASS_EXTERNAL = 2;
    List<Symbol> symbols = new ArrayList<Symbol>();
    try {
      Coff.Symbol[] peSymbols = pe.getSymbols();
      for (int i = 0; i < peSymbols.length; i++) {
        Coff.Symbol peSymbol = peSymbols[i];
        // only use function symbols, see PE Spec: Auxiliary symbol records
        if (!(peSymbol.n_sclass == STORAGE_CLASS_EXTERNAL && peSymbol.isFunction() && peSymbol.n_scnum > 0))
          continue;
        String name = peSymbol.getName(pe.getStringTable());
        // FIXME: this is probably wrong and needs to be read from the addresses array and added to the base address
        long address = peSymbol.n_value;
        long size = 0;
        if (peSymbol.n_numaux == 1) {
          peSymbol = peSymbols[++i];
          size = ByteBuffer.wrap(peSymbol._n_name, 4, 4).getInt(); // parse an int because this is an auxiliary record
        }
        byte[] data = new byte[0];
        PESymbol symbol = new PESymbol(name, address, size, data);
        symbols.add(symbol);
        symbolNames.put(name, symbol);
        symbolAddresses.put(address, symbol);
      }
      exportedSymbols = symbols;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override protected void initImportedSymbols () {
    // TODO: extract imported symbols and initialize them
    importedSymbols = new ArrayList<Symbol>();
  }

  @Override public String toString () {
    return pe.getFilename();
  }

}
