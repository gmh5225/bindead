package bindead.abstractsyntax.memderef;

import javalx.numeric.Range;
import bindead.data.Linear;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.exceptions.DomainStateException.UnimplementedMethodException;

public class ExplicitOffset extends DerefOffset {
  private final Range offset;

  public ExplicitOffset (Range r) {
    offset = r;
  }

  public ExplicitOffset (int offset2) {
    this(Range.from(offset2));
  }

  @Override public Range getRange (QueryChannel state) {
    return offset;
  }

  @Override public String toString () {
    return offset.toString();
  }

  @Override public SymbolicOffset add (Linear ofs) {
    throw new UnimplementedMethodException();
  }

  @Override
  public Linear upperBound () {
    if (!offset.isFinite())
      return null;
    return Linear.linear(offset.getMax());
  }

  @Override
  public Linear lowerBound () {
    if (!offset.isFinite())
      return null;
    return Linear.linear(offset.getMin());
  }
}
