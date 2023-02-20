package bindead.domains.segments.heap;

import javalx.numeric.BigInt;
import rreil.lang.MemVar;
import bindead.data.MemVarSet;
import bindead.data.NumVar.AddrVar;
import bindead.debug.PrettyDomain;

public class HeapRegion implements Comparable<HeapRegion> {
  final boolean isSummary;
  public final AddrVar address;
  public final MemVar memId;
  // TODO hsi: use NumVar for size (currently we ignore sizes)
  final BigInt size;

  // currently, whether a region is a summary is tracked in the 'summary' and 'concrete' attributes of HeapSegments

  private HeapRegion (AddrVar address, MemVar id, BigInt size, boolean isSummary) {
    assert address != null;
    this.address = address;
    this.isSummary = isSummary;
    assert id != null;
    this.memId = id;
    assert size != null;
    this.size = size;
  }

  HeapRegion (AddrVar address, MemVar regionName, BigInt size) {
    this(address, regionName, size, false);
  }

  @Override public String toString () {
    final String summStr = isSummary ? " (Summary)" : "(Concrete)";
    return memId + ":" + size + summStr;
  }

  HeapRegion setMemId (MemVar to) {
    return new HeapRegion(address, to, size, isSummary);
    }

  public HeapRegion setAddr (AddrVar toAddr) {
    return new HeapRegion(toAddr, memId, size, isSummary);
      }

  HeapRegion turnIntoSummary () {
    return setSummaryFlag(true);
  }

  HeapRegion setSummaryFlag (boolean s) {
    return new HeapRegion(address, memId, size, s);
  }

  public MemVarSet getChildSupportSet () {
    return MemVarSet.of(memId);
  }

  @Override public int compareTo (HeapRegion other) {
    int t1 = address.compareTo(other.address);
    if (t1 != 0)
      return t1;
    return memId.compareTo(other.memId);
  }

  public void toCompactString (StringBuilder builder, PrettyDomain child) {
    builder.append("    " + memId + "@" + address + " = ");
    child.memVarToCompactString(builder, memId);
    if (isSummary)
      builder.append("<Summary>");
    builder.append("\n");
  }

}
