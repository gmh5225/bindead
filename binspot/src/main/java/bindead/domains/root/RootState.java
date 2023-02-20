package bindead.domains.root;

import javalx.numeric.BigInt;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.tree.FiniteRangeTree;
import rreil.lang.MemVar;
import bindead.data.MemVarSet;
import bindead.data.NumVar;
import bindead.domainnetwork.interfaces.ContentCtx;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.domainnetwork.interfaces.RegionCtx;

import com.jamesmurty.utils.XMLBuilder;

/**
 * The data-structure of the root-domain.
 */
public class RootState extends FunctorState {
  static final RootState EMPTY = new RootState();
  final MemVarSet regions;
  final AVLMap<MemVar,NumVar> regionToAddress;
  final AVLMap<NumVar,MemVar> addressToRegion;
  final AVLMap<MemVar,RegionCtx> contexts;
  final FiniteRangeTree<ContentCtx> segments;
  final AVLMap<BigInt, MemVar> concreteAddresses;

  RootState () {
    this(
        MemVarSet.empty(),
        AVLMap.<MemVar,NumVar>empty(),
        AVLMap.<NumVar,MemVar>empty(),
        AVLMap.<MemVar,RegionCtx>empty(),
        FiniteRangeTree.<ContentCtx>empty(),
        AVLMap.<BigInt, MemVar>empty());
  }

  RootState (
      MemVarSet regions,
      AVLMap<MemVar,NumVar> regionToAddress,
      AVLMap<NumVar,MemVar> addressToRegion,
      AVLMap<MemVar,RegionCtx> contexts,
      FiniteRangeTree<ContentCtx> segments,
      AVLMap<BigInt, MemVar> concreteAddress) {
    this.regions = regions;
    this.regionToAddress = regionToAddress;
    this.addressToRegion = addressToRegion;
    this.contexts = contexts;
    this.segments = segments;
    this.concreteAddresses = concreteAddress;
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append(regions);
    if (!addressToRegion.isEmpty()) {
      builder.append(", ");
      builder.append(" symaddrs: ");
      builder.append(addressToRegion);
    }
    if (!concreteAddresses.isEmpty()) {
      builder.append(", ");
      builder.append(" addrs: ");
      builder.append(concreteAddresses);
    }
    return builder.toString();
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    return builder;
  }

}
