package bindead.domains.congruences;

import static bindead.data.Linear.linear;
import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Congruence;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.ThreeWaySplit;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Lhs;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.combinators.ZenoStateBuilder;
import bindead.exceptions.DomainStateException.VariableSupportSetException;

class CongruenceStateBuilder extends ZenoStateBuilder {
  AVLMap<NumVar, Congruence> state;

  public CongruenceStateBuilder (CongruenceState state) {
    this.state = state.congruences;
  }

  public CongruenceState build () {
    return new CongruenceState(state);
  }

  public void remove (NumVar lhs) {
    state = state.remove(lhs);
  }

  public void substituteInCongruences (NumVar x, NumVar y) {
    Option<Congruence> congruenceOfXOption = state.get(x);
    if (congruenceOfXOption.isNone())
      throw new VariableSupportSetException();
    state = state.remove(x);
    state = state.bind(y, congruenceOfXOption.get());
  }

  public void expand (FoldMap pairs) {
    for (VarPair vp : pairs) {
      Option<Congruence> valueOfPermanentOption = state.get(vp.getPermanent());
      if (valueOfPermanentOption.isNone())
        throw new VariableSupportSetException();
      Congruence valueOfPermanent = valueOfPermanentOption.get();
      state = state.bind(vp.getEphemeral(), valueOfPermanent);
    }
  }

  public void fold (FoldMap pairs) {
    for (VarPair pair : pairs) {
      NumVar permanent = pair.getPermanent();
      NumVar ephemeral = pair.getEphemeral();
      Option<Congruence> valueOfPermanentOption = state.get(permanent);
      if (valueOfPermanentOption.isNone())
        throw new VariableSupportSetException();
      Congruence valueOfPermanent = valueOfPermanentOption.get();
      Option<Congruence> valueOfEphemeralOption = state.get(ephemeral);
      if (valueOfEphemeralOption.isNone())
        throw new VariableSupportSetException();
      Congruence valueOfEphemeral = valueOfEphemeralOption.get();
      Congruence joinedCongruence = valueOfPermanent.join(valueOfEphemeral);
      getChildOps().addScale(permanent, valueOfPermanent, joinedCongruence);
      getChildOps().addScale(ephemeral, valueOfEphemeral, joinedCongruence);
      state = state.remove(ephemeral);
      state = state.bind(permanent, joinedCongruence);
    }
  }

  public void copyAndPaste (VarSet vars, CongruenceState ctx) {
    for (NumVar var : vars) {
      Congruence value = ctx.congruences.get(var).getOrNull();
      if (value == null)
        throw new VariableSupportSetException();
      state = state.bind(var, value);
    }
  }

  /**
   * The method makes the children of two CongruenceState objects compatible
   * for each variable by adding child operations
   *
   * For example, if a variable x is congruent to 0(mod 4) in the current state, then x/4 was stored in the child
   * If x is congruent with 0(mod 6) in the other state, then x/6 was stored in the other child
   * Instead, x/2 (x/gcd(4,6)) must be stored in both children to be able to make operations
   * on the child domains (like join, fold, etc).
   *
   * @param other the state which needs to be made compatible to the current state
   */
  public void makeCompatible (CongruenceStateBuilder other) {
    ThreeWaySplit<AVLMap<NumVar, Congruence>> split = state.split(other.state);

    for (P2<NumVar, Congruence> pair : split.inBothButDiffering()) {
      NumVar var = pair._1();
      Congruence c1 = state.get(var).get();
      Congruence c2 = other.state.get(var).get();
      Congruence newCongruence = c1.join(c2);
      this.getChildOps().addScale(var, c1, newCongruence);
      other.getChildOps().addScale(var, c2, newCongruence);
    }

    for (P2<NumVar, Congruence> pair : split.onlyInFirst()) {
      // VariableSupportSet failure
      assert false : "This code should not be reachable as the variable set is made compatible in parent domains.";
      NumVar var = pair._1();
      Congruence c1 = state.get(var).get();
      Congruence c2 = Congruence.ONE;
      Congruence newCongruence = c1.join(c2);
      this.getChildOps().addScale(var, c1, newCongruence);
      other.getChildOps().addIntro(var);
    }

    for (P2<NumVar, Congruence> pair : split.onlyInSecond()) {
      // VariableSupportSet failure
      assert false : "This code should not be reachable as the variable set is made compatible in parent domains.";
      NumVar var = pair._1();
      Congruence c1 = Congruence.ONE;
      Congruence c2 = other.state.get(var).get();
      Congruence newCongruence = c1.join(c2);
      this.getChildOps().addIntro(var);
      other.getChildOps().addScale(var, c2, newCongruence);
    }
  }

  /**
   * Apply equalities that were inferred in the child domain to improve this domain.
   */
  public void reduceFromNewEqualities (SetOfEquations newEqualities) {
    // equalities are of the form "x-c = 0" in the child
    for (Linear equality : newEqualities) {
      NumVar var = equality.getKey();
      assert equality.getCoeff(var).isOne();
      assert equality.isSingleTerm();
      BigInt constantInChild = equality.getConstant().negate();
      Congruence oldCongruence = state.get(var).get();
      BigInt constant = constantInChild.mul(oldCongruence.getScale()).add(oldCongruence.getOffset());
      Congruence newCongruence = new Congruence(BigInt.ZERO, constant);
      state = state.bind(var, newCongruence);
      // readjust the value in the child after the scaling
      getChildOps().addAssignment(new Assign(new Lhs(var), new Rlin(linear(constant), BigInt.ONE)));
    }
  }

  /**
   * Insert all relevant congruences into the given expression.
   *
   * @param con the expression
   * @param d the divisor with which the expression was scaled down (may be {@code null})
   * @return a new expression that contains scaled variables according to the congruences
   */
  public Linear inlineIntoLinear (Linear con, Linear.Divisor d) {
    return CongruenceState.inlineIntoLinear(con, d, state);
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("Childops: ");
    builder.append(getChildOps());
    builder.append("\n");
    builder.append(build());
    return builder.toString();
  }

}
