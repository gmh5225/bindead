package bindead.domains.pointsto;

import java.util.List;
import java.util.Set;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.BigInt;
import javalx.numeric.Interval;
import rreil.lang.util.Type;
import bindead.abstractsyntax.finite.Finite.Rhs;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.domainnetwork.channels.WarningsContainer;
import bindead.domainnetwork.combinators.FiniteStateBuilder;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domains.pointsto.PointsToSet.PointsToEntry;
import bindead.exceptions.DomainStateException;
import bindead.exceptions.Unreachable;

/**
 * A class to hold the data structures of the points-to domain while it is being modified.
 *
 * @author Holger Siegel
 */
class PointsToStateBuilder<D extends FiniteDomain<D>> extends FiniteStateBuilder {
  final private static FiniteFactory fin = FiniteFactory.getInstance();

  PointsToMap pointsToMap;

  PointsToStateBuilder (PointsToState state) {
    this.pointsToMap = state.getPointsToMap();
  }

  void bindPts (PointsToSet targetPts) {
    pointsToMap = pointsToMap.bind(targetPts);
  }

  PointsToState build () {
    return new PointsToState(pointsToMap);
  }

  void copyAndPaste (VarSet vars, PointsTo<D> from) {
    VarSet childVars = vars;
    for (NumVar v : vars) {
      PointsToSet vpts = from.queryPts(v);
      childVars = childVars.union(vpts.localVars());
      bindPts(vpts);
    }
    getChildOps().addCopyAndPaste(childVars, from.childState);
  }

  void evalAssign (int size, NumVar lhsVar, HyperPointsToSet hyperRhs) {

    assert !lhsVar.isAddress();
    PointsToSet targetPts = PointsToSet.empty(lhsVar);
    for (P2<AddrVar, Linear> c : hyperRhs.getTerms()) {
      NumVar newFlag = NumVar.freshFlag();
      getChildOps().addIntro(newFlag);
      AddrVar addr = c._1();
      Linear coeff = c._2();
      getChildOps().addAssignment(fin.variable(0, newFlag), fin.linear(0, coeff));
      targetPts = targetPts.bind(addr, newFlag);
    }
    if (!hyperRhs.sumOfFlags.isZero()) {
      NumVar var = NumVar.freshFlag();
      getChildOps().addIntro(var);
      targetPts = targetPts.withSumVar(var);
      getChildOps().addAssignment(fin.variable(size, var), fin.linear(size, hyperRhs.sumOfFlags));
    }
    Rhs offset = hyperRhs.offset;
    killPtsVars(lhsVar);
    bindPts(targetPts);
    getChildOps().addAssignment(fin.variable(size, lhsVar), offset);
  }

  @Deprecated// use PointsToState.translate
  HyperPointsToSet translate (int lhsSize, Rhs rhs, WarningsContainer wc) {
    return new RhsTranslator(lhsSize, build(), wc).run(rhs);
  }

  void killPtsVars (NumVar lhsVar) {
    Option<PointsToSet> pts = pointsToMap.get(lhsVar);
    if (pts.isSome())
      pts.get().killLocalVars(getChildOps());
  }


  void remove (NumVar eph) {
    pointsToMap = pointsToMap.remove(eph);
  }


  void introduce (NumVar variable, Type type) {
    boolean isAddress = type.equals(Type.Address);
    assert isAddress == variable.isAddress();
    bindPts(PointsToSet.empty(variable));
  }

  void project (NumVar var, QueryChannel qc) {
    if (var.isAddress())
      projectAddressFromPTSs((AddrVar) var, qc);
    PointsToSet pts = getPts(var);
    for (NumVar var1 : pts.localVars()) {
      getChildOps().addKill(var1);
    }
    remove(var);
    getChildOps().addKill(var);
  }

  private void projectAddressFromPTSs (AddrVar var, QueryChannel qc) {
    // use reverse map?
    for (PointsToSet currentPts : pointsToMap) {
      PointsToEntry currentEntry = currentPts.getEntry(var);
      if (currentEntry == null)
        continue;
      bindPts(currentPts.remove(var));
      BigInt flagValue = qc.queryRange(currentEntry.flag).getConstantOrNull();
      if (flagValue != null && !flagValue.isZero()) {
        getChildOps().addAssignment(fin.variable(0, currentPts.var), fin.range(0, Interval.top()));
      }
      getChildOps().addKill(currentEntry.flag);
    }
  }

  /* substitute outer numVars and addresses */
  void substitute (NumVar from, NumVar to) {
    assert from.isAddress() == to.isAddress();
    if (from.isAddress()) {
      AddrVar toAddr = (AddrVar) to;
      AddrVar fromAddr = (AddrVar) from;
      // use reverse map?
      for (PointsToSet t : pointsToMap) {
        PointsToSet newPts = t.substituteAddress(toAddr, fromAddr, getChildOps());
        if (newPts != null)
          bindPts(newPts);
      }
    }
    PointsToSet p = getPts(from);
    remove(from);
    assert !pointsToMap.contains(to);
    P2<PointsToSet, FoldMap> newP = p.cloneWithNewVars(to);
    for (VarPair vp : newP._2()) {
      getChildOps().addSubst(vp.getPermanent(), vp.getEphemeral());
    }
    bindPts(newP._1());
    getChildOps().addSubst(from, to);
  }

  final PointsToSet getPts (NumVar numVar) {
    Option<PointsToSet> pts = pointsToMap.get(numVar);
    if (pts.isNone())
      throw new DomainStateException.VariableSupportSetException(numVar);
    return pts.get();
  }

  final PointsToSet getPts (Rlin numVar) {
    HyperPointsToSet hpts =
      new RhsTranslator(numVar.getSize(), build(), null).visitLinear(numVar.getLinearTerm(), numVar.getSize());
    // FIXME: refVar should be a Linear Expression
    throw new UnimplementedException("cannot implement");
  }

  void assumeConcrete (NumVar var) {
    PointsToSet pts = getPts(var);
    if (pts.outFlagOfGhostNode == null)
      return;
    getChildOps().addKill(pts.outFlagOfGhostNode);
    bindPts(pts.withGhostNode(null));
  }


  public void assumePointsTo (HyperPointsToSet pointer, AddrVar target, VarSet contents) {
    for (NumVar content : contents)
      assumeConcrete(content);
    assumePointsTo(pointer, target);
  }

  void assumePointsTo (HyperPointsToSet pointer, AddrVar target) {
    boolean hasOne = false;
    for (P2<AddrVar, Linear> p : pointer) {
      AddrVar addr = p._1();
      if (target != null && addr.equalTo(target)) {
        hasOne = true;
        testEqualTo(p._2(), Linear.ONE);
      } else
        testEqualTo(p._2(), Linear.ZERO);
    }
    if (target == null) {
      assert !hasOne : "weird: there was a NULL entry in the points-to set";
        testEqualTo(pointer.sumOfFlags, Linear.ZERO);
    } else {
      if (!hasOne)
        throw new Unreachable();
        testEqualTo(pointer.sumOfFlags, Linear.ONE);
    }
  }

  private void testEqualTo (Linear linear, Linear value) {
    Test t = fin.equalTo(0, value, linear);
    getChildOps().addTest(t);
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("Childops: ");
    builder.append(getChildOps());
    builder.append('\n');
    builder.append(build());
    return builder.toString();
  }

  public void concretizeAndDisconnectNG (AddrVar s, VarSet cs) {
    for (PointsToSet pts : pointsToMap)
      if (cs.contains(pts.var))
        concretize(pts);
      else
        disconnect(s, pts);
  }

  private void disconnect (AddrVar s, PointsToSet pts) {
    if (pts.hasEntry(s)) {
      PointsToEntry e = pts.getEntry(s);
      getChildOps().addAssignment(fin.variable(0, e.flag), fin.literal(0, BigInt.ZERO));
    }
  }

  private void concretize (PointsToSet pts) {
    if (pts.hasGhostNode()) {
      getChildOps().addKill(pts.outFlagOfGhostNode);
      bindPts(pts.withGhostNode(null));
    }
  }

  public void bendBackGhostEdgesNG (AddrVar s, AddrVar c, VarSet svs, VarSet cvs, VarSet pts, VarSet ptc) {
    ListVarPair toExpandOnChild = new ListVarPair();
    for (NumVar sv : svs)
      bendBackGhostFlag(toExpandOnChild, sv, c);
    for (NumVar cv : cvs)
      bendBackGhostFlag(toExpandOnChild, cv, s);
    getChildOps().addExpandNG(toExpandOnChild);
    for (NumVar cv : cvs)
      killGhostFlag(cv);
    // since the connector data of pointers to the summary s has been treated like a part of s,
    // pointers to s are bent to c and have to be moved back to s
    for (NumVar sumvar : pts) {
      unfoldConnectorTarget(sumvar, c, s);
      unfoldConnectorTarget(sumvar, s, c);
    }
    for (NumVar concvar : ptc) {
      unfoldConnectorTarget(concvar, c, s);
      unfoldConnectorTarget(concvar, s, c);
    }
  }

  private void unfoldConnectorTarget (NumVar current, AddrVar s, AddrVar c) {
    PointsToSet epts = pointsToMap.get(current).get();
    //assert !epts.hasEntry(c) : "pts " + epts + " of var " + current + " should not have entry " + c;
    if (epts.hasEntry(s) && !epts.hasEntry(c)) {
      NumVar concFlag = NumVar.fresh();
      getChildOps().addIntro(concFlag);
      // getChildOps().addAssignment(fin.variable(0, concFlag), fin.range(0, Interval.ZEROorONE));
      bindPts(epts.bind(c, concFlag));
    }
  }

  private void killGhostFlag (NumVar cv) {
    PointsToSet pts = pointsToMap.get(cv).get();
    if (!pts.hasGhostNode())
      return;
    getChildOps().addKill(pts.outFlagOfGhostNode);
    bindPts(pts.withGhostNode(null));
  }

  private void bendBackGhostFlag (ListVarPair childExp, NumVar var, AddrVar target) {
    PointsToSet spts = getPts(var);
    if (spts.hasGhostNode()) {
      // this should only happen for regions with self-pointers
      assert !spts.hasEntry(target);
      NumVar newFlag = NumVar.fresh();
      spts = spts.bind(target, newFlag);
      childExp.add(spts.outFlagOfGhostNode, newFlag);
      bindPts(spts);
    }
  }

  void expandNG (List<VarPair> nvps) {
    FoldMap foldMap = FoldMap.fromList(nvps);
    ListVarPair innerExpansion = new ListVarPair();
    for (PointsToSet pts : pointsToMap)
      if (foldMap.isPermanent(pts.var))
        expandInnerNG(null, null, foldMap, innerExpansion, pts);
    getChildOps().addExpandNG(innerExpansion);
  }

  void expandNG (AddrVar summary, AddrVar concrete, List<VarPair> nvps) {
    FoldMap foldMap = FoldMap.fromList(nvps);
    ListVarPair innerExpansion = new ListVarPair();
    for (PointsToSet pts : pointsToMap)
      if (foldMap.isPermanent(pts.var))
        expandInnerNG(summary, concrete, foldMap, innerExpansion, pts);
      else if (pts.hasEntry(summary))
        expandOuterNG(summary, concrete, foldMap, innerExpansion, pts);
    introduce(concrete, Type.Address);
    getChildOps().addExpandNG(summary, concrete, innerExpansion);
  }

  private void expandOuterNG (AddrVar summary, AddrVar concrete, FoldMap foldMap, List<VarPair> innerVars,
      PointsToSet pts) {
    assert summary != null;
    assert concrete != null;
    assert !pts.hasEntry(concrete);
    assert !foldMap.isEphemeral(pts.var);
    PointsToEntry sumEntry = pts.getEntry(summary);
    if (sumEntry != null) {
      final NumVar freshFlag = PointsToStateBuilder.makeExpansion(innerVars, sumEntry.flag);
      bindPts(pts.bind(concrete, freshFlag));
    }
  }

  private void expandInnerNG (AddrVar summary, AddrVar concrete, FoldMap foldMap, List<VarPair> innerVars,
      PointsToSet pts) {
    assert summary == null == (concrete == null);
    NumVar eph = foldMap.getEphemeral(pts.var);
    innerVars.add(new VarPair(pts.var, eph));
    PointsToSet ephPts;
    // TODO also rename self-edges
    if (!pts.sumOfFlags.isConstantZero()) {
      NumVar c = PointsToStateBuilder.makeExpansion(innerVars, pts.sumOfFlags.numVar);
      ephPts = PointsToSet.empty(eph, c);
    } else
      ephPts = PointsToSet.empty(eph);
    if (pts.hasGhostNode()) {
      NumVar oflag = PointsToStateBuilder.makeExpansion(innerVars, pts.outFlagOfGhostNode);
      ephPts = ephPts.withGhostNode(oflag);
    }
    for (PointsToEntry entry : pts) {
      NumVar ef = PointsToStateBuilder.makeExpansion(innerVars, entry.flag);
      AddrVar address = entry.address;
      if (summary != null)
      {
        if (address.equalTo(summary))
          address = concrete;
      }
      ephPts = ephPts.bind(address, ef);
    }
    bindPts(ephPts);
  }

  PointsToSet bendGhostEdge (AddrVar other, ListVarPair innerFold, NumVar var) {
    PointsToSet ppts1 = getPts(var);
    PointsToEntry o = ppts1.getEntry(other);
    if (o == null) {
      NumVar zeroFlag1 = NumVar.fresh();
      getChildOps().addIntro(zeroFlag1, BigInt.ZERO);
      NumVar zeroFlag = zeroFlag1;
      ppts1 = ppts1.bind(other, zeroFlag);
      bindPts(ppts1);
    }
    PointsToEntry o1 = ppts1.getEntry(other);
    if (ppts1.outFlagOfGhostNode != null) {
      innerFold.add(new VarPair(o1.flag, ppts1.outFlagOfGhostNode));
    }
    ppts1 = ppts1.remove(other).withGhostNode(o1.flag);
    return ppts1;
  }

  void bendGhostEdgesNG (AddrVar summary, AddrVar concrete, VarSet svs, VarSet cvs, VarSet pts, VarSet ptc) {
    ListVarPair innerFold = new ListVarPair();
    for (NumVar var : svs) {
      PointsToSet ppts = bendGhostEdge(concrete, innerFold, var);
      bindPts(ppts);
    }
    for (NumVar var : cvs) {
      PointsToSet epts = bendGhostEdge(summary, innerFold, var);
      epts = epts.renameAddress(concrete, summary);
      bindPts(epts);
    }
    for (NumVar var : pts)
      foldPointers(var, summary, concrete);
    for (NumVar var : ptc) {
      foldPointers(var, concrete, summary);
    }
    getChildOps().addFoldNG(innerFold);
  }

  // hsi: here it would be more precise if we performed another, third, fold operation.
  // But for our purposes, it will suffice to assign [0,1] to the "folded" flag.
  private void foldPointers (NumVar var, AddrVar targetToKeep, AddrVar targetToRemove) {
    assert targetToKeep != null;
    assert targetToRemove != null;
    PointsToSet epts = getPts(var);
    // remove pointer to summary from pts
    PointsToEntry entryToKeep = epts.getEntry(targetToKeep);
    PointsToEntry entryToRemove = epts.getEntry(targetToRemove);
    if (entryToKeep == null && entryToRemove == null)
      return;
    if (entryToKeep != null && entryToRemove == null) {
      getChildOps().addAssignment(fin.variable(0, entryToKeep.flag), fin.range(0, Interval.ZEROorONE));
    } else if (entryToKeep == null && entryToRemove != null) {
      getChildOps().addAssignment(fin.variable(0, entryToRemove.flag), fin.range(0, Interval.ZEROorONE));
      epts = epts.remove(entryToRemove).bind(targetToKeep, entryToRemove.flag);
      bindPts(epts);
    } else if (entryToKeep != null && entryToRemove != null) {
      getChildOps().addAssignment(fin.variable(0, entryToKeep.flag), fin.range(0, Interval.ZEROorONE));
      getChildOps().addKill(entryToRemove.flag);
      epts = epts.remove(entryToRemove);
      bindPts(epts);
    }
  }

  void foldInnerVar (FoldMap outer, ListVarPair innerVars, PointsToSet current) {
    PointsToSet current1 = current;
    NumVar eph = outer.getEphemeral(current1.var);
    PointsToSet ephPts = getPts(eph);
    innerVars.add(current1.var, eph);
    current1 = step2a1__foldSumFlag(innerVars, current1, ephPts);
    current1 = step2a2__foldGhostNode(innerVars, current1, ephPts);
    current1 = step2a3__foldPtsEntries(innerVars, current1, ephPts);
    bindPts(current1);
    remove(eph);
  }

  void foldInnerVar (AddrVar p, AddrVar e, FoldMap outer, ListVarPair innerVars, PointsToSet current) {
    PointsToSet current1 = current;
    NumVar eph = outer.getEphemeral(current1.var);
    PointsToSet ephPts = getPts(eph);
    innerVars.add(current1.var, eph);
    current1 = step2a1__foldSumFlag(innerVars, current1, ephPts);
    current1 = step2a2__foldGhostNode(innerVars, current1, ephPts);
    current1 = step2a3__foldPtsEntries(p, e, innerVars, current1, ephPts);
    bindPts(current1);
    remove(eph);
  }

  PointsToSet step2a1__foldSumFlag (ListVarPair innerVars, PointsToSet tfst, PointsToSet tsnd) {
    final boolean fstHasSum = tfst.sumOfFlags.isConstantZero();
    final boolean sndHasSum = tsnd.sumOfFlags.isConstantZero();
    if (!sndHasSum && !fstHasSum) {
      innerVars.add(tfst.sumOfFlags.numVar, tsnd.sumOfFlags.numVar);
    } else if (!sndHasSum && fstHasSum) {
      NumVar v = introZeroVar();
      tfst = tfst.withSumVar(v);
      innerVars.add(v, tsnd.sumOfFlags.numVar);
    } else if (sndHasSum && !fstHasSum) {
      NumVar v = introZeroVar();
      innerVars.add(tfst.sumOfFlags.numVar, v);
    }
    return tfst;
  }

  PointsToSet step2a2__foldGhostNode (ListVarPair innerVars, PointsToSet tfst, PointsToSet tsnd) {
    boolean fstHasGhost = tfst.hasGhostNode();
    boolean sndHasGhost = tsnd.hasGhostNode();
    if (!fstHasGhost && sndHasGhost) {
      tfst = tfst.withGhostNode(introZeroVar());
      innerVars.add(tfst.outFlagOfGhostNode, tsnd.outFlagOfGhostNode);
    } else if (fstHasGhost && !sndHasGhost) {
      tsnd = tsnd.withGhostNode(introZeroVar());
      innerVars.add(tfst.outFlagOfGhostNode, tsnd.outFlagOfGhostNode);
    } else if (fstHasGhost && sndHasGhost) {
      innerVars.add(tfst.outFlagOfGhostNode, tsnd.outFlagOfGhostNode);
    }
    return tfst;
  }

  NumVar introZeroVar () {
    NumVar fstGhost = NumVar.fresh();
    getChildOps().addIntroZero(fstGhost);
    return fstGhost;
  }

  PointsToSet step2a3__foldPtsEntries (AddrVar p, AddrVar e, ListVarPair innerVars, PointsToSet tfst, PointsToSet tsnd) {
    // XXX have to make sure that tfst only points to perm and tsnd only points to eph
    //assert tfst.getEntry(e) == null;
    //assert tsnd.getEntry(p) == null;
    PointsToEntry sndSelfPtr = tsnd.getEntry(e);
    if (sndSelfPtr != null) {
      tsnd = tsnd.bind(p, sndSelfPtr.flag);
      tsnd = tsnd.remove(e);
//      assert sndSelfPtr == null;
    }
    return step2a3__foldPtsEntries(innerVars, tfst, tsnd);
  }

  PointsToSet step2a3__foldPtsEntries (ListVarPair innerVars, PointsToSet tfst, PointsToSet tsnd) {
    Set<AddrVar> allAddresses = tsnd.allAddresses();
    allAddresses.addAll(tfst.allAddresses());
    for (AddrVar av : allAddresses) {
      PointsToEntry ev = tsnd.getEntry(av);
      NumVar evFlag;
      if (ev == null) {
        evFlag = NumVar.fresh();
        getChildOps().addIntroZero(evFlag);
      } else
        evFlag = ev.flag;
      PointsToEntry pv = tfst.getEntry(av);
      NumVar pvFlag;
      if (pv == null) {
        pvFlag = NumVar.fresh();
        getChildOps().addIntroZero(pvFlag);
        tfst = tfst.bind(av, pvFlag);
      } else
        pvFlag = pv.flag;
      innerVars.add(pvFlag, evFlag);
    }
    return tfst;
  }

  void foldOuterVar (AddrVar p, AddrVar e, ListVarPair innerVars, PointsToSet current) {
    PointsToEntry pe = current.getEntry(p);
    PointsToEntry ee = current.getEntry(e);
    if (pe == null && ee != null) {
      NumVar freshVar = introZeroVar();
      pe = new PointsToEntry(p, freshVar);
      PointsToSet removed = current.bind(pe).remove(ee);
      bindPts(removed);
      innerVars.add(pe.flag, ee.flag);
    } else if (pe != null && ee == null) {
      NumVar freshVar = introZeroVar();
      ee = new PointsToEntry(e, freshVar);
      innerVars.add(pe.flag, ee.flag);
    } else if (pe != null && ee != null) {
      bindPts(current.remove(ee));
      innerVars.add(pe.flag, ee.flag);
    }
  }

  void foldNG (AddrVar p, AddrVar e, ListVarPair nvps) {
    FoldMap outer = FoldMap.fromList(nvps);
    ListVarPair innerVars = new ListVarPair();
    for (PointsToSet current : pointsToMap)
      if (outer.isPermanent(current.var)) {
        foldInnerVar(p, e, outer, innerVars, current);
      } else if (!outer.isEphemeral(current.var)) {
        foldOuterVar(p, e, innerVars, current);
      }
    remove(e);
    getChildOps().addFoldNG(p, e, innerVars);
  }

  void foldNG (ListVarPair nvps) {
    FoldMap outer = FoldMap.fromList(nvps);
    ListVarPair innerVars = new ListVarPair();
    for (PointsToSet current : pointsToMap)
      if (outer.isPermanent(current.var)) {
        foldInnerVar(outer, innerVars, current);
      }
    getChildOps().addFoldNG(innerVars);
  }

  static NumVar makeExpansion (List<VarPair> innerVars, NumVar epf) {
    final NumVar freshFlag = NumVar.fresh();
    innerVars.add(new VarPair(epf, freshFlag));
    return freshFlag;
  }
}
