package bindead.domains.fields;

import static javalx.data.Option.none;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.FiniteRange;
import javalx.numeric.Interval;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.ThreeWaySplit;
import javalx.persistentcollections.tree.OverlappingRanges;
import rreil.lang.MemVar;
import bindead.data.MemVarSet;
import bindead.data.VarSet;
import bindead.debug.PrettyDomain;
import bindead.debug.StringHelpers;
import bindead.domainnetwork.interfaces.ContentCtx;
import bindead.domainnetwork.interfaces.RegionCtx;

class RegionMap {
  final AVLMap<MemVar, Region> map;

  private RegionMap (AVLMap<MemVar, Region> map) {
    this.map = map;
  }

  private RegionCtx getContext (MemVar varId) {
    return map.get(varId).get().context;
  }

  Option<ContentCtx> getSegment (MemVar varId) {
    return getContext(varId).getSegment();
  }

  static RegionMap empty () {
    return new RegionMap(AVLMap.<MemVar, Region>empty());
  }

  RegionMap remove (MemVar region, FiniteRange range) {
    Region fields = map.get(region).get();
    return bind(region, fields.remove(range));
  }

  Option<VariableCtx> get (MemVar varId, FiniteRange range) {
    Option<Region> fields = map.get(varId);
    if (fields.isSome()) {
      return fields.get().fields.get(range);
    }
    return none();
  }

  /**
   * Searches all overlapping variable contexts contained in the given region for the interval {@code key}.
   *
   * @return List of pairs of interval key and corresponding variable context, that is all overlapping contexts.
   */
  OverlappingRanges<VariableCtx> searchOverlaps (MemVar region, Interval range) {
    Region tree = map.get(region).getOrNull();
    if (tree == null)
      return new OverlappingRanges<VariableCtx>();
    return tree.fields.searchOverlaps(range);
  }

  boolean isEmpty () {
    return map.isEmpty();
  }

  Region get (MemVar regionId) {
    Region option = getOrNull(regionId);
    assert option != null : "not containing " + regionId;
    return option;
  }

  Region getOrNull (MemVar regionId) {
    return map.get(regionId).getOrNull();
  }

  boolean contains (MemVar regionId) {
    return map.contains(regionId);
  }

  RegionMap bind (MemVar regionId, Region fields) {
    return new RegionMap(map.bind(regionId, fields));
  }

  RegionMap remove (MemVar regionId) {
    return new RegionMap(map.remove(regionId));
  }

  ThreeWaySplit<RegionMap> split (RegionMap other) {
    ThreeWaySplit<AVLMap<MemVar, Region>> split = map.split(other.map);
    return ThreeWaySplit.make(
        new RegionMap(split.onlyInFirst()),
        new RegionMap(split.inBothButDiffering()),
        new RegionMap(split.onlyInSecond()));
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    Set<MemVar> sorted = StringHelpers.sortLexically(map.keys());
    Iterator<MemVar> iterator = sorted.iterator();
    builder.append('{');
    while (iterator.hasNext()) {
      MemVar key = iterator.next();
      Region value = map.getOrNull(key);
      builder.append(key);
      builder.append('=');
      builder.append(value);
      if (iterator.hasNext())
        builder.append(", ");
    }
    return builder.append('}').toString();
  }

  int size () {
    return map.size();
  }

  public MemVarSet keys () {
    return MemVarSet.from(map.keys());
  }

  public void appendInfo (StringBuilder builder, PrettyDomain childDomain) {
    Set<P2<MemVar, Region>> empties = new HashSet<P2<MemVar, Region>>();
    Set<P2<MemVar, Region>> nonEmpties = new HashSet<P2<MemVar, Region>>();
    for (P2<MemVar, Region> memory : map) {
      Region region = memory._2();
      if (region.isEmpty())
        empties.add(memory);
      else
        nonEmpties.add(memory);
    }
    if (!empties.isEmpty()) {
      builder.append("    ");
      for (P2<MemVar, Region> memory : empties) {
        MemVar regionVariable = memory._1();
        builder.append(regionVariable + ", ");
      }
      builder.setLength(builder.length() - 2);
      builder.append(" : {}");
      builder.append("\n");
    }
    for (P2<MemVar, Region> memory : nonEmpties) {
      MemVar regionVariable = memory._1();
      Region region = memory._2();
      builder.append("    " + regionVariable + " : ");
      region.appendInfo(builder, childDomain);
      builder.append("\n");
    }
  }

  VarSet contentVars (MemVar s) {
    VarSet ss = VarSet.empty();
    for (P2<?, VariableCtx> f : get(s).fields)
      ss = ss.add(f._2().getVariable());
    return ss;
  }
}
