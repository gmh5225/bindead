package bindead.domains.segments.heap;

import javalx.exceptions.UnimplementedException;
import rreil.lang.MemVar;
import bindead.debug.PrettyDomain;

public class Connector {
  final public ConnectorId id;
  public ConnectorData data;;

  public Connector (MemVar sourceAddr, PathString pathString, MemVar targetAddr, MemVar sourceContents,
      MemVar targetContents) {
    this.id = new ConnectorId(sourceAddr, pathString, targetAddr);
    this.data = new ConnectorData(sourceContents, targetContents);
  }

  public Connector (ConnectorId _1, ConnectorData _2) {
    id = _1;
    data = _2;
  }

  @Override public String toString () {
    return "[" + id + data + "]";
  }

  @Deprecated @Override public boolean equals (Object o) {
    throw new UnimplementedException("please compare id and data individually!");
  }

  @Override @Deprecated public int hashCode () {
    throw new UnimplementedException();
  }

  public boolean spansTo (HeapRegion node) {
    return id.spansTo(node);
  }

  public boolean spansTo (MemVar mv) {
    return id.spansTo(mv);
  }

  public boolean spansFrom (HeapRegion node) {
    return id.spansFrom(node);
  }


  public boolean spansFrom (MemVar mv) {
    return id.spansFrom(mv);
  }

  public boolean spansFrom (MemVar src, PathString pathString) {
    return id.spansFrom(src, pathString);

  }
  public boolean spans (MemVar perm, MemVar eph) {
    return id.spans(perm, eph);
  }

  public void printCompact (StringBuilder builder, PrettyDomain child) {
    builder.append("    " + id + '\n');
    builder.append("      from: ");
    child.memVarToCompactString(builder, data.src);
    builder.append("\n      to:   ");
    child.memVarToCompactString(builder, data.tgt);
  }

}
