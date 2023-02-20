package bindead.domains.segments.heap;

import javalx.numeric.Range;
import rreil.lang.MemVar;

public class ConnectorId implements Comparable<ConnectorId> {
  final public MemVar src;
  final public PathString pathString;
  final public MemVar tgt;

  public ConnectorId (MemVar src, PathString pathString, MemVar tgt) {
    this.src = src;
    this.pathString = pathString;
    this.tgt = tgt;
  }

  @Override public int compareTo (ConnectorId other) {
    int c1 = src.compareTo(other.src);
    if (c1 != 0)
      return c1;
    int c2 = tgt.compareTo(other.tgt);
    if (c2 != 0)
      return c2;
    int c3 = pathString.compareTo(other.pathString);
    return c3;
  }

  public boolean attachedTo (MemVar node) {
    return src.equals(node) || tgt.equals(node);
  }

  boolean comesFrom (MemVar from, Range ofs) {
    return from.equals(src) && pathString.is(ofs);
  }

  public boolean isSelfLoop () {
    return src.equals(tgt);
  }

  ConnectorId renameEdgeNodeConnections (MemVar to, MemVar from) {
    MemVar sa = src.equals(from) ? to : src;
    MemVar ta = tgt.equals(from) ? to : tgt;
    return new ConnectorId(sa, pathString, ta);
  }

  @Override public String toString () {
    return src + "--" + pathString.prefix + "-->" + tgt;
  }

  public boolean spans (MemVar perm, MemVar eph) {
    return spansFrom(perm) && spansTo(eph);
  }

  public boolean spansFrom (HeapRegion node) {
    return spansFrom(node.memId);
  }

  public boolean spansFrom (MemVar mv) {
    return src.equals(mv);
  }

  public boolean spansTo (HeapRegion node) {
    return spansTo(node.memId);
  }

  public boolean spansTo (MemVar mv) {
    return tgt.equals(mv);
  }

  ConnectorId bendSrcTo (HeapRegion src) {
    return new ConnectorId(src.memId, pathString, tgt);
  }

  ConnectorId bendTgtTo (HeapRegion tgt) {
    return new ConnectorId(src, pathString, tgt.memId);
  }

  public boolean attachedTo (MemVar region, Range offsetRange) {
    return attachedTo(region) && pathString.is(offsetRange);
  }

  public ConnectorId moveConnector (MemVar from, MemVar to) {
    MemVar newSrc = spansFrom(from) ? to : src;
    MemVar newtgt = spansTo(from) ? to : tgt;
    return new ConnectorId(newSrc, pathString, newtgt);
  }

  public boolean spansFrom (MemVar region, PathString pathString2) {
    return spansFrom(region) && pathString.is(pathString2);
  }
}