package bindead.domains.fields;

import java.util.LinkedList;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.FiniteRange;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.tree.OverlappingRanges;
import rreil.lang.MemVar;
import bindead.data.MemVarSet;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.interfaces.ContentCtx;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.domains.segments.heap.PathString;
import binparse.Permission;

import com.jamesmurty.utils.XMLBuilder;

/**
 * The data structure of the memory domain. Use the nested builder classes for construction and manipulation
 * of memory contexts.
 */
class FieldState extends FunctorState {
  protected static final FieldState EMPTY = new FieldState();
  protected final RegionMap regions;
  protected final AVLMap<MemVar, ClobberedMap> clobbered;

  private FieldState () {
    this(RegionMap.empty(), AVLMap.<MemVar, ClobberedMap>empty());
  }

  protected FieldState (RegionMap regions, AVLMap<MemVar, ClobberedMap> clobbered) {
    this.regions = regions;
    this.clobbered = clobbered;
  }

  @Override public String toString () {
    return "#" + regions.size() + " " + regions.toString();
  }

  @Override public XMLBuilder toXML (XMLBuilder xml) {
    xml = xml.e("FIELD");
    for (P2<MemVar, Region> regionIdAndFields : regions.map) {
      Region region = regionIdAndFields._2();
      if (region.fields.isEmpty()) {
        xml = xml.e("Entry")
            .a("type", "Field")
            .e("MemoryVariable")
            .t(regionIdAndFields._1().toString()).up().up();
        continue;
      }

      Option<ClobberedMap> map = clobbered.get(regionIdAndFields._1());
      for (P2<FiniteRange, VariableCtx> fields : region.fields) {
        boolean isClobbered = false;
        if (map.isSome())
          isClobbered = map.get().isClobbered(fields._1());
        xml = xml.e("Entry")
            .a("type", "Field")
            .e("MemoryVariable")
            .t(regionIdAndFields._1().toString()).up();
        xml = xml.e("Variable")
            .t(fields.snd.getVariable().toString()).up()
            .e("offset")
            .e("lowerBound")
            .t(fields._1().low().toString())
            .up()
            .e("upperBound")
            .t(fields._1().high().toString())
            .up()
            .up();//offset
        xml = xml.e("Clobbered")
            .t(Boolean.toString(isClobbered))
            .up();
        xml = xml.up();//entry
      }
    }
    xml = xml.up();//field
    xml = xml.up();
    return xml;
  }

  public boolean canWriteTo (MemVar varId) {
    Option<ContentCtx> maybeSegment = regions.getSegment(varId);
    if (maybeSegment.isSome()) {
      ContentCtx segmentCtx = maybeSegment.get();
      return Permission.isWritable(segmentCtx.getPermissions());
    }
    return true;
  }

  <D extends FiniteDomain<D>> List<P2<PathString, AddrVar>> findPossiblePointerTargets (D childState,
      MemVar sourceId) {
    List<P2<PathString, AddrVar>> l = new LinkedList<P2<PathString, AddrVar>>();
    Region r = regions.getOrNull(sourceId);
    // hsi: NULL test is needed because sometimes SP does not exist.
    // Sooner or later we should eliminate this kind of awkward behaviour.
    if (r == null)
      return l;
    for (P2<FiniteRange, VariableCtx> field : r.fields) {
      PathString ps = new PathString(field._1().low());
      NumVar variable = field._2().getVariable();
      for (AddrVar targetaddr : childState.findPossiblePointerTargets(variable)) {
        l.add(new P2<PathString, AddrVar>(ps, targetaddr));
      }
    }
    return l;
  }

  /**
   * Query all fields overlapping the given interval contained in the given region.
   *
   * @param region The region to search for overlapping fields.
   * @param field The field.
   * @return A list of all overlapping fields.
   */
  public OverlappingRanges<VariableCtx> queryOverlappingFields (MemVar region, FiniteRange field) {
    return regions.searchOverlaps(region, field.toInterval());
  }

  public boolean containsRegion (MemVar mv) {
    return regions.contains(mv);
  }

  public MemVarSet getSupportSet () {
    return regions.keys();
  }

  @Override public void toCompactString (String domainName, StringBuilder builder, PrettyDomain childDomain) {
    //builder.append(domainName + ": #" + regions.size() + "\n");
    //regions.appendInfo(builder, childDomain);
  }

}
