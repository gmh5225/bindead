package bindead.domainnetwork.combinators;

import static bindead.data.Linear.linear;
import static bindead.data.Linear.term;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P3;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Congruence;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import rreil.lang.util.Type;
import bindead.abstractsyntax.zeno.Zeno;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Bin;
import bindead.abstractsyntax.zeno.Zeno.Lhs;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.Zeno.ZenoBinOp;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.domains.affine.Substitution;
import bindead.exceptions.Unreachable;

/**
 * A datatype containing pending modifications to a child domain.
 */
public abstract class ZenoChildOp {

  public abstract <D extends ZenoDomain<D>> D apply (D state);

  /**
   * A child op can implement this interface if it performs several operations on the child domain and, for each of these
   * application, may perform another child operation.
   *
   * @author Axel Simon
   */
  private interface CanHoist {
    public abstract void setHoisted (ZenoChildOp.Sequence op);
  }

  private static class Introduction extends ZenoChildOp {
    private final NumVar var;
    private BigInt value = null;

    protected Introduction (NumVar var) {
      this.var = var;
    }

    protected Introduction (NumVar var, BigInt value) {
      this.var = var;
      this.value = value;
    }

    @Override public <D extends ZenoDomain<D>> D apply (D state) {
      return state.introduce(var, Type.Zeno, Option.fromNullable(value));
    }

    @Override public String toString () {
      return "intro " + var + (value == null ? "" : " := " + value);
    }
  }

  private static class Kill extends ZenoChildOp {
    private final VarSet vars;

    protected Kill (VarSet kill) {
      this.vars = kill;
    }

    @Override public <D extends ZenoDomain<D>> D apply (D state) {
      return state.project(vars);
    }

    @Override public String toString () {
      return "kill " + vars;
    }
  }

  private static class Subst extends ZenoChildOp {
    private final NumVar from, to;

    protected Subst (NumVar from, NumVar to) {
      this.from = from;
      this.to = to;
    }

    @Override public <D extends ZenoDomain<D>> D apply (D state) {
      return state.substitute(from, to);
    }

    @Override public String toString () {
      return "[" + from + "\\" + to + "]";
    }
  }

  private static class Assignment extends ZenoChildOp {
    private final Assign stmt;

    protected Assignment (Assign stmt) {
      this.stmt = stmt;
    }

    @Override public <D extends ZenoDomain<D>> D apply (D state) {
      return state.eval(stmt);
    }

    @Override public String toString () {
      return stmt.toString();
    }
  }

  private static class Bottom extends ZenoChildOp {
    @Override public <D extends ZenoDomain<D>> D apply (D state) {
      throw new Unreachable();
    }

    @Override public String toString () {
      return "<unreachable>";
    }
  }

  private static class Test extends ZenoChildOp {
    private final Zeno.Test test;

    public Test (Zeno.Test test) {
      this.test = test;
    }

    @Override public <D extends ZenoDomain<D>> D apply (D state) {
      return state.eval(test);
    }

    @Override public String toString () {
      return test.toString();
    }
  }

  private static class Wrap extends ZenoChildOp implements CanHoist {
    private final NumVar var;
    /**
     * The value range that the variable should be wrapped to.
     */
    private final Interval range;
    /**
     * Shifts and other operations do not increase the value above or below a given range anymore.
     * These operations saturate at the bounds.
     */
    private final boolean saturating;
    private ZenoChildOp.Sequence nested = null;

    public Wrap (NumVar var, Interval range, boolean saturating) {
      this.var = var;
      this.range = range;
      this.saturating = saturating;
    }

    /**
     * Tests the variable inside the chosen quadrant range q_l <= x <= q_u.
     *
     * @return The state with the variable restricted to the quadrant or {@code null} if all the values of the variable
     *         are outside the quadrant.
     */
    private <D extends ZenoDomain<D>> D restrictToQuadrant (D inState) {
      D newState = inState;
      ZenoFactory zeno = ZenoFactory.getInstance();
      BigInt lower = range.low().asInteger();
      BigInt upper = range.high().asInteger();
      try {
        Linear lin = linear(upper.negate(), term(var));
        bindead.abstractsyntax.zeno.Zeno.Test test = zeno.comparison(lin, ZenoTestOp.LessThanOrEqualToZero);
        newState = newState.eval(test);
      } catch (Unreachable _) {
        return null;
      }
      try {
        Linear lin = linear(lower, term(BigInt.MINUSONE, var));
        bindead.abstractsyntax.zeno.Zeno.Test test = zeno.comparison(lin, ZenoTestOp.LessThanOrEqualToZero);
        newState = newState.eval(test);
      } catch (Unreachable _) {
        return null;
      }
      return newState;
    }

    /**
     * Assign the lower bound of the quadrant to the variable.
     */
    private <D extends ZenoDomain<D>> D setToLower (D state) {
      if (state == null)
        return null;
      ZenoFactory zeno = ZenoFactory.getInstance();
      Assign setToLower = zeno.assign(zeno.variable(var), zeno.linear(Linear.linear(range.low().asInteger())));
      return state.eval(setToLower);
    }

    /**
     * Assign the upper bound of the quadrant to the variable.
     */
    private <D extends ZenoDomain<D>> D setToUpper (D state) {
      if (state == null)
        return null;
      ZenoFactory zeno = ZenoFactory.getInstance();
      Assign setToUpper = zeno.assign(zeno.variable(var), zeno.linear(Linear.linear(range.high().asInteger())));
      return state.eval(setToUpper);
    }

    /**
     * Assign the value of the whole range to the variable.
     */
    private <D extends ZenoDomain<D>> D setToRange (D state) {
      if (state == null)
        return null;
      ZenoFactory zeno = ZenoFactory.getInstance();
      Assign setToUpper = zeno.assign(zeno.variable(var), zeno.range(range));
      return state.eval(setToUpper);
    }

    private <D extends ZenoDomain<D>> D applyNested (D state) {
      if (state == null)
        return null;
      if (nested == null)
        return state;
      try {
        // apply each child op; we can't use the Sequence.apply since it clears childTrans after it finishes
        for (ZenoChildOp action : nested.childTrans) {
          state = action.apply(state);
        }
      } catch (Unreachable _) {
        state = null;
      }
      return state;
    }

    private static Assign sub (NumVar variable, BigInt value) {
      return add(variable, value.negate());
    }

    private static Assign add (NumVar variable, BigInt value) {
      ZenoFactory zeno = ZenoFactory.getInstance();
      return zeno.assign(zeno.variable(variable), zeno.linear(Linear.linear(value, Linear.term(variable))));
    }

    @Override public <D extends ZenoDomain<D>> D apply (D state) {
      BigInt span = range.getSpan();
      D stateRes = restrictToQuadrant(state);
      if (stateRes == null) {
        // The current state restricted to the quadrant is not feasible. We query the range and
        // shift the whole input state so that the state is feasible.
        Interval variableValue = state.queryRange(var).convexHull();
        // Nested application may mean that a quadrant is not feasible due to equality constraints between the
        // operands, even though the operands are unrestricted.
        BigInt steps;
        if (variableValue.low().isFinite())
          steps = variableValue.low().sub(range.low()).asInteger().divRoundDown(span);
        else if (variableValue.high().isFinite())
          steps = variableValue.high().sub(range.low()).asInteger().divRoundDown(span);
        else
          return setToRange(state);
        if (!span.isZero() && !steps.isZero()) {
          Assign shift = sub(var, steps.mul(span));
          state = state.eval(shift);
        }
        stateRes = restrictToQuadrant(state);
        // in case the domain wasn't properly reduced, we might still get bottom
        if (stateRes == null)
          return null;
      }
      // Compute a fixpoint using a current and previous iterate called stateRes and stateRef, respectively.
      stateRes = applyNested(stateRes);
      D stateRef = stateRes;
      D stateNext = state;
      D statePrev = state;
      P3<D, D, D> result = addFromNeighbourQuadrants(stateRes, stateNext, statePrev);
      stateRes = result._1();
      if (stateRes == null)
        throw new Unreachable();

      if (stateRef != null && stateRef != stateRes)
        stateRes = stateRef.join(stateRes);
      if (stateRef != null && stateRes.subsetOrEqual(stateRef))
        return stateRes;
      stateRef = stateRes;
      stateNext = result._2();
      statePrev = result._3();
      result = addFromNeighbourQuadrants(stateRes, stateNext, statePrev);
      stateRes = result._1();
      if (stateRes == null)
        throw new Unreachable();

      if (stateRes.subsetOrEqual(stateRef))
        return stateRes;

      // still something was added from outer quadrants. Just set the variable to the quadrant and stop the iteration.
      stateRes = setToRange(stateRes);
      return applyNested(stateRes);
    }

    private <D extends ZenoDomain<D>> P3<D, D, D> addFromNeighbourQuadrants (D stateRes, D stateNext, D statePrev) {
      BigInt span = range.getSpan();
      Assign shiftUp = add(var, span);
      Assign shiftDown = sub(var, span);

      stateNext = stateNext.eval(shiftDown);
      D toAdd = restrictToQuadrant(stateNext);
      if (saturating)
        toAdd = setToUpper(toAdd);
      toAdd = applyNested(toAdd);
      if (toAdd != null)
        stateRes = stateRes == null ? toAdd : stateRes.join(toAdd);
      statePrev = statePrev.eval(shiftUp);
      toAdd = restrictToQuadrant(statePrev);
      if (saturating)
        toAdd = setToLower(toAdd);
      toAdd = applyNested(toAdd);
      if (toAdd != null)
        stateRes = stateRes == null ? toAdd : stateRes.join(toAdd);
      return P3.tuple3(stateRes, stateNext, statePrev);
    }

    @Override public String toString () {
      String res = "wrap(" + var + ", " + range;
      if (saturating)
        res = res + "(saturate)";
      if (nested != null)
        res = res + "{ " + nested + " }";
      res = res + ")";
      return res;
    }

    @Override public void setHoisted (ZenoChildOp.Sequence op) {
      nested = op;
    }
  }

  private static class And extends ZenoChildOp {
    private final NumVar var;
    private final Linear l;
    private final Linear r;
    private final int size;

    protected And (NumVar var, Linear left, Linear right, int size) {
      this.var = var;
      this.l = left;
      this.r = right;
      this.size = size;
    }

    @Override public <D extends ZenoDomain<D>> D apply (D state) {
      ZenoFactory zeno = ZenoFactory.getInstance();
      Zeno.Lhs lhs = zeno.variable(var);

      Range lRange = state.queryRange(l);
      Range rRange = state.queryRange(r);
      Linear arg;
      Range argRange;
      Range maskRange;
      if (rRange.isConstant()) {
        if (lRange.isConstant())
          return state.eval(zeno.assign(lhs, zeno.literal(lRange.getConstantOrNull().and(rRange.getConstantOrNull()))));
        arg = l;
        argRange = lRange;
        maskRange = rRange;
      } else if (lRange.isConstant()) {
        arg = r;
        argRange = rRange;
        maskRange = lRange;
      } else {
        // return an interval that encloses both ranges
        int lowerZero = Math.min(lRange.lowerZeroBits(size), rRange.lowerZeroBits(size));
        int upperOne = size - Math.max(lRange.upperZeroBits(size), rRange.upperOneBits(size));
        if (lowerZero == 0) {
          Zeno.Rhs rhs = zeno.range(Range.from(Interval.unsignedTop(upperOne)));
          return state.eval(zeno.assign(lhs, rhs));
        }
        Interval interval = Interval.unsignedTop(upperOne - lowerZero);
        state = state.eval(zeno.assign(lhs, zeno.range(Range.from(interval))));
        Linear lin = Linear.linear(Linear.term(BigInt.powerOfTwo(lowerZero), var));
        return state.eval(zeno.assign(lhs, zeno.linear(lin)));
      }

      int maskLowerZero = maskRange.lowerZeroBits(size);
      int maskFirstOne = size - maskRange.upperZeroBits(size);

      if (maskFirstOne <= 0)
        return state.eval(zeno.assign(lhs, zeno.literal(Bound.ZERO)));

      // the mask is approximated by a vector 000111100 where maskFirstOne and maskLowerZero denote the starting indices
      // (counting from one) of the first one and the first zero after the ones, respectively
      int argFirstOne = size - argRange.upperZeroBits(size);
      int argOnes = argRange.upperOneBits(argFirstOne);

      // subtract a constant if the argument has some leading 1s that are all filtered out by the mask
      argOnes = Math.min(argOnes, argFirstOne - maskFirstOne);
      if (argOnes > 0) {
        BigInt subVal = BigInt.powerOfTwo(argOnes).sub(1).shl(argFirstOne - argOnes);
        arg = arg.sub(subVal);
      }

      state = state.eval(zeno.assign(lhs, zeno.linear(arg)));

      if (maskLowerZero > 0) {
        int shift = maskLowerZero;
        BigInt fac = Bound.TWO.pow(shift);
        Bin ass = zeno.binary(zeno.linear(arg), ZenoBinOp.Shr, zeno.literal(BigInt.of(shift)));
        state = state.eval(zeno.assign(lhs, ass));
        state = state.eval(zeno.assign(lhs, zeno.linear(Linear.linear(fac, var))));
      }
      return state;
    }

    @Override public String toString () {
      return var + "= and(" + l + "," + r + ")";
    }
  }

  private static class Or extends ZenoChildOp {
    private final NumVar var;
    private final Linear left;
    private final Linear right;
    private final int size;

    protected Or (NumVar var, Linear left, Linear right, int size) {
      this.var = var;
      this.left = left;
      this.right = right;
      this.size = size;
    }

    @Override public <D extends ZenoDomain<D>> D apply (D state) {
      ZenoFactory zeno = ZenoFactory.getInstance();
      Zeno.Lhs lhs = zeno.variable(var);
      Range l = state.queryRange(left);
      Range r = state.queryRange(right);
      return state.eval(zeno.assign(lhs, zeno.range(l.unsignedOr(r, size))));
    }

    @Override public String toString () {
      return var + "= or(" + left + "," + right + ")";
    }
  }

  private static class XOr extends ZenoChildOp {
    private final NumVar var;
    private final Linear left;
    private final Linear right;
    private final int size;

    protected XOr (NumVar var, Linear left, Linear right, int size) {
      this.var = var;
      this.left = left;
      this.right = right;
      this.size = size;
    }

    @Override public <D extends ZenoDomain<D>> D apply (D state) {
      ZenoFactory zeno = ZenoFactory.getInstance();
      Zeno.Lhs lhs = zeno.variable(var);
      Range l = state.queryRange(left);
      Range r = state.queryRange(right);
      return state.eval(zeno.assign(lhs, zeno.range(l.unsignedXor(r, size))));
    }

    @Override public String toString () {
      return var + "= xor(" + left + "," + right + ")";
    }
  }

  /**
   * A sequence of operations on the child domain.
   */
  public static class Sequence implements Iterable<ZenoChildOp> {
    private final List<ZenoChildOp> childTrans = new ArrayList<ZenoChildOp>(6);

    private ZenoChildOp getLastElement () {
      return childTrans.get(childTrans.size() - 1);
    }

    private void removeLastElement () {
      childTrans.remove(childTrans.size() - 1);
    }

    @Override public Iterator<ZenoChildOp> iterator () {
      return childTrans.iterator();
    }

    /**
     * Walk backwards through the list of child ops until a child op (the host) is found that can hoist a sequence of
     * child ops. Push all child ops encountered so far into the host. Example:
     *
     * <pre>
     * {@code
     *  x := [-128,127];
     *  wrap(x,[0,255], {});
     *  intro y;
     *  y := x-5;
     *  kill x
     * }
     * </pre>
     *
     * Here, the {@code wrap} child op implements the #CanHoist interface and can thus hoist a sequence of child ops.
     * After a call to this function, the child ops are re-grouped as follows:
     *
     * <pre>
     * {@code
     *  x := [-128,127];
     *  wrap(x,[0,255], {intro y; y := x-5; kill x})
     * }
     * </pre>
     *
     * The net effect is that the wrap operation will execute the three child ops on each quadrant it computes for x,
     * rather than merging all the quadrants for x and then executing the three child ops. Since numeric domains are
     * usually not meet distributive, the repeated application of child ops is often more precise.
     *
     * This function applies hoisting repeatedly until only the very last child op is a host.
     */
    public void hoist () {
      int hostIndex = childTrans.size() - 2; // check for hosts in all but the last entry
      while (hostIndex >= 0) {
        if (childTrans.get(hostIndex) instanceof ZenoChildOp.CanHoist) {
          ZenoChildOp.CanHoist host = (ZenoChildOp.CanHoist) childTrans.get(hostIndex);
          Sequence toHoist = new Sequence();
          List<ZenoChildOp> ops = childTrans.subList(hostIndex + 1, childTrans.size());
          toHoist.childTrans.addAll(ops);
          ops.clear();
          host.setHoisted(toHoist);
          hoist();
          return;
        }
        hostIndex--;
      }
    }

    /**
     * Add a new variable to the child domain.
     *
     * @param var the variable
     */
    public void addIntro (NumVar var) {
      /*// hsi: this would be a bug! introducing after killing is not a non-op!
        ZenoChildOp.Kill kill = (ZenoChildOp.Kill) getLastElement();
        if (kill.vars == var) {
          removeLastElement();
          return;
        }
      }
       */
      childTrans.add(new ZenoChildOp.Introduction(var));
    }

    /**
     * Add a new variable to the child domain.
     *
     * @param var the variable
     * @param value the initial value
     */
    public void addIntro (NumVar var, BigInt value) {
      childTrans.add(new ZenoChildOp.Introduction(var, value));
    }

    /**
     * Add a variable to be projected out.
     *
     * @param var the variable to be projected out
     */
    @Deprecated// use adKill(VarSet) for faster projection
    public void addKill (NumVar var) {
      addKill(VarSet.of(var));
    }

    public void addKill (VarSet vars) {
      if (vars.isEmpty())
        return;
      if (!childTrans.isEmpty() && getLastElement() instanceof ZenoChildOp.Kill) {
        Kill kill = (ZenoChildOp.Kill) getLastElement();
        removeLastElement();
        addKill(kill.vars.union(vars));
        return;
      }
      // simple peephole optimization: if we add a kill of v right after an intro of v, then we remove the intro
      if (!childTrans.isEmpty() && getLastElement() instanceof ZenoChildOp.Introduction) {
        ZenoChildOp.Introduction intro = (ZenoChildOp.Introduction) getLastElement();
        if (vars.contains(intro.var)) {
          removeLastElement();
          addKill(vars.remove(intro.var));
          return;
        }
      }
      childTrans.add(new ZenoChildOp.Kill(vars));
    }

    public void addDirectSubstitution (NumVar kill, NumVar add) {
      if (kill != add)
        childTrans.add(new ZenoChildOp.Subst(kill, add));
    }

    /**
     * Add a test to the child domain operations.
     *
     * @param test
     */
    public void addTest (Zeno.Test test) {
      childTrans.add(new ZenoChildOp.Test(test));
    }

    /**
     * Add an assignment to the child domain operations
     *
     * @param stmt the assignment
     */
    public void addAssignment (Assign stmt) {
      childTrans.add(new ZenoChildOp.Assignment(stmt));
    }

    /**
     * Add an assignment that creates the variable {@code to} in the child domain and removes {@code from}. Both
     * variables may be the same in which case they are neither introduced nor removed. It is probably better to
     * use #addEquality .
     *
     * @param stmt the assignment
     * @param from the variable that is removed afterwards or {@code null} if no old variable is to be removed
     * @param to the variable that is introduced beforehand or {@code null} if an existing variable should be overwritten
     */
    public void addAssignment (Assign stmt, NumVar from, NumVar to) {
      if (from != to && to != null)
        addIntro(to);
      childTrans.add(new ZenoChildOp.Assignment(stmt));
      if (from != to && from != null)
        addKill(from);
    }

    public void addAssignment (Substitution subst, NumVar to) {
      assert to != null;
      BigInt toCoeff = subst.getExpr().getCoeff(to);
      Linear le = subst.getExpr().dropTerm(to).subTerm(subst.getFac(), subst.getVar());
      ZenoFactory f = ZenoFactory.getInstance();
      Assign stmt = f.assign(f.variable(to), f.linear(le, toCoeff.negate()));
      addAssignment(stmt);
    }

    /**
     * Indicate that the domain is actually bottom. This action throws the {@link Unreachable} exception when
     * applied to the child domain.
     */
    public void addBottom () {
      childTrans.clear();
      childTrans.add(new ZenoChildOp.Bottom());
    }

    /**
     * Add an equality to the child domain. The lhs variable of the equality is added to the child domain.
     * This method is a wrapper that chooses the most efficient way of adding this equality.
     *
     * @param eq the equality
     */
    public void addEquality (Linear eq) {
      addLinearTransform(eq, null);
    }

    /**
     * Perform a linear transformation on the child domain. The lhs variable of the equality is added to the child
     * domain while the {@code from} variable is removed. This method is a wrapper that chooses the most efficient
     * way of performing the transformation.
     *
     * @param eq the equality
     * @param from a variable that is part of the equality but not its leading variable
     */
    public void addLinearTransform (Linear eq, NumVar from) {
      NumVar to = eq.getKey();
      BigInt coeff = eq.getCoeff(to).negate();
      eq = eq.dropTerm(to);
      // is this an assignment x = c where c is a constant?
      if (eq.isConstantOnly()) {
        assert from == null;
        // This division may have a non-zero remainder in which case an Unreachable exception
        // is thrown; if this action is executed in makeCompatible, then the exception needs to be caught
        // and the domain that is not unsatisfiable needs to be returned. A lot of hassle for a case that can only
        // occur if some reduction has not been done.
        BigInt c = eq.getConstant().div(coeff);
        if (c.mul(coeff).isEqualTo(eq.getConstant()))
          addIntro(to, c);
        else
          addBottom();
        return;
      }
      // is it a simple assignment of the form to = from?
      if (eq.isSingleTerm() &&
        eq.getConstant().isZero() &&
        eq.getKey() == from &&
        eq.getCoeff(from).isEqualTo(coeff)) {
        addDirectSubstitution(from, to);
        return;
      }
      // its something more complex
      ZenoFactory f = ZenoFactory.getInstance();
      Assign stmt = f.assign(f.variable(to), f.linear(eq, coeff));
      addAssignment(stmt, from, to);
    }

    /**
     * The method scales a variable according to two congruences.
     * For example, if x is congruent to 1 (modulo 8),
     * (x-1)/8 is stored in the child domain. If the new congruence
     * is supposed to be 1 (modulo 4) by adding the
     * assignment operation: x = (x-1+1)/2 the child domain is scaled accordingly.
     *
     * @param var variable to be scaled
     * @param oldC the old congruence of the variable
     * @param newC the new congruence of the variable
     */
    public void addScale (NumVar var, Congruence oldC, Congruence newC) {
      if (oldC.equals(newC))
        return;
      BigInt oldScale = oldC.getScale();
      BigInt newScale = newC.getScale();
      assert oldScale.remainder(newScale).isZero() || newScale.remainder(oldScale).isZero() : "The scales are not multiples of each other.";
      BigInt offset = oldC.getOffset().sub(newC.getOffset());
      Assign stmt = new Assign(new Lhs(var), new Rlin(linear(offset, term(oldScale, var)), newScale));
      addAssignment(stmt);
    }

    /**
     * The method scales down a variable according to a congruence
     * For example, if x is congruent to 1 (modulo 4),
     * (x-1)/4 is stored in the child domain by adding the
     * assignment operation: x = (x-1)/4
     *
     * @param var variable to be scaled
     * @param c congruence
     */
    public void addScaleDown (NumVar var, Congruence c) {
      assert !c.getScale().isZero();
      Assign stmt =
        new Assign(new Lhs(var), new Rlin(linear(c.getOffset().negate(), term(var)), c.getScale()));
      addAssignment(stmt);
    }

    /**
     * Create an assignment to the variable {@code to}, namely {@code to = S-1(to)} where {@code S} is the substitution
     *
     * @param subst the substitution containing {@code to}
     * @param to the variable that is being assigned to
     */
    public void addSubstitution (Substitution subst, NumVar to) {
      NumVar from = subst.getVar();
      assert to != null;
      // check if this substitution is a simple variable renaming or whether it has to be expressed using assignments
      if (subst.isSimple()) {
        assert to == subst.getExpr().getKey();
        // it's a renaming; this maps onto the substitution domain operation
        addDirectSubstitution(from, to);
      } else {
        BigInt toCoeff = subst.getExpr().getCoeff(to);
        Linear le = subst.getExpr().dropTerm(to).subTerm(subst.getFac(), subst.getVar());
        ZenoFactory f = ZenoFactory.getInstance();
        Assign stmt = f.assign(f.variable(to), f.linear(le, toCoeff.negate()));
        addAssignment(stmt, from, to);
      }
    }

    public void addWrap (NumVar var, Interval range, boolean saturating) {
      childTrans.add(new ZenoChildOp.Wrap(var, range, saturating));
    }

    public void addAnd (NumVar var, Linear left, Linear right, int size) {
      childTrans.add(new And(var, left, right, size));
    }

    public void addOr (NumVar var, Linear left, Linear right, int size) {
      childTrans.add(new Or(var, left, right, size));
    }

    public void addXOr (NumVar var, Linear left, Linear right, int size) {
      childTrans.add(new XOr(var, left, right, size));
    }

    public int length () {
      return childTrans.size();
    }

    public boolean isEmpty () {
      return length() == 0;
    }

    public <D extends ZenoDomain<D>> D apply (D state) {
      D newState = state;
      for (ZenoChildOp action : childTrans) {
        newState = action.apply(newState);
        if (newState == null)
          throw new Unreachable();
      }
      assert newState != null;
      childTrans.clear();
      return newState;
    }

    @Override public String toString () {
      String res = "";
      String sep = "";
      for (ZenoChildOp ct : childTrans) {
        res += sep + ct.toString();
        sep = "; ";
      }
      return res;
    }
  }
}
