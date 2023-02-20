package bindead.domainnetwork.interfaces;

import java.math.BigInteger;
import java.util.Set;

import javalx.data.Option;
import javalx.numeric.BigInt;
import javalx.numeric.FiniteRange;
import binparse.Endianness;
import binparse.Permission;

/**
 * Managing the (initial) contents of code and data segments. A content context
 * consists of access permissions for the underlying data where the data might
 * be initialized (i.e. a byte array with the corresponding contents is
 * supplied) or uninitialized.
 */
public class ContentCtx {
  private final String name;
  private final BigInt address;
  private final long size;
  private final Set<Permission> permissions;
  private final byte[] data;
  private final Endianness endianness;

  public ContentCtx (String name, BigInt address, long size, Set<Permission> permissions, byte[] data,
      Endianness endianness) {
    this(name, address, size, permissions, Option.fromNullable(data), Option.fromNullable(endianness));
  }

  private ContentCtx (String name, BigInt address, long size, Set<Permission> permissions, Option<byte[]> data,
      Option<Endianness> endianness) {
    this.name = name;
    this.address = address;
    this.size = size;
    this.permissions = permissions;
    this.data = data.getOrElse(new byte[0]);
    this.endianness = endianness.getOrElse(Endianness.BIG);
  }

  /**
   * Start address of this segment in bytes.
   */
  public BigInt getAddress () {
    return address;
  }

  /**
   * End address of this segment in bytes, i.e. start + size.
   */
  public BigInt getEndAddress () {
    return address.add(BigInt.of(size));
  }

  /**
   * Size of this segment in bytes.
   */
  public long getSize () {
    return size;
  }

  public Set<Permission> getPermissions () {
    return permissions;
  }

  public String getName () {
    return name;
  }

  @Override public String toString () {
    return "SegmentCtx{name=" + name +
      ", address=" + address +
      ", size=" + size +
      ", permissions=" + permissions +
      ", data=" + data +
      ", endianness=" + endianness + '}';
  }

  /**
   * Read a value from the data in this segment.
   *
   * @param offset Offset in bytes from the beginning of the segment to start the read from.
   * @param size Amount in bytes to read starting at offset.
   * @param endianness The endiannes of the data that should be read.
   * @return The data read from this segment.
   */
  public BigInt read (int offset, int size, Endianness endianness) {
    byte[] slice = slice(offset, size);
    // BigInteger constructor needs big-endian slices.
    if (endianness == Endianness.LITTLE)
      Endianness.reverseByteOrder(slice);
    return BigInt.of(new BigInteger(slice));
  }

  /**
   * Return a slice of bytes of the data in this segment.
   *
   * @param byteOffset Offset in bytes from the beginning of the segment to start the read from.
   * @param byteLength Amount in bytes to read starting at offset.
   * @return The data slice that was read from this segment.
   */
  private byte[] slice (int byteOffset, int byteLength) {
    byte[] slice = new byte[byteLength];
    assert byteOffset >= 0;
    assert data.length >= byteOffset + byteLength : "Data length= " + data.length + " < offset + readAmount= "
      + byteOffset + " + " + byteLength + " in ctx " + this;
    System.arraycopy(data, byteOffset, slice, 0, byteLength);
    return slice;
  }

  public FiniteRange addressableSpaceInBytes () {
    return FiniteRange.of(getAddress(), getSize());
  }
}
