package bindead.domains.segments.heap;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.ThreeWaySplit;
import rreil.lang.MemVar;
import rreil.lang.util.Type;
import bindead.data.MemVarSet;
import bindead.data.NumVar.AddrVar;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domains.segments.basics.SegCompatibleState;

public class MakeHeapCompatibleWorker<D extends MemoryDomain<D>> {
  final HeapSegBuilder<D> first;
  final HeapSegBuilder<D> second;

  public MakeHeapCompatibleWorker (HeapSegBuilder<D> first, HeapSegBuilder<D> second) {
    this.first = first;
    this.second = second;
  }

  SegCompatibleState<D> makeCompatible () {
    movePartitionsInSecondToCompatiblePosition();

    assert first.seg.connectorsAreSane();
    assert second.seg.connectorsAreSane();
    makeRegionsCompatible();
    makeEdgeNodesCompatible();
    makeKnownRegionsCompatible();
    assert first.seg.connectorsAreSane();
    assert second.seg.connectorsAreSane();
    assert first.seg.allKnownRegions.containsTheSameAs(second.seg.allKnownRegions);
    return new SegCompatibleState<D>(first.seg, first.child, second.child);
  }

  private void movePartitionsInSecondToCompatiblePosition () {
    HeapPartitioning thisPartitions = first.seg.partition(first.child);
    HeapPartitioning otherPartitions = second.seg.partition(second.child);
    for (MemVarSet mvs : otherPartitions.plist) {
      if (mvs.size() != 1)
        continue;
      MemVar fromVar = mvs.getMin().get();
      MemVarSet regs = otherPartitions.heapToRegs.get(fromVar).get();
      HeapRegion regionInSecond = second.seg.getRegion(fromVar);
      for (P2<MemVar, MemVarSet> regionNameInFirst : thisPartitions.heapToRegs)
        if (regionNameInFirst._2().containsTheSameAs(regs)
          && !otherPartitions.heapToRegs.contains(regionNameInFirst._1())) {
          HeapRegion regionInFirst = first.seg.getRegion(regionNameInFirst._1());
          second.renameRegion(regionInSecond.memId, regionInSecond.address, regionInFirst.memId, regionInFirst.address);
          break;
        }
    }
  }

  private void makeKnownRegionsCompatible () {
    first.seg = first.seg.addKnownRegions(second.seg.allKnownRegions);
    second.seg = second.seg.addKnownRegions(first.seg.allKnownRegions);
  }

  void makeEdgeNodesCompatible () {
    final ThreeWaySplit<ConnectorSet> es = first.seg.connectors.split(second.seg.connectors);
    for (Connector en : es.onlyInFirst()) {
      copyAndPasteConnector(second, first, en);
    }
    for (Connector en : es.onlyInSecond()) {
      copyAndPasteConnector(first, second, en);
    }
    for (Connector en : es.inBothButDiffering()) {
      ConnectorData secondData = second.seg.connectors.get(en.id).data;
      final ConnectorData firstData = en.data;
      second.child = second.child.substituteRegion(secondData.src, firstData.src);
      second.child = second.child.substituteRegion(secondData.tgt, firstData.tgt);
    }
  }


  private void copyAndPasteConnector (HeapSegBuilder<D> to, HeapSegBuilder<D> from, Connector en) {
    final boolean CNP = true;
    // FIXME HSI only copy if there is no pts edge in 'to'.!
    if (CNP) {
      to.child = to.child.copyAndPaste(MemVarSet.of(en.data.src, en.data.tgt), from.child);
    } else {
      to.child = to.child.introduceRegion(en.data.src, RegionCtx.EMPTYSTICKY);
      to.child = to.child.introduceRegion(en.data.tgt, RegionCtx.EMPTYSTICKY);
    }
    to.seg = to.seg.bindConnector(en.id, en.data);
  }

  void makeRegionsCompatible () {
    final ThreeWaySplit<AVLMap<AddrVar, HeapRegion>> s =
      first.seg.heapRegions.byAddress.split(second.seg.heapRegions.byAddress);
    for (final HeapRegion e : s.onlyInFirst().values()) {
      addDummyRegion(second, e, first);
    }
    for (final HeapRegion e : s.onlyInSecond().values()) {
      addDummyRegion(first, e, second);
    }
    for (HeapRegion thisSeg : s.inBothButDiffering().values()) {
      // second.renameMemVarInRegion(thisSeg.address, thisSeg.memId);
      HeapRegion otherSeg = second.seg.heapRegions.get(thisSeg.address);
      assert thisSeg.memId.equals(otherSeg.memId);
      if (thisSeg.isSummary && !otherSeg.isSummary) {
        second.seg = second.seg.bindRegion(otherSeg.turnIntoSummary());
      } else if (!thisSeg.isSummary && otherSeg.isSummary) {
        first.seg = first.seg.bindRegion(thisSeg.turnIntoSummary());
      }
    }
  }

  private static <D extends MemoryDomain<D>> void addDummyRegion
      (HeapSegBuilder<D> builder, final HeapRegion e, HeapSegBuilder<D> otherBuilder) {
    builder.child = builder.child.introduce(e.address, Type.Address, Option.<BigInt>none());
    builder.child = builder.child.copyAndPaste(MemVarSet.of(e.memId), otherBuilder.child);
    //builder.child = builder.child.introduceRegion(e.memId, RegionCtx.EMPTYSTICKY);
    builder.seg = builder.seg.bindRegion(e);
  }


}
