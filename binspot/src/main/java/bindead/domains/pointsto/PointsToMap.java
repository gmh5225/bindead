package bindead.domains.pointsto;

import java.util.Iterator;

import javalx.data.Option;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.ThreeWaySplit;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarSet;

class PointsToMap implements Iterable<PointsToSet> {
  // XXX make private
  final AVLMap<NumVar, PointsToSet> pointsToMap;

  PointsToMap () {
    pointsToMap = AVLMap.<NumVar, PointsToSet>empty();
  }

  // XXX make private
  PointsToMap (AVLMap<NumVar, PointsToSet> ptm) {
    pointsToMap = ptm;
  }

  @Override public Iterator<PointsToSet> iterator () {
    return pointsToMap.values().iterator();
  }

  boolean contains (NumVar key) {
    return pointsToMap.contains(key);
  }

  Option<PointsToSet> get (NumVar key) {
    return pointsToMap.get(key);
  }

  PointsToMap bind (PointsToSet pts) {
    return new PointsToMap(pointsToMap.bind(pts.var, pts));
  }

  PointsToMap remove (NumVar key) {
    return new PointsToMap(pointsToMap.remove(key));
  }

  ThreeWaySplit<PointsToMap> split (PointsToMap other) {
    ThreeWaySplit<AVLMap<NumVar, PointsToSet>> split = pointsToMap.split(other.pointsToMap);
    return ThreeWaySplit.<PointsToMap>make(new PointsToMap(split.onlyInFirst()),
        new PointsToMap(split.inBothButDiffering()), new PointsToMap(
          split.onlyInSecond()));
  }

  public boolean isEmpty () {
    return pointsToMap.isEmpty();
  }

  public VarSet getNonScalars () {
    VarSet nons = VarSet.empty();
    for (PointsToSet pts : pointsToMap.values()) {
      if (!pts.isScalar())
        nons = nons.add(pts.var);
      nons = nons.union(pts.localVars());
    }
    return nons;
  }

  public int pointersCount () {
    return getNonScalars().size();
  }

  public PointsToMap removeEntry (NumVar src, AddrVar tgt) {
    PointsToSet pts = pointsToMap.get(src).get();
    pts = pts.remove(tgt);
    AVLMap<NumVar, PointsToSet> m2 = pointsToMap.bind(src, pts);
    return new PointsToMap(m2);
  }

  VarSet getSupport () {
    VarSet supp = VarSet.empty();
    for (NumVar v : pointsToMap.keys())
      supp = supp.add(v);
    return supp;
  }


  @Override public String toString () {
    return pointsToMap.toString();
  }
}
