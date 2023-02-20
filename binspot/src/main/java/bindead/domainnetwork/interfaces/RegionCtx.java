package bindead.domainnetwork.interfaces;

import javalx.data.Option;
import javalx.numeric.BigInt;

/**
 * Region contexts are used when introducing new regions. A region context
 * might have the {@code sticky} flag set, which means that when joining
 * regions, all associated fields are kept. When the sticky flag is not
 * set, non-matching fields are to be removed.
 */
public class RegionCtx {
  public static final RegionCtx EMPTYSTICKY = new RegionCtx();

  private final Option<ContentCtx> segment;

  private RegionCtx () {
    this(null);
  }

  public RegionCtx (ContentCtx segment) {
    this.segment = Option.fromNullable(segment);
  }

  public Option<BigInt> getAddress () {
    if (segment.isSome())
      return Option.some(segment.get().getAddress());
    return Option.none();
  }

  public boolean isAddressable () {
    return segment.isSome();
  }

  public Option<ContentCtx> getSegment () {
    return segment;
  }

  public boolean isSticky () {
    return true;
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("RegionCtx{");
    if (segment.isSome())
      builder.append(", " + segment.get());
    builder.append("}");
    return builder.toString();
  }
}
