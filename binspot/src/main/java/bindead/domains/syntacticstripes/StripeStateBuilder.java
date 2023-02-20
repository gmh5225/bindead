package bindead.domains.syntacticstripes;

import static bindead.data.Linear.linear;
import static bindead.data.Linear.term;
import static bindead.domains.syntacticstripes.StripeStateBuilder.Constraint.MAX;
import static bindead.domains.syntacticstripes.StripeStateBuilder.Constraint.MIN;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.AVLSet;
import javalx.persistentcollections.ThreeWaySplit;
import bindead.abstractsyntax.zeno.Zeno.Lhs;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.data.Linear;
import bindead.data.Linear.Divisor;
import bindead.data.Linear.Term;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.domainnetwork.combinators.ZenoStateBuilder;
import bindead.domains.affine.Substitution;

/**
 * Mutable variant of {@link StripeState}.
 */
final class StripeStateBuilder extends ZenoStateBuilder {
  private static final ZenoFactory zeno = ZenoFactory.getInstance();
  private static final String SPECIALVARIABLEPREFIX = "w";
  AVLMap<Linear, Stripe> stripes;
  AVLMap<NumVar,AVLSet<Linear>> reverse;
  AVLMap<NumVar,Constraint> narrowingconstraints;
  VarSet specials;

  /**
   * Create a mutable variant of the given stripe-state {@code stripes}.
   *
   * @param stripes The immutable state.
   */
  public StripeStateBuilder (StripeState stripes) {
    this(stripes, false);
  }

  /**
   * Create a mutable variant of the given stripe-state {@code stripes}.
   *
   * @param stripes The immutable state.
   */
  public StripeStateBuilder (StripeState stripes, boolean keeppredicates) {
    this.stripes = stripes.stripes;
    this.reverse = stripes.reverse;
    this.specials = stripes.specials;
    if (keeppredicates)
      this.narrowingconstraints = stripes.narrowingconstraints;
    else
      this.narrowingconstraints = StripeState.EMPTYCONSTRAINTS;
  }

  /**
   * Build an immutable {@link StripeState} object.
   *
   * @return
   */
  public StripeState build () {
    return new StripeState(stripes, reverse, specials, narrowingconstraints);
  }

  // NOTE: might be broken as the x and y order was different
  public void applyDirectSubstitution (NumVar x, NumVar y) {
    applyDirectSubstitutionToConstraints(x, y);
    AVLSet<Linear> usedIn = reverse.getOrNull(x);
    for (Linear oldLin : usedIn) {
      Stripe special = stripes.get(oldLin).get();
      BigInt coeff = oldLin.getCoeff(x);
      Linear newLin = oldLin.dropTerm(x);
      newLin = newLin.addTerm(coeff, y).toEquality();
      normalizeSpecial(oldLin, newLin, special);
      removeLinear(oldLin, special.special);
      insertStripe(newLin, special);
    }
  }

  private void applyDirectSubstitutionToConstraints(NumVar x, NumVar y) {
    Constraint c = narrowingconstraints.get(x).getOrNull();
    if (c == null) return;
    removeConstraint(c);
    addConstraint(c.applySubstitution(x, y));
  }

  public void applyAffineTransformation (Transformation trans) {
    Linear newEq = trans.getNewEquality();
    if (newEq == null)
      // x := ax + by + ... + c
      applyInvertibleSubstitution(trans);
    else
      // x := ay + ... + c
      applyNonInvertibleSubstitution(trans);
  }

  private void applyInvertibleSubstitution (Transformation trans) {
    NumVar x = trans.getLhs();
    Linear rhs = trans.getRhs();
    AVLSet<Linear> usedIn = reverse.get(x).getOrElse(StripeState.EMPTYSET);
    applyInvertibleSubstitutionToConstraints(trans);
    if (usedIn.isEmpty()) {
      // Nothing to substitute but we can still insert a new stripe according
      // to the right-hand side of the given transformation.
      if (rhs.getVars().size() >= 2) {
        Stripe w = allocateSpecial();
        Substitution sigma = trans.genSubstitution();
        Divisor gcd = Divisor.one();
        Linear newLin = sigma.getExpr();
        BigInt c = newLin.getConstant();
        newLin = newLin.dropConstant().lowestForm(gcd);
        newLin = normalize(newLin, gcd);
        getChildOps().addAssignment(
            zeno.assign(
                zeno.variable(w.special),
                zeno.linear(linear(c, term(x)), gcd.get())));
        //normalizeSpecial(oldLin, newLin, w);
        insertStripe(newLin, w);
      }
    } else {
      Substitution subst = trans.genSubstitution();
      applySubstitution$(subst);
    }
  }

  private void applyInvertibleSubstitutionToConstraints (Transformation trans) {
    NumVar x = trans.getLhs();
    Substitution sigma = trans.genSubstitution();
    applyInvertibleSubstitutionToConstraints(x, sigma);
  }

  private void applyInvertibleSubstitutionToConstraints (NumVar x, Substitution sigma) {
    Constraint c = narrowingconstraints.getOrNull(x);
    if (c == null) return;
    removeConstraint(c);
    addConstraint(c.applySubstitution(sigma));
  }

  private void applySubstitution$ (Substitution subst) {
    NumVar var = subst.getVar();
    AVLSet<Linear> usedIn =
      reverse.get(var).getOrElse(StripeState.EMPTYSET);
    // Apply substitution to all linear expressions.
    for (Linear oldLin : usedIn) {
      Stripe special = stripes.get(oldLin).get();
      // First try to apply the given substitution.
      Linear newLin = oldLin.applySubstitution(subst);
      BigInt c = newLin.getConstant();
      newLin = newLin.dropConstant().toEquality();
      if (newLin.getVars().size() < 2) {
        // If its not possible to apply the given substitution, try to
        // "shorten" the stripe.
        // XXX: need to consider `c` in this case?
        if (oldLin.getVars().size() > 2) {
          newLin = shortenStripe(oldLin, special, var);
          removeLinear(oldLin, special.special);
          insertStripe(newLin, special);
        } else {
          removeLinear(oldLin, special.special);
          getChildOps().addKill(special.special);
        }
      } else {
        // Introduce new special variable w' = w. We need to do this because
        // when joining e.g. x + y = w and 2x + y = w, we would violate
        // the invariant that w is used only once (We could also handle this
        // in `join`).
        Stripe newSpecial = allocateSpecial();
        getChildOps().addAssignment(
            zeno.assign(
                zeno.variable(newSpecial.special),
                zeno.linear(special.special)));
        normalizeSpecial(oldLin, newLin, newSpecial, subst.getFac(), c);
        removeLinear(oldLin, special.special);
        insertStripe(newLin, newSpecial);
        getChildOps().addKill(special.special);
      }
    }
  }

  private Linear shortenStripe (Linear oldLin, Stripe special, NumVar x) {
    Divisor gcd = Divisor.one();
    Linear newLin = oldLin.dropTerm(x).lowestForm(gcd);
    newLin = normalize(newLin, gcd);
    // w := (w + x) / gcd
    getChildOps().addAssignment(
        zeno.assign(
            zeno.variable(special.special),
            zeno.linear(Linear.linear(
                Linear.term(special.special),
                Linear.term(oldLin.getCoeff(x), x)), gcd.get())));
//    normalizeSpecial(oldLin, newLin, special); TODO: find out why this is not necessary anymore
    return newLin;
  }

  private void normalizeSpecial (Linear oldLin, Linear newLin, Stripe special) {
    NumVar key = chooseVar(oldLin, newLin);
    if (key == null) return; // REFACTOR: Linear seriously needs to be rewritten!
    BigInt a = oldLin.getCoeff(key), b = newLin.getCoeff(key);
    if (a.mul(b).isNegative())
      // Propagate sign-change due to normalization to the special var:
      //   w := -1w
      getChildOps().addAssignment(
          zeno.assign(
              zeno.variable(special.special),
              zeno.linear(Linear.linear(
                  Linear.term(Bound.MINUSONE, special.special)))));
  }

  private void normalizeSpecial
      (Linear oldLin, Linear newLin, Stripe special, BigInt f, BigInt c) {
    NumVar key = chooseVar(oldLin, newLin);
    BigInt a = oldLin.getCoeff(key), b = newLin.getCoeff(key);
    if (a.mul(b).mul(f).isPositive())
      // w := f*w + c
      getChildOps().addAssignment(
          zeno.assign(
              zeno.variable(special.special),
              zeno.linear(Linear.linear(
                  c,
                  Linear.term(f, special.special)))));
    else
      // w := -f*w + c
      getChildOps().addAssignment(
          zeno.assign(
              zeno.variable(special.special),
              zeno.linear(Linear.linear(
                  c,
                  Linear.term(f.negate(), special.special)))));
  }

  // Returns a variable that is in both {@code oldLin} and {@code newLin}
  private static NumVar chooseVar (Linear oldLin, Linear newLin) {
    VarSet oldVars = oldLin.getVars();
    VarSet newVars = newLin.getVars();
    for (NumVar x : oldVars) {
      if (newVars.contains(x))
        return x;
    }
    return null;
  }

  private void applyNonInvertibleSubstitution (Transformation trans) {
    NumVar var = trans.getLhs();
    Linear newEq = trans.getNewEquality();
    assert newEq != null;
    applyProjection(var, trans.channel);
    if (newEq.getVars().size() >= 2) {
      Stripe special = allocateSpecial();
      Linear lin = allocateStripe(newEq, special);
      insertStripe(lin, special);
    }
  }

  private Stripe allocateSpecial () {
    NumVar special = NumVar.fresh(SPECIALVARIABLEPREFIX);
    getChildOps().addIntro(special);
    //specials = specials.add(special);
    return new Stripe(special);
  }

  private Linear allocateStripe (Linear lin, Stripe special) {
    BigInt c = lin.getConstant();
    BigInt coeff = lin.getCoeff(lin.getKey());
    Linear newLin = lin.dropConstant().toEquality();
    c = coeff.isEqualTo(lin.getCoeff(lin.getKey())) ? c : c.negate();
    getChildOps().addAssignment(
        zeno.assign(
            zeno.variable(special.special),
            zeno.literal(c)));
    return newLin;
  }

  private void bootstrapStripe (Stripe special, Linear lin) {
    assert isNormalized(lin);
    getChildOps().addIntro(special.special);
    getChildOps().addAssignment(
        zeno.assign(
            zeno.variable(special.special),
            zeno.linear(lin.negate())));
  }

  private void insertStripe (Linear lin, Stripe stripe) {
    assert isNormalized(lin) : "trying to insert non-normalized linear expression";
    assert lin.getConstant().isZero() : "trying to insert stripe with non-zero constant";
    // Check for "stripe-collision"
    if (stripes.contains(lin)) {
      //Stripe oldSpecial = stripes.get(lin).get();
      //removeLinear(lin, oldSpecial.special);
      getChildOps().addKill(stripe.special);
      return;
    }
    stripes = stripes.bind(lin, stripe);
    reverse = reverse.bind(stripe.special, StripeState.EMPTYSET.add(lin));
    specials = specials.add(stripe.special);
    for (Term t : lin) {
      NumVar x = t.getId();
      AVLSet<Linear> vs =
        reverse.get(x).getOrElse(StripeState.EMPTYSET).add(lin);
      reverse = reverse.bind(x, vs);
    }
  }

  private static boolean isNormalized (Linear lin) {
    BigInt coeff = lin.getCoeff(lin.getKey());
    return coeff.isPositive();
  }

  void removeLinear (Linear lin, NumVar special) {
    for (Term t : lin) {
      NumVar x = t.getId();
      reverse = reverse.bind(x, reverse.get(x).get().remove(lin));
    }
    reverse = reverse.bind(special, reverse.get(special).get().remove(lin));
    stripes = stripes.remove(lin);
    specials = specials.remove(special);
  }

  public static void makeCompatible
      (StripeStateBuilder fst,  StripeStateBuilder snd) {
    ThreeWaySplit<AVLMap<Linear, Stripe>> diff = fst.stripes.split(snd.stripes);
    // Add all stripes only in `fst` to `snd`.
    for (P2<Linear, Stripe> entry : diff.onlyInFirst()) {
      Linear lin = entry._1();
      Stripe special = entry._2();
      AVLSet<Linear> usedIn = snd.reverse.get(special.special).getOrElse(StripeState.EMPTYSET);
      if (usedIn.isEmpty()) {
        snd.bootstrapStripe(special, lin);
        snd.insertStripe(lin, special);
      } else {
        // It might happen that:
        //   S1 {x+y+w=0;x+y+z+u=0} and S2 {x+y+z+w=0}
        // so we get to "conflicting" stripes: x+y+w=0 and x+y+z+w=0
        // the problem arises when `u` gets substituted to `w`. El-cheap-o
        // solution: remove the stripe.
        fst.removeLinear(lin, special.special);
        fst.getChildOps().addKill(special.special);
      }
    }
    // Add all stripes only in `snd` to `fst`.
    for (P2<Linear, Stripe> entry : diff.onlyInSecond()) {
      Linear lin = entry._1();
      Stripe special = entry._2();
      AVLSet<Linear> usedIn = fst.reverse.get(special.special).getOrElse(StripeState.EMPTYSET);
      if (usedIn.isEmpty()) {
        fst.bootstrapStripe(special, lin);
        fst.insertStripe(lin, special);
      } else {
        snd.removeLinear(lin, special.special);
        snd.getChildOps().addKill(special.special);
      }
    }
    // Process differing stripes.
    // Check if a equal linear equations maps to different special variables
    for (P2<Linear, Stripe> binding : diff.inBothButDiffering()) {
      Linear lin = binding._1();
      Stripe fromFst = binding._2();
      Stripe fromSnd = snd.stripes.get(binding._1()).get();
      snd.removeLinear(lin, fromSnd.special);
      snd.insertStripe(lin, fromFst);
      snd.getChildOps().addDirectSubstitution(fromSnd.special, fromFst.special);
    }
    // Process constraints
    ThreeWaySplit<AVLMap<NumVar,Constraint>> splitConstraints = fst.narrowingconstraints.split(snd.narrowingconstraints);
    fst.narrowingconstraints = fst.narrowingconstraints.difference(splitConstraints.onlyInFirst());
    fst.narrowingconstraints = fst.narrowingconstraints.difference(splitConstraints.inBothButDiffering());
    snd.narrowingconstraints = snd.narrowingconstraints.difference(splitConstraints.onlyInSecond());
    snd.narrowingconstraints = snd.narrowingconstraints.difference(splitConstraints.inBothButDiffering());
  }

  public static void makeCompatibleForWidening
      (StripeStateBuilder fst, StripeStateBuilder snd) {
    ThreeWaySplit<AVLMap<Linear, Stripe>> diff = fst.stripes.split(snd.stripes);
    for (P2<Linear, Stripe> binding : diff.onlyInFirst()) {
      Linear lin = binding._1();
      Stripe special = binding._2();
      fst.removeLinear(lin, special.special);
      fst.getChildOps().addKill(special.special);
    }
    for (P2<Linear, Stripe> binding : diff.onlyInSecond()) {
      Linear lin = binding._1();
      Stripe special = binding._2();
      snd.removeLinear(lin, special.special);
      snd.getChildOps().addKill(special.special);
    }
    // Process differing stripes.
    for (P2<Linear, Stripe> binding : diff.inBothButDiffering()) {
      Linear lin = binding._1();
      Stripe fromFst = binding._2();
      Stripe fromSnd = snd.stripes.get(binding._1()).get();
      snd.removeLinear(lin, fromSnd.special);
      snd.insertStripe(lin, fromFst);
      snd.getChildOps().addDirectSubstitution(fromSnd.special, fromFst.special);
    }
  }

  // Assumption: States have been made compatible.
  void findNarrowingConstraints (QueryChannel proxyOfFst, QueryChannel proxyOfSnd) {
    for (P2<Linear, Stripe> entry : stripes) {
      Linear lin = entry._1();
      Stripe special = entry._2();
      Interval fromFst = proxyOfFst.queryRange(special.special).convexHull();
      Interval fromSnd = proxyOfSnd.queryRange(special.special).convexHull();
      if (fromFst.compareTo(fromSnd) == 0 || !fromFst.isFinite() || !fromSnd.isFinite())
        continue;
      Interval inFst = proxyOfFst.queryRange(lin).convexHull();
      Interval inSnd = proxyOfSnd.queryRange(lin).convexHull();
      // Check if stripe changed
      if (inFst.compareTo(inSnd) != 0 && inFst.isFinite() && inSnd.isFinite()) {
        Linear q = lin, w = special.asLinear();
        BigInt uw1 = fromFst.high().asInteger();
        BigInt uw2 = fromSnd.high().asInteger();
        BigInt lw1 = fromFst.low().asInteger();
        BigInt lw2 = fromSnd.low().asInteger();
        BigInt uq1 = inFst.high().asInteger();
        BigInt uq2 = inSnd.high().asInteger();
        BigInt lq1 = inFst.low().asInteger();
        BigInt lq2 = inSnd.low().asInteger();
        maybeAddConstraint(MIN, w, MIN, q, lw1, lq1, lw2, lq2);
        maybeAddConstraint(MAX, w, MIN, q, uw1, lq1, uw2, lq2);
        maybeAddConstraint(MIN, w, MAX, q, lw1, uq1, lw2, uq2);
        maybeAddConstraint(MAX, w, MAX, q, uw1, uq1, uw2, uq2);
      }
    }
  }

  private void maybeAddConstraint
      (boolean maxX, Linear x, boolean maxY, Linear y, BigInt x1, BigInt y1, BigInt x2, BigInt y2) {
    Constraint c = Constraint.findConstraintOrNull(maxX, x, maxY, y, x1, y1, x2, y2);
    if (c != null)
      addConstraint(c);
  }

  private void addConstraint (Constraint c) {
    // XXX: A Variable might occur in more than one constraint!
    //   AVLMap<NumVar,ConstraintSet>
    for (NumVar x : c.getVars()) {
      narrowingconstraints = narrowingconstraints.bind(x, c);
    }
  }

  private void removeConstraint (Constraint c) {
    // XXX: A Variable might occur in more than one constraint!
    //   AVLMap<NumVar,ConstraintSet>
    for (NumVar x : c.getVars()) {
      narrowingconstraints = narrowingconstraints.remove(x);
    }
  }

  public void applyProjection (NumVar x, QueryChannel channel) {
    AVLSet<Linear> usedIn = reverse.get(x).getOrElse(StripeState.EMPTYSET);
    AVLSet<Linear> equalities = StripeState.EMPTYSET;
    // Find all equalities, that is {w|ax + bx + ... + w; w = [c,c]}
    for (Linear lin : usedIn) {
      Range range = querySpecial(channel, lin);
      BigInt c = range.getConstantOrNull();
      if (c != null)
        equalities = equalities.add(lin.add(c));
    }
    for (Linear eq : equalities) {
      for (Linear oldLin : usedIn.remove(eq)) {
        tryInsertEquality(eq, x, oldLin);
      }
    }
    // Remove all stripes still mentioning `variable`.
    // NOTE: `tryCreateNewStripe` did not remove any stripes.
    removeVariable(x);
  }

  // Try to apply the given substitution and creating a new stripe
  // out of it. The stripe mentioning `oldLin` will not be removed.
  private void tryInsertEquality (Linear eq, NumVar x, Linear oldLin) {
    // Apply the given substitution and check that the resulting
    // stripe ranges over at least two variables. `oldLin` already
    // includes the value of its special variable as constant.
    Stripe oldSpecial = stripes.get(oldLin).get();
    oldLin = oldLin.add(oldSpecial.special);
    Substitution sigma = eq.genSubstitution(x);
    Linear lin = oldLin.applySubstitution(sigma);
    applyInvertibleSubstitutionToConstraints(x, sigma);
    if (lin.getVars().size() > 2) {
      Stripe newSpecial = allocateSpecial();
      Divisor d = Divisor.one();
      Linear newLin = lin.dropConstant().lowestForm(d);
      newLin = newLin.dropTerm(oldSpecial.special).lowestForm(d);
      newLin = normalize(newLin, d);
      BigInt coeff = lin.getCoeff(oldSpecial.special);
      BigInt c = lin.getConstant();
      //normalizeSpecial(oldLin, newLin, oldSpecial);
      getChildOps().addAssignment(
          zeno.assign(
              zeno.variable(newSpecial.special),
              zeno.linear(
                  Linear.linear(c, Linear.term(coeff, oldSpecial.special)),
                  d.get())));
      insertStripe(newLin, newSpecial);
    }
  }

  public static Linear normalize (Linear lin, Divisor d) {
    Linear newLin = lin.toEquality();
    BigInt c1 = lin.getCoeff(lin.getKey());
    BigInt c2 = newLin.getCoeff(lin.getKey());
    if (!c1.isEqualTo(c2))
      d.negate();
    return newLin;
  }

  public void removeVariable (NumVar numVar) {
    // Remove var from constraints
    Constraint c = narrowingconstraints.get(numVar).getOrNull();
    if (c != null)
      removeConstraint(c);
    // Remove var from stripe system
    AVLSet<Linear> usedIn = reverse.get(numVar).getOrElse(StripeState.EMPTYSET);
    for (Linear lin : usedIn) {
      Stripe special = stripes.get(lin).get();
      if (lin.getVars().size() > 2) {
        Linear newLin = shortenStripe(lin, special, numVar);
        removeLinear(lin, special.special);
        insertStripe(newLin, special);
      } else {
        removeLinear(lin, special.special);
        getChildOps().addKill(special.special);
      }
    }
  }

  private Range querySpecial (QueryChannel channel, Linear key) {
    NumVar special = stripes.get(key).get().special;
    return channel.queryRange(Linear.linear(special));
  }

  @Override public String toString () {
    return "StripeStateBuilder{" + stripes + '}';
  }

  static class Constraint {
    static final boolean MAX = true;
    static final boolean MIN = false;
    final boolean max1;
    final BigInt a1;
    final Linear special;
    final boolean max2;
    final BigInt a2;
    final Linear stripe;
    final BigInt c;

    Constraint (boolean max1, BigInt a1, Linear special, boolean max2, BigInt a2, Linear stripe, BigInt c) {
      this.max1 = max1;
      this.a1 = a1;
      this.special = special;
      this.max2 = max2;
      this.a2 = a2;
      this.stripe = stripe;
      this.c = c;
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder("<");
      builder.append(max2 ? "max(" : "min(");
      builder.append(a2 + "(" + stripe + ")").append(") + ");
      builder.append(max1 ? "max(" : "min(");
      builder.append(a1 + "(" + special + ")").append(')');
      builder.append(" <= ").append(c).append('>');
      return builder.toString();
    }

    VarSet getVars () {
      return special.getVars().union(stripe.getVars());
    }

    Constraint applySubstitution (Substitution sigma) {
      Divisor d = Divisor.one();
      Linear newStripe = stripe.applySubstitution(sigma).lowestForm(d);
      // Check if stripe was normalized during the substitution
      if (d.get().isNegative())
        return new Constraint(!max1, a1, special, !max2, a2, newStripe, c.negate());
      else
        return new Constraint(max1, a1, special, max2, a2, newStripe, c);
    }

    Constraint applySubstitution (NumVar y, NumVar x) {
      Linear newStripe = stripe.applySubstitution(y, x);
      if (isNormalized(newStripe))
        return new Constraint(max1, a1, special, max2, a2, newStripe, c);
      else
        return new Constraint(!max1, a1, special, !max2, a2, newStripe.toEquality(), c.negate());
    }

    Test asTestOrNull (QueryChannel channel) {
      Interval fromStripe = channel.queryRange(stripe).convexHull().mul(a2);
      if (!fromStripe.isFinite())
        return null;
      BigInt x = max2 ? fromStripe.high().asInteger() : fromStripe.low().asInteger();
      x = c.sub(x);
      Linear lin = special.sub(x);
      lin = max1 ? lin : lin.smul(a1).negate();
      return zeno.comparison(lin, ZenoTestOp.LessThanOrEqualToZero);
    }

    static Constraint findConstraintOrNull
        (boolean maxX, Linear x, boolean maxY, Linear y, BigInt x1, BigInt y1, BigInt x2, BigInt y2) {
      BigInt a = y2.sub(y1);
      BigInt b = x1.sub(x2);
      BigInt c = a.mul(x1).add(b.mul(y1));
      // ax + by <= c
      if (!a.isZero() && !b.isZero())
        return new Constraint(maxX, a, x, maxY, b, y, c);
      return null;
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = 1;
      result = prime * result + (a1 == null ? 0 : a1.hashCode());
      result = prime * result + (a2 == null ? 0 : a2.hashCode());
      result = prime * result + (c == null ? 0 : c.hashCode());
      result = prime * result + (max1 ? 1231 : 1237);
      result = prime * result + (max2 ? 1231 : 1237);
      result = prime * result + (special == null ? 0 : special.hashCode());
      result = prime * result + (stripe == null ? 0 : stripe.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Constraint other = (Constraint) obj;
      if (a1 == null) {
        if (other.a1 != null) return false;
      } else if (!a1.isEqualTo(other.a1)) return false;
      if (a2 == null) {
        if (other.a2 != null) return false;
      } else if (!a2.isEqualTo(other.a2)) return false;
      if (c == null) {
        if (other.c != null) return false;
      } else if (!c.isEqualTo(other.c)) return false;
      if (max1 != other.max1) return false;
      if (max2 != other.max2) return false;
      if (special == null) {
        if (other.special != null) return false;
      } else if (!special.equals(other.special)) return false;
      if (stripe == null) {
        if (other.stripe != null) return false;
      } else if (!stripe.equals(other.stripe)) return false;
      return true;
    }
  }

  static class Transformation {
    private final QueryChannel channel;
    private final Divisor divisor;
    private final NumVar lhs;
    private final Linear rhs;
    private final Linear newEquality;

    Transformation (QueryChannel channel, Divisor divisor, NumVar lhs, Linear rhs) {
      this.channel = channel;
      this.divisor = divisor;
      this.lhs = lhs;
      this.rhs = rhs;
      this.newEquality = genEqualityOrNull();
    }

    public Transformation (QueryChannel channel, Lhs lhs, Rlin rhs) {
      this(channel, new Divisor(rhs.getDivisor()), lhs.getId(), rhs.getLinearTerm());
    }

    private Linear genEqualityOrNull () {
      return rhs.addTerm(divisor.get().negate(), lhs)==null ?
    		  null : rhs.subTerm(divisor.get(), lhs).toEquality();
    }

    public Substitution genSubstitution () {
      return Substitution.invertingSubstitution(rhs, divisor.get(), lhs);
    }

    public Divisor getDivisor () {
      return divisor;
    }

    public NumVar getLhs () {
      return lhs;
    }

    public Linear getRhs () {
      return rhs;
    }

    public Linear getNewEquality () {
      return newEquality;
    }

    @Override public String toString () {
      return "Transformation{" +
        lhs +
        " = " +
        rhs +
        " / " +
        divisor +
        "}";
    }
  }
}
