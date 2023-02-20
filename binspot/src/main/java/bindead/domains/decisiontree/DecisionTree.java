package bindead.domains.decisiontree;

import java.util.Iterator;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.exceptions.UnimplementedException;
import javalx.fn.Fn;
import javalx.fn.Fn2;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.AVLSet;
import javalx.persistentcollections.ThreeWaySplit;
import rreil.lang.util.Type;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.debug.DomainPrintHelpers;
import bindead.debug.DomainStringBuilder;
import bindead.debug.XmlPrintHelpers;
import bindead.domainnetwork.channels.DebugChannel;
import bindead.domainnetwork.channels.Domain;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.interfaces.AnalysisCtx;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.domains.affine.Substitution;
import bindead.exceptions.Unreachable;

import com.jamesmurty.utils.XMLBuilder;

public abstract class DecisionTree<D extends ZenoDomain<D>> extends Domain<DecisionTree<D>>
    implements ZenoDomain<DecisionTree<D>> {
  protected final Option<D> state;
  protected final AVLMap<Test, DecisionTree<D>> children;
  private final AnalysisCtx analysisContext;

  protected DecisionTree (String name, Option<D> state, AVLMap<Test, DecisionTree<D>> children, final AnalysisCtx ctx) {
    super(name);
    this.state = state.fmap(new Fn<D, D>() {
      @Override public D apply (D s) {
        return s.setContext(ctx);
      }
    });
    this.children = children.mapOnValues(new Fn<DecisionTree<D>, DecisionTree<D>>() {
      @Override public DecisionTree<D> apply (DecisionTree<D> a) {
        return a.setContext(ctx);
      }
    });
    this.analysisContext = ctx;
  }

  protected abstract DecisionTree<D> build (Option<D> state, AVLMap<Test, DecisionTree<D>> children);

  private DecisionTree<D> build (Option<D> child) {
    return build(child, DecisionTree.<D>noChild());
  }

  @Override public AnalysisCtx getContext () {
    return analysisContext;
  }

  @Override public DecisionTree<D> setContext (AnalysisCtx ctx) {
    throw new UnimplementedException(
      "Needs to be implemented in subclassers, passing the context through the constructor.");
  }

  protected static <D extends ZenoDomain<D>> AVLMap<Test, DecisionTree<D>> noChild () {
    return AVLMap.<Test, DecisionTree<D>>empty();
  }

  private static <D extends ZenoDomain<D>> Option<D> join (Option<D> arg1, Option<D> arg2) {
    if (arg1.isNone())
      return arg2;
    if (arg2.isNone())
      return arg1;
    return Option.<D>some(arg1.get().join(arg2.get()));
  }

  protected static <D extends ZenoDomain<D>> Option<D> applyTest (Test t, Option<D> state) {
    if (t == null)
      return state;
    if (state.isNone())
      return state;
    try {
      return Option.some(state.get().eval(t));
    } catch (Unreachable _) {
      return Option.<D>none();
    }
  }

  private P2<DecisionTree<D>, DecisionTree<D>> makeCompatible (DecisionTree<D> other) {
    Option<D> thisState = state;
    Option<D> thatState = other.state;
    ThreeWaySplit<AVLMap<Test, DecisionTree<D>>> split = children.split(other.children);
    AVLMap<Test, DecisionTree<D>> thisChildren = split.inBothButDiffering();
    AVLMap<Test, DecisionTree<D>> thatChildren = other.children.intersection(split.inBothButDiffering());
    for (P2<Test, DecisionTree<D>> pair : split.onlyInFirst()) {
      Test t = pair._1();
      thatState = applyTest(t.not(), thatState);
      Option<D> testState = applyTest(t, thatState);
      P2<DecisionTree<D>, DecisionTree<D>> recurse =
        pair._2().makeCompatible(build(testState));
      thisChildren = thisChildren.bind(t, recurse._1());
      thatChildren = thatChildren.bind(t, recurse._2());
    }
    for (P2<Test, DecisionTree<D>> pair : split.onlyInSecond()) {
      Test t = pair._1();
      thisState = applyTest(t.not(), thisState);
      Option<D> testState = applyTest(t, thisState);
      P2<DecisionTree<D>, DecisionTree<D>> recurse =
        build(testState).makeCompatible(pair._2());
      thisChildren = thisChildren.bind(t, recurse._1());
      thatChildren = thatChildren.bind(t, recurse._2());
    }
    for (P2<Test, DecisionTree<D>> pair : split.inBothButDiffering()) {
      Test t = pair._1();
      P2<DecisionTree<D>, DecisionTree<D>> recurse =
        pair._2().makeCompatible(other.children.get(t).get());
      thisChildren = thisChildren.bind(t, recurse._1());
      thatChildren = thatChildren.bind(t, recurse._2());

    }
    return P2.<DecisionTree<D>, DecisionTree<D>>tuple2(
        build(thisState, thisChildren),
        build(thatState, thatChildren));
  }


  @Override public DecisionTree<D> join (DecisionTree<D> other) {
    P2<DecisionTree<D>, DecisionTree<D>> compat = makeCompatible(other);
    return accept(new BinaryVisitor<D>() {
      @Override public Option<D> visit (Option<D> arg1, Option<D> arg2) {
        return DecisionTree.<D>join(arg1, arg2);
      }
    }, compat._1(), compat._2());
  }

  @Override public DecisionTree<D> widen (DecisionTree<D> other) {
    P2<DecisionTree<D>, DecisionTree<D>> compat = makeCompatible(other);
    return accept(new BinaryVisitor<D>() {
      @Override public Option<D> visit (Option<D> arg1, Option<D> arg2) {
        if (arg1.isNone())
          return arg2;
        if (arg2.isNone())
          return arg1;
        assert arg1.get().subsetOrEqual(arg2.get());
        return Option.<D>some(arg1.get().widen(arg2.get()));
      }
    }, compat._1(), compat._2()).restrict().propagate();
  }

  @Override public boolean subsetOrEqual (DecisionTree<D> other) {
    P2<DecisionTree<D>, DecisionTree<D>> compat = makeCompatible(other);
    return compat._1().subsetOrEqualImpl(compat._2());
  }

  private boolean subsetOrEqualImpl (DecisionTree<D> other) {
    if (state.isSome()) {
      if (other.state.isSome()) {
        if (!state.get().subsetOrEqual(other.state.get()))
          return false;
      } else
        return false;
    }
    ThreeWaySplit<AVLMap<Test, DecisionTree<D>>> diff = children.split(other.children);
    for (P2<Test, DecisionTree<D>> onlyThis : diff.onlyInFirst()) {
      if (onlyThis._2().state.isSome())
        return false;
    }
    for (P2<Test, DecisionTree<D>> inBoth : diff.inBothButDiffering()) {
      DecisionTree<D> thisChild = inBoth._2();
      DecisionTree<D> thatChild = other.children.get(inBoth._1()).get();
      if (!thisChild.subsetOrEqual(thatChild))
        return false;
    }
    return true;
  }

  @Override public DecisionTree<D> expand (final FoldMap pairs) {
    return accept(new UnaryVisitor<D>() {
      @Override public Option<D> visit (D arg) {
        return Option.<D>some(arg.expand(pairs));
      }
    });
  }

  @Override public DecisionTree<D> fold (final FoldMap pairs) {
    return accept(new UnaryVisitor<D>() {
      @Override public Option<D> visit (D arg) {
        return Option.<D>some(arg.expand(pairs));
      }
    });
  }

  @Override public DecisionTree<D> copyAndPaste (VarSet vars, DecisionTree<D> from) {
    assert false;  // this is probably not correct since the support set in this and the from domain may differ, so
                  // rather stop
    final VarSet vars_ = vars;
    P2<DecisionTree<D>, DecisionTree<D>> compat = makeCompatible(from);
    return accept(new BinaryVisitor<D>() {
      @Override public Option<D> visit (Option<D> arg1, Option<D> arg2) {
        if (arg1.isNone())
          return arg2;
        if (arg2.isNone())
          return arg1;
        return Option.<D>some(arg1.get().copyAndPaste(vars_, arg2.get()));
      }
    }, compat._1(), compat._2());
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    XMLBuilder xml = builder;
    XmlPrintHelpers.openDomain(builder, name, XmlPrintHelpers.DomainType.Zeno, XmlPrintHelpers.DomainKind.Functor);
    builder = xmlPrint(builder);
    XmlPrintHelpers.closeDomain(builder);
    return xml;
  }

  private XMLBuilder xmlPrint (XMLBuilder xml) {
    if (state.isSome()) {
      xml = state.get().toXML(xml);
    }
    for (P2<Test, DecisionTree<D>> child : children) {
      xml = xml.e("guarded").a("guard", child._1().toString());
      xml = child._2().xmlPrint(xml);
      xml = xml.up();
    }
    return xml;
  }

  @Override public void toString (DomainStringBuilder builder) {
    builder.append(name, DomainPrintHelpers.printState(name, genString()));
  }

  @Override public String toString () {
    return DomainPrintHelpers.printState(name, genString());
  }

  private String genString () {
    int amountOfPotentialChildren = children.size() + 1; // the +1 is for the current state
    int amountOfExistingChildren = state.isSome() ? 1 : 0;
    for (DecisionTree<D> child : children.values()) {
      amountOfExistingChildren = amountOfExistingChildren + (child.state.isSome() ? 1 : 0);
    }
    String res = "#" + amountOfExistingChildren + "/" + amountOfPotentialChildren;
    if (amountOfPotentialChildren == 1 && amountOfExistingChildren == 0)
      res = "";
    if (state.isSome()) {
      res = res + "\n" + state.get().toString();
    }
    for (P2<Test, DecisionTree<D>> child : children) {
      res = res + "\nwhen " + child._1().toString() + ": ";
      res = res + child._2().genString();
    }
    return res.replaceAll("\n", "\n  ");
  }

  @Override public SynthChannel getSynthChannel () {
    SynthChannel res = state.isSome() ? state.get().getSynthChannel() : new SynthChannel();
    for (P2<Test, DecisionTree<D>> child : children) {
      DecisionTree<D> childTree = child._2();
      if (childTree.state.isNone())
        continue;
      // TODO: here we would need to test if the new equations hold in the other state to be more precise
      // and not lose them through the intersection
      res = res.intersect(childTree.getSynthChannel());
    }
    return res;
  }

  @Override public Range queryRange (Linear lin) {
    Range res = queryRange(null, lin);
    if (res == null)
      throw new Unreachable();
    else
      return res;
  }

  private Range queryRange (Range res, Linear lin) {
    if (res == null) {
      if (state.isSome())
        res = state.get().queryRange(lin);
    } else {
      if (state.isSome())
        res = res.union(state.get().queryRange(lin));
    }
    for (P2<Test, DecisionTree<D>> child : children) {
      res = child._2().queryRange(res, lin);
    }
    return res;
  }

  @Override public DecisionTree<D> eval (final Assign stmt) {
    return accept(new UnaryVisitor<D>() {
      @Override public P2<Test, Option<D>> visitChangeTest (Test t, Option<D> arg) {
        return P2.<Test, Option<D>>tuple2(t, arg.isSome() ? visit(arg.get()) : arg);
      }

      @Override public Option<D> visit (D arg) {
        return Option.<D>some(arg.eval(stmt));
      }
    }).propagate();
  }

  @Override public DecisionTree<D> eval (Test test) throws Unreachable {
    final Test test_ = test;
    DecisionTree<D> result = accept(new UnaryVisitor<D>() {
      @Override public Option<D> visit (D arg) {
        try {
          return Option.<D>some(arg.eval(test_));
        } catch (Unreachable _) {
          return Option.<D>none();
        }
      }
    });
    if (!result.isReachable())
      throw new Unreachable();
    return result;
  }

  public boolean isReachable () {
    if (state.isSome())
      return true;
    for (P2<Test, DecisionTree<D>> child : children) {
      if (child._2().isReachable())
        return true;
    }
    return false;
  }

  @Override public DecisionTree<D> introduce (NumVar variable, Type type, Option<BigInt> value) {
    final NumVar variable_ = variable;
    final Type type_ = type;
    final Option<BigInt> value_ = value;
    return accept(new UnaryVisitor<D>() {
      @Override public Option<D> visit (D arg) {
        return Option.<D>some(arg.introduce(variable_, type_, value_));
      }
    });
  }

  @Override public DecisionTree<D> project (VarSet vars) {
    final VarSet variable_ = vars;
    return accept(new UnaryVisitor<D>() {
      @Override public P2<Test, Option<D>> visitChangeTest (Test t, Option<D> arg) {
        // if the test contains the variable to project out, remove the whole test from the tree
        if (t != null && t.getVars().containsAny(variable_))
          t = null;
        return P2.<Test, Option<D>>tuple2(t, arg.isSome() ? visit(arg.get()) : arg);
      }

      @Override public Option<D> visit (D arg) {
        return Option.<D>some(arg.project(variable_));
      }
    });
  }

  @Override public DecisionTree<D> substitute (final NumVar x, final NumVar y) {
    final Substitution subst = new Substitution(x, Linear.linear(y), Bound.ONE);
    return accept(new UnaryVisitor<D>() {
      @Override public P2<Test, Option<D>> visitChangeTest (Test t, Option<D> arg) {
        // if the test contains the variable to project out, remove the whole test from the tree
        if (t != null)
          t = t.applySubstitution(subst);
        return P2.<Test, Option<D>>tuple2(t, arg.isSome() ? visit(arg.get()) : arg);
      }

      @Override public Option<D> visit (D arg) {
        return Option.<D>some(arg.substitute(x, y));
      }
    });
  }

  /**
   * The unary visitor is instantiated for all domain operations taking one argument,
   *
   * @author Axel Simon
   *
   * @param <D> the child
   */
  protected static abstract class UnaryVisitor<D extends ZenoDomain<D>> {
    /**
     * Modify the state in a decision tree that resides just below the given test. Returns a tuple
     * with the test (possibly modified) and the new state. The returned test may be {@code null} in
     * which case this state is merged with its parent.
     *
     * @param test the test, this parameter is {@code null} if this is the root node
     * @param state the state
     * @return a state for the father, the new test and the new state
     */
    public P2<Test, Option<D>> visitChangeTest (Test test, Option<D> state) {
      return P2.<Test, Option<D>>tuple2(test, state.isSome() ? visit(state.get()) : state);
    }

    public abstract Option<D> visit (D state);

    /**
     * Return the new tests that were collected during the visit.
     *
     * @param state
     */
    public AVLSet<Test> newTests (D state) {
      return AVLSet.empty();
    }
  }

  /**
   * The binary visitor is instantiated for join, widen and other domain operations that take two domains.
   *
   * @author Axel Simon
   *
   * @param <D> the child
   */
  private static abstract class BinaryVisitor<D extends ZenoDomain<D>> {
    /**
     * Merge the two arguments into one. Return the resulting state and a set of tests for which the resulting state
     * should be split over.
     *
     * @param arg1 the first domain
     * @param arg2 the second domain
     * @return a new set of tests and the resulting domain
     */
    private P2<AVLSet<Test>, Option<D>> visitCreateChildren (Option<D> arg1, Option<D> arg2) {
      return P2.<AVLSet<Test>, Option<D>>tuple2(AVLSet.<Test>empty(), visit(arg1, arg2));
    }

    public abstract Option<D> visit (Option<D> arg1, Option<D> arg2);
  }

  private DecisionTree<D> accept (BinaryVisitor<D> v, DecisionTree<D> this_, DecisionTree<D> other) {
    P2<AVLSet<Test>, Option<D>> merged = v.visitCreateChildren(this_.state, other.state);
    Option<D> newState = merged._2();
    AVLMap<Test, DecisionTree<D>> newChildren = noChild();
    Iterator<P2<Test, DecisionTree<D>>> thisIter = this_.children.iterator();
    Iterator<P2<Test, DecisionTree<D>>> thatIter = other.children.iterator();
    while (thisIter.hasNext()) {
      P2<Test, DecisionTree<D>> pair = thisIter.next();
      DecisionTree<D> thisChild = pair._2();
      assert thatIter.hasNext();
      DecisionTree<D> thatChild = thatIter.next()._2();
      newChildren = newChildren.bind(pair._1(), accept(v, thisChild, thatChild));
    }
    for (Test t : merged._1()) {
      if (newChildren.contains(t))
        continue;
      Test predicate = t.not();
      Option<D> childState = applyTest(predicate, newState);
      newState = applyTest(t, newState); // this is likely to be redundant
      newChildren = newChildren.bind(predicate, this_.build(childState));
    }
    return this_.build(newState, newChildren);
  }

  protected DecisionTree<D> accept (UnaryVisitor<D> v) {
    Fn2<DecisionTree<D>, DecisionTree<D>, DecisionTree<D>> merger =
      new Fn2<DecisionTree<D>, DecisionTree<D>, DecisionTree<D>>() {

        @Override public DecisionTree<D> apply (DecisionTree<D> a, DecisionTree<D> b) {
          return build(DecisionTree.<D>join(a.state, b.state), a.children.union(this, b.children));
        }
      };
    P2<Test, Option<D>> resVisitState = v.visitChangeTest(null, state);

    // check if we are to add new tests
    AVLSet<Test> newTests = state.isSome() ? v.newTests(state.get()) : AVLSet.<Test>empty();
    AVLMap<Test, DecisionTree<D>> curChildren = children;
    for (Test t : newTests) {
      if (curChildren.contains(t))
        continue;
      curChildren = curChildren.bind(t, build(Option.<D>none()));
    }

    P2<Option<D>, AVLMap<Test, DecisionTree<D>>> recurseRes = accept(v, merger, curChildren);
    // the recursion might have removed tests in which case it returns a non-bottom state that needs to be merged with
    // the root
    Option<D> newState = join(resVisitState._2(), recurseRes._1());
    return build(newState, recurseRes._2());
  }

  private P2<Option<D>, AVLMap<Test, DecisionTree<D>>> accept (UnaryVisitor<D> v,
      Fn2<DecisionTree<D>, DecisionTree<D>, DecisionTree<D>> merger, AVLMap<Test, DecisionTree<D>> children) {
    Option<D> extraParentState = Option.<D>none();
    AVLMap<Test, DecisionTree<D>> newTree = noChild();
    for (P2<Test, DecisionTree<D>> child : children) {

      P2<Test, Option<D>> resVisitState = v.visitChangeTest(child._1(), child._2().state);
      Option<D> newState = resVisitState._2();

      // check if we are to add new tests
      AVLSet<Test> newTests = child._2().state.isSome() ? v.newTests(child._2().state.get()) : AVLSet.<Test>empty();
      AVLMap<Test, DecisionTree<D>> curChildren = child._2().children;
      for (Test t : newTests) {
        if (curChildren.contains(t))
          continue;
        curChildren = curChildren.bind(t, child._2().build(Option.<D>none())); // child._2(). is only here because build
                                                                               // can't be static
      }

      P2<Option<D>, AVLMap<Test, DecisionTree<D>>> resVisitChildren = accept(v, merger, curChildren);
      AVLMap<Test, DecisionTree<D>> newChildren = resVisitChildren._2();

      if (resVisitState._1() == null) {
        // the test is to be removed, implying that all children of this node must be inserted at this level
        extraParentState = join(extraParentState, newState);
        newTree = newTree.union(merger, newChildren);
      } else {
        // insert the modified child into the existing tree while taking care that two children might have the same test
        // after the transformation and thus must be merged
        DecisionTree<D> elem = child._2().build(newState, newChildren); // child._2(). is only here because build can't
                                                                        // be static
        Option<DecisionTree<D>> prevElem = newTree.get(resVisitState._1());
        if (prevElem.isNone())
          newTree = newTree.bind(resVisitState._1(), elem);
        else
          newTree = newTree.bind(resVisitState._1(), merger.apply(prevElem.get(), elem));
      }
    }
    return P2.<Option<D>, AVLMap<Test, DecisionTree<D>>>tuple2(extraParentState, newTree);
  }

  private DecisionTree<D> restrict () {
    // HACK: we remove the location information in order to force the ThresholdsWidening domain
    // ignore the following tests and not track them as thresholds. Otherwise we would duplicate
    // each of the child-tests for each program point again as new widening thresholds
    Option<D> newState = stripLocation(state);
    AVLMap<Test, DecisionTree<D>> newTree = noChild();
    for (P2<Test, DecisionTree<D>> child : children) {
      if (newState.isSome() && child._2().state.isSome()) {
        // only restrict this state if the child has a non-bottom state, otherwise we need to propagate this state into this partition
        try {
          newState = Option.<D>some(newState.get().eval(child._1().not()));
        } catch (Unreachable _) {
          newState = Option.<D>none();
        }
      }
      newTree = newTree.bind(child._1(), child._2().restrict());
    }
    return build(newState, newTree);
  }

  private DecisionTree<D> propagate () {
    // HACK: we remove the location information in order to force the ThresholdsWidening domain
    // ignore the following tests and not track them as thresholds. Otherwise we would duplicate
    // each of the child-tests for each program point again as new widening thresholds
    Option<D> newState = stripLocation(state);
    P2<Option<D>, AVLMap<Test, DecisionTree<D>>> recurseRes = propagate(null, newState, children);
    // restrict the current state to the state that does not belong to children
    if (newState.isSome()) {
      for (P2<Test, DecisionTree<D>> child : recurseRes._2())
        newState = applyTest(child._1().not(), newState);
    }
    // add the spillage from the child
    newState = join(newState, recurseRes._1());
    return build(newState, recurseRes._2());
  }

  /**
   * Propagate spilling state towards the children and request state space that spills from the children.
   *
   * @param guard a test with which the state of this child is restricted or {@code null}
   * @param spillToChild the state that spills into the children
   * @param tree the children
   * @return the state that spills from the children together with the new children
   */
  private P2<Option<D>, AVLMap<Test, DecisionTree<D>>> propagate (Test guard, Option<D> spillToChildren,
      AVLMap<Test, DecisionTree<D>> tree) {
    Test guardNot = guard == null ? null : guard.not();
    AVLMap<Test, DecisionTree<D>> newTree = noChild();
    Option<D> spillFromChild = Option.<D>none();
    for (P2<Test, DecisionTree<D>> child : tree) {
      // we calculate additional state for this child (and its children) from what we've given from the parent
      Test childTest = child._1();
      Option<D> spillToChild = applyTest(childTest, spillToChildren);
      Option<D> newChildState = spillToChild;
      // restrict newChildState to the state that does not belong to grand-children
      DecisionTree<D> childTree = child._2();
      for (P2<Test, DecisionTree<D>> grandChild : childTree.children)
        newChildState = applyTest(grandChild._1().not(), newChildState);
      // propagate this parent's spill state to all grand-children
      P2<Option<D>, AVLMap<Test, DecisionTree<D>>> recurseRes = propagate(childTest, spillToChild, childTree.children);
      // distribute what the grand children spill to this child and the parent
      newChildState = join(newChildState, applyTest(guard, recurseRes._1()));
      spillFromChild = join(spillFromChild, applyTest(guardNot, recurseRes._1()));
      // distribute our current state
      Option<D> childState = stripLocation(childTree.state); // HACK: to force ThresholdsWidening to ignore the test
      newChildState = join(newChildState, applyTest(childTest, childState));
      spillFromChild = join(spillFromChild, applyTest(childTest.not(), childState));
      DecisionTree<D> newChild = childTree.build(newChildState, recurseRes._2());
      newTree = newTree.bind(childTest, newChild);
    }
    return P2.<Option<D>, AVLMap<Test, DecisionTree<D>>>tuple2(spillFromChild, newTree);
  }

  private Option<D> stripLocation (Option<D> state) {
    return state.fmap(new Fn<D, D>() {
      @Override public D apply (D a) {
        return a.setContext(a.getContext().withoutLocation());
      }
    });
  }

  /**
   * Currently, this is a no-op on all Zeno domains. Should never be called as it is
   * caught in FiniteZenoFunctor.
   */
  @Override public DecisionTree<D> assumeConcrete (NumVar var) {
    throw new UnimplementedException("should never be called");
  }

  /**
   * Empty implementation for convenience. Override in subclasses if debug information should be propagated.<br>
   * <br>
   * {@inheritDoc}
   */
  @Override public DebugChannel getDebugChannel () {
    return new DebugChannel();
  }

}
