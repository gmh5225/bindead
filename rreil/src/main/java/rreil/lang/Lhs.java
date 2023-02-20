package rreil.lang;

import javalx.numeric.FiniteRange;
import rreil.lang.Rhs.Rvar;

public final class Lhs implements AssemblerParseable, Reconstructable {
  private final MemVar region;
  private final int offset;
  private final int size;

  public Lhs (int size, int offset, MemVar regionId) {
    this.region = regionId;
    this.offset = offset;
    this.size = size;
    assert size > 0 : "We do not support empty access ranges.";
  }

  public MemVar getRegionId () {
    return region;
  }

  public int getOffset () {
    return offset;
  }

  public int getSize () {
    return size;
  }

  /**
   * Translates this left-hand side variable to a right-hand side variable using this offset and size.
   *
   * @return This as right-hand side variable.
   */
  public Rvar asRvar () {
    return new Rvar(size, offset, region);
  }

  public FiniteRange bitRange () {
    return FiniteRange.of(getOffset(), getOffset() + getSize() - 1);
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
    if (!(obj instanceof Lhs))
      return false;
    Lhs other = (Lhs) obj;
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

  @Override public String reconstructCode () {
    return "new Lhs(" + size + ", " + offset + ", " + region.reconstructCode() + ")";
  }

  @Override public String toAssemblerString () {
    return region.toString();
  }

  @Override public final String toString () {
    return asRvar().toString();
  }
}