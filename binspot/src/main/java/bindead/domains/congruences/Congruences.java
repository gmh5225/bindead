package bindead.domains.congruences;

import java.util.Iterator;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Congruence;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.MultiMap;
import javalx.persistentcollections.ThreeWaySplit;
import rreil.lang.util.Type;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.Linear.Term;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.combinators.ZenoFunctor;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.domains.affine.Substitution;
import bindead.exceptions.DomainStateException.VariableSupportSetException;
import bindead.exceptions.Unreachable;

/**
 * A domain that infers and stores congruence information per variable. The domain does not keep its child maximally
 * reduced but stores all variables also in the child.
 */
public class Congruences<D extends ZenoDomain<D>> extends ZenoFunctor<CongruenceState, D, Congruences<D>> {
  public static final String NAME = "CONGRUENCES";

  public Congruences (D child) {
    super(NAME, CongruenceState.EMPTY, child);
  }

  private Congruences (CongruenceState state, D childState) {
    super(NAME, state, childState);
  }

  @Override public Congruences<D> introduce (NumVar variable, Type type, Option<BigInt> value) {
    CongruenceStateBuilder builder = new CongruenceStateBuilder(state);
    Congruence c;
    Option<BigInt> childValue = Option.<BigInt>none();
    if (value.isSome()) {
      c = new Congruence(Bound.ZERO, value.get());
      childValue = Option.<BigInt>some(value.get());
    } else {
      c = Congruence.ONE;
    }
    builder.state = builder.state.bind(variable, c);
    D newChildState = childState.introduce(variable, type, childValue);
    newChildState = builder.applyChildOps(newChildState);
    return new Congruences<D>(builder.build(), newChildState);
  }

  @Override public Congruences<D> project (VarSet vars) {
    CongruenceStateBuilder builder = new CongruenceStateBuilder(state);
    for (NumVar variable : vars)
      builder.remove(variable);
    D newChildState = childState.project(vars);
    return build(builder.build(), newChildState);
  }

  @Override public Congruences<D> build (CongruenceState state, D childState) {
    return new Congruences<D>(state, childState);
  }

  @Override public Congruences<D> join (Congruences<D> other) {
    CongruenceStateBuilder thisBuilder = new CongruenceStateBuilder(state);
    CongruenceStateBuilder otherBuilder = new CongruenceStateBuilder(other.state);
    thisBuilder.makeCompatible(otherBuilder);

    ThreeWaySplit<AVLMap<NumVar, Congruence>> split = split(thisBuilder.state, otherBuilder.state);
    AVLMap<NumVar, Congruence> current = otherBuilder.state.intersection(Congruence.join, split.inBothButDiffering());
    thisBuilder.state = current.union(Congruence.join, thisBuilder.state);

    D thisChildState = thisBuilder.applyChildOps(childState);
    D otherChildState = otherBuilder.applyChildOps(other.childState);
    CongruenceState compatibleState = thisBuilder.build();

    D newChildState = thisChildState.join(otherChildState);

    return build(compatibleState, newChildState);
  }

  private static ThreeWaySplit<AVLMap<NumVar, Congruence>> split (AVLMap<NumVar, Congruence> state,
      AVLMap<NumVar, Congruence> other) {
    ThreeWaySplit<AVLMap<NumVar, Congruence>> split = state.split(other);
    if (!split.onlyInFirst().isEmpty())
      throw new VariableSupportSetException();
    if (!split.onlyInSecond().isEmpty())
      throw new VariableSupportSetException();
    return split;
  }

  @Override public Congruences<D> widen (Congruences<D> other) {
    CongruenceStateBuilder thisBuilder = new CongruenceStateBuilder(state);
    CongruenceStateBuilder otherBuilder = new CongruenceStateBuilder(other.state);
    thisBuilder.makeCompatible(otherBuilder);

    ThreeWaySplit<AVLMap<NumVar, Congruence>> split = split(thisBuilder.state, other.state.congruences);
    AVLMap<NumVar, Congruence> current = otherBuilder.state.intersection(Congruence.join, split.inBothButDiffering());
    thisBuilder.state = current.union(Congruence.join, thisBuilder.state);

    D thisChildState = thisBuilder.applyChildOps(childState);
    D otherChildState = otherBuilder.applyChildOps(other.childState);
    D newChildState = thisChildState.widen(otherChildState);

    return build(thisBuilder.build(), newChildState);
  }

  @Override public boolean subsetOrEqual (Congruences<D> other) {
    ThreeWaySplit<AVLMap<NumVar, Congruence>> split = split(state.congruences, other.state.congruences);
    for (Iterator<P2<NumVar, Congruence>> it = split.inBothButDiffering().iterator(); it.hasNext();) {
      P2<NumVar, Congruence> entry = it.next();
      if (!entry._2().subsetOrEqual(other.state.congruences.get(entry._1()).get()))
        return false;
    }

    CongruenceStateBuilder thisBuilder = new CongruenceStateBuilder(state);
    CongruenceStateBuilder otherBuilder = new CongruenceStateBuilder(other.state);
    thisBuilder.makeCompatible(otherBuilder);
    D thisChildState = thisBuilder.applyChildOps(childState);
    D otherChildState = otherBuilder.applyChildOps(other.childState);
    return thisChildState.subsetOrEqual(otherChildState);
  }

  @Override public Congruences<D> eval (Assign stmt) {
    CongruenceStateBuilder builder = new CongruenceStateBuilder(state);
    // build new assignment which is scaled according to the congruences
    ScalingVisitor.run(stmt, builder);
    // evaluate new assignment on child domain
    D newChildState = builder.applyChildOps(childState);
    return build(builder.build(), newChildState);
  }

  @Override public Congruences<D> eval (Test test) throws Unreachable {
    CongruenceStateBuilder builder = new CongruenceStateBuilder(state);
    // build new test which is scaled according to the congruences
    ScalingVisitor.run(test, builder);
    // evaluate new test on child domain
    D newChildState = builder.applyChildOps(childState);
    builder.reduceFromNewEqualities(newChildState.getSynthChannel().getEquations());
    // reducing from new equalities can modify the child as it applies the scale to constant values
    newChildState = builder.applyChildOps(newChildState);
    return build(builder.build(), newChildState);
  }

  @Override public Congruences<D> substitute (NumVar x, NumVar y) {
    CongruenceStateBuilder builder = new CongruenceStateBuilder(state);
    builder.substituteInCongruences(x, y);
    D newChildState = childState.substitute(x, y);
    return build(builder.build(), newChildState);
  }

  @Override public Congruences<D> expand (FoldMap vars) {
    CongruenceStateBuilder builder = new CongruenceStateBuilder(state);
    builder.expand(vars);
    D newChildState = childState.expand(vars);
    return build(builder.build(), newChildState);
  }

  @Override public Congruences<D> fold (FoldMap vars) {
    CongruenceStateBuilder builder = new CongruenceStateBuilder(state);
    builder.fold(vars);
    D newChildState = builder.applyChildOps(childState);
    newChildState = newChildState.fold(vars);
    return build(builder.build(), newChildState);
  }

  @Override public Congruences<D> copyAndPaste (VarSet vars, Congruences<D> from) {
    CongruenceStateBuilder builder = new CongruenceStateBuilder(state);
    D newChildState = childState.copyAndPaste(vars, from.childState);
    builder.copyAndPaste(vars, from.state);
    return build(builder.build(), newChildState);
  }

  @Override public Range queryRange (Linear expr) {
    Congruence congruence = evaluateCongruences(expr, state.congruences);
    Linear inlinedExpr = state.inlineIntoLinear(expr, null);
    if (inlinedExpr.isConstantOnly())
      return Range.from(inlinedExpr.getConstant()).ensureCongruent(congruence);
    else
      return childState.queryRange(inlinedExpr).ensureCongruent(congruence);
  }

  @Override public SynthChannel getSynthChannel () {
    SynthChannel oldChannel = super.getSynthChannel(); // get child channel with local vars removed
    SynthChannel newChannel = oldChannel.clone();
    SetOfEquations newEquations = SetOfEquations.empty();
    for (Linear equation : oldChannel.getEquations()) {
      newEquations = newEquations.add(scaleLinear(equation));
    }
    newChannel.setEquations(newEquations);
    MultiMap<Test, Test> newImplications = MultiMap.empty();
    ZenoFactory factory = ZenoFactory.getInstance();
    for (P2<Test, Test> implication : oldChannel.getImplications()) {
      Test lhs = implication._1();
      Test newLhs = factory.comparison(scaleLinear(lhs.getExpr()), lhs.getOperator());
      Test rhs = implication._2();
      Test newRhs = factory.comparison(scaleLinear(rhs.getExpr()), rhs.getOperator());
      newImplications = newImplications.add(newLhs, newRhs);
    }
    newChannel.setImplications(newImplications);
    return newChannel;
  }

  private Linear scaleLinear (Linear equation) {
    Linear newEquation = equation;
    for (NumVar var : equation.getVars()) {
      // var_p = value of variable in parent
      // var_c = value of variable in child
      // the relation is var_p = var_c * scale + offset => var_c = (var_p - offset) / scale
      // the linear expression c * var_c + rest = 0 becomes then:
      // c * var_c - c * offset + scale * rest = 0
      // expressed as substitution: var_c \ (var_c - offset) / scale
      // this substitution is applied to each variable in the equation
      if (state.congruences.contains(var)) {
        Congruence congruence = state.congruences.get(var).get();
        BigInt scale = congruence.getScale();
        BigInt offset = congruence.getOffset();
        if (scale.isZero()) {
          // the variable is constant but the child reports it as a "new" equality. Assume the child has the same value
          Range childValue = childState.queryRange(var);
          assert childValue.sub(offset).isZero() : "The child domain has a different constant value for variable: "
            + var + "=" + childValue + " than the Congruences domain: " + offset;
          continue;
        } else if (scale.isOne() && offset.isZero()) {
          // the identity congruence can be ignored
          continue;
        }
        // convert child linear using arithmetic transformations
//        BigInt coefficient = newEquation.getCoeff(var);
//        newEquation = newEquation.dropTerm(var).smul(scale);
//        newEquation = newEquation.sub(coefficient.mul(offset));
//        newEquation = newEquation.addTerm(coefficient, var);
        // or express the conversion using a substitution for the variable
        Substitution substitution = new Substitution(var, Linear.linear(offset.negate(), Linear.term(var)), scale);
        newEquation = newEquation.applySubstitution(substitution);
      }
    }
    return newEquation;
  }

  @Override public void varToCompactString (StringBuilder builder, NumVar var) {
    state.appendVar(var, builder, childState);
  }

  /**
   * Calculate a congruence for the given variable assignments.
   *
   * @param linear A linear expression
   * @param congruences the variable assignments
   * @return the congruence value of the linear expression
   */
  public static Congruence evaluateCongruences (Linear linear, AVLMap<NumVar, Congruence> congruences) {
    Congruence res = new Congruence(Bound.ZERO, Bound.ZERO);
    // congruence sum of congr(x) * coeff(x) for each variable in the linear expression
    for (Term term : linear) {
      Congruence value = congruences.get(term.getId()).getOrElse(Congruence.ONE).mul(term.getCoeff());
      res = res.add(value);
    }
    res = res.add(new Congruence(Bound.ZERO, linear.getConstant()));
    return res;
  }

}
