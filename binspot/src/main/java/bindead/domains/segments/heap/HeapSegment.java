package bindead.domains.segments.heap;

import java.util.LinkedList;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.BigInt;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLMap;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.RReil.PrimOp;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Lin;
import rreil.lang.Rhs.Rval;
import rreil.lang.util.Type;
import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.abstractsyntax.memderef.AbstractPointer;
import bindead.abstractsyntax.memderef.SymbolicOffset;
import bindead.data.MemVarSet;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.debug.PrettyDomain;
import bindead.debug.StringHelpers;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domains.pointsto.PointsToProperties;
import bindead.domains.segments.basics.RegionAccess;
import bindead.domains.segments.basics.SegCompatibleState;
import bindead.domains.segments.basics.Segment;
import bindead.domains.segments.basics.SegmentWithState;
import bindead.exceptions.DomainStateException.InvariantViolationException;
import bindead.exceptions.Unreachable;

/**
 * @author Holger Siegel
 */
public class HeapSegment<D extends MemoryDomain<D>> extends Segment<D> {
  public static final String NAME = "Heap";

  final boolean DEBUG = PointsToProperties.INSTANCE.debugOther.isTrue();

  final HeapRegionMap heapRegions;;

  final MemVarSet allKnownRegions;

  final ConnectorSet connectors;


  public HeapSegment () {
    heapRegions = HeapRegionMap.empty();
    allKnownRegions = MemVarSet.empty();
    connectors = ConnectorSet.empty();
  }

  HeapSegment (HeapRegionMap a, ConnectorSet ens, MemVarSet regs) {
    heapRegions = a;
    allKnownRegions = regs;
    connectors = ens;
  }

  @SuppressWarnings("unused") void msg (String s) {
    if (DEBUG)
      System.out.println("\nHeapSegment:\n" + s);
  }

  @SuppressWarnings("unused") private void msgs (String s) {
    if (DEBUG) {
      System.out.println("\nHeapSegment:\n" + s);
      System.out.println("\nin state:\n" + this);
    }
  }

  @Override public P3<List<MemVar>, Boolean, D> initialize (D state) {
    return P3.<List<MemVar>, Boolean, D>tuple3(new LinkedList<MemVar>(), Boolean.FALSE, state);
  }

  @Override public SegmentWithState<D> triggerAssignment (Lhs lhs, Rhs rhs, D state) {
    throw new InvariantViolationException();
  }

  public HeapRegion getRegion (MemVar region) {
    return heapRegions.get(region);
  }


  @Override public List<RegionAccess<D>> dereference (Lin fieldThatPoints, AbstractPointer pointer, D state) {
    if (!responsibleFor(pointer))
      return null;
    checkOffset(pointer, state);
    if (isSummary(pointer.address))
      return dereferenceSummaryNG(fieldThatPoints, pointer, state);
    else
      return dereferenceConcreteNG(fieldThatPoints, pointer, state);
  }

  private boolean isSummary (AddrVar address) {
    return heapRegions.get(address).isSummary;
  }

  private List<RegionAccess<D>> dereferenceConcreteNG (Lin fieldThatPoints, AbstractPointer pointer, D state) {
    final List<RegionAccess<D>> res = new LinkedList<RegionAccess<D>>();
    try {
      // just concretize
      final HeapRegion accessedRegion = heapRegions.get(pointer.address);
      HeapSegBuilder<D> hsb2 = new HeapSegBuilder<D>(this, state);
      hsb2.assumeEdgeNG(fieldThatPoints, accessedRegion);
      // TODO apply edgenode(s)
      AbstractMemPointer mp = new AbstractMemPointer(accessedRegion.memId, new SymbolicOffset(pointer.offset));
      res.add(new RegionAccess<D>(mp, hsb2.build()));
    } catch (final Unreachable u) {
      // ignore
    }
    return res;
  }

  private List<RegionAccess<D>> dereferenceSummaryNG (Lin fieldThatPoints, AbstractPointer pointer, D state) {
    final List<RegionAccess<D>> res = new LinkedList<RegionAccess<D>>();
    final HeapRegion summary = heapRegions.get(pointer.address);

    HeapSegBuilder<D> hsb = new HeapSegBuilder<D>(this, state);
    HeapRegion concrete = hsb.expandNG(summary);
    SegmentWithState<D> built = hsb.build();
    hsb.msg("expanded");

    AbstractMemPointer abstractMemPointer = rebasePointer(pointer, concrete);
    try {
      HeapSegBuilder<D> hsb2 = new HeapSegBuilder<>((HeapSegment<D>) built.segment, built.state);
      hsb2.concretizeAndDisconnectNG(summary, concrete);
      hsb2.msg("concretized and disconnected ");
      hsb2.assumeEdgeNG(fieldThatPoints, concrete);
      SegmentWithState<D> build = hsb2.build();
      RegionAccess<D> e = new RegionAccess<D>(abstractMemPointer, build);
      hsb.msg("materialized " + concrete);
      res.add(e);
    } catch (final Unreachable u) {
      // ignore
    }

    try {
      HeapSegBuilder<D> hsb2 = new HeapSegBuilder<>((HeapSegment<D>) built.segment, built.state);
      hsb2.msg("starting expandAndBendGhostEdges");
      hsb2.expandAndBendGhostEdgesNG(summary, concrete);
      hsb2.msg("expanded and bent");
      hsb2.assumeEdgeNG(fieldThatPoints, concrete);
      hsb2.msg("assumed edge");
      res.add(new RegionAccess<D>(abstractMemPointer, hsb2.build()));
    } catch (final Unreachable u) {
      // ignore
    }
    return res;
  }

  private AbstractMemPointer rebasePointer (AbstractPointer pointer, HeapRegion concrete) {
    final HeapRegion summary = heapRegions.get(pointer.address);
    SymbolicOffset offset = new SymbolicOffset(pointer.offset.add(summary.address).sub(concrete.address));
    AbstractMemPointer abstractMemPointer = new AbstractMemPointer(concrete.memId, offset);
    return abstractMemPointer;
  }

  private void checkOffset (AbstractPointer pointer, D state) {
    @SuppressWarnings("unused")
    final Range ofs = pointer.getExplicitOffset(state);
    // TODO hsi: compare ofs to bounds of heap segment (currently we ignore all sizes)
  }

  private boolean responsibleFor (AbstractPointer pointer) {
    return !pointer.isAbsolute() && heapRegions.byAddress.contains(pointer.address);
  }

  private SegmentWithState<D> primitiveFree (Rval arg, D state) {
    throw new UnimplementedException("not yet implemented");
  }

  // low level, no checks for dangling pointers
  HeapSegment<D> removeRegion (HeapRegion region) {
    AddrVar rad = region.address;
    MemVar rmid = region.memId;
    HeapSegment<D> heapSegment =
      new HeapSegment<D>(heapRegions.remove(rad), connectors, allKnownRegions.remove(rmid));
    return heapSegment;
  }

  // low level, no checks for dangling pointers
  HeapSegment<D> removeRegion (MemVar mv) {
    HeapRegion region = heapRegions.get(mv);
    AddrVar rad = region.address;
    HeapSegment<D> heapSegment =
      new HeapSegment<D>(heapRegions.remove(rad), connectors, allKnownRegions.remove(mv));
    return heapSegment;
  }

  @Override public SegmentWithState<D> tryPrimitive (PrimOp prim, D state) {
    if (prim.is("malloc", 1, 1))
      return primitiveMalloc(prim, state);
    else if (prim.is("free", 0, 1))
      return primitiveFree(prim.getInArg(0), state);
    else if (prim.is("foldRegions", 0, 0))
      return primitiveFoldRegions(state);
    else
      return null;
  }

  private SegmentWithState<D> primitiveFoldRegions (D state) {
    // msg("**** primitive fold() ****");
    SegmentWithState<D> summarizeHeap = summarizeHeap(state);
    // msg("folded.\n" + summarizeHeap);
    return summarizeHeap;
  }

  HeapSegment<D> bindRegion (HeapRegion segment) {
    return new HeapSegment<D>(heapRegions.bind(segment), connectors, allKnownRegions.insert(segment.memId));
  }

  HeapSegment<D> bindConnector (ConnectorId id, ConnectorData data) {
    return withConnectors(connectors.add(id, data));
  }

  HeapSegment<D> removeConnector (ConnectorId id) {
    return withConnectors(connectors.remove(id));
  }

  HeapSegment<D> withConnectors (ConnectorSet cs) {
    return new HeapSegment<D>(heapRegions, cs, allKnownRegions);
  }

  private SegmentWithState<D> primitiveMalloc (PrimOp prim, D state) {
    final Rval arg = prim.getInArg(0);
    final Range sz = state.queryRange(arg);
    final BigInt size = sz.getConstantOrNull();
    if (size == null) {
      // for now, we only allow constant sizes
      assert false : "size " + arg + "==" + sz + " is not constant";
      return null;
    }
    final MemVar regionName = MemVar.fresh();
    final AddrVar addr = NumVar.freshAddress();
    state = introOnChild(state, regionName, addr);
    final HeapSegment<D> a = createRegion(addr, regionName, size);
    Lhs outArg = prim.getOutArg(0);
    state = state.assignSymbolicAddressOf(outArg, addr);
    // msg("primitve malloc returns " + a);
    assert a.connectorsAreSane();
    return a.informAboutAssign(state, outArg.getRegionId(), Range.ZERO, null, Range.TOP);
  }

  private D introOnChild (D state, MemVar regionName, AddrVar addr) {
    state = state.introduce(addr, Type.Address, Option.<BigInt>none());
    state = state.introduceRegion(regionName, RegionCtx.EMPTYSTICKY);
    return state;
  }


  @Override public String toString () {
    final StringBuilder builder = new StringBuilder();
    if (!heapRegions.byAddress.isEmpty()) {
      builder.append(heapRegions.byAddress);
    }
    if (builder.toString().isEmpty())
      return StringHelpers.indentMultiline(NAME + ": ", connectors.toString());
    else
      return StringHelpers.indentMultiline(NAME + ": ", builder.toString() + '\n' + connectors.toString());
  }

  @Override public void toCompactString (StringBuilder builder, PrettyDomain childDomain) {
    builder.append(NAME + " (other regions: " + nonHeapRegions() + ")\n");
    heapRegions.toCompactString(builder, childDomain);
    connectors.toCompactString(builder, childDomain);
  }


  @Override public SegCompatibleState<D> makeCompatible (Segment<D> otherSegRaw, D thisState, D otherState) {
    HeapSegment<D> other = (HeapSegment<D>) otherSegRaw;
    HeapSegBuilder<D> first = new HeapSegBuilder<D>(this, thisState);
    HeapSegBuilder<D> second = new HeapSegBuilder<D>(other, otherState);
    return new MakeHeapCompatibleWorker<D>(first, second).makeCompatible();
  }

  @Override public SegmentWithState<D> summarizeHeap (D thisState) {
    HeapSegBuilder<D> builder = new HeapSegBuilder<D>(this, thisState);
    builder.summarizeHeap();
    return builder.build();
  }

  HeapPartitioning partition (D child) {
    // XXX partition by ref
    // TODO in later experiments, partition by, for example, size
    final MemVarSet nodes = heapRegions.regions();
    final MemVarSet registers = nonHeapRegions();
    AVLMap<MemVar, MemVarSet> heapToRegs = AVLMap.empty();
    for (MemVar node : nodes)
      heapToRegs = heapToRegs.bind(node, MemVarSet.empty());
    for (MemVar register : registers) {
      List<P2<PathString, AddrVar>> tgts = child.findPossiblePointerTargets(register);
      for (P2<PathString, AddrVar> tgt : tgts) {
        AddrVar tgtAddr = tgt._2();
        HeapRegion tgtRegion = heapRegions.get(tgtAddr);
        if (tgtRegion == null)
          continue;
        // msg("register " + register + " -> tgt " + tgtRegion.memId);
        heapToRegs = heapToRegs.bind(tgtRegion.memId, heapToRegs.getOrNull(tgtRegion.memId).insert(register));
      }
    }
    //msg("registers " + registers);
    //msg("mapping " + heapToRegs);
    return new HeapPartitioning(heapToRegs);
  }

  @Override public boolean connectorsAreSane () {
    return true;
    /*    for (final P2<EdgeNodeId, EdgeNodeData> h : edgeNodes) {
         EdgeNodeId id = h._1();
         assert heapRegions.contains(id.src);
         assert heapRegions.contains(id.tgt);
       }
       return true;
     */
  }

  @Override public MemVarSet getChildSupportSet () {
    MemVarSet s = MemVarSet.empty();
    for (HeapRegion x : heapRegions.byAddress.values())
      s = s.insertAll(x.getChildSupportSet());
    return s.insertAll(connectors.getChildSupportSet());
  }

  HeapSegment<D> createRegion (AddrVar addr, final MemVar name, BigInt size) {
    return bindRegion(new HeapRegion(addr, name, size));
  }


  @Override public SegmentWithState<D> informAboutAssign (final D childState, MemVar toRegion, Range toOffset,
      MemVar fromRegion,
      Range fromOffset) {
    HeapSegBuilder<D> builder = new HeapSegBuilder<D>(this, childState);
    builder.informAboutAssign(toRegion, toOffset, fromRegion, fromOffset);
    return builder.build();
  }

  ConnectorSet getEdgeNodesAttachedTo (MemVar region) {
    return connectors.getEdgenodesAttachedTo(region);
  }

  HeapSegment<D> bindEdgeNode (MemVar toSource, PathString pathString, MemVar toTarget, final MemVar newSrcCont,
      final MemVar newDestCont) {
    ConnectorId toId = new ConnectorId(toSource, pathString, toTarget);
    ConnectorData toData = new ConnectorData(newSrcCont, newDestCont);
    return bindConnector(toId, toData);
  }

  public HeapSegment<D> bendEdgenodesToSummary (MemVar concrete, MemVar summary) {
    ConnectorSet toBend = connectors.getConnectorComingFrom(concrete);
    ConnectorSet ens = connectors;
    for (Connector en : toBend) {
      assert en.id.tgt.equals(concrete);
      ens = ens.remove(en.id);
      ens = ens.add(new ConnectorId(en.id.src, en.id.pathString, summary), en.data);
    }
    return withConnectors(ens);
  }

  public HeapSegment<D> addKnownRegions (MemVarSet allKnownRegions2) {
    return new HeapSegment<>(heapRegions, connectors, allKnownRegions.insertAll(allKnownRegions2));
  }

  MemVarSet nonHeapRegions () {
    return allKnownRegions.difference(heapRegions.regions());
  }

}