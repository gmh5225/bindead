package rreil.lang;

import javalx.numeric.BigInt;

/**
 * An address for RREIL code consisting of a base address and a sub-address (offset).
 */
public final class RReilAddr implements Comparable<RReilAddr>, AssemblerParseable, Reconstructable {
  public static final RReilAddr ZERO = RReilAddr.valueOf(0);
  public static final RReilAddr ONE = RReilAddr.valueOf(1);
  private final long base;
  private final int offset;

  public RReilAddr (final long base, final int offset) {
    this.base = base;
    this.offset = offset;
  }

  public long base () {
    return base;
  }

  public int offset () {
    return offset;
  }

  /**
   * Returns {@code true} if this address may denote a native instruction,
   * that is, if it does not contain an offset.
   */
  public boolean isNative () {
    return offset == 0;
  }

  @Override public int compareTo (final RReilAddr other) {
    if (base == other.base)
      return offset == other.offset ? 0 : offset < other.offset ? -1 : 1;
    else
      return base < other.base ? -1 : 1;
  }

  public static RReilAddr valueOf (long base, int offset) {
    return new RReilAddr(base, offset);
  }

  public static RReilAddr valueOf (long base) {
    return new RReilAddr(base, 0);
  }

  public static RReilAddr valueOf (BigInt base) {
    return new RReilAddr(base.longValue(), 0);
  }

  /**
   * Parse RReil addresses of the form "[0x][base].[offset]" from strings. The offset part is optional, if omitted it
   * will be set to {@code 0}. The address can be in decimal or hexadecimal form if prefixed by the string {@code 0x}.
   */
  public static RReilAddr valueOf (String address) {
    int radix = 10;
    if (address.toLowerCase().startsWith("0x")) {
      radix = 16;
      address = address.substring(2);
    }
    String[] addressSplit = address.split("\\.");
    if (addressSplit.length > 2)
      throw new IllegalArgumentException("The address string: \"" + address +
        "\" cannot be parsed to a RREIL address. The string must be of the form [0x][base].[offset]");
    String baseString;
    String offsetString;
    baseString = addressSplit[0];
    if (addressSplit.length == 2)
      offsetString = addressSplit[1];
    else
      offsetString = "0";
    return new RReilAddr(Long.parseLong(baseString, radix), Integer.parseInt(offsetString, radix));
  }

  /**
   * Returns this RREIL address with the base incremented.
   *
   * @return This address with a next higher base
   */
  public RReilAddr nextBase () {
    return new RReilAddr(base + 1, offset);
  }

  /**
   * @return A RREIL address with the base incremented by {@code opcodeLength} and zero offset
   */
  public RReilAddr nextBase (long opcodeLength) {
    return new RReilAddr(base + opcodeLength, 0);
  }

  /**
   * Returns this RREIL address with the offset incremented.
   *
   * @return This address with a next higher offset
   */
  public RReilAddr nextOffset () {
    return new RReilAddr(base, offset + 1);
  }

  /**
   * Returns this RREIL address with the specified offset.
   *
   * @param newOffset The offset that the new RREIL address should be set to
   * @return This address with a new offset
   */
  public RReilAddr withOffset (int newOffset) {
    return new RReilAddr(base, newOffset);
  }

  @Override public String toString () {
    return String.format("%08x.%02x", base, offset);
  }

  /**
   * If the offset is 0 then it is not printed.
   */
  public String toShortString () {
    if (offset != 0)
      return String.format("%x.%02x", base, offset);
    else
      return String.format("%x", base);
  }

  /**
   * If the offset is 0 then it is not printed.
   */
  public String toShortStringWithHexPrefix () {
    if (offset != 0)
      return String.format("0x%x.%02x", base, offset);
    else
      return String.format("0x%x", base);
  }

  public String toStringWithHexPrefix () {
    return String.format("0x%08x.%02x", base, offset);
  }

  @Override public String toAssemblerString () {
    return toStringWithHexPrefix() + ":";
  }

  @Override public String reconstructCode () {
    return "new RReilAddr(" + base + ", " + offset + ")";
  }

  @Override public boolean equals (Object obj) {
    if (!(obj instanceof RReilAddr))
      return false;
    final RReilAddr other = (RReilAddr) obj;
    if (this.base != other.base)
      return false;
    if (this.offset != other.offset)
      return false;
    return true;
  }

  @Override public int hashCode () {
    int hash = 5;
    hash = 61 * hash + (int) (this.base ^ (this.base >>> 32));
    hash = 61 * hash + this.offset;
    return hash;
  }
}
