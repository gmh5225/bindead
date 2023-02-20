package binparse.elf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javalx.data.Option;
import javalx.numeric.FiniteRange;

import org.eclipse.cdt.utils.elf.Elf;
import org.eclipse.cdt.utils.elf.Elf.Dynamic;
import org.eclipse.cdt.utils.elf.Elf.Rela_Relocation;

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
 * Wrapper / Adapter class providing the {@link binparse.Binary} interface for {@code Elf} files.
 */
public class ElfBinary extends AbstractBinary {
  private final Elf elf;
  private final static Map<String, String> cpuTranslation = new HashMap<String, String>();
  static {
    cpuTranslation.put("x86", x86_32);
    cpuTranslation.put("x86_64", x86_64);
  }

  private final static BinaryFactory factory = new BinaryFactory() {
    @Override public Binary getBinary (String path) throws IOException {
      return new ElfBinary(path);
    }
  };

  public ElfBinary (Elf file) {
    this.elf = file;
  }

  public ElfBinary (String file) throws IOException {
    this(new Elf(file));
  }

  public static BinaryFactory getFactory () {
    return factory;
  }

  @Override public Option<File> getFile () {
    return Option.some(new File(elf.getFilename()));
  }

  @Override public String getFileName () {
    return new File(elf.getFilename()).getName();
  }

  @Override public String getArchitectureName () {
    try {
      return cpuTranslation.get(elf.getAttributes().getCPU());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override public Endianness getEndianness () {
    Endianness endianness;
    try {
      endianness = elf.getAttributes().isLittleEndian() ? Endianness.LITTLE : Endianness.BIG;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return endianness;
  }

  @Override public int getArchitectureSize () {
    try {
      switch (elf.getELFhdr().e_ident[Elf.ELFhdr.EI_CLASS]) {
      case Elf.ELFhdr.ELFCLASS32:
        return 32;
      case Elf.ELFhdr.ELFCLASS64:
        return 64;
      default:
        return 0;
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override public BinaryType getType () {
    try {
      return BinaryType.values()[elf.getAttributes().getType() - 1];
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override public BinaryFileFormat getFileFormat () {
    return BinaryFileFormat.ELF;
  }

  @Override public long getEntryAddress () {
    try {
      return elf.getELFhdr().e_entry.getValue().longValue();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override public Option<Symbol> getMainFunction () {
    return getSymbol("main");
  }

  @Override public boolean hasDebugInformation () {
    try {
      return elf.getAttributes().hasDebug();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override protected void initExportedSymbols () {
    try {
      elf.loadSymbols();
      List<Symbol> symbols = new ArrayList<Symbol>();
      for (Elf.Symbol elfSymbol : elf.getSymtabSymbols()) {
        String name = elfSymbol.toString();
        long address = elfSymbol.st_value.getValue().longValue();
        long size = elfSymbol.st_size;
        byte[] data = null;
        try {
          data = elfSymbol.loadSymbolData();
        } catch (ArrayIndexOutOfBoundsException e) {
          // some symbols do not have any data but the ELF code just bails out there with an exception
          data = new byte[0];
        }
        ElfSymbol.Type type = ElfSymbol.Type.values()[elfSymbol.st_type()];
        ElfSymbol.BindingType bindingType = ElfSymbol.BindingType.values()[elfSymbol.st_bind()];
        ElfSymbol symbol = new ElfSymbol(name, address, size, data, type, bindingType);
        if (!isUseful(symbol))
          continue;
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
    try {
      elf.loadSymbols();
      List<Symbol> symtabSymbols = new ArrayList<Symbol>();
      for (Elf.Symbol elfSymbol : elf.getDynamicSymbols()) {
        String name = elfSymbol.toString();
        long address = elfSymbol.st_value.getValue().longValue();
        long size = elfSymbol.st_size;
        byte[] data = null;
        try {
          data = elfSymbol.loadSymbolData();
        } catch (ArrayIndexOutOfBoundsException e) {
          // some symbols do not have any data but the ELF code just bails out there with an exception
          data = new byte[0];
        }
        ElfSymbol.Type type = ElfSymbol.Type.values()[elfSymbol.st_type()];
        ElfSymbol.BindingType bindingType = ElfSymbol.BindingType.values()[elfSymbol.st_bind()];
        ElfSymbol symbol = new ElfSymbol(name, address, size, data, type, bindingType);
        symtabSymbols.add(symbol);
      }
      List<Symbol> symbols = new ArrayList<Symbol>();
      for (Symbol symbol : getPLTSymbols(symtabSymbols)) {
        if (!isUseful((ElfSymbol) symbol))
          continue;
        symbols.add(symbol);
        symbolNames.put(symbol.getNameOrAddress(), symbol);
        symbolAddresses.put(symbol.getAddress(), symbol);
      }
      importedSymbols = symbols;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Decide if the symbol is useful for the disassembly. There are lots of symbols in ELF that describe section markers
   * or have no name or address defined.
   */
  private static boolean isUseful (ElfSymbol symbol) {
    if (!symbol.getType().equals(ElfSymbol.Type.Function))
      return false;
    if (symbol.getAddress() == 0)
      return false;
    if (symbol.getName().isNone())
      return false;
    return true;
  }

  @Override protected void initSegments () {
    List<Segment> segments = new ArrayList<Segment>();
    try {
      for (Elf.Section elfSection : elf.getSections()) {
        // skip the inclusion of some sections that are meant only for meta information about the binary
        // if (elfSection.sh_type == Elf.Section.SHT_NULL || elfSection.sh_type == Elf.Section.SHT_SYMTAB ||
        // elfSection.sh_type == Elf.Section.SHT_STRTAB || elfSection.sh_type == Elf.Section.SHT_NOTE)
        // continue;
        // do not include sections that are not loaded into memory at execution time
        if ((elfSection.sh_flags & Elf.Section.SHF_ALLOC) == 0)
          continue;
        // ignore thread local storage sections for now as they can alias other sections and cause problems
        if ((elfSection.sh_flags & Elf.Section.SHF_TLS) != 0)
          continue;
        String name = elfSection.toString();
        long address = elfSection.sh_addr.getValue().longValue();
        long size = elfSection.sh_size;
        byte[] data = elfSection.loadSectionData();
        Set<Permission> permissions = EnumSet.of(Permission.Read); // it must always be readable
        if ((elfSection.sh_flags & Elf.Section.SHF_EXECINTR) != 0)
          permissions.add(Permission.Execute);
        if ((elfSection.sh_flags & Elf.Section.SHF_WRITE) != 0)
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

  @Override public String toString () {
    return elf.getFilename();
  }

  private Option<Segment> getPLTRelocationSection () throws IOException {
    int DT_JMPREL = 23;
    Elf.Section dynamicSection = elf.getSectionByName(".dynamic");
    if (dynamicSection != null) {
      Dynamic[] dynamics = elf.getDynamicSections(dynamicSection);
      for (Dynamic dynamicEntry : dynamics) {
        if (dynamicEntry.d_tag == DT_JMPREL) {
          long pltRelocationSectionAddress = dynamicEntry.d_val;
          return getSegment(pltRelocationSectionAddress);
        }
      }
    }
    return Option.none();
  }

  /**
   * Returns a list of dynamic symbols with the symbol address pointing into the PLT trampoline for that symbol.
   *
   * @param dynamicSymbols The symbols from the dynamic symbols table. The order of the list must be the same as they
   *          appear in the symbol table.
   */
  private List<Symbol> getPLTSymbols (List<Symbol> dynamicSymbols) throws IOException {
    String gotPltName = ".got.plt";
    Option<Segment> got = getSegment(gotPltName);
    if (got.isNone())
      return Collections.emptyList();
    // TODO: do we also need the type of the relocations? i.e. rela vs. rel
    Option<Segment> relaSection = getPLTRelocationSection();
    if (relaSection.isNone())
      return Collections.emptyList();
    long gotAddress = got.get().getAddress();
    Elf.Section gotSection = elf.getSectionByName(gotPltName);
    Elf.Section pltRelocationsSection = elf.getSectionByName(relaSection.get().getName().get());
    List<Rela_Relocation> relocations = elf.getPLTRelocations(pltRelocationsSection);
    List<Symbol> updatedDynamicSymbols = new ArrayList<Symbol>();
    for (Rela_Relocation relocationEntry : relocations) {
      if (relocationEntry.relocationType != Rela_Relocation.R_386_JMP_SLOT)
        continue;
      ElfSymbol dynamicSymbol = (ElfSymbol) dynamicSymbols.get(relocationEntry.symbolTableIndex - 1);
      long addressInGOT = relocationEntry.r_offset;
      long offsetInGOT = addressInGOT - gotAddress;
      // XXX: subtracting from the address 6 is x86 specific. The value we get below is the address of the push instruction
      // following the indirect jump through the GOT. This is where the calls in the program go to.
      // Alternatively to going through the got we can use the index into the relocation (like objdump) as
      // offset into the PLT where: reloc_index + 2 * arch specific size (x86 = 16bytes) == address of trampoline
      // This would give us actually the address of the first jump into the GOT as opposed to the address of the push
      // instruction following this jump that we get with the above method.
      long symbolPltTrampolineAddress = gotSection.readChunkAtOffset(offsetInGOT) - 6;
      String adjustedName = null;
      if (dynamicSymbol.getName().isSome())
        adjustedName = dynamicSymbol.getName().get() + "@plt"; // print the symbol name similar to objdump
      // XXX: should we give this thing a size? if then it would be the 16 bytes for x86
      Symbol updatedDynamicSymbol = new ElfSymbol(adjustedName, symbolPltTrampolineAddress,
        dynamicSymbol.getSize(), dynamicSymbol.getData(), dynamicSymbol.getType(), dynamicSymbol.getBindingType());
      updatedDynamicSymbols.add(updatedDynamicSymbol);
    }
    return updatedDynamicSymbols;
  }

}
