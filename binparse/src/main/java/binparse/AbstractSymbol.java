package binparse;

import javalx.data.Option;

/**
 * A symbol in an binary file.
 */
public abstract class AbstractSymbol implements Symbol {
  protected final String name;
  protected final long address;
  protected final long size;
  protected final byte[] data;

  public AbstractSymbol (String name, long address, long size, byte[] data) {
    if (name == null || name.isEmpty())
      this.name = null;
    else
      this.name = name;
    this.address = address;
    this.size = size;
    this.data = data;
  }

  @Override public long getAddress () {
    return address;
  }

  @Override public Option<String> getName () {
    return Option.fromNullable(name);
  }

  @Override public String getNameOrAddress () {
    if (name == null)
      return getUnnamedSymbolFor(address);
    else
      return name;
  }

  @Override public byte[] getData () {
    return data;
  }

  @Override public long getSize () {
    return size;
  }

  /**
   * Returns the name a symbol at a given address. If there is no symbol at that address or it has no name then the
   * address as string is returned.
   * @param address The address to look for a symbol at
   * @param file The binary to search for the symbol
   * @return The name for a symbol at the given address or the address as a string
   */
  public static String getNameFor (long address, Binary file) {
    if (file == null)
      return getUnnamedSymbolFor(address);
    Option<Symbol> symbol = file.getSymbol(address);
    if (symbol.isSome())
      return symbol.get().getNameOrAddress();
    else
      return getUnnamedSymbolFor(address);
  }

  private static String getUnnamedSymbolFor (long address) {
    return String.format("(unnamed)@0x%08X", address);
  }

  @Override public String toString () {
    return getNameOrAddress();
  }

}
