package bindead.domains.apron;

import static bindead.data.Linear.linear;
import static bindead.data.Linear.term;
import static bindead.domains.apron.Marshaller.fromApronInterval;
import static bindead.domains.apron.Marshaller.fromApronLinear;
import static bindead.domains.apron.Marshaller.toApronExpr;
import static bindead.domains.apron.Marshaller.toApronLinear;
import static bindead.domains.apron.Marshaller.toApronScalar;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.exceptions.UncheckedExceptionWrapper;
import javalx.fn.Predicate;
import javalx.mutablecollections.CollectionHelpers;
import javalx.numeric.BigInt;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import javalx.persistentcollections.BiMap;
import rreil.lang.util.Type;
import apron.Abstract1;
import apron.ApronException;
import apron.Coeff;
import apron.Environment;
import apron.Lincons1;
import apron.Linexpr1;
import apron.Linterm1;
import apron.Manager;
import apron.Texpr1CstNode;
import apron.Texpr1Intern;
import bindead.abstractsyntax.zeno.Zeno;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarPair;
import bindead.data.VarSet;
import bindead.debug.DomainStringBuilder;
import bindead.debug.StringHelpers;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.interfaces.AnalysisCtx;
import bindead.domainnetwork.interfaces.ZenoHeadDomain;
import bindead.exceptions.DomainStateException;
import bindead.exceptions.DomainStateException.VariableSupportSetException;
import bindead.exceptions.Unreachable;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Abstract Numeric Domain that uses the Apron Library.
 *
 * @author Bogdan Mihaila
 */
public abstract class Apron extends ZenoHeadDomain<Apron> {
  public static final String NAME = "APRON";
  protected final Abstract1 state;
  protected final BiMap<NumVar, String> variablesMapping;
  private final SynthChannel channel;

  public Apron () {
    super(NAME, AnalysisCtx.unknown());  // start with unknown analysis context
    variablesMapping = BiMap.empty();
    channel = new SynthChannel();
    Environment env = new Environment();
    try {
      state = new Abstract1(getDomainManager(), env);
    } catch (ApronException e) {
      throw new UncheckedExceptionWrapper(e);
    }
  }

  protected Apron (Abstract1 state, BiMap<NumVar, String> variablesMapping, SynthChannel synth, AnalysisCtx ctx) {
    super(NAME, ctx);
    this.state = state;
    this.variablesMapping = variablesMapping;
    if (synth != null)
      channel = synth;
    else
      channel = new SynthChannel();
// NOTE: the bottom test seems to be too expensive, thus it is disabled temporarily for performance
//    try {
//      assert !state.isBottom(state.getCreationManager());
//    } catch (ApronException e) {
//      throw new UncheckedExceptionWrapper(e);
//    }
  }

  /**
   * Override in subclass and return the Apron domain instantiation that should be used,
   * e.g. intervals (Box), octagons (Octagon), polyhedra (Polka, PplPoly),
   * polyhedra with linear congruences (PplGrid, PolkaGrid).
   */
//    return new Box();
//    return new Octagon();
//    return new Polka(false);
//    return new PplPoly(false);
//    return new PplGrid();
//    return new PolkaGrid(false);
  protected abstract Manager getDomainManager ();

  protected abstract Apron build (Abstract1 state, BiMap<NumVar, String> variablesMapping,
      SynthChannel synth, AnalysisCtx ctx);

  private Apron build (Abstract1 state) {
    return build(state, variablesMapping, null, getContext());
  }

  private Apron build (Abstract1 state, SynthChannel synth) {
    return build(state, variablesMapping, synth, getContext());
  }

  private Apron build (Abstract1 state, BiMap<NumVar, String> variablesMapping) {
    return build(state, variablesMapping, null, getContext());
  }

  @Override public Apron setContext (AnalysisCtx ctx) {
    return build(state, variablesMapping, null, ctx);
  }

  @Override public boolean subsetOrEqual (Apron other) {
    try {
      return state.isIncluded(state.getCreationManager(), other.state);
    } catch (ApronException e) {
      throw new UncheckedExceptionWrapper(e);
    }
  }

  @Override public Apron join (Apron other) {
    try {
      Abstract1 newState = state.joinCopy(state.getCreationManager(), other.state);
      return build(newState);
    } catch (ApronException e) {
      throw new UncheckedExceptionWrapper(e);
    }
  }

  /**
   * {@inheritDoc}<br>
   * Apron expects {@code this} to be (less-or-equal) included in {@code other}.
   */
  @Override public Apron widen (Apron other) {
    try {
      Abstract1 newState = state.widening(state.getCreationManager(), other.state);
      return build(newState);
    } catch (ApronException e) {
      throw new UncheckedExceptionWrapper(e);
    }
  }

  public Apron meet (Apron other) {
    try {
      Abstract1 newState = state.meetCopy(state.getCreationManager(), other.state);
      return build(newState);
    } catch (ApronException e) {
      throw new UncheckedExceptionWrapper(e);
    }
  }

  @Override public Apron expand (FoldMap pairs) {
    // Semantics:
    // forall (x, y) ∈ pairs
    // defined as: (D.intro(y) meet D.intro(y).swap(x, y))
    // expressed with Apron transfer functions:
    // D' = D.substitute(x, y).intro(x) -> y now in D' with state of x
    // D" = D.intro(y)
    // D' meet D"
    // or:
    // D.c&p(y, D')

    // The simple variable-wise fold also exists in Apron.
    // Our multi-fold keeps relational information between any of the xs that also holds for the respective ys.
    // Thus we use the above operations and a meet.
    Apron thisPrime = substitute(toTuplesList(pairs, false));
    thisPrime = thisPrime.introduceWithTop(pairs.getPermanent());
    Apron thisDoublePrime = introduceWithTop(pairs.getEphemeral());
    return thisPrime.meet(thisDoublePrime);
  }

  @Override public Apron fold (FoldMap pairs) {
    // Semantics:
    // forall (x, y) ∈ pairs
    // defined as: (D join D.swap(x, y)).drop(x)
    // expressed with our transfer functions:
    // D' = D.project(x).substitute(y, x) -> x now in D' with state of y
    // D" = D join D'
    // D".project(y)
    // or:
    // D" = D.project(y)
    // D" join D'

    // The simple variable-wise expand also exists in Apron.
    // Our multi-expand keeps relational information between any of the xs that also holds for the respective ys.
    // Thus we use the above operations and a join.
    Apron thisPrime = project(pairs.getPermanent(), true);
    thisPrime = thisPrime.substitute(toTuplesList(pairs, true));
    Apron thisDoublePrime = project(pairs.getEphemeral(), true);
    return thisPrime.join(thisDoublePrime);
  }

  private static P2<Apron, Apron> makeCompatForMeet (Apron first, Apron second) {
    VarSet firstVars = first.getVars();
    VarSet secondVars = second.getVars();
    VarSet onlyInFirst = firstVars.difference(secondVars);
    VarSet onlyInSecond = secondVars.difference(firstVars);
    Apron newFirst = first.introduceWithTop(onlyInSecond);
    Apron newSecond = second.introduceWithTop(onlyInFirst);
    return P2.tuple2(newFirst, newSecond);
  }

  private static List<P2<NumVar, NumVar>> toTuplesList (FoldMap pairs, boolean reversed) {
    List<P2<NumVar, NumVar>> result = new LinkedList<P2<NumVar, NumVar>>();
    for (VarPair pair : pairs) {
      if (reversed)
        result.add(P2.tuple2(pair.getEphemeral(), pair.getPermanent()));
      else
        result.add(P2.tuple2(pair.getPermanent(), pair.getEphemeral()));
    }
    return result;
  }

  @Override public Apron copyAndPaste (VarSet vars, Apron from) {
    // assume that this support set does not contain the variables
    assert !this.getVars().containsAll(vars);
    // Semantics:
    // expressed with Apron transfer functions:
    // forall x ∈ vars(FROM)\vars; all the variables in FROM without the ones in vars
    // D' = FROM.project(x) -> D' contains now only states of vars, everything else is set to TOP
    // forall x ∈ vars
    // D" = D.intro(x)
    // D' meet D"
    // we also need to make the support sets compatible for the meet and remove the added variables afterwards
    // this is because c&p is used during compatible making in Undef and that means that the support sets
    // here are not necessarily compatible yet
    Apron varsOnlyState = from.project(from.getVars().difference(vars), false);
    Apron addedVarsState = introduceWithTop(vars);
    VarSet finalSupportSet = addedVarsState.getVars();
    P2<Apron, Apron> compatibleStates = makeCompatForMeet(varsOnlyState, addedVarsState);
    varsOnlyState = compatibleStates._1();
    addedVarsState = compatibleStates._2();
    Apron result = varsOnlyState.meet(addedVarsState);
    VarSet addedVars = result.getVars().difference(finalSupportSet);
    result = result.project(addedVars, true);
    return result;
  }

  private VarSet getVars () {
    VarSet result = VarSet.empty();
    for (NumVar var : variablesMapping.keys()) {
      result = result.add(var);
    }
    return result;
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    // TODO Auto-generated method stub
    throw new DomainStateException.UnimplementedMethodException();
  }

  @Override public Range queryRange (Linear expr) {
    Linexpr1 apronLinear;
    try {
      apronLinear = toApronLinear(expr, state.getEnvironment(), variablesMapping);
    } catch (VariableSupportSetException e) { // the mapping did not contain one of the variables so assume TOP for it
      return Range.from(Interval.TOP);
    }
    try {
      apron.Interval interval = state.getBound(state.getCreationManager(), apronLinear);
      return Range.from(fromApronInterval(interval));
    } catch (ApronException e) {
      throw new UncheckedExceptionWrapper(e);
    }
  }

  @Override public SetOfEquations queryEqualities (NumVar variable) {
    SetOfEquations equalities = SetOfEquations.empty();
    final String apronVariable = variablesMapping.get(variable).get();
    Lincons1[] allConstraints;
    try {
      allConstraints = state.toLincons(state.getCreationManager());
    } catch (ApronException e) {
      throw new UncheckedExceptionWrapper(e);
    }
    List<Lincons1> constraints = CollectionHelpers.filter(allConstraints, new Predicate<Lincons1>() {
      @Override public Boolean apply (Lincons1 constraint) {
        if (constraint.getKind() != Lincons1.EQ)
          return false;
        // constraint.getKind() == Lincons1.EQMOD could be useful, too. TODO: see how we can and if we can integrate
        // with our congruences and domains.
        if (!constraint.isLinear())
          return false;
        if (constraint.getCoeff(apronVariable).isZero()) // variable does not appear in constraint
          return false;
        return true;
      }
    });
    for (Lincons1 constraint : constraints) {
      Linexpr1 apronLinear = new Linexpr1(state.getEnvironment(), constraint.getLinterms(), constraint.getCst());
      equalities = equalities.add(fromApronLinear(apronLinear, variablesMapping));
    }
    return equalities;
  }

  @Override public SynthChannel getSynthChannel () {
    return channel;
  }

  @Override public Apron eval (Assign stmt) {
    // msg("assign " + stmt);
    NumVar lhs = stmt.getLhs().getId();
    // some domains expect that the lhs is introduced automatically if it does not exist
    if (!variablesMapping.contains(lhs)) {
      // introduce on demand the lhs if it does not exist as some domains do not introduce variables
      // before they assign to them (looking at you Wrapping!)
      Apron updatedDomain = introduce(lhs, Type.Zeno, Option.<BigInt>none());
      return updatedDomain.eval(stmt);
    }
    String apronLhs = variablesMapping.get(lhs).get();
    Texpr1Intern assign = toApronExpr(stmt.getRhs(), state.getEnvironment(), variablesMapping, this);
    try {
      Abstract1 newState = state.assignCopy(state.getCreationManager(), apronLhs, assign, null);
      SynthChannel synth = extractNewConstantEqualities(state, newState, VarSet.of(stmt.getLhs().getId()));
      if (!synth.getEquations().isEmpty())
        if (AnalysisProperties.INSTANCE.debugTests.isTrue())
          System.out.println("Synth: " + synth);
      return build(newState, synth);
    } catch (ApronException e) {
      throw new UncheckedExceptionWrapper(e);
    }
  }

  @Override public Apron eval (Test test) throws Unreachable {
    // msg("test " + test);
    if (test.getOperator().equals(Zeno.ZenoTestOp.NotEqualToZero)) {
      // special handling of disequalities because Apron is imprecise and [0, 1] != 0 would not yield [1, 1] but [0, 1]
      // to improve precision we use two inequalities instead
      if (AnalysisProperties.INSTANCE.debugTests.isTrue())
        System.out.println("Apron splits test: " + test);
      P2<Test, Test> tests = test.splitEquality();
      Apron firstState = null;
      Apron secondState = null;
      try {
        firstState = eval(tests._1());
      } catch (Unreachable _) {
      }
      try {
        secondState = eval(tests._2());
      } catch (Unreachable _) {
      }
      Apron result = joinNullables(firstState, secondState);
      if (result == null)
        throw new Unreachable();
      return result;
    }
    if (AnalysisProperties.INSTANCE.debugTests.isTrue())
      System.out.println("Numeric test: " + test);
    Linexpr1 linear = toApronLinear(test.getExpr(), state.getEnvironment(), variablesMapping);
    Lincons1 constraints;
    switch (test.getOperator()) {
    case EqualToZero:
      constraints = new Lincons1(Lincons1.EQ, linear);
      break;
// NOTE: disequalities get a special treatment above
//    case NotEqualToZero:
//      constraints = new Lincons1(Lincons1.DISEQ, linear);
//      break;
    case LessThanOrEqualToZero: // need to negate the linear as Apron has only an greater-or-equal operator
      linear = toApronLinear(test.getExpr().negate(), state.getEnvironment(), variablesMapping);
      constraints = new Lincons1(Lincons1.SUPEQ, linear);
      break;
    default:
      throw new DomainStateException.InvariantViolationException();
    }
    try {
      Abstract1 newState = state.meetCopy(state.getCreationManager(), constraints);
      if (newState.isBottom(newState.getCreationManager()))
        throw new Unreachable();
      SynthChannel synth = extractNewConstantEqualities(state, newState, test.getVars());
      if (!synth.getEquations().isEmpty())
        if (AnalysisProperties.INSTANCE.debugTests.isTrue())
          System.out.println("Synth: " + synth);
      return build(newState, synth);
    } catch (ApronException e) {
      throw new UncheckedExceptionWrapper(e);
    }
  }

  /**
   * Looks for all the variables that might have changed their value after a transfer function and if they became
   * constant return a linear equality for it.
   *
   * @param oldState The state before the transfer function
   * @param newState The state after the transfer function
   * @param vars The variables used in the transfer function
   * @return A set of new equalities caused by the transfer function
   * @throws ApronException
   */
  private SynthChannel extractNewConstantEqualities (Abstract1 oldState, Abstract1 newState, VarSet vars)
      throws ApronException {
    Map<NumVar, BigInt> newEqualities = new HashMap<NumVar, BigInt>();
    VarSet transitiveVars = getVarsWithTransitiveRelations(vars);
    for (NumVar variable : transitiveVars) {
      String apronVariable = variablesMapping.get(variable).get();
      apron.Interval oldValue = oldState.getBound(oldState.getCreationManager(), apronVariable);
      apron.Interval newValue = newState.getBound(newState.getCreationManager(), apronVariable);
      if (newValue.isScalar() && !newValue.equals(oldValue))
        newEqualities.put(variable, Marshaller.fromApronScalarNoRounding(newValue.inf()));
    }
    SynthChannel synth = new SynthChannel();
    for (Entry<NumVar, BigInt> pair : newEqualities.entrySet()) {
      synth.addEquation(linear(pair.getValue().negate(), term(pair.getKey())).toEquality());
    }
    // msg("extracted with varset " + vars + "\n" + synth);
    return synth;
  }

  VarSet getVarsWithTransitiveRelations (VarSet vars) throws ApronException {
    // msg("calculating VarsWithTransitiveRelations " + vars);
    Lincons1[] allConstraints = state.toLincons(state.getCreationManager());
    VarSet transitiveVars = VarSet.empty();
    for (NumVar variable : vars) {
      transitiveVars = transitiveVars.add(variable);
      final String apronVariable = variablesMapping.get(variable).get();
      for (Lincons1 constraint : allConstraints) {
        if (!constraint.getCoeff(apronVariable).isZero())
          for (Linterm1 term : constraint.getLinterms()) {
            transitiveVars = transitiveVars.add(variablesMapping.getKey(term.getVariable()).get());
          }
      }
    }
    // msg("returns " + transitiveVars);
    return transitiveVars;
  }

  @Override public Apron introduce (NumVar variable, Type type, Option<BigInt> value) {
    if (variablesMapping.contains(variable))
      System.out.println();
    assert !variablesMapping.contains(variable);
    String apronVar = variable.getName();
    BiMap<NumVar, String> newVariablesMapping = variablesMapping.bind(variable, apronVar);
    Environment newEnv = state.getEnvironment().add(new String[] {apronVar}, null);
    try {
      Abstract1 newState = state.changeEnvironmentCopy(state.getCreationManager(), newEnv, false);
      assert newState.getEnvironment().hasVar(apronVar);
      if (value.isSome()) {
        Coeff apronValue = toApronScalar(value.get());
        Texpr1Intern assign = new Texpr1Intern(newEnv, new Texpr1CstNode(apronValue));
        newState.assign(newState.getCreationManager(), apronVar, assign, null); // destructive update
        newState.assign(state.getCreationManager(), apronVar, assign, null); // destructive update
      } else {
        switch (type) {
        case Bool: { // [0, 1]
          Linexpr1 lowerLinear = toApronLinear(linear(variable), newState.getEnvironment(), newVariablesMapping);
          Lincons1 lowerConstraints = new Lincons1(Lincons1.SUPEQ, lowerLinear); // var >= 0
          newState.meet(state.getCreationManager(), lowerConstraints); // destructive update
          Linear upperLinearRaw = linear(BigInt.MINUSONE, variable).add(BigInt.ONE);
          Linexpr1 upperLinear = toApronLinear(upperLinearRaw, newState.getEnvironment(), newVariablesMapping);
          Lincons1 upperConstraints = new Lincons1(Lincons1.SUPEQ, upperLinear); // var <= 1
          newState.meet(state.getCreationManager(), upperConstraints); // destructive update
          break;
        }
        case Address: {// [0, +oo]
          Linexpr1 linear = toApronLinear(linear(variable), newState.getEnvironment(), newVariablesMapping);
          Lincons1 constraints = new Lincons1(Lincons1.SUPEQ, linear); // var >= 0
          newState.meet(state.getCreationManager(), constraints); // destructive update
          break;
        }
        case Zeno: // [-oo, +oo]
        default: // no need to assign here anything as var is introduced by default with TOP. Makes things faster.
          break;
        }
      }
      return build(newState, newVariablesMapping);
    } catch (ApronException e) {
      throw new UncheckedExceptionWrapper(e);
    }
  }

  private Apron introduceWithTop (VarSet vars) {
    BiMap<NumVar, String> newVariablesMapping = variablesMapping;
    List<String> apronVars = new LinkedList<String>();
    for (NumVar var : vars) {
      assert !variablesMapping.contains(var);
      String apronVar = var.getName();
      newVariablesMapping = newVariablesMapping.bind(var, apronVar);
      apronVars.add(apronVar);
    }
    Environment newEnv = state.getEnvironment().add(apronVars.toArray(new String[apronVars.size()]), null);
    try {
      Abstract1 newState = state.changeEnvironmentCopy(state.getCreationManager(), newEnv, false);
      return build(newState, newVariablesMapping);
    } catch (ApronException e) {
      throw new UncheckedExceptionWrapper(e);
    }
  }

  /**
   * Projects out the given variables. If {@code removeFromSupportSet} is set then the variables are also removed from
   * the support set. Sometimes it is useful for operations that require a compatible support set to project out the
   * variables by setting them to TOP but to not remove them from the support set.
   */
  private Apron project (VarSet vars, boolean removeFromSupportSet) {
    List<String> apronVars = new LinkedList<String>();
    BiMap<NumVar, String> newVariablesMapping = variablesMapping;
    for (NumVar variable : vars) {
      // some domains like the old Affine domain projects out variables even if they are not part of the child
      if (!variablesMapping.contains(variable))
        continue;
      String apronVar = variablesMapping.get(variable).get();
      if (removeFromSupportSet)
        newVariablesMapping = newVariablesMapping.remove(variable);
      assert state.getEnvironment().hasVar(apronVar);
      apronVars.add(apronVar);
    }
    try {
      String[] apronVarsArray = apronVars.toArray(new String[apronVars.size()]);
      if (removeFromSupportSet) {
        Environment newEnv = state.getEnvironment().remove(apronVarsArray);
        Abstract1 newState = state.changeEnvironmentCopy(state.getCreationManager(), newEnv, false);
        return build(newState, newVariablesMapping);
      } else {
        Abstract1 newState = state.forgetCopy(state.getCreationManager(), apronVarsArray, false);
        return build(newState, newVariablesMapping);
      }
    } catch (ApronException e) {
      throw new UncheckedExceptionWrapper(e);
    }
  }

  @Override public Apron project (VarSet vars) {
    return project(vars, true);
  }

  private Apron substitute (List<P2<NumVar, NumVar>> substitutions) {
    List<String> apronVarsFrom = new LinkedList<String>();
    List<String> apronVarsTo = new LinkedList<String>();
    BiMap<NumVar, String> newVariablesMapping = variablesMapping;
    for (P2<NumVar, NumVar> pair : substitutions) {
      NumVar x = pair._1();
      NumVar y = pair._2();
      assert newVariablesMapping.contains(x);
      assert !newVariablesMapping.contains(y);
      String apronVarX = newVariablesMapping.get(x).get();
      String apronVarY = y.getName();
      newVariablesMapping = newVariablesMapping.remove(x).bind(y, apronVarY);
      assert state.getEnvironment().hasVar(apronVarX);
      assert !state.getEnvironment().hasVar(apronVarY);
      apronVarsFrom.add(apronVarX);
      apronVarsTo.add(apronVarY);
    }
    try {
      int size = apronVarsFrom.size();
      Abstract1 newState =
        state.renameCopy(state.getCreationManager(), apronVarsFrom.toArray(new String[size]),
            apronVarsTo.toArray(new String[size]));
      return build(newState, newVariablesMapping);
    } catch (ApronException e) {
      throw new UncheckedExceptionWrapper(e);
    }
  }

  @Override public Apron substitute (NumVar x, NumVar y) {
    return substitute(Collections.singletonList(P2.tuple2(x, y)));
  }

  @Override public void toString (DomainStringBuilder builder) {
    builder.append(NAME, toString());
  }

  /**
   * Prints the values for each variable in the domain. Override to implement non default printing.
   */
  protected String printDomainValues () {
    return state.toString();
  }

  @Override public String toString () {
    String domainValues = PrettyPrinters.asIntervals(state, variablesMapping, true) + "\n" + printDomainValues();
    return StringHelpers.indentMultiline(NAME + ": #" + variablesMapping.size() + " ", domainValues);
  }
}
