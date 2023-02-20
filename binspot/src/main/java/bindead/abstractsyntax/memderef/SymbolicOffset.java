package bindead.abstractsyntax.memderef;

import javalx.numeric.Range;
import bindead.data.Linear;
import bindead.domainnetwork.channels.QueryChannel;

public class SymbolicOffset extends DerefOffset {
  private final Linear offset;

  public SymbolicOffset (Linear o) {
    offset = o;
  }

  @Override public Range getRange (QueryChannel state) {
    return state.queryRange(offset);
  }

  @Override public String toString () {
    return offset.toString();
  }

  @Override public SymbolicOffset add (Linear ofs) {
    return new SymbolicOffset(offset.add(ofs));
  }

  @Override
  public Linear upperBound () {
    return offset;
  }

  @Override
  public Linear lowerBound () {
    return offset;
  }
}
