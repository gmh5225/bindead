package bindead.domains.segments.heap;

import java.util.LinkedList;
import java.util.List;

import javalx.data.products.P2;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLSet;
import rreil.lang.MemVar;
import rreil.lang.Rhs.Lin;
import bindead.data.MemVarPair;
import bindead.data.MemVarSet;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domains.pointsto.PointsToProperties;
import bindead.domains.segments.basics.SegmentWithState;
import bindead.exceptions.Unreachable;

public class HeapSegBuilder<D extends MemoryDomain<D>> {
  static final boolean withTransitiveInfo = !true;
  private static final boolean withPrimConnectors = true;
  final boolean DEBUG = PointsToProperties.INSTANCE.debugOther.isTrue();

  void msg (String msg) {
    if (!DEBUG)
      return;
    System.out.print("\nHeapSegBuilder: ");
    System.out.println(msg);
  }

  void msgs (String msg) {
    //assert false;
    if (!DEBUG)
      return;
    System.out.print("\nHeapSegBuilder: ");
    System.out.println(msg);
    System.out.print("\nin state: ");
    System.out.println(toCompactString());
    //System.out.println(toString());
  }

  HeapSegment<D> seg;
  D child;

  HeapSegBuilder (HeapSegment<D> seg, D child) {
    this.seg = seg;
    this.child = child;
  }

  private void remove (Connector en) {
    seg = seg.removeConnector(en.id);
    child = child.projectRegion(en.data.src);
    child = child.projectRegion(en.data.tgt);
  }


  void renameRegion (MemVar from, AddrVar fromAddr, MemVar to, AddrVar toAddr) {
    HeapRegion regions = seg.heapRegions.get(from).setMemId(to).setAddr(toAddr);
    ConnectorSet conns = ConnectorSet.empty();
    for (Connector conn : seg.connectors) {
      ConnectorId movedConnectorId = conn.id.moveConnector(from, to);
      MemVar newTgt = MemVar.fresh();
      child = child.substituteRegion(conn.data.tgt, newTgt);
      MemVar newSrc = MemVar.fresh();
      child = child.substituteRegion(conn.data.src, newSrc);
      ConnectorData movedConnectorData = new ConnectorData(newSrc, newTgt);
      conns = conns.add(movedConnectorId, movedConnectorData);
    }
    seg = seg.removeRegion(from).bindRegion(regions).withConnectors(conns);
    child = child.substituteRegion(from, to);
    child = child.substitute(fromAddr, toAddr);
  }

  void summarizeHeap () {
    if (withPrimConnectors)
      createPrimeConnectors(seg.allKnownRegions);
    msg("created prime connectors");
    final HeapPartitioning partitions = seg.partition(child);
    // TODO when making compatible, rename heap regions according to partition map
    for (final MemVarSet area : partitions) {
      if (area.size() < 2)
        continue;
      if (withTransitiveInfo) {
        createTransitiveConnectorClosure(area);
        msg("created transitive connectors");
      }
      foldNodesOfPartition(area);
      msgs("summarized area " + area);
    }
    assert seg.connectorsAreSane();
  }

  private void createPrimeConnectors (MemVarSet area) {
    // TODO do for all known regions
    for (MemVar sourceNode : area) {
      List<P2<PathString, AddrVar>> targets = child.findPossiblePointerTargets(sourceNode);
      for (P2<PathString, AddrVar> target : targets) {
        HeapRegion targetRegion = seg.heapRegions.get(target._2());
        if (targetRegion == null)
          continue;
        if (!area.contains(targetRegion.memId))
          continue;
        PathString path = target._1();
        ConnectorId enid = new ConnectorId(sourceNode, path, targetRegion.memId);
        if (seg.connectors.contains(enid))
          continue;
        final MemVar sourceContents = MemVar.fresh();
        final MemVar targetContents = MemVar.fresh();
        child = child.copyMemRegion(sourceNode, sourceContents);
        child = child.copyMemRegion(targetRegion.memId, targetContents);
        ConnectorData endata = new ConnectorData(sourceContents, targetContents);
        Connector connector = new Connector(enid, endata);
        // updateTransitiveConnectors(connector);
        seg = seg.bindConnector(enid, endata);
      }
    }
  }

  private void updateTransitiveConnectors (Connector current) {
    ConnectorSet incoming = new ConnectorSet();
    for (Connector in : seg.connectors)
      if (in.spansTo(current.id.src)) {
        incoming = incoming.add(in);
        // TODO implement in HeapSegBuilder
        assert false : in + " * " + current;
        throw new UnimplementedException();
      }
    ConnectorSet outgoing = new ConnectorSet();
    for (Connector out : seg.connectors)
      if (out.spansFrom(current.id.tgt)) {
        outgoing = outgoing.add(out);
        // TODO implement in HeapSegBuilder
        throw new UnimplementedException();
      }
    for (Connector in : incoming)
      for (Connector out : outgoing) {
        // TODO implement in HeapSegBuilder
        throw new UnimplementedException();
      }
  }

  /*
   * We use the FLOYD-WARSHALL ALGORITHM for computing the closure!
   *
   * (so we have some semiring structure on our transitive relations...)
   *
   * - use transitive self-nodes only in combination with other nodes
   * - how can we describe the semiring used for floyd-warshall?
   */
  private void createTransitiveConnectorClosure (MemVarSet area) {
    for (MemVar region : area) {
      ConnectorSet firsts = seg.connectors.getConnectorGoingTo(region);
      ConnectorSet seconds = seg.connectors.getConnectorComingFrom(region);
      for (Connector snd : seconds)
        for (Connector fst : firsts) {
          if (!(area.contains(snd.id.tgt) || area.contains(fst.id.src)))
            continue;
          if (fst.id.src.equals(region) && snd.id.tgt.equals(region))
            continue;
          makeTransitiveNode(fst, snd);
        }
    }
  }

  private void makeTransitiveNode (final Connector edge1, final Connector edge2) {
    if (edge1.id.src == edge1.id.tgt && edge2.id.src != edge2.id.tgt) {
      ConnectorData newData = makeTransitiveConnector(edge1.data, edge2.data);
      foldConnectorData(edge2.data, newData);
    } else if (edge1.id.src != edge1.id.tgt && edge2.id.src == edge2.id.tgt) {
      ConnectorData newData = makeTransitiveConnector(edge1.data, edge2.data);
      foldConnectorData(edge1.data, newData);
    } else if (edge1.id.src != edge2.id.src && edge1.id.tgt != edge2.id.tgt) {
      ConnectorData newData = makeTransitiveConnector(edge1.data, edge2.data);
      ConnectorId id = new ConnectorId(edge1.id.src, edge1.id.pathString.plus(edge2.id.pathString), edge2.id.tgt);
      if (seg.connectors.contains(id)) {
        assert !false : "implement";
        Connector existingConnector = seg.connectors.get(id);
        foldConnectorData(existingConnector.data, newData);
      } else
        seg = seg.bindConnector(id, newData);
    } else
      assert false : "should not happen.";
  }

  private ConnectorData makeTransitiveConnector (ConnectorData edge1data, ConnectorData edge2data) {
    // TODO hsi: test inner nodes equal
    MemVar newSrc = makeCopyOfRegion(edge1data.src);
    MemVar newTgt = makeCopyOfRegion(edge2data.tgt);
    return new ConnectorData(newSrc, newTgt);
  }

  private void foldConnectorData (ConnectorData permData, final ConnectorData ephData) {
    List<MemVarPair> vars = new LinkedList<MemVarPair>();
    vars.add(new MemVarPair(permData.src, ephData.src));
    vars.add(new MemVarPair(permData.tgt, ephData.tgt));
    child = child.foldNG(vars);
  }

  private MemVar makeCopyOfRegion (MemVar targetContents) {
    final MemVar newTgt = MemVar.fresh();
    child = child.copyMemRegion(targetContents, newTgt);
    return newTgt;
  }

  @SuppressWarnings("unused") private static Connector transitiveNode (Connector edge1, Connector edge2, MemVar c1,
      MemVar c2) {
    ConnectorId id = new ConnectorId(edge1.id.src, edge1.id.pathString.plus(edge2.id.pathString), edge2.id.tgt);
    ConnectorData data = new ConnectorData(c1, c2);
    return new Connector(id, data);
  }

  private void foldNodesOfPartition (MemVarSet area) {
    // always fold onto the oldest region
    final MemVar perm = area.getMin().get();
    for (final MemVar eph : area)
      if (!eph.equals(perm))
        foldRegionsNG(perm, eph);
  }

  public SegmentWithState<D> build () {
    return new SegmentWithState<D>(seg, child);
  }

  @Override public String toString () {
    //    return toCompactString();
    return "seg: " + seg + "\nstate: " + child;
  }

  public String toCompactString () {
    return build().toCompactString();
  }

  public void assumeEdgeNG (Lin fieldThatPoints, HeapRegion region) {
    child = child.assumeEdgeNG(fieldThatPoints, region.address);
    applyPrimeConnectors(fieldThatPoints, region);
  }

  public void concretizeAndDisconnectNG (AddrVar summary, AVLSet<MemVar> concreteNodes) {
    child = child.concretizeAndDisconnectNG(summary, concreteNodes);
  }

  private void applyPrimeConnectors (Lin fieldThatPoints, HeapRegion target) {
    for (Connector connector : seg.connectors)
      if (connector.id.isSelfLoop() && !seg.getRegion(connector.id.src).isSummary)
        remove(connector);
    for (Connector connector : seg.connectors) {
      HeapRegion reg = seg.heapRegions.get(connector.id.tgt);
      assert reg != null;
      AddrVar tgtAddr = reg.address;
      Range flagRange =
        child.queryPtsEdge(connector.id.src, connector.id.pathString.prefix, fieldThatPoints.getSize(), tgtAddr);
      if (flagRange.isOne()) {
        child = child.assumeRegionsAreEqual(connector.id.src, connector.data.src);
        child = child.assumeRegionsAreEqual(connector.id.tgt, connector.data.tgt);
        if (withTransitiveInfo)
          applyTransitiveConnectors(connector.id);
        remove(connector);
      } else if (!withTransitiveInfo && flagRange.isZero() && !(connector.id.isSelfLoop() && reg.isSummary))
        remove(connector);
    }
  }

  private void applyTransitiveConnectors (ConnectorId id) {
    for (Connector sc : seg.connectors)
      if (sc.spansFrom(id.src, id.pathString)) {
        if (sc.id.isSelfLoop())
          continue;
        for (Connector tc : seg.connectors)
          if (tc.spansFrom(id.tgt) && tc.spansTo(sc.id.tgt)) {
            String message = "current connector is " + id + ": specialize connector " + tc + " with "
              + sc;
            msg(message);
            try {
              applyTransitiveConnectorViaCnP(id, sc, tc);
            } catch (Unreachable u) {
              msg("equated, unreachable");
              //throw new UnimplementedException("remove unreachable connectors");
            }
            //throw new UnimplementedException(message + "\nin " + toCompactString());
          }
      }
  }

  private void applyTransitiveConnectorViaCnP (ConnectorId singleStep, Connector sc, Connector tc) {
    D cs = child;
    cs = cs.assumeRegionsAreEqual(singleStep.src, sc.data.src);
    cs = cs.assumeRegionsAreEqual(tc.data.tgt, sc.data.tgt);
    new HeapSegBuilder<>(seg, cs).msgs("equated.");
    // TODO insert specialized connector via c&p
    D child2 = child.projectRegion(tc.data.src).projectRegion(tc.data.tgt);
    child = child2.copyAndPaste(MemVarSet.of(tc.data.src, tc.data.tgt), cs);
  }

  private void applyTransitiveConnectorViaExpand (ConnectorId singleStep, Connector sc, Connector tc) {
    System.err.println("single " + singleStep + " " + sc + " " + tc);
    MemVar scSrcCopy = MemVar.fresh();
    MemVar scTgtCopy = MemVar.fresh();
    List<MemVarPair> expansion = new LinkedList<>();
    expansion.add(new MemVarPair(sc.data.src, scSrcCopy));
    expansion.add(new MemVarPair(sc.data.tgt, scTgtCopy));
    System.err.println(expansion);
    D fallback = child;
    try {
      child = child.expandNG(expansion);
      child = child.assumeRegionsAreEqual(singleStep.src, scSrcCopy);
      child = child.assumeRegionsAreEqual(tc.data.tgt, scTgtCopy);
      child = child.projectRegion(scSrcCopy).projectRegion(scTgtCopy);
    } catch (Unreachable _) {
      child = fallback;
    }
  }

  void foldRegionsNG (final MemVar perm, final MemVar eph) {
    msg("summarize " + perm + " <- " + eph);
    MemVarSet summaryContents = MemVarSet.of(perm);
    MemVarSet concreteContents = MemVarSet.of(eph);
    MemVarSet pointingToConcrete = MemVarSet.empty();
    MemVarSet pointingToSummary = MemVarSet.empty();
    for (Connector en : seg.connectors) {
      if (en.spansTo(perm)) {
        summaryContents = summaryContents.insert(en.data.tgt);
        if (!en.spansFrom(eph))
          pointingToSummary = pointingToSummary.insert(en.data.src);
      }
      else if (en.spansTo(eph)) {
        concreteContents = concreteContents.insert(en.data.tgt);
        if (!en.spansFrom(perm))
          pointingToConcrete = pointingToConcrete.insert(en.data.src);
      }
      if (en.spansFrom(perm)) {
        summaryContents = summaryContents.insert(en.data.src);
      }
      else if (en.spansFrom(eph)) {
        concreteContents = concreteContents.insert(en.data.src);
      }
    }
    HeapRegion permRegion = seg.getRegion(perm);
    HeapRegion ephRegion = seg.getRegion(eph);
    child =
      child.bendGhostEdgesNG(permRegion.address, ephRegion.address, summaryContents, concreteContents,
          pointingToSummary, pointingToConcrete);
    msg("fold Regions: bent ghost edges");

    List<MemVarPair> doubleEdgeNodePairs = new LinkedList<MemVarPair>();
    //  bend edgenodes between s and c, if double, add to list
    for (Connector en : seg.connectors)
      if (en.spans(perm, eph) || en.spans(eph, perm)) {
        ConnectorId toId = new ConnectorId(en.id.src, en.id.pathString, en.id.src);
        if (seg.connectors.contains(toId)) {
          foldOnto(doubleEdgeNodePairs, en, toId);
        } else {
          seg = bendEdgeNode(en, toId);
        }
      }
    child = child.foldNG(doubleEdgeNodePairs);
    msg("fold Regions: bent connectors between s and c");

    /* FIXME source of connectors that point to summarizees from the outside should
     * not contain pointers to eph region. Have to bend these pointers beforehand.
     *
     */
    List<MemVarPair> allContents = new LinkedList<MemVarPair>();
    allContents.add(new MemVarPair(perm, eph));
    // find all edgenodes to s/c that are paired
    // bend unpaired ENs from c to s
    for (Connector en : seg.connectors) {
      if (en.spans(eph, eph)) {
        ConnectorId toId = new ConnectorId(perm, en.id.pathString, perm);
        if (seg.connectors.contains(toId)) {
          foldOnto(allContents, en, toId);
        } else {
          seg = bendEdgeNode(en, toId);
        }
      } else if (en.spansTo(eph)) {
        ConnectorId toId = new ConnectorId(en.id.src, en.id.pathString, perm);
        if (seg.connectors.contains(toId)) {
          foldOnto(allContents, en, toId);
        } else {
          seg = bendEdgeNode(en, toId);
        }
      }
      else if (en.spansFrom(eph)) {
        ConnectorId toId = new ConnectorId(perm, en.id.pathString, en.id.tgt);
        if (seg.connectors.contains(toId)) {
          foldOnto(allContents, en, toId);
        } else {
          seg = bendEdgeNode(en, toId);
        }
      }
    }
    child = child.foldNG(permRegion.address, ephRegion.address, allContents);
    seg = seg.removeRegion(ephRegion);
    seg = seg.bindRegion(permRegion.setSummaryFlag(true));
    msg("summarized.");
  }

  private void foldOnto (List<MemVarPair> doubleEdgeNodePairs, Connector en, ConnectorId ontoId) {
    Connector onto = seg.connectors.get(ontoId);
    doubleEdgeNodePairs.add(new MemVarPair(onto.data.src, en.data.src));
    doubleEdgeNodePairs.add(new MemVarPair(onto.data.tgt, en.data.tgt));
    seg = seg.removeConnector(en.id);
  }

  private HeapSegment<D> bendEdgeNode (Connector en, ConnectorId toId) {
    MemVar newSrc = MemVar.fresh();
    child = child.substituteRegion(en.data.src, newSrc);
    MemVar newTgt = MemVar.fresh();
    child = child.substituteRegion(en.data.tgt, newTgt);
    ConnectorData movedData = new ConnectorData(newSrc, newTgt);
    return seg.removeConnector(en.id).bindConnector(toId, movedData);
  }

  // expand a region. result is heap with two symmetrical summary regions.
  // returned region still has ghost edges, but summary flag is already set to false.
  HeapRegion expandNG (final HeapRegion summary) {
    HeapRegion concrete = new HeapRegion(NumVar.freshAddress(), MemVar.fresh(), summary.size);
    seg = seg.bindRegion(concrete);
    final LinkedList<MemVarPair> toExpand = new LinkedList<MemVarPair>();
    toExpand.add(new MemVarPair(summary.memId, concrete.memId));

    for (Connector en : seg.connectors) {
      if (en.spans(summary.memId, summary.memId)) {
        ConnectorId ccid = en.id.bendSrcTo(concrete).bendTgtTo(concrete);
        expandConnector(toExpand, ccid, en.data);
      } else if (en.spansTo(summary)) {
        ConnectorId xcid = en.id.bendTgtTo(concrete);
        expandConnector(toExpand, xcid, en.data);
      } else if (en.spansFrom(summary)) {
        ConnectorId cxid = en.id.bendSrcTo(concrete);
        expandConnector(toExpand, cxid, en.data);
      }
    }
    assert seg.connectorsAreSane();
    child = child.expandNG(summary.address, concrete.address, toExpand);
    return concrete;
  }

  private void expandConnector (final LinkedList<MemVarPair> expandList, ConnectorId id, ConnectorData data) {
    MemVar newSrc = MemVar.fresh();
    expandList.add(new MemVarPair(data.src, newSrc));
    MemVar newTgt = MemVar.fresh();
    expandList.add(new MemVarPair(data.tgt, newTgt));
    ConnectorData cdata = new ConnectorData(newSrc, newTgt);
    seg = seg.bindConnector(id, cdata);
  }

  void expandAndBendGhostEdgesNG (final HeapRegion summary, HeapRegion concrete) {
    final LinkedList<MemVarPair> toExpand = new LinkedList<MemVarPair>();
    for (Connector en : seg.connectors) {
      if (en.spans(summary.memId, summary.memId)) {
        ConnectorId scid = en.id.bendTgtTo(concrete);
        expandConnector(toExpand, scid, en.data);
      } else if (en.spans(concrete.memId, concrete.memId)) {
        ConnectorId csid = en.id.bendTgtTo(summary);
        expandConnector(toExpand, csid, en.data);
      }
    }

    MemVarSet toBendAsSummary = MemVarSet.of(summary.memId);
    MemVarSet toBendAsConcrete = MemVarSet.of(concrete.memId);
    MemVarSet pointingToSummary = MemVarSet.empty();
    MemVarSet pointingToConcrete = MemVarSet.empty();
    for (Connector en : seg.connectors) {
      msg("testing " + en);
      if (en.spansTo(summary)) {
        toBendAsSummary = toBendAsSummary.insert(en.data.tgt);
        if (!en.spansFrom(concrete))
          pointingToSummary = pointingToSummary.insert(en.data.src);
      }
      else if (en.spansTo(concrete)) {
        toBendAsConcrete = toBendAsConcrete.insert(en.data.tgt);
        if (!en.spansFrom(summary))
          pointingToConcrete = pointingToConcrete.insert(en.data.src);

      }
      if (en.spansFrom(summary))
        toBendAsSummary = toBendAsSummary.insert(en.data.src);
      else if (en.spansFrom(concrete))
        toBendAsConcrete = toBendAsConcrete.insert(en.data.src);
      msg("s: " + toBendAsSummary);
      msg("c: " + toBendAsConcrete);
    }

    assert seg.connectorsAreSane();
    child = child.expandNG(toExpand);
    //msgs("expanded connectors, preparing to bend edges");
    msg("pointingToSummary " + pointingToSummary);
    msg("pointingToConcrete " + pointingToConcrete);
    msg("toBendAsSummary " + toBendAsSummary);
    msg("toBendAsConcrete " + toBendAsConcrete);
    child = child.bendBackGhostEdgesNG(summary.address, concrete.address, toBendAsSummary, toBendAsConcrete,
        pointingToSummary, pointingToConcrete);
  }

  void concretizeAndDisconnectNG (final HeapRegion summary, HeapRegion concrete) {
    AVLSet<MemVar> concreteNodes = AVLSet.<MemVar>singleton(concrete.memId).add(summary.memId);
    for (Connector en : seg.connectors) {
      if (en.spansFrom(concrete) || en.spansFrom(summary))
        concreteNodes = concreteNodes.add(en.data.src);
      if (en.spansTo(concrete) || en.spansTo(summary))
        concreteNodes = concreteNodes.add(en.data.tgt);
    }
    concretizeAndDisconnectNG(summary.address, concreteNodes);
  }

  void informAboutAssign (MemVar toRegion, Range toOffset, MemVar fromRegion, Range fromOffset) {
    assert toRegion != null;
    msg("informed about assign: " + showRO(toRegion, toOffset) + " <- " + showRO(fromRegion, fromOffset));
    ConnectorSet ens = seg.connectors;
    // TODO special case where from and to regions are the same
    // TODO collect connectors from source (or empty set if null)
    for (Connector en : seg.connectors)
      if (en.id.attachedTo(toRegion, toOffset)) {
        // TODO instead of deleting, set overwritten fields to top in connectors attached to other offsets
        ens = ens.remove(en.id);
        child = child.projectRegion(en.data.src);
        child = child.projectRegion(en.data.tgt);
      }
    // TODO also remove transitive connectors
    MemVarSet regs = seg.allKnownRegions;
    if (toRegion != null)
      regs = regs.insert(toRegion);
    if (fromRegion != null)
      regs = regs.insert(fromRegion);
    seg = new HeapSegment<D>(seg.heapRegions, ens, regs);
  }

  String showRO (MemVar toRegion, Range toOffset) {
    return toRegion + ":" + toOffset;
  }
}
