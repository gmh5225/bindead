package bindead.domains.segments.heap;

import javalx.data.products.P2;
import javalx.persistentcollections.AVLMap;
import rreil.lang.MemVar;
import bindead.data.MemVarSet;
import bindead.data.NumVar.AddrVar;
import bindead.debug.PrettyDomain;

public class HeapRegionMap {
  private static final HeapRegionMap EMPTY = new HeapRegionMap();

  static HeapRegionMap empty () {
    return EMPTY;
  }

  /**
   * A map from symbolic addresses to pairs of region size (in bytes) and region id.
   */
  final public AVLMap<AddrVar, HeapRegion> byAddress;
  final public AVLMap<MemVar, HeapRegion> fromMemVar;

  private HeapRegionMap () {
    byAddress = AVLMap.<AddrVar, HeapRegion>empty();
    fromMemVar = AVLMap.<MemVar, HeapRegion>empty();
  }

  public HeapRegionMap (AVLMap<AddrVar, HeapRegion> a, AVLMap<MemVar, HeapRegion> m) {
    byAddress = a;
    fromMemVar = m;
  }

  public boolean contains (MemVar memVar) {
    return fromMemVar.contains(memVar);
  }

  public HeapRegionMap remove (AddrVar addr) {
    HeapRegion r = byAddress.getOrNull(addr);
    return new HeapRegionMap(byAddress.remove(addr), fromMemVar.remove(r.memId));
  }

  public HeapRegionMap bind (HeapRegion segment) {
    return new HeapRegionMap(byAddress.bind(segment.address, segment), fromMemVar.bind(segment.memId, segment));
  }

  public HeapRegion get (MemVar from) {
    assert from != null;
    return fromMemVar.getOrNull(from);
  }

  HeapRegion get (AddrVar from) {
    assert from != null;
    return byAddress.get(from).getOrNull();
  }

  public void toCompactString (StringBuilder builder, PrettyDomain childDomain) {
    for (P2<MemVar, HeapRegion> r : fromMemVar)
      r._2().toCompactString(builder, childDomain);
  }

  public Iterable<MemVar> allRegions () {
    return fromMemVar.keys();
  }

  public MemVarSet regions () {
    return MemVarSet.empty().insertAll(allRegions());
  }

}