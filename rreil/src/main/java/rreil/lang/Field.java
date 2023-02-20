package rreil.lang;

import javalx.numeric.FiniteRange;
import javalx.xml.XmlPrintable;
import rreil.lang.Rhs.Rvar;

import com.jamesmurty.utils.XMLBuilder;

/**
 * {@code Field}s are used to introduce and query variable-values. A field
 * always has an associated region-identifier. Regions are either registers or
 * arbitrary stack/memory regions.
 *
 * @author mb0
 */
public final class Field implements XmlPrintable {
  private final int size;
  private final int offset;
  private final MemVar region;

  public static Field field (Rvar rhs) {
    return new Field(rhs.getSize(), rhs.offset, rhs.getRegionId());
  }

  public static Field field (Lhs lhs) {
    return new Field(lhs.getSize(), lhs.getOffset(), lhs.getRegionId());
  }

  public static FiniteRange finiteRangeKey (int offset, int size) {
    return FiniteRange.of(offset, offset + size - 1);
  }

  public Field (int size, int offset, MemVar region) {
    this.size = size;
    this.offset = offset;
    this.region = region;
  }

  public FiniteRange finiteRangeKey () {
    return finiteRangeKey(offset, size);
  }

  public int getSize () {
    return size;
  }

  public int getOffset () {
    return offset;
  }

  public MemVar getRegion () {
    return region;
  }

  @Override public String toString () {
    return String.format("<Field %s:%d/%d>", region, size, offset);
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    /*
     * <Field region=r size=sz offset=offs/>
     */
    return builder.e("Field")
        .a("region", region.toString())
        .a("size", String.valueOf(size))
        .a("offset", String.valueOf(offset)).up();
  }

  @Override public int hashCode () {
    final int prime = 31;
    int result = 1;
    result = prime * result + offset;
    result = prime * result + ((region == null) ? 0 : region.hashCode());
    result = prime * result + size;
    return result;
  }

  @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Field))
      return false;
    Field other = (Field) obj;
    if (offset != other.offset)
      return false;
    if (region == null) {
      if (other.region != null)
        return false;
    } else if (!region.equals(other.region))
      return false;
    if (size != other.size)
      return false;
    return true;
  }

}
