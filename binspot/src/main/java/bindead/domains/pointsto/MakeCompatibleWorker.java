package bindead.domains.pointsto;

import javalx.data.products.P3;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.ThreeWaySplit;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.domainnetwork.combinators.FiniteSequence;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domains.pointsto.PointsToSet.PointsToEntry;
import bindead.exceptions.DomainStateException.VariableSupportSetException;


public class MakeCompatibleWorker<D extends FiniteDomain<D>> {
  final PointsToStateBuilder<D> builder1;
  final PointsToStateBuilder<D> builder2;

  public MakeCompatibleWorker (PointsToState state1, PointsToState state2) {
    this.builder1 = new PointsToStateBuilder<D>(state1);
    this.builder2 = new PointsToStateBuilder<D>(state2);
  }

  P3<PointsToState, D, D> makeCompatible (D cs1, D cs2) {
    level1_supportset();
    D fstchildState = builder1.applyChildOps(cs1);
    D sndchildState = builder2.applyChildOps(cs2);
    return new P3<PointsToState, D, D>(builder1.build(), fstchildState, sndchildState);
  }

  private void level1_supportset () {
    ThreeWaySplit<PointsToMap> split = builder1.pointsToMap.split(builder2.pointsToMap);
    if (!split.onlyInFirst().isEmpty())
      throw new VariableSupportSetException("only in first: "+split.onlyInFirst().getSupport());
    if (!split.onlyInSecond().isEmpty())
      throw new VariableSupportSetException("only in second: "+split.onlyInSecond().getSupport());
    for (PointsToSet fstPts : split.inBothButDiffering()) {
      level2_pointstoSet(fstPts);
    }
  }

  private void level2_pointstoSet (PointsToSet fst) {
    PointsToSet snd = builder2.getPts(fst.var);
    fst = level3a_sumFlags(fst, snd);
    fst = level3b_ghostEdges(fst, snd);
    fst = level3c_pointsToEntries(fst, snd);
    builder1.bindPts(fst);
  }

  private PointsToSet level3a_sumFlags (PointsToSet fst, final PointsToSet snd) {
    NumVarOrZero fstSum = fst.sumOfFlags;
    NumVarOrZero sndSum = snd.sumOfFlags;
    boolean zeroSum = fstSum.isConstantZero();
    boolean zeroOtherSum = sndSum.isConstantZero();
    if (zeroSum && !zeroOtherSum) {
      builder1.getChildOps().addIntroZero(sndSum.numVar);
      fst = fst.withSumVar(sndSum.numVar);
    } else if (!zeroSum && zeroOtherSum) {
      builder2.getChildOps().addIntroZero(fstSum.numVar);
    } else if (!zeroSum && !zeroOtherSum && !fstSum.equals(sndSum)) {
      builder2.getChildOps().addSubst(sndSum.numVar, fstSum.numVar);
    }
    return fst;
  }

  private PointsToSet level3b_ghostEdges (PointsToSet fst, final PointsToSet snd) {
    FiniteSequence otherChildOps = builder2.getChildOps();
    boolean hasGhost = fst.hasGhostNode();
    boolean hasSndGhost = snd.hasGhostNode();
    if (!hasGhost && hasSndGhost) {
      builder1.getChildOps().addIntroZero(snd.outFlagOfGhostNode);
      fst = fst.withGhostNode(snd.outFlagOfGhostNode);
    } else if (hasGhost && !hasSndGhost) {
      otherChildOps.addIntroZero(fst.outFlagOfGhostNode);
    } else if (hasGhost && hasSndGhost
      && !fst.outFlagOfGhostNode.equalTo(snd.outFlagOfGhostNode)) {
      otherChildOps.addSubst(snd.outFlagOfGhostNode, fst.outFlagOfGhostNode);
    }
    return fst;
  }

  PointsToSet level3c_pointsToEntries (PointsToSet fst, final PointsToSet snd) {
    ThreeWaySplit<AVLMap<AddrVar, PointsToEntry>> tws = fst.splitPts(snd);
    for (AddrVar t : tws.inBothButDiffering().keys()) {
      NumVar leftFlag = fst.getEntry(t).flag;
      NumVar rightFlag = snd.getEntry(t).flag;
      if (leftFlag == rightFlag)
        continue;
      assert leftFlag != rightFlag : "left " + leftFlag + " right " + rightFlag;
      builder2.getChildOps().addSubst(rightFlag, leftFlag);
    }
    for (PointsToEntry t : tws.onlyInSecond().values()) {
      fst = fst.bind(t.address, t.flag);
      builder1.getChildOps().addIntroZero(t.flag);
    }
    for (PointsToEntry t : tws.onlyInFirst().values()) {
      builder2.getChildOps().addIntroZero(t.flag);
    }
    return fst;
  }

}
