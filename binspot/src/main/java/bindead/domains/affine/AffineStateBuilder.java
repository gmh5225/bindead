package bindead.domains.affine;

import java.util.HashMap;
import java.util.Vector;

import javalx.data.products.P2;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.ThreeWaySplit;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.Linear.Divisor;
import bindead.data.NumVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.domainnetwork.combinators.ZenoStateBuilder;
import bindead.exceptions.Unreachable;

/**
 * The result of an affine transformation on the equality system of the affine domain.
 */
class AffineStateBuilder extends ZenoStateBuilder {
  // the class contains mutable versions of the members of AffineCtx and a sequence of operations that need to be
  // performed on the child domain
  private AVLMap<NumVar, Linear> affine;
  private AVLMap<NumVar, VarSet> reverse;
  private VarSet newEqualities = VarSet.empty();

  /**
   * Create a mutable affine domain object from an immutable one.
   *
   * @param dom the immutable affine domain
   */
  AffineStateBuilder (AffineState dom) {
    this.affine = dom.affine;
    this.reverse = dom.reverse;
  }

  /**
   * Extract the mutable state of this object into an immutable {@link AffineCtx} object.
   *
   * @return an immutable affine domain
   */
  AffineState build () {
    return new AffineState(affine, reverse, newEqualities);
  }

  /**
   * Perform an affine transformation on the equality system such that {@code d*var = rhs} holds. Creates up to two
   * substitutions and up to one variable that needs projecting out.
   *
   * @param d the factor in front of the variable, represented by a {@link bindead.data.numeric.Linear.Divisor} object
   * @param var the variable
   * @param rhs the new value of the scaled variable, may only contain parameter variables
   */
  void affineTrans (Linear.Divisor d, NumVar var, Linear rhs) {
    if (!rhs.getCoeff(var).isZero()) {
      // this is an invertible substitution
      Substitution subst = Substitution.invertingSubstitution(rhs, d.get(), var);
      getChildOps().addSubstitution(subst, var);
      // apply this substitution to the matrix
      VarSet indices = reverse.get(var).getOrElse(VarSet.empty());
      NumVar rhsKey = rhs.getKey();
      VarSet simpleIndices = indices.getSmaller(rhsKey);
      applySubstInRows(simpleIndices, subst);
      int noOfEqs = indices.size() - simpleIndices.size();
      if (noOfEqs == 0)
        return;

      // There are some variables in indices\simpleIndices whose equations would
      // get a new key when applying subst to them. For these variables,
      // we extract their equations.
      Linear[] equations = new Linear[noOfEqs];
      for (int idx = 0; idx < equations.length; idx++) {
        equations[idx] = removeLhsVar(indices.get(idx + simpleIndices.size()));
      }
      // For each equation, apply the substitution to get a new equation that we inline and
      // insert into the matrix. From this new equation, calculate a new substitution newSubst that
      // removes the leading variable. We apply newSubst to the rhs of
      // subst, thereby ensuring that subst does not mention the new key of the equation.
      // This combined substitution is then used to transform the next equation.
      for (Linear equation : equations) {
        NumVar rowVar = equation.getKey();
        Linear newEquation = equation.applySubstitution(subst);
        Substitution newSubst = inlineLinear(newEquation, rowVar);
        insertLinear(newEquation);
        subst = subst.applySubst(newSubst);
      }
    } else {
      Linear newEq = rhs.subTerm(d.get(), var);
      newEq = newEq.toEquality();
      // this is a non-invertible substitution, remove information on var
      removeVariable(var);
      // before we insert newEq, we have to ensure that its leading variable is not a parametric variable
      // in the current constraint system
      if (!newEq.getKey().equalTo(var))
        inlineLinear(newEq, var);
      insertLinear(newEq);
    }
  }

  /**
   * Intersect the domain with the given equality. All variables in the passed-in equality must be parametric, that is,
   * all leading variables in this domain must have been inlined into {@code con} using
   * {@link AffineCtx#inlineIntoLinear}.
   *
   * @param con the equality over parametric variables
   * @throws Unreachable
   */
  void intersectWithLinear (Linear con) throws Unreachable {
    NumVar key = con.getKey();
    if (key == null) {
      if (con.getConstant().isZero())
        return;
      throw new Unreachable();
    }
    newEqualities = newEqualities.add(key);
    inlineLinear(con, null);
    insertLinear(con);
  }

  /**
   * Remove a variable from the constraint system. May result in a substitution or a variable that needs to
   * be projected out from the parameter domain.
   *
   * @param var the variable that is to be removed
   */
  void removeVariable (NumVar var) {
    Linear removed = removeLhsVar(var);
    if (removed == null)
      removeRhsVar(var, true);
  }

  /**
   * Ensure that the given variable is a parameter variable, i.e. that it is not a defining variable in the
   * affine domain.
   *
   * @param numVar the variable to push into the child domain
   */
  void promoteVariable (NumVar numVar) {
    Linear removed = removeLhsVar(numVar);
    if (removed != null)
      getChildOps().addEquality(removed);
    else
      removeRhsVar(numVar, false);
  }

  /**
   * Apply the given substitution to the rows whose keys are in {@code vars}.
   * The substitution may not add variables that are in the range of the {@code affine} itself. This is normally
   * fulfilled if all existing
   * equalities have been inlined into the terms from which the substitution
   * was created. It may also not add any parameter variables that are smaller
   * than the key variables in {@code vars}.
   *
   * @param vars
   *          the keys of the rows
   * @param subst
   *          the substitution to be applied
   */
  private void applySubstInRows (VarSet vars, Substitution subst) {
    for (NumVar rowVar : vars) {
      Linear row = affine.getOrNull(rowVar);
      Linear newRow = row.applySubstitution(subst).toEquality();
      // ensure that only substitutions are applied to rows that do not change the leading variable
      assert newRow.getKey() == row.getKey();
      affine = affine.bind(rowVar, newRow);
      Vector<P2<Boolean, NumVar>> diff = Linear.diffVars(newRow, row);
      for (P2<Boolean, NumVar> t : diff) {
        NumVar var = t._2();
        VarSet vs = reverse.get(var).getOrElse(VarSet.empty());
        if (t._1())
          reverse = reverse.bind(var, vs.remove(rowVar));
        else
          reverse = reverse.bind(var, vs.add(rowVar));
      }
    }
  }

  /**
   * Inline the given equality into the constraint system.
   *
   * @param con the new equality into which all equalities of the constraint system must have been inlined
   * @param add the variable in {@code con} that is a new parameter variable (i.e. it is new in the child domain)
   *          or {@code null} to indicate that there is no new variable (this is used when simply restricting the affine
   *          space during evalTest)
   */
  protected Substitution inlineLinear (Linear con, NumVar add) {
    NumVar key = con.getKey();
    Substitution subst = con.genSubstitution(key);
    VarSet vs = reverse.get(key).getOrElse(VarSet.empty());
    applySubstInRows(vs, subst);
    if (add == null) {
      getChildOps().addKill(key);
      newEqualities = newEqualities.union(vs);
    } else
      getChildOps().addSubstitution(subst, add);
    return subst;
  }

  /**
   * Insert an equality into the constraint system. All equalities from the constraint system must have been inlined
   * into the passed-in equality. This can be accomplished using {@link AffineCtx#inlineIntoLinear}.
   *
   * @param expr the linear expression to be inserted into the constraint system
   */
  protected void insertLinear (Linear expr) {
    NumVar var = expr.getKey();
    expr = expr.toEquality();
    affine = affine.bind(var, expr);
    for (Linear.Term t : expr) {
      if (!t.getId().equalTo(var)) {
        VarSet vs = reverse.get(t.getId()).getOrElse(VarSet.empty());
        vs = vs.add(var);
        reverse = reverse.bind(t.getId(), vs);
      }
    }
  }

  /**
   * Remove a left-hand-side variable from the affine domain.
   *
   * @param key the key of the inequality to be removed
   * @return the equality removed from the system or {@code null} if the variable was not part of the equality system
   */
  protected Linear removeLhsVar (NumVar key) {
    Linear eq = affine.get(key).getOrNull();
    if (eq == null)
      return null;
    affine = affine.remove(key);
    for (Linear.Term t : eq) {
      NumVar eqVar = t.getId();
      if (eqVar.equalTo(key))
        continue;
      VarSet eqVs = reverse.get(eqVar).get();
      reverse = reverse.bind(eqVar, eqVs.remove(key));
    }
    return eq;
  }

  /**
   * Remove a parametric variable (a non-leading variable) from the constraint system. If the given variable occurs
   * in the system, a substitution is stored that describes how the child domain over the parameter variables
   * need to change and an equality is returned that describes how the removed variable relates to other variables.
   *
   * @param var the non-leading variable that is to be removed
   */
  protected void removeRhsVar (NumVar var, boolean removeFromChild) {
    VarSet vs = reverse.get(var).getOrElse(VarSet.empty());
    if (vs.isEmpty()) {
      reverse = reverse.remove(var);
      if (removeFromChild)
        getChildOps().addKill(var);
    } else {
      NumVar lastVar = vs.get(vs.size()-1);
      // let eq contain the last row of the equality system: remove this row
      Linear eq = removeLhsVar(lastVar);
      assert eq != null;
      // replace var with the information in eq, i.e. create a substitution from eq
      Substitution subst = eq.genSubstitution(var);
      vs = vs.remove(lastVar);
      applySubstInRows(vs, subst);
      getChildOps().addLinearTransform(eq, removeFromChild ? var : null);
    }
  }

  /**
   * Reduce the this system to the affine space that is common to this and the second system. The result are equality
   * sets that describe the affine space in which this domain lies, the calculated affine space in which both
   * domains lie and the affine space that only holds for the second system.
   *
   * @param snd the second system of the comparison
   * @param forSubset when true the domains are not modified and a Boolean variable is returned indicating if
   *          a subset inclusion holds between this and the other affine domain
   *
   * @return {@code true} if this domain describes a smaller affine space than the second domain and {@code forSubset} is
   *         true
   */
  boolean makeCompatible (AffineStateBuilder snd, boolean forSubset) {
    AffineStateBuilder fst = this;
    ThreeWaySplit<AVLMap<NumVar, Linear>> diff = fst.affine.split(snd.affine);
    HashMap<Integer, NumVar> intToVar = new HashMap<Integer, NumVar>();
    // split is left-biased so that the diff._2() entries contain the data from affine
    Equations l = new Equations(diff.inBothButDiffering(), diff.onlyInFirst(), intToVar);
    // in order to obtain the common keys with the data from other.affine, we use the left-biased intersection
    Equations r = new Equations(snd.affine.intersection(diff.inBothButDiffering()), diff.onlyInSecond(), intToVar);
    if (!forSubset) {
      for (Equation eq : l) {
        fst.removeLhsVar(eq.getKey(intToVar));
      }
      for (Equation eq : r) {
        snd.removeLhsVar(eq.getKey(intToVar));
      }
    }
    Equations.AffineHullResult res = Equations.affineHull(l, r);
    if (!forSubset) {
      for (Equation eq : res.common) {
        fst.insertLinear(Linear.linear(eq, intToVar));
        snd.insertLinear(Linear.linear(eq, intToVar));
      }
    }
    for (Equation eq : res.onlyFst) {
      fst.getChildOps().addEquality(Linear.linear(eq, intToVar));
    }
    for (Equation eq : res.onlySnd) {
      snd.getChildOps().addEquality(Linear.linear(eq, intToVar));
    }
    return res.onlySnd.isEmpty();
  }

  FoldMap fold (FoldMap pairs) {
    VarSet lhsVars = VarSet.empty();
    VarSet ephemeralVars = VarSet.empty();
    for (VarPair pair : pairs) {
      ephemeralVars = ephemeralVars.add(pair.getEphemeral());
      if (affine.contains(pair.getPermanent()))
        lhsVars = lhsVars.add(pair.getPermanent());
      else
        lhsVars = lhsVars.union(reverse.get(pair.getPermanent()).getOrElse(VarSet.empty()));
      if (affine.get(pair.getEphemeral()).isSome())
        lhsVars = lhsVars.add(pair.getEphemeral());
      else
        lhsVars = lhsVars.union(reverse.get(pair.getEphemeral()).getOrElse(VarSet.empty()));
    }

    HashMap<Integer, NumVar> intToVar = new HashMap<Integer, NumVar>();
    Equations l = new Equations(lhsVars.size());
    Equations r = new Equations(lhsVars.size());
    for (NumVar i : lhsVars) {
      Linear lin = removeLhsVar(i);
      assert lin != null;
      l.add(lin.toEquation(intToVar));
      r.add(lin.renameVar(pairs).toEquation(intToVar));
    }
    r.gauss();
    Equations.AffineHullResult res = Equations.affineHull(l, r);
    for (Equation eq : res.onlyFst) {
      getChildOps().addEquality(Linear.linear(eq, intToVar));
    }
    for (Equation eq : res.onlySnd) {
      getChildOps().addEquality(Linear.linear(eq, intToVar));
    }

    // project out all ephemeral variables
    Vector<Linear> common = new Vector<Linear>(res.common.size());
    for (Equation eq : res.common)
      common.add(Linear.linear(eq, intToVar));

    Vector<Linear> assignments = Linear.eliminate(common, ephemeralVars);
    for (Linear eq : assignments)
      getChildOps().addEquality(eq);

    // insert all remaining equalities and remember their leading variables
    VarSet notToExpand = VarSet.empty();
    for (Linear eq : common) {
      notToExpand = notToExpand.add(eq.getKey());
      insertLinear(eq);
    }
    // kill the ephemeral variable of each pair whose permanent variable is stored in the affine domain
    FoldMap newPairs = new FoldMap();
    for (VarPair pair : pairs) {
      if (notToExpand.contains(pair.getPermanent()))
        getChildOps().addKill(pair.getEphemeral());
      else
        newPairs.add(pair);
    }
    return newPairs;
  }

  void sane () {
    for (P2<NumVar, VarSet> pair : reverse) {
      VarSet vs = pair._2();
      for (NumVar v : vs) {
        Linear l = affine.get(v).get();
        assert !l.getCoeff(pair._1()).isZero();
      }
    }
    for (P2<NumVar, Linear> pair : affine) {
      Linear eq = pair._2();
      NumVar key = eq.getKey();
      assert key == pair._1();
      for (Linear.Term t : eq) {
        if (t.getId() != key) {
          VarSet vs = reverse.get(t.getId()).getOrElse(VarSet.empty());
          assert vs.contains(key);
        }
      }
    }
  }

  Linear inlineIntoLinear (Linear eq, Divisor d) {
    return AffineState.inlineIntoLinear(eq, d, affine);
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("Childops: " + getChildOps());
    builder.append("\n");
    builder.append(build());
    return builder.toString();
  }

}
