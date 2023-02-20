package bindead.domains.fields;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.BigInt;
import javalx.numeric.FiniteRange;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLSet;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Lin;
import rreil.lang.Rhs.Rval;
import rreil.lang.Test;
import rreil.lang.util.Type;
import bindead.abstractsyntax.finite.Finite;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.abstractsyntax.memderef.AbstractPointer;
import bindead.data.Linear;
import bindead.data.MemVarPair;
import bindead.data.MemVarSet;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarSet;
import bindead.debug.DomainPrintProperties;
import bindead.debug.DomainStringBuilder;
import bindead.domainnetwork.channels.DebugChannel;
import bindead.domainnetwork.channels.Domain;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.interfaces.AnalysisCtx;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domains.segments.heap.PathString;
import bindead.exceptions.Unreachable;

import com.jamesmurty.utils.XMLBuilder;

public class FieldsDisjunction<D extends MemoryDomain<D>>
    extends Domain<FieldsDisjunction<D>> implements MemoryDomain<FieldsDisjunction<D>> {
  public static final String NAME = "FIELDSDISJUNCTION";

  private final List<D> childState;

  private FieldsDisjunction (List<D> child) {
    super(NAME);
    this.childState = child;
  }

  public FieldsDisjunction (D child) {
    super(NAME);
    this.childState = new LinkedList<D>();
    this.childState.add(child);
  }

  /* ***********
   * Operations from class FunctorDomain:
   *********** */

  private FieldsDisjunction<D> build (List<D> childState) {
    if (childState.isEmpty())
      throw new Unreachable();
    return new FieldsDisjunction<D>(childState);
  }


  /* ***********
   * Operations from interface MemoryDomain:
   *********** */

  @Override public FieldsDisjunction<D> evalAssign (Lhs lhs, Rhs rhs) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.evalAssign(lhs, rhs);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public FieldsDisjunction<D> evalLoad (Lhs value, AbstractMemPointer location) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.evalLoad(value, location);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public FieldsDisjunction<D> evalStore (AbstractMemPointer location, Lin value) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.evalStore(location, value);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public FieldsDisjunction<D> eval (Test test) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.eval(test);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }


  @Override public P3<AVLSet<AddrVar>, FieldsDisjunction<D>, Rlin> findPointerTargets (Lin ptr) {
    AVLSet<AddrVar> adrs = AVLSet.<AddrVar>empty();
    List<D> fs = new LinkedList<D>();
    Rlin rlin = null;
    for (D c : childState) {
      try {
        P3<AVLSet<AddrVar>, D, Rlin> res = c.findPointerTargets(ptr);
        adrs = adrs.union(res._1());
        fs.add(res._2());
        // hsi: this will fail sometimes, because our design is broken: the Fields domain
        // exports NumVars, although these are not known outside.
        if (rlin == null)
          rlin = res._3();
        else
          assert rlin.equals(res._3()) : "not equal: " + res._3() + " and " + rlin;
      } catch (Unreachable _) {
        // nop
      }
    }
    if (rlin == null)
      throw new Unreachable();
    return P3.<AVLSet<AddrVar>, FieldsDisjunction<D>, Rlin>tuple3(adrs, build(fs), rlin);
  }

  @Deprecated @Override public List<P2<AbstractPointer, FieldsDisjunction<D>>> deprecatedDeref (int size,
      Rval ptr,
      VarSet summaries) throws Unreachable {
    List<P2<AbstractPointer, FieldsDisjunction<D>>> rs = new LinkedList<P2<AbstractPointer, FieldsDisjunction<D>>>();
    for (D c : childState) {
      try {
        for (P2<AbstractPointer, D> p : c.deprecatedDeref(size, ptr, summaries)) {
          rs.add(P2.<AbstractPointer, FieldsDisjunction<D>>tuple2(p._1(), new FieldsDisjunction<D>(p._2())));
        }
      } catch (Unreachable _) {
      }
    }
    return rs;
  }

  @Override public FieldsDisjunction<D> introduceRegion (MemVar region, RegionCtx regionCtx) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.introduceRegion(region, regionCtx);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public FieldsDisjunction<D> substituteRegion (MemVar from, MemVar to) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.substituteRegion(from, to);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public FieldsDisjunction<D> assignSymbolicAddressOf (Lhs lhs, NumVar symbolicAddress) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.assignSymbolicAddressOf(lhs, symbolicAddress);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }


  @Override public FieldsDisjunction<D> projectRegion (MemVar region) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.projectRegion(region);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public FieldsDisjunction<D> assumePointsToAndConcretize (Lin pointerValue, AddrVar target, MemVar region) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.assumePointsToAndConcretize(pointerValue, target, region);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public List<P2<PathString, AddrVar>> findPossiblePointerTargets (MemVar id) throws Unreachable {
    List<P2<PathString, AddrVar>> rs = new LinkedList<P2<PathString, AddrVar>>();
    for (D c : childState) {
      try {
        List<P2<PathString, AddrVar>> r = c.findPossiblePointerTargets(id);
        rs.addAll(r);
      } catch (Unreachable _) {
      }
    }
    return rs;
  }

  @Override public FieldsDisjunction<D> copyMemRegion (MemVar fromVar, MemVar toVar) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.copyMemRegion(fromVar, toVar);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public final Range queryRange (Rval value) {
    Iterator<D> it = childState.iterator();
    assert it.hasNext();
    Range result = it.next().queryRange(value);
    while (it.hasNext()) {
      result = result.union(it.next().queryRange(value));
    }
    return result;
  }

  @Override public Range queryRange (Lin value) {
    Iterator<D> it = childState.iterator();
    assert it.hasNext();
    Range result = it.next().queryRange(value);
    while (it.hasNext()) {
      result = result.union(it.next().queryRange(value));
    }
    return result;
  }

  /**
   * Query the range valuation of interval-field {@code key} contained in {@code region}.
   *
   * @param region The region in which to search for the field.
   * @param key The interval-field.
   * @return The range valuation of the field or null if the field cannot be resolved.
   */
  @Override public final Range queryRange (MemVar region, FiniteRange key) {
    Iterator<D> it = childState.iterator();
    assert it.hasNext();
    Range result = it.next().queryRange(region, key);
    while (it.hasNext()) {
      result = result.union(it.next().queryRange(region, key));
    }
    return result;
  }

  // hsi: not nice
  @Override public DebugChannel getDebugChannel () {
    return childState.get(0).getDebugChannel();
  }

  @Override public Option<NumVar> pickSpecificField (MemVar region, FiniteRange access) {
    throw new UnimplementedException();
  }

  @Override public Option<NumVar> resolveVariable (MemVar region, FiniteRange bits) {
    return pickSpecificField(region, bits);
  }

  // for simplicity, we just copy and paste from the first alternative of the other domain
  @Override public FieldsDisjunction<D> copyAndPaste (MemVarSet vars, FieldsDisjunction<D> from) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.copyAndPaste(vars, from.childState.get(0));
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public MemVarSet getSupportSet () {
    MemVarSet vs = MemVarSet.empty();
    for (D c : childState)
      vs = vs.insertAll(c.getSupportSet());
    return vs;
  }



  /** MemoryFiniteFunctor *****************************************************************************/


  /* ***********
   * Operations from interface MemoryDomain:
   *********** */

  @Override public final FieldsDisjunction<D> evalFiniteAssign (Finite.Lhs lhs, Finite.Rhs rhs) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.evalFiniteAssign(lhs, rhs);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public final FieldsDisjunction<D> eval (Finite.Test test) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.eval(test);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public final FieldsDisjunction<D> introduce (NumVar numericVariable, Type type, Option<BigInt> value) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.introduce(numericVariable, type, value);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public final FieldsDisjunction<D> project (NumVar variable) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.project(variable);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public FieldsDisjunction<D> substitute (NumVar from, NumVar to) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.substitute(from, to);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  /** FunctorDomain *****************************************************************************/

  private final boolean compactPrinting = DomainPrintProperties.INSTANCE.printCompact.isTrue();


  /* ***********
   * Operations from interface SemiLattice:
   *********** */

  @Override public FieldsDisjunction<D> join (FieldsDisjunction<D> other) {
    LinkedList<D> rs = new LinkedList<D>(childState);
    rs.addAll(other.childState);
    return build(rs);
  }

  @Override public FieldsDisjunction<D> widen (FieldsDisjunction<D> other) {
    List<D> rs = new LinkedList<D>();
    D thisCollapsed = collapse();
    D otherCollapsed = other.collapse();
    rs.add(thisCollapsed.widen(otherCollapsed));
    return build(rs);
  }

  private D collapse () {
    Iterator<D> it = childState.iterator();
    assert it.hasNext();
    D result = it.next();
    while (it.hasNext()) {
      result = result.join(it.next());
    }
    return result;
  }

  @Override public boolean subsetOrEqual (FieldsDisjunction<D> other) {
    return collapse().subsetOrEqual(other.collapse());
  }

  @Override public AnalysisCtx getContext () {
    return childState.get(0).getContext();
  }

  @Override public FieldsDisjunction<D> setContext (AnalysisCtx ctx) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.setContext(ctx);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  /* ***********
   * Operations from interface QueryChannel:
   *********** */

  @Override public SetOfEquations queryEqualities (NumVar variable) {
    // too complicated, ignoring
    return SetOfEquations.empty();
  }

  @Override public Range queryRange (Linear lin) {
    Iterator<D> it = childState.iterator();
    assert it.hasNext();
    Range result = it.next().queryRange(lin);
    while (it.hasNext()) {
      result = result.union(it.next().queryRange(lin));
    }
    return result;
  }

  @Override public SynthChannel getSynthChannel () {
    // cannot join synth channels, so we fall back to the top channel
    return new SynthChannel();
  }

  /**
   * If a functor domain uses the default {@link querySynth} implementation
   * then this function is called to check with of the passed-in variables are
   * local to the functor domain. A functor domain that introduces its own
   * variables must override this method by one which returns the subset of the
   * input variables that are only known within this domain. This subset is
   * then removed from all information propagated up the synthesized channel.
   *
   * @param toTest the variable set in the synthesized channel
   * @return the subset of {@code toTest} that are owned by this functor domain
   */
  // Default: a domain does not hold local variables
  // REFACTOR hsi: too complicated, just ask domain via something like (bool exportEqualitiedFor(var))
  public VarSet localSubset (VarSet toTest) {
    return VarSet.empty();
  }

  @Override public final XMLBuilder toXML (XMLBuilder builder) {
    for (D c : childState)
      builder = c.toXML(builder);
    return builder;
  }

  @Override public final void toString (DomainStringBuilder builder) {
    for (D c : childState) {
      builder.append(name, "\nAlternative");
      c.toString(builder);
    }
  }

  // only overwritten by wrapping domain
  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    if (compactPrinting) {
      toCompactString(builder);
    } else {
      for (int i = 0; i < childState.size(); i++) {
        builder.append("\nAlternative " + i + ":\n");
        builder.append(childState.get(i).toString());
      }
    }
    return builder.toString();
  }

  @Override public final void toCompactString (StringBuilder builder) {
    if (childState.size() == 1)
      childState.get(0).toCompactString(builder);
    else
      for (D c : childState) {
        builder.append("\nAlternative:\n");
        c.toCompactString(builder);
      }
  }

  @Override public void varToCompactString (StringBuilder builder, NumVar var) {
    Iterator<D> it = childState.iterator();
    assert it.hasNext();
    D first = it.next();
    if (!it.hasNext()) {
      first.varToCompactString(builder, var);
      return;
    }
    builder.append("(");
    first.varToCompactString(builder, var);
    while (it.hasNext()) {
      builder.append(" v ");
      it.next().varToCompactString(builder, var);
    }
    builder.append(")");
  }

  @Override public FieldsDisjunction<D> assumeEdgeNG (Lin fieldThatPoints, AddrVar address) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.assumeEdgeNG(fieldThatPoints, address);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public FieldsDisjunction<D> expandNG (List<MemVarPair> mvps) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.expandNG(mvps);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public FieldsDisjunction<D> expandNG (AddrVar address, AddrVar address2, List<MemVarPair> mvps) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.expandNG(address, address2, mvps);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public FieldsDisjunction<D> concretizeAndDisconnectNG (AddrVar summary, AVLSet<MemVar> concreteNodes) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.concretizeAndDisconnectNG(summary, concreteNodes);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public FieldsDisjunction<D> bendBackGhostEdgesNG (AddrVar summary, AddrVar concrete,
      MemVarSet sContents, MemVarSet cContents, MemVarSet pointingToSummary, MemVarSet pointingToConcrete) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.bendBackGhostEdgesNG(summary, concrete, sContents, cContents, pointingToSummary, pointingToConcrete);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public FieldsDisjunction<D> foldNG (AddrVar summary, AddrVar concrete, List<MemVarPair> mvps) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.foldNG(summary, concrete, mvps);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public FieldsDisjunction<D> foldNG (List<MemVarPair> mvps) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.foldNG(mvps);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public FieldsDisjunction<D> bendGhostEdgesNG (AddrVar summary, AddrVar concrete, MemVarSet sContents,
      MemVarSet cContents, MemVarSet pointingToSummary, MemVarSet pointingToConcrete) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.bendGhostEdgesNG(summary, concrete, sContents, cContents, pointingToSummary, pointingToConcrete);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public Range queryPtsEdge (MemVar from, BigInt offset, int size, AddrVar to) {
    Iterator<D> it = childState.iterator();
    assert it.hasNext();
    Range result = it.next().queryPtsEdge(from, offset, size, to);
    while (it.hasNext()) {
      result = result.union(it.next().queryPtsEdge(from, offset, size, to));
    }
    return result;
  }

  @Override public FieldsDisjunction<D> assumeRegionsAreEqual (MemVar first, MemVar second) {
    List<D> rs = new LinkedList<D>();
    for (D c : childState) {
      try {
        D r = c.assumeRegionsAreEqual(first, second);
        rs.add(r);
      } catch (Unreachable _) {
      }
    }
    return build(rs);
  }

  @Override public void memVarToCompactString (StringBuilder builder, MemVar var) {
    if (childState.size() == 1)
      childState.get(0).memVarToCompactString(builder, var);
    else {
      for (int i = 0; i < childState.size(); i++) {
        builder.append("\n        " + i + ": ");
        childState.get(i).memVarToCompactString(builder, var);
      }
    }
  }

  @Override public List<FieldsDisjunction<D>> enumerateAlternatives () {
    List<FieldsDisjunction<D>> ds = new LinkedList<FieldsDisjunction<D>>();
    for (D child : childState) {
      for (D c : child.enumerateAlternatives())
        ds.add(new FieldsDisjunction<D>(c));
    }
    return ds;
  }
}
