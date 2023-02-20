package bindead.domains.fields;

import java.util.LinkedList;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.FiniteRange;
import javalx.numeric.Interval;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.AVLSet;
import javalx.persistentcollections.ThreeWaySplit;
import javalx.persistentcollections.tree.FiniteRangeTree;
import javalx.persistentcollections.tree.OverlappingRanges;
import rreil.lang.Field;
import rreil.lang.MemVar;
import rreil.lang.Rhs.Lin;
import rreil.lang.Rhs.Rlit;
import rreil.lang.Rhs.Rval;
import rreil.lang.Rhs.Rvar;
import rreil.lang.Test;
import bindead.abstractsyntax.finite.Finite;
import bindead.abstractsyntax.finite.Finite.Cmp;
import bindead.abstractsyntax.finite.Finite.Lhs;
import bindead.abstractsyntax.finite.Finite.Rhs;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.ListVarPair;
import bindead.data.MemVarPair;
import bindead.data.MemVarSet;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.domainnetwork.combinators.FiniteStateBuilder;
import bindead.domainnetwork.interfaces.ContentCtx;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domains.fields.FieldGraph.Partitioning;
import bindead.domains.pointsto.PointsToProperties;
import bindead.exceptions.DomainStateException;
import bindead.exceptions.DomainStateException.InvariantViolationException;
import bindead.exceptions.DomainStateException.VariableSupportSetException;
import binparse.Endianness;

/**
 * Builder class for {@link bindead.domains.fields.FieldsCtx}s.
 */
class FieldStateBuilder<D extends FiniteDomain<D>> extends FiniteStateBuilder {
  private static final FiniteFactory fin = FiniteFactory.getInstance();
  RegionMap regions;
  private AVLMap<MemVar, ClobberedMap> clobbered;

  private final List<NumVar> tempVars;

  private final boolean DEBUG = PointsToProperties.INSTANCE.debugOther.isTrue();

  @SuppressWarnings("unused") private void msg (String s) {
    if (DEBUG)
      System.out.println("FieldStateBuilder: " + s + "\n");
  }

  /**
   * Create a builder from the given context {@code ctx}.
   *
   * @param state The initial context for the builder.
   */
  FieldStateBuilder (FieldState state) {
    regions = state.regions;
    clobbered = state.clobbered;
    tempVars = new LinkedList<NumVar>();
  }

  FieldState build () {
    return new FieldState(regions, clobbered);
  }

  private ClobberedMap getClobberedMap (MemVar varId) {
    return clobbered.get(varId).getOrElse(ClobberedMap.empty());
  }

  /**
   * Resolves the given field to a left-hand side expression. If the field does
   * not exist, a fresh variable context will be created. Any overlapping
   * fields will be removed.
   *
   * @param field The field to be resolved.
   * @param channel The channel to query child domains
   * @return The left-hand side finite variable to be used for further
   *         processing.
   */
  Finite.Lhs resolveLhs (rreil.lang.Lhs lhs) {
    MemVar regionId = lhs.getRegionId();
    FiniteRange range = lhs.bitRange();
    markClobbered(regionId, range);
    Option<VariableCtx> matching = regions.get(regionId, range);

    /* TODO do not project all, keep upper bits
     * for assigments like
     *
     * X :=32 0
     * X :=8  Y
     * Z :=32 X
     *
     * where with the urrent implementation variable Z will
     * contain TOP instead of the zero-extended value of Y
     */

    // Check for a concrete/full field match.
    OverlappingRanges<VariableCtx> overlaps = regions.searchOverlaps(regionId, range.toInterval());
    // Project out all overlapping variable contexts not matching key.
    for (P2<FiniteRange, VariableCtx> overlapping : overlaps) {
      if (matching.isNone() || !overlapping._1().isEqualTo(range)) {
        removeOverlappedField(regionId, overlapping);
      }
    }
    final NumVar variable;
    if (matching.isSome()) {
      variable = matching.get().getVariable();
    } else {
      // Introduce fresh left-hand side variable.
      variable = introduceAndResolve(Field.field(lhs));
    }
    return fin.variable(lhs.getSize(), variable);
  }

  private void removeOverlappedField (MemVar regionId, P2<FiniteRange, VariableCtx> ctx) {
    regions = regions.remove(regionId, ctx._1());
    getChildOps().addKill(ctx._2().getVariable());
  }

  /**
   * Resolves the given field to a linear expression denoting the fields
   * numeric valuation. Returns {@code null} if the given field cannot be
   * resolved to a linear expression.
   *
   * @param field The field to be resolved.
   * @param channel The channel to query child domains
   * @return The finite expression to be used for further processing /
   *         evaluation or {@code null} if the field cannot be resolved.
   */
  private Finite.Rlin resolve (Field field) {
    // First check for a concrete/full field match (this is faster than a search for overlaps).
    MemVar varId = field.getRegion();
    FiniteRange range = field.finiteRangeKey();
    Option<VariableCtx> matching = regions.get(varId, range);
    if (matching.isSome())
      return fin.linear(matching.get());
    OverlappingRanges<VariableCtx> overlapping = regions.searchOverlaps(varId, range.toInterval());
    // Second, if there are no overlapping fields, introduce a fresh variable context.
    if (overlapping.isEmpty()) {
      // Introduce fresh variable.
      NumVar fieldVar = allocate(field);
      return fin.linear(field.getSize(), fieldVar);
    }
    // Otherwise, resolve overlapping fields.
    return resolveFromOverlapping(overlapping, field);
  }

  private NumVar allocate (Field field) {
    MemVar regionId = field.getRegion();
    Option<Region> optionalRegion = regions.map.get(regionId);
    if (optionalRegion.isNone()) {
      // this may happen in the DebugPrinter, but nowhere else
      String string = "field " + field + " refers to unintroduced region " + field.getRegion();
      throw new DomainStateException.InvariantViolationException(string);
      // assert false : string;
    }
    NumVar fieldVar = introduceAndResolve(field);
    int size = field.getSize();
    MemVar varId = field.getRegion();
    FiniteRange intervalKey = field.finiteRangeKey();
    int bitOffset = field.getOffset();
    Rhs rhs;
    Option<ContentCtx> ctx = regions.getSegment(varId);
    if (ctx.isSome() && !isClobbered(varId, intervalKey)) {
      assertByteAlignment(size, bitOffset);
      // hsi: offset and size should be okay here, otherwise something else went wrong
      BigInt value = ctx.get().read(bitOffset / 8, size / 8, Endianness.LITTLE);
      rhs = fin.literal(size, value);
    } else
      rhs = fin.range(size, Interval.unsignedTop(size));
    Lhs lhs = fin.variable(size, fieldVar);
    getChildOps().addAssignment(lhs, rhs);
    return fieldVar;
  }

  private static void assertByteAlignment (int size, int bitOffset) {
    if (size % 8 != 0 || bitOffset % 8 != 0)
      throw new InvariantViolationException();
  }

  private boolean isClobbered (MemVar varId, FiniteRange range) {
    return getClobberedMap(varId).isClobbered(range);
  }

  private void markClobbered (MemVar varId, FiniteRange range) {
    ClobberedMap clobberdInRegion = getClobberedMap(varId);
    clobbered = clobbered.bind(varId, clobberdInRegion.markClobbered(range));
  }

  /**
   * Introduces the field as a variable and resolves it.
   *
   * @param field The field to be resolved.
   * @return The resolved field.
   */
  private NumVar introduceAndResolve (Field field) {
    NumVar variable;
    // NOTE: Introduce flag sized variables as flags. We do this as flags won't be widened if inside the boolean range.
    if (field.finiteRangeKey().isEqualTo(Field.finiteRangeKey(0, 1)))
      variable = NumVar.freshFlag();
    else
      variable = NumVar.fresh();
    variable.setRegionName(field.getRegion(), field.getOffset());
    MemVar regionId = field.getRegion();
    Option<Region> optionalRegion = regions.map.get(regionId);
    // assert optionalRegion.isSome();
    Region region = optionalRegion.get();
    regions = regions.bind(regionId, region.addField(field, variable));
    getChildOps().addIntro(variable);
    return variable;
  }

  private Rlin resolveFromOverlapping (OverlappingRanges<VariableCtx> overlapping, Field field) {
    overlapping.sortByFiniteRangeKey();
    if (isConvertible(overlapping, field)) {
      return downcastConversion(overlapping, field);
    }
    Rlin rhs = tryLinearization(overlapping, field.finiteRangeKey());
    if (rhs != null)
      return rhs;
    if (!isClobbered(overlapping, field)) {
      NumVar fieldVar = allocate(field);
      return fin.linear(field.getSize(), fieldVar);
    }
    return null;
  }

  private boolean isClobbered (OverlappingRanges<VariableCtx> overlapping, Field field) {
    for (P2<FiniteRange, VariableCtx> overlap : overlapping)
      if (isClobbered(field.getRegion(), overlap._1()))
        return true;
    return false;
  }

  private Rlin downcastConversion (OverlappingRanges<VariableCtx> overlapping, Field field) {
    // Create "temporary" field
    NumVar fieldVar = introduceAndResolve(field);
    int size = field.getSize();
    Rlin rhs = fin.linear(overlapping.getFirst()._2());
    getChildOps().addAssignment(fin.variable(size, fieldVar), fin.convert(rhs));
    return fin.linear(size, fieldVar);
  }

  private static boolean isConvertible (OverlappingRanges<VariableCtx> overlapping, Field field) {
    if (overlapping.size() == 1) {
      FiniteRange existing = overlapping.getFirst()._1();
      FiniteRange key = field.finiteRangeKey();
      if (lowAlignedAndContainedFinite(key, existing))
        return true;
    }
    return false;
  }

  private static boolean lowAlignedAndContainedFinite (FiniteRange key, FiniteRange existing) {
    if (key.low().isFinite() && key.high().isFinite())
      return key.low().asInteger().isEqualTo(existing.low().asInteger()) && key.high().compareTo(existing.high()) <= 0;
    return false;
  }

  private Rlin tryLinearization (OverlappingRanges<VariableCtx> overlapping, FiniteRange key) {
    FieldGraph g = FieldGraph.build(overlapping);
    Partitioning path = g.findPartitioning(key.low(), key.high());
    Option<FiniteRange> span = path.span();
    if (span.isNone() || !key.isEqualTo(span.get()))
      return null;
    return linearize(path, key.getSpan().intValue());
  }

  /**
   * Linearize the given path to a linear expression.
   *
   * @param path The path to linearize.
   * @param size The size annotation of the right-hand side (the resulting
   *          linear expression).
   * @return The linearized path.
   */
  private Rlin linearize (Partitioning path, int size) {
    Linear lin = Linear.ZERO;
    int baseOffset = path.get(0)._1().low().asInteger().intValue();
    for (int i = 0; i < path.size() - 1; i++) {
      P2<FiniteRange, VariableCtx> ctx = path.get(i);
      FiniteRange intervalKey = ctx._1();
      NumVar sourceVar = ctx._2().getVariable();
      int currentOffset = intervalKey.low().asInteger().intValue();
      int span = intervalKey.getSpan().intValue();
      BigInt coeff = BigInt.powerOfTwo(currentOffset - baseOffset);
      Rhs converted = fin.convert(fin.linear(span, sourceVar));
      // assert false : "make convvar from " + path + " converted " + converted+" of size "+size;
      NumVar tempVar = makeConvertedVar(size, converted);
      lin = lin.addTerm(coeff, tempVar);
    }

    P2<FiniteRange, VariableCtx> ctx = path.get(path.size() - 1);
    FiniteRange intervalKey = ctx._1();
    int currentOffset = intervalKey.low().asInteger().intValue();
    BigInt coeff = BigInt.powerOfTwo(currentOffset - baseOffset);
    lin = lin.addTerm(coeff, ctx._2().getVariable());

    return fin.linear(size, lin);
  }

  private NumVar makeConvertedVar (int size, Rhs converted) {
    /**
     * FIXME hsi: we cannot return linear expressions that refer to temp vars. (See applyDelayedChildOps)
     * solutions:
     * (1) create overlapping entry for temp var in intervalTree
     * (2) delegate killing of tempVars to user of MemDeref
     * (3) return range instead
     * (4) Do not export NumVars wrapped in Rlin's, find a better design based on (MemVar, Offset) tuples
     *
     * I prefer solution (4)
     *
     * Note that assignments already work even if the rhs contains temp vars. this is because.
     */
    NumVar tempVar = NumVar.fresh();
    getChildOps().addIntro(tempVar);
    getChildOps().addAssignment(fin.variable(size, tempVar), converted);
    // assert false : "we do not want to use tempVars anymore!";
    tempVars.add(tempVar);
    return tempVar;
  }

  /**
   * Introduce a fresh region with id {@code region} and the given region context.
   *
   * @param varId The new region identifier.
   * @param regionCtx The region context.
   * @return The updated builder.
   * @throws DomainStateException if the region identifier already exists in this builder
   */
  void introduce (MemVar varId, RegionCtx regionCtx) throws DomainStateException {
    if (regions.contains(varId))
      throw new VariableSupportSetException("tried to introduce MemVar " + varId + " that already exists");
    regions = regions.bind(varId, Region.empty(regionCtx));
  }

  void rename (MemVar from, MemVar to) {
    Region region = regions.get(from);
    FoldMap toRename = new FoldMap();
    region = region.unfoldCopy(toRename, to);
    for (VarPair vp : toRename)
      getChildOps().addSubst(vp.getPermanent(), vp.getEphemeral());
    regions = regions.remove(from).bind(to, region);
    Option<ClobberedMap> clob = clobbered.get(from);
    if (clob.isSome())
      clobbered = clobbered.remove(from).bind(to, clob.get());
  }

  /**
   * Ensure that the variable support set is compatible across {@code this} and {@code other}.
   *
   * @param other
   */
  void makeCompatible (FieldStateBuilder<D> other) {
    //  msg("ensureCompatibleVariableSupport " + this);
    // msg("ensureCompatibleVariableSupport " + other);
    ThreeWaySplit<RegionMap> split = regions.split(other.regions);
    assert split.onlyInFirst().isEmpty() : "this region-map contains regions not in other: " + split.onlyInFirst();
    assert split.onlyInSecond().isEmpty() : "other region-map contains regions not in this: " + split.onlyInSecond();
    RegionMap differing = split.inBothButDiffering();
    for (P2<MemVar, Region> binding : differing.map) {
      MemVar varId = binding._1();
      Region fst = binding._2();
      Region snd = other.regions.get(varId);
      Region intervalTree = fst.mergeRegions(snd, getChildOps(), other.getChildOps());
      regions = regions.bind(varId, intervalTree);
    }
    mergeClobbered(other);
  }

  private void mergeClobbered (FieldStateBuilder<D> other) {
    ThreeWaySplit<AVLMap<MemVar, ClobberedMap>> split = clobbered.split(other.clobbered);
    for (P2<MemVar, ClobberedMap> binding : split.onlyInSecond()) {
      clobbered = clobbered.bind(binding._1(), binding._2());
    }
    for (P2<MemVar, ClobberedMap> binding : split.inBothButDiffering()) {
      MemVar r = binding._1();
      ClobberedMap fromFst = binding._2();
      ClobberedMap fromSnd = other.clobbered.get(r).get();
      clobbered = clobbered.bind(r, fromFst.union(fromSnd));
    }
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("Childops: " + getChildOps());
    builder.append("\n");
    builder.append(build());
    return builder.toString();
  }

  /**
   * Remove all fields overlapping the given range.
   *
   * @param varId The region identifier of whom to remove the overlapping
   *          fields.
   * @param range The range.
   * @return The updated builder.
   */
  void removeOverlapping (MemVar varId, Interval range) {
    OverlappingRanges<VariableCtx> overlapping = regions.searchOverlaps(varId, range);
    for (P2<FiniteRange, VariableCtx> ctx : overlapping) {
      getChildOps().addKill(ctx._2().getVariable());
      regions = regions.remove(varId, ctx._1());
    }
  }

  <D2 extends FiniteDomain<D2>> D2 applyDelayedKillOps (D2 state) {
    //  msg("delayedChildOps and temps " + tempVars);
    for (NumVar v : tempVars) {
      state = state.project(v);
    }
    return state;
  }

  void projectRegion (MemVar regionId) {
    Region region = regions.get(regionId);
    removeClobbered(regionId);
    regions = regions.remove(regionId);
    for (P2<FiniteRange, VariableCtx> vctx : region.fields) {
      VariableCtx vc = vctx._2();
      getChildOps().addKill(vc.getVariable());
    }
  }

  void removeClobbered (MemVar regionId) {
    clobbered = clobbered.remove(regionId);
  }

  /**
   * Translate an assignment in L(imp) to an assignment in L(finite).
   */
  void runAssign (QueryChannel channel, rreil.lang.Lhs lhs, rreil.lang.Rhs rhs) {
    Finite.Lhs flhs = resolveLhs(lhs);
    Finite.Rhs frhs = resolveRhs(rhs);
    if (frhs == null)
      frhs = fin.range(lhs.getSize(), Interval.unsignedTop(lhs.getSize()));
    getChildOps().addAssignment(flhs, frhs);
  }

  /**
   * Translate a rhs in L(imp) to an assignment in L(finite).
   */
  Finite.Rhs resolveRhs (rreil.lang.Rhs rhs) {
    FieldVisitor<D> visitor = new FieldVisitor<D>(this);
    return rhs.accept(visitor, null);
  }

  /**
   * Translate a rhs in L(imp) to an assignment in L(finite).
   */
  Finite.Rlin resolveRval (rreil.lang.Rhs.Rval rhs) {
    FieldVisitor<D> visitor = new FieldVisitor<D>(this);
    return (Rlin) rhs.accept(visitor, null);
  }

  @Deprecated// hsi: we cannot return Rlin's to where the NumVars are not defined
  Finite.Rlin resolve (Rval value) {
    if (value instanceof Rlit)
      return fin.literal(value.getSize(), ((Rlit) value).getValue());
    Rvar rvar = (Rvar) value;
    final Field field = Field.field(rvar);
    return resolve(field);
  }

  private Option<Rlin> resolve (Lin value) {
    Rhs resolved = resolveRhs(value);
    if (resolved instanceof Rlin) {
      // FIXME: should be an Rlin:
      return Option.some((Rlin) resolved);
      //NumVar pointerNumVar = ((Rlin)resolved).getLinearTerm().getSingleVarOrNull();
      //return Option.fromNullable(pointerNumVar);
    }
    return Option.none();
  }

  /**
   * Translate a test in L(imp) to a test in L(finite).
   */
  void runTest (QueryChannel channel, Test test) {
    Rhs cmp = resolveRhs(test.getComparison());
    assert cmp != null;
    assert cmp instanceof Finite.Cmp;
    getChildOps().addTest(fin.test((Cmp) cmp));
  }

  D applyReorderedChildOps (D cs) {
    final D cs1 = applyChildOps(cs);
    final D cs2 = applyDelayedKillOps(cs1);
    return cs2;
  }


  boolean hasNoTempVars () {
    return tempVars.isEmpty();
  }

  void assumePointsTo (Lin pointerValue, AddrVar target, MemVar region) {
    // FIXME: should be an Rlin from resolve and requires handling here.
    Option<Rlin> resolved = resolve(pointerValue);
    if (resolved.isSome()) {
      Rlin pointerNumVar = resolved.get();
      VarSet contents = VarSet.empty();
      if (region != null) {
        Region reg = regions.get(region);
        for (P2<FiniteRange, VariableCtx> x : reg.fields) {
          contents = contents.add(x._2().getVariable());
        }
      }
      getChildOps().addDerefTarget(pointerNumVar, target, contents);
    }
  }

  public void copyMemRegion (MemVar fromAdr, MemVar toAdr) {
    Region from = regions.get(fromAdr);
    Region to = Region.empty(from.context);
    for (P2<FiniteRange, VariableCtx> x : from.fields) {
      VariableCtx ctx = x._2();
      NumVar newVar = NumVar.fresh();
      getChildOps().addHardcopy(newVar, ctx.getVariable());
      to = to.addField(x._1(), newVar);
    }
    regions = regions.bind(toAdr, to);
  }

  void copyAndPaste (MemVarSet memvars, Fields<D> other) {
    VarSet cnpVars = VarSet.empty();
    for (MemVar mv : memvars) {
      Region r = other.state.regions.get(mv);
      cnpVars = cnpVars.union(r.containedNumVars());
      regions = regions.bind(mv, r);
    }
    getChildOps().addCopyAndPaste(cnpVars, other.childState);
  }

  public void testmemvarsEqual (MemVar m1, MemVar m2) {
    regions.get(m1).testEqual(regions.get(m2), getChildOps());
  }

  public void assumeEdgeNG (Lin fieldThatPoints, AddrVar address) {
    Option<Rlin> resolved = resolve(fieldThatPoints);
    if (resolved.isSome()) {
      Rlin pointerNumVar = resolved.get();
      assert pointerNumVar != null;
      getChildOps().addAssumeEdgeNG(pointerNumVar, address);
    }
  }

  void unfoldClobbered (MemVar permanent, MemVar ephemeral) {
    ClobberedMap orNull = clobbered.getOrNull(permanent);
    if (orNull == null)
      return;
    clobbered = clobbered.bind(ephemeral, orNull);
  }


  public void expandNG (List<MemVarPair> mvps) {
    final ListVarPair numVars = collectExpandVars(mvps);
    getChildOps().addExpandNG(numVars);
  }

  void expandNG (AddrVar address, AddrVar address2, List<MemVarPair> mvps) {
    final ListVarPair numVars = collectExpandVars(mvps);
    getChildOps().addExpandNG(address, address2, numVars);
  }

  public void foldNG (AddrVar address, AddrVar address2, List<MemVarPair> mvps) {
    final ListVarPair numVars = collectFoldVars(mvps);
    for (MemVarPair mvp : mvps)
      regions = regions.remove(mvp.getEphemeral());
    getChildOps().addFoldNG(address, address2, numVars);
  }

  public void foldNG (List<MemVarPair> mvps) {
    final ListVarPair numVars = collectFoldVars(mvps);
    for (MemVarPair mvp : mvps)
      regions = regions.remove(mvp.getEphemeral());
    getChildOps().addFoldNG(numVars);
  }

  private ListVarPair collectFoldVars (List<MemVarPair> mvps) {
    final ListVarPair numVars = new ListVarPair();
    for (MemVarPair mvp : mvps) {
      MemVar permanent = mvp.getPermanent();
      MemVar ephemeral = mvp.getEphemeral();
      Region summary = regions.get(permanent);
      Region concrete = regions.get(ephemeral);
      ThreeWaySplit<FiniteRangeTree<VariableCtx>> split = summary.fields.split(concrete.fields);
      for (P2<FiniteRange, VariableCtx> x : split.onlyInFirst()) {
        NumVar v = NumVar.fresh();
        getChildOps().addIntro(v);
        numVars.add(x._2().getVariable(), v);
      }
      for (P2<FiniteRange, VariableCtx> x : split.onlyInSecond()) {
        NumVar v = NumVar.fresh();
        getChildOps().addIntro(v);
        numVars.add(v, x._2().getVariable());
        summary = summary.addField(x._1(), v);
        regions = regions.bind(permanent, summary);
      }
      for (P2<FiniteRange, VariableCtx> x : split.inBothButDiffering()) {
        FiniteRange r = x._1();
        VariableCtx sv = x._2();
        VariableCtx cv = concrete.fields.getOrNull(r);
        assert cv != null;
        numVars.add(new VarPair(sv.getVariable(), cv.getVariable()));
      }
      /*      FiniteRangeTree<VariableCtx> copiedFields = FiniteRangeTree.<VariableCtx>empty();
           for (P2<FiniteRange, VariableCtx> p2 : summary.fields) {
             throw new UnimplementedException();
             VariableCtx ctx = p2._2();
             NumVar nv = NumVar.fresh();
             FiniteRange range = p2._1();
             setFieldVarName(ephemeral, nv, range);
             VariableCtx newCtx = new VariableCtx(ctx.getSize(), nv);
             copiedFields = copiedFields.bind(range, newCtx);
             numVars.add(new VarPair(ctx.getVariable(), nv));
           }
           Region fields2 = new Region(copiedFields, r1.context);
           regions = regions.bind(ephemeral, fields2);
           unfoldClobbered(permanent, ephemeral);
         }*/
    }
    return numVars;
  }

  private ListVarPair collectExpandVars (List<MemVarPair> mvps) {
    final ListVarPair numVars = new ListVarPair();
    for (MemVarPair mvp : mvps) {
      MemVar permanent = mvp.getPermanent();
      MemVar ephemeral = mvp.getEphemeral();
      Region r1 = regions.get(permanent);
      // msg("unfoldCopy " + varsToExpand + " region: " + region);
      FiniteRangeTree<VariableCtx> copiedFields = FiniteRangeTree.<VariableCtx>empty();
      for (P2<FiniteRange, VariableCtx> p2 : r1.fields) {
        // XXX have to avoid self pointers here!!!
        //
        VariableCtx ctx = p2._2();
        NumVar nv = NumVar.fresh();
        FiniteRange range = p2._1();
        setFieldVarName(ephemeral, nv, range);
        VariableCtx newCtx = new VariableCtx(ctx.getSize(), nv);
        copiedFields = copiedFields.bind(range, newCtx);
        numVars.add(new VarPair(ctx.getVariable(), nv));
      }
      Region fields2 = new Region(copiedFields, r1.context);
      regions = regions.bind(ephemeral, fields2);
      unfoldClobbered(permanent, ephemeral);
    }
    return numVars;
  }

  void setFieldVarName (MemVar ephemeral, NumVar nv, FiniteRange range) {
    if (ephemeral != null) {
      Bound low = range.low();
      int intValue = low.isFinite() ? low.asInteger().intValue() : 0;
      nv.setRegionName(ephemeral, intValue);
    }
  }

  public void concretizeAndDisconnectNG (AddrVar summary, AVLSet<MemVar> concreteNodes) {
    VarSet vs = VarSet.empty();
    for (MemVar m : concreteNodes)
      vs = vs.union(regions.get(m).containedNumVars());
    getChildOps().addConcretiseAndDisconnectNG(summary, vs);
  }

  public void bendBackGhostEdgesNG (AddrVar concrete, AddrVar summary, MemVarSet sContents, MemVarSet cContents,
      MemVarSet pointingToSummary, MemVarSet pointingToConcrete) {
    VarSet svs = collectContents(sContents);
    VarSet cvs = collectContents(cContents);
    VarSet pts = collectContents(pointingToSummary);
    VarSet ptc = collectContents(pointingToConcrete);
    getChildOps().addBendBackGhostEdgesNG(summary, concrete, svs, cvs, pts, ptc);
  }

  public void bendGhostEdgesNG (AddrVar concrete, AddrVar summary, MemVarSet sContents, MemVarSet cContents,
      MemVarSet pointingToSummary, MemVarSet pointingToConcrete) {
    VarSet svs = collectContents(sContents);
    VarSet cvs = collectContents(cContents);
    VarSet pts = collectContents(pointingToSummary);
    VarSet ptc = collectContents(pointingToConcrete);
    getChildOps().addBendGhostEdgesNG(summary, concrete, svs, cvs, pts, ptc);
  }

  private VarSet collectContents (MemVarSet sContents) {
    VarSet svs = VarSet.empty();
    for (MemVar v : sContents) {
      svs = svs.union(regions.contentVars(v));
    }
    return svs;
  }
}
