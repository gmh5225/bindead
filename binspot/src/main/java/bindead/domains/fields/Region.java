package bindead.domains.fields;

import java.util.Iterator;

import javalx.data.products.P2;
import javalx.numeric.Bound;
import javalx.numeric.FiniteRange;
import javalx.persistentcollections.ThreeWaySplit;
import javalx.persistentcollections.tree.FiniteRangeTree;
import rreil.lang.Field;
import rreil.lang.MemVar;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.FoldMap;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.combinators.FiniteSequence;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domains.pointsto.PointsToProperties;

class Region {
  final FiniteRangeTree<VariableCtx> fields;
  final RegionCtx context;

  private final boolean DEBUG = PointsToProperties.INSTANCE.debugOther.isTrue();

  @SuppressWarnings("unused") private void msg (String s) {
    if (DEBUG)
      System.out.println("fields.Region: " + s + "\n");
  }

  Region (final FiniteRangeTree<VariableCtx> fields, RegionCtx ctx) {
    this.fields = fields;
    this.context = ctx;
  }

  static Region empty (RegionCtx ctx) {
    return new Region(FiniteRangeTree.<VariableCtx>empty(), ctx);
  }

  Region addField (Field field, NumVar fieldVar) {
    FiniteRange interval = field.finiteRangeKey();
    return new Region(fields.bind(interval, new VariableCtx(field.getSize(), fieldVar)), context);
  }

  Region addField (FiniteRange interval, NumVar en) {
    int size = interval.getSpan().intValue();
    assert size > 0;
    return new Region(fields.bind(interval, new VariableCtx(size, en)), context);
  }

  Region remove (FiniteRange field) {
    return new Region(fields.remove(field), context);
  }


  private RegionCtx mergeContexts (Region snd) {
    // we assume that "special" contexts only exist for predefined regions.
    // therefore, we can safely pick the first context instead of merging
    return context;
  }

  private void makeCompatible (Region snd, FiniteSequence oco, FiniteRangeTree<VariableCtx> inBothButDiffering) {
    for (P2<FiniteRange, VariableCtx> field : inBothButDiffering) {
      NumVar fromFst = fields.get(field._1()).get().getVariable();
      NumVar fromSnd = snd.fields.get(field._1()).get().getVariable();
      if (!fromFst.equalTo(fromSnd))
        oco.addSubst(fromSnd, fromFst);
    }
  }

  Region mergeRegions (Region snd, FiniteSequence co, FiniteSequence oco) {
    ThreeWaySplit<FiniteRangeTree<VariableCtx>> split = fields.split(snd.fields);
    // Introduce all variables that are only in this in other.
    for (P2<FiniteRange, VariableCtx> field : split.onlyInFirst()) {
      NumVar variable = field._2().getVariable();
      oco.addIntro(variable);
    }
    // Introduce all variables that are only in other in this.
    for (P2<FiniteRange, VariableCtx> field : split.onlyInSecond()) {
      NumVar variable = field._2().getVariable();
      co.addIntro(variable);
    }
    // Apply substitutions.
    makeCompatible(snd, oco, split.inBothButDiffering());
    return new Region(fields.union(split.onlyInSecond()), mergeContexts(snd));
  }

  ThreeWaySplit<FiniteRangeTree<VariableCtx>> splitFields (Region eph) {
    return fields.split(eph.fields);
  }

  Region unfoldCopy (FoldMap varsToExpand, MemVar region) {
    // msg("unfoldCopy " + varsToExpand + " region: " + region);
    FiniteRangeTree<VariableCtx> copiedFields = FiniteRangeTree.<VariableCtx>empty();
    for (P2<FiniteRange, VariableCtx> p2 : fields) {
      VariableCtx ctx = p2._2();
      NumVar nv = NumVar.fresh();
      if (region != null) {
        Bound low = p2._1().low();
        int intValue = low.isFinite() ? low.asInteger().intValue() : 0;
        nv.setRegionName(region, intValue);
      }
      VariableCtx newCtx = new VariableCtx(ctx.getSize(), nv);
      copiedFields = copiedFields.bind(p2._1(), newCtx);
      varsToExpand.add(ctx.getVariable(), nv);
    }
    // msg("unfoldCopy with vars " + varsToExpand );
    // we simply reuse the context
    return new Region(copiedFields, context);
  }

  public VarSet containedNumVars () {
    VarSet vs = VarSet.empty();
    for (P2<FiniteRange, VariableCtx> vctx : fields) {
      NumVar v = vctx._2().getVariable();
      vs = vs.add(v);
    }
    return vs;
  }

  public boolean isEmpty () {
    return fields.isEmpty();
  }

  public void testEqual (Region r2, FiniteSequence childOps) {
    ThreeWaySplit<FiniteRangeTree<VariableCtx>> split = fields.split(r2.fields);
    // Project out fields from this.
    for (P2<FiniteRange, VariableCtx> field : split.inBothButDiffering()) {
      FiniteRange interval = field._1();
      VariableCtx variableCtx1 = fields.get(interval).get();
      NumVar fromFst = variableCtx1.getVariable();
      VariableCtx variableCtx2 = r2.fields.get(interval).get();
      NumVar fromSnd = variableCtx2.getVariable();
      int size = variableCtx1.getSize();
      if (size != variableCtx2.getSize())
        continue;
      Test test =
        FiniteFactory.getInstance().equalTo(size, fromFst, fromSnd);
      childOps.addTest(test);
    }
  }

  public void appendInfo (StringBuilder builder, PrettyDomain childDomain) {
    builder.append('{');
    Iterator<P2<FiniteRange, VariableCtx>> iterator = fields.iterator();
    while (iterator.hasNext()) {
      P2<FiniteRange, VariableCtx> element = iterator.next();
      FiniteRange key = element._1();
      VariableCtx value = element._2();
      builder.append(key);
      builder.append('=');
      childDomain.varToCompactString(builder, value.getVariable());
      if (iterator.hasNext())
        builder.append(", ");
    }
    builder.append("}");
  }

  @Override public String toString () {
    Iterator<P2<FiniteRange, VariableCtx>> iterator = fields.iterator();
    String result;
    if (!iterator.hasNext())
      result = "{}";
    else {
      StringBuilder builder = new StringBuilder();
      builder.append('{');
      while (iterator.hasNext()) {
        P2<FiniteRange, VariableCtx> element = iterator.next();
        FiniteRange key = element._1();
        VariableCtx value = element._2();
        builder.append(key.toString());
        builder.append('=');
        builder.append(value.getVariable());
        if (iterator.hasNext())
          builder.append(", ");
      }
      result = builder.append('}').toString();
    }
    return result;
  }
}
