package bindead.domains.predicates.finite;

import static bindead.domains.predicates.finite.PredicatesState.Flag.asFalse;
import static bindead.domains.predicates.finite.PredicatesState.Flag.asTrue;
import static bindead.domains.predicates.finite.PredicatesState.Flag.toFlag;
import javalx.data.Option;
import javalx.data.products.P2;
import javalx.fn.Fn2;
import javalx.fn.Predicate;
import javalx.mutablecollections.CollectionHelpers;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.AVLSet;
import javalx.persistentcollections.ThreeWaySplit;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.debug.PrettyDomain;
import bindead.debug.XmlPrintHelpers;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.domains.affine.Substitution;

import com.jamesmurty.utils.XMLBuilder;

/**
 * The state for the predicates domain. It associates two sets of predicates with a flag.
 * A set of predicates that are implied by the flag and a set of predicates that are implied by the negation of the flag.
 *
 * @author Bogdan Mihaila
 */
class PredicatesState extends FunctorState {
  public static final PredicatesState EMPTY = new PredicatesState();
  private final AVLMap<Flag, AVLSet<Test>> predicates;
  private final AVLMap<NumVar, AVLSet<Flag>> varOccurrences;

  // f → {pred} (conjunction of predicates) and reverse mappings /* later maybe f → {{pred}} for CNF of predicates
  // invariant: f ∉ vars(pred) /* separate variable sets for flags and predicates
  // the preds for f and ¬f are treated as separate sets because there is mostly no relationship between them
  // especially with the synthesized predicates from the child domains one set is _not_ the negation of the other

  private PredicatesState (AVLMap<Flag, AVLSet<Test>> predicates, AVLMap<NumVar, AVLSet<Flag>> varOccurrences) {
    this.predicates = predicates;
    this.varOccurrences = varOccurrences;
    assert isConsistent(); // quite expensive operation so would be wise to disable it when not needed anymore
  }

  private PredicatesState () {
    this(AVLMap.<Flag, AVLSet<Test>>empty(), AVLMap.<NumVar, AVLSet<Flag>>empty());
  }

  private boolean isConsistent () {
    // check if the flags and predicates vars are separate sets
    VarSet flagVars = VarSet.empty();
    VarSet predicateVars = VarSet.empty();
    for (P2<Flag, AVLSet<Test>> element : predicates) {
      Flag flag = element._1();
      flagVars = flagVars.add(flag.flagVariable);
      AVLSet<Test> flagPredicates = element._2();
      predicateVars = predicateVars.union(getVariablesInPredicates(flagPredicates));
    }
    if (flagVars.containsAny(predicateVars))
      return false;
    // check that f and ¬f do not contain the same predicate
    for (P2<Flag, AVLSet<Test>> element : predicates) {
      Flag flag = element._1();
      AVLSet<Test> flagPredicates = element._2();
      AVLSet<Test> negatedFlagPredicates = predicates.get(flag.negate()).getOrElse(AVLSet.<Test>empty());
      if (!flagPredicates.intersection(negatedFlagPredicates).isEmpty())
        return false;
    }
    // check the reverse mappings of the flags
    for (P2<Flag, AVLSet<Test>> element : predicates) {
      Flag flag = element._1();
      for (NumVar var : getVariablesInPredicates(element._2())) {
        AVLSet<Flag> occurences = varOccurrences.get(var).getOrElse(AVLSet.<Flag>empty());
        if (!occurences.contains(flag))
          return false;
      }
    }
    // check the reverse mapping of the occurring variables
    for (P2<NumVar, AVLSet<Flag>> element : varOccurrences) {
      NumVar var = element._1();
      for (Flag flag : element._2()) {
        Option<AVLSet<Test>> predsOption = predicates.get(flag);
        if (predsOption.isNone())
          return false;
        if (!getVariablesInPredicates(predsOption.get()).contains(var))
          return false;
      }
    }
    return true;
  }

  public MutableState toMutable () {
    return new MutableState(predicates, varOccurrences);
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    builder = builder.e(XmlPrintHelpers.sanitize(Predicates.NAME));
    for (P2<Flag, AVLSet<Test>> implications : predicates) {
      Test flagTest = implications._1().asTest();
      for (Test consequence : implications._2()) {
        builder = builder.e("Entry").a("type", "Implication");
        builder = builder.e("Premise");
        builder = flagTest.toXML(builder);
        builder = builder.up(); // Premise
        builder = builder.e("Consequence");
        builder = consequence.toXML(builder);
        builder = builder.up(); // Consequence
        builder = builder.up(); // Entry
      }
    }
    builder = builder.up();
    return builder;
  }

  @Override public void toCompactString (String domainName, StringBuilder builder, PrettyDomain childDomain) {
    builder.append(domainName + ": " + toString());
    builder.append('\n');
  }

  /**
   * Split the map containing a var x value tuple as the key into two maps where the first map
   * contains the true valuation for the variable and the second map the false valuation.
   */
  private P2<AVLMap<NumVar, AVLSet<Test>>, AVLMap<NumVar, AVLSet<Test>>> splitFlagsMap () {
    AVLMap<NumVar, AVLSet<Test>> truePredicates = AVLMap.empty();
    AVLMap<NumVar, AVLSet<Test>> falsePredicates = AVLMap.empty();
    for (P2<Flag, AVLSet<Test>> element : predicates) {
      Flag flag = element._1();
      if (flag.isTrue())
        truePredicates = truePredicates.bind(flag.flagVariable, element._2());
      else
        falsePredicates = falsePredicates.bind(flag.flagVariable, element._2());
    }
    return P2.tuple2(truePredicates, falsePredicates);
  }

  @Override public String toString () {
    return "#" + predicates.size() + " " + contentToString();
  }

  /**
   * Prints first all the flags that have a predicates set for their true evaluation and false evaluation.
   * Then the ones that only are true and at last the ones that are only false.
   */
  private String contentToString () {
    P2<AVLMap<NumVar, AVLSet<Test>>, AVLMap<NumVar, AVLSet<Test>>> splitup = splitFlagsMap();
    AVLMap<NumVar, AVLSet<Test>> truePredicates = splitup._1();
    AVLMap<NumVar, AVLSet<Test>> falsePredicates = splitup._2();
    ThreeWaySplit<AVLMap<NumVar, AVLSet<Test>>> threeWaySplitup = truePredicates.split(falsePredicates);
    StringBuilder builder = new StringBuilder();
    builder.append("{");
    for (P2<NumVar, AVLSet<Test>> element : threeWaySplitup.inBothButDiffering()) {
      NumVar flag = element._1();
      AVLSet<Test> trueSet = element._2();
      AVLSet<Test> falseSet = falsePredicates.getOrNull(flag);
      builder.append(flag);
      builder.append("→");
      builder.append(trueSet);
      builder.append(" ¬");
      builder.append(flag);
      builder.append("→");
      builder.append(falseSet);
      builder.append(", ");
    }
    for (P2<NumVar, AVLSet<Test>> element : threeWaySplitup.onlyInFirst()) {
      NumVar flag = element._1();
      AVLSet<Test> trueSet = element._2();
      builder.append(flag);
      builder.append("→");
      builder.append(trueSet);
      builder.append(", ");
    }
    for (P2<NumVar, AVLSet<Test>> element : threeWaySplitup.onlyInSecond()) {
      NumVar flag = element._1();
      AVLSet<Test> falseSet = element._2();
      builder.append("¬");
      builder.append(flag);
      builder.append("→");
      builder.append(falseSet);
      builder.append(", ");
    }
    if (builder.length() > 2)
      builder.setLength(builder.length() - 2);
    builder.append("}");
    return builder.toString();
  }

  static VarSet getVariablesInPredicates (AVLSet<Test> preds) {
    VarSet vars = VarSet.empty();
    for (Test pred : preds) {
      vars = vars.union(pred.getVars());
    }
    return vars;
  }

  protected static class Flag implements Comparable<Flag> {
    private final NumVar flagVariable;
    private final boolean flagValuation;

    private Flag (NumVar flagVariable, boolean flagValuation) {
      assert flagVariable != null;
      this.flagVariable = flagVariable;
      this.flagValuation = flagValuation;
    }

    public static Flag toFlag (NumVar variable, boolean flagValuation) {
      return new Flag(variable, flagValuation);
    }

    public static Flag asTrue (NumVar variable) {
      return new Flag(variable, true);
    }

    public static Flag asFalse (NumVar variable) {
      return new Flag(variable, false);
    }

    public Flag negate () {
      return new Flag(flagVariable, !flagValuation);
    }

    public boolean isTrue () {
      return flagValuation;
    }

    public NumVar getVariable () {
      return flagVariable;
    }

    public Test asTest () {
      FiniteFactory finite = FiniteFactory.getInstance();
      if (isTrue()) {
        return finite.equalToOne(1, flagVariable);
      } else {
        return finite.equalToZero(1, flagVariable);
      }
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = 1;
      result = prime * result + (flagValuation ? 1231 : 1237);
      result = prime * result + (flagVariable == null ? 0 : flagVariable.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof Flag))
        return false;
      Flag other = (Flag) obj;
      if (flagValuation != other.flagValuation)
        return false;
      if (flagVariable == null) {
        if (other.flagVariable != null)
          return false;
      } else if (!flagVariable.equalTo(other.flagVariable))
        return false;
      return true;
    }

    @Override public int compareTo (Flag other) {
      int result = this.flagVariable.compareTo(other.flagVariable);
      if (result == 0)
        // result = Boolean.compare(this.flagValuation, other.flagValuation);
        result = new Boolean(flagValuation).compareTo(other.flagValuation);
      return result;
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      if (!isTrue())
        builder.append("¬");
      builder.append(flagVariable);
      return builder.toString();
    }
  }

  /**
   * Same state data as the parent class but exposes an API to modify the state.
   */
  static class MutableState {
    private AVLMap<Flag, AVLSet<Test>> predicates;
    private AVLMap<NumVar, AVLSet<Flag>> varOccurrences;

    public MutableState (AVLMap<Flag, AVLSet<Test>> predicates, AVLMap<NumVar, AVLSet<Flag>> varOccurrences) {
      this.predicates = predicates;
      this.varOccurrences = varOccurrences;
    }

    public PredicatesState toImmutable () {
      return new PredicatesState(predicates, varOccurrences);
    }

    @Override public String toString () {
      return toImmutable().toString();
    }

    public boolean isFlag (NumVar flag) {
      return predicates.contains(asTrue(flag)) || predicates.contains(asFalse(flag));
    }

    public boolean isPredicatesVar (NumVar var) {
      return varOccurrences.contains(var);
    }

    public VarSet getAllPredicateVars () {
      return VarSet.fromKeys(varOccurrences);
    }

    public VarSet getAllFlagVars () {
      VarSet flagVars = VarSet.empty();
      for (P2<Flag, AVLSet<Test>> element : predicates) {
        Flag flag = element._1();
        flagVars = flagVars.add(flag.flagVariable);
      }
      return flagVars;
    }

    /**
     * Return the flags that imply predicates that contain any of the passed in variables.
     */
    public AVLSet<Test> getPredicates (NumVar flag, boolean flagValuation) {
      return getPredicates(toFlag(flag, flagValuation));
    }

    /**
     * Return the flags that imply predicates that contain any of the passed in variables.
     */
    public AVLSet<Test> getPredicates (Flag flag) {
      return predicates.get(flag).getOrElse(AVLSet.<Test>empty());
    }

    /**
     * Return the flags that contain one of the variables.
     */
    public AVLSet<Flag> getFlagsFromVars (VarSet vars) {
      AVLSet<Flag> result = AVLSet.empty();
      for (NumVar variable : vars) {
        Flag trueFlag = asTrue(variable);
        if (predicates.contains(trueFlag))
          result = result.add(trueFlag);
        Flag falseFlag = asFalse(variable);
        if (predicates.contains(falseFlag))
          result = result.add(falseFlag);
      }
      return result;
    }

    public void addPredicate (NumVar flagVar, boolean flagValuation, Test predicate) {
      Flag flag = toFlag(flagVar, flagValuation);
      AVLSet<Test> newPreds = getPredicates(flag).add(predicate);
      for (NumVar var : predicate.getVars()) {
        addVariableOccurrenceInPredicatesOf(flag, var);
      }
      setPredicates(flag, newPreds);
    }

    public void addPredicates (Flag flag, AVLSet<Test> predicatesToAdd) {
      AVLSet<Test> newPreds = getPredicates(flag).union(predicatesToAdd);
      for (NumVar var : getVariablesInPredicates(predicatesToAdd)) {
        addVariableOccurrenceInPredicatesOf(flag, var);
      }
      setPredicates(flag, newPreds);
    }

    public void setPredicates (Flag flag, AVLSet<Test> newPredicates) {
      if (newPredicates.isEmpty()) {
        predicates = predicates.remove(flag);
      } else {
        predicates = predicates.bind(flag, newPredicates);
      }
    }

    /**
     * The predicates of x will be implied by y.
     */
    public void substituteFlag (NumVar x, NumVar y) {
      assert isFlag(x);
      Flag xTrue = asTrue(x);
      if (predicates.contains(xTrue)) {
        Flag yTrue = asTrue(y);
        for (NumVar var : getVariablesInPredicatesOf(xTrue)) {
          replaceVariableOccurrenceInPredicatesOf(xTrue, yTrue, var);
        }
        addPredicates(yTrue, getPredicates(xTrue));
        predicates = predicates.remove(xTrue);
      }
      Flag xFalse = asFalse(x);
      if (predicates.contains(xFalse)) {
        Flag yFalse = asFalse(y);
        for (NumVar var : getVariablesInPredicatesOf(xFalse)) {
          replaceVariableOccurrenceInPredicatesOf(xFalse, yFalse, var);
        }
        addPredicates(yFalse, getPredicates(xFalse));
        predicates = predicates.remove(xFalse);
      }
    }

    public void copyFlagPredicates (NumVar target, NumVar source) {
      assert isFlag(source);
      assert !source.equalTo(target);
      assert !isFlag(target);
      assert !isPredicatesVar(target);
      Flag sourceTrue = asTrue(source);
      Flag targetTrue = asTrue(target);
      Flag sourceFalse = asFalse(source);
      Flag targetFalse = asFalse(target);
      for (NumVar var : getVariablesInPredicatesOf(sourceTrue)) {
        addVariableOccurrenceInPredicatesOf(targetTrue, var);
      }
      for (NumVar var : getVariablesInPredicatesOf(sourceFalse)) {
        addVariableOccurrenceInPredicatesOf(targetFalse, var);
      }
      setPredicates(targetTrue, getPredicates(sourceTrue));
      setPredicates(targetFalse, getPredicates(sourceFalse));
    }

    public void copyFlagPredicatesNegated (NumVar target, NumVar source) {
      assert isFlag(source);
      assert !source.equalTo(target);
      assert !isFlag(target);
      assert !isPredicatesVar(target);
      Flag sourceTrue = asTrue(source);
      Flag targetTrue = asTrue(target);
      Flag sourceFalse = asFalse(source);
      Flag targetFalse = asFalse(target);
      for (NumVar var : getVariablesInPredicatesOf(sourceTrue)) {
        addVariableOccurrenceInPredicatesOf(targetFalse, var);
      }
      for (NumVar var : getVariablesInPredicatesOf(sourceFalse)) {
        addVariableOccurrenceInPredicatesOf(targetTrue, var);
      }
      setPredicates(targetFalse, getPredicates(sourceTrue));
      setPredicates(targetTrue, getPredicates(sourceFalse));
    }

    public void negateFlag (NumVar flagVar) {
      Flag trueFlag = asTrue(flagVar);
      Flag falseFlag = asFalse(flagVar);
      VarSet truePredicatesVars = getVariablesInPredicatesOf(trueFlag);
      VarSet falsePredicatesVars = getVariablesInPredicatesOf(falseFlag);
      for (NumVar var : truePredicatesVars) {
        removeVariableOccurrenceInPredicatesOf(trueFlag, var);
      }
      for (NumVar var : falsePredicatesVars) {
        removeVariableOccurrenceInPredicatesOf(falseFlag, var);
      }
      for (NumVar var : truePredicatesVars) {
        addVariableOccurrenceInPredicatesOf(falseFlag, var);
      }
      for (NumVar var : falsePredicatesVars) {
        addVariableOccurrenceInPredicatesOf(trueFlag, var);
      }
      AVLSet<Test> truePredicates = getPredicates(trueFlag);
      AVLSet<Test> falsePredicates = getPredicates(falseFlag);
      setPredicates(trueFlag, falsePredicates);
      setPredicates(falseFlag, truePredicates);
    }

    public VarSet getVariablesInPredicatesOf (NumVar flag) {
      return getVariablesInPredicates(getPredicates(asTrue(flag))).union(
          getVariablesInPredicates(getPredicates(asFalse(flag))));
    }

    private VarSet getVariablesInPredicatesOf (Flag flag) {
      return getVariablesInPredicates(getPredicates(flag));
    }

    public void addVariableOccurrenceInPredicatesOf (Flag flag, NumVar var) {
      AVLSet<Flag> occurrences = getOccurrencesOf(var);
      varOccurrences = varOccurrences.bind(var, occurrences.add(flag));
    }

    private void replaceVariableOccurrenceInPredicatesOf (Flag toReplace, Flag replacer, NumVar var) {
      AVLSet<Flag> occurrences = getOccurrencesOf(var);
      varOccurrences = varOccurrences.bind(var, occurrences.remove(toReplace).add(replacer));
    }

    public void removeVariableOccurrenceInPredicatesOf (Flag toRemove, NumVar var) {
      AVLSet<Flag> occurrences = getOccurrencesOf(var);
      AVLSet<Flag> newOccurrences = occurrences.remove(toRemove);
      if (newOccurrences.isEmpty())
        varOccurrences = varOccurrences.remove(var);
      else
        varOccurrences = varOccurrences.bind(var, newOccurrences);
    }

    public AVLSet<Flag> getOccurrencesOf (NumVar var) {
      return varOccurrences.get(var).getOrElse(AVLSet.<Flag>empty());
    }

    public void removeFlag (NumVar flagVar) {
      Flag trueFlag = asTrue(flagVar);
      Flag falseFlag = asFalse(flagVar);
      for (NumVar var : getVariablesInPredicatesOf(trueFlag)) {
        removeVariableOccurrenceInPredicatesOf(trueFlag, var);
      }
      for (NumVar var : getVariablesInPredicatesOf(falseFlag)) {
        removeVariableOccurrenceInPredicatesOf(falseFlag, var);
      }
      predicates = predicates.remove(trueFlag);
      predicates = predicates.remove(falseFlag);
    }

    public void removePredicatesContaining (final NumVar var) {
      for (Flag flag : getOccurrencesOf(var)) {
        AVLSet<Test> predicatesOfFlag = getPredicates(flag);
        AVLSet<Test> newPredicatesOfFlag = CollectionHelpers.filter(predicatesOfFlag, new Predicate<Test>() {
          @Override public Boolean apply (Test test) {
            return !test.getVars().contains(var);
          }
        });
        setPredicates(flag, newPredicatesOfFlag);
        AVLSet<Test> removedPredicates = predicatesOfFlag.difference(newPredicatesOfFlag);
        VarSet varsInNewPredicates = getVariablesInPredicates(newPredicatesOfFlag);
        for (NumVar variable : getVariablesInPredicates(removedPredicates)) {
          if (!varsInNewPredicates.contains(variable))
            removeVariableOccurrenceInPredicatesOf(flag, variable);
        }
      }
    }

    public AVLSet<Substitution> generateSubstitutionsFor (NumVar var, QueryChannel child) {
      AVLSet<Substitution> substitutions = AVLSet.empty();
      if (child == null)
        return AVLSet.empty();
      SetOfEquations equalities = child.queryEqualities(var);
      if (equalities.isEmpty())
        return AVLSet.empty();
      for (Linear equality : equalities) {
        substitutions = substitutions.add(equality.genSubstitution(var));
      }
      return substitutions;
    }

    public void union (MutableState other) {
      predicates = unionPredicates(this.predicates, other.predicates);
      Fn2<AVLSet<Flag>, AVLSet<Flag>, AVLSet<Flag>> selector2 = new Fn2<AVLSet<Flag>, AVLSet<Flag>, AVLSet<Flag>>() {
        @Override public AVLSet<Flag> apply (AVLSet<Flag> a, AVLSet<Flag> b) {
          return a.union(b);
        }
      };
      varOccurrences = varOccurrences.union(selector2, other.varOccurrences);
    }

    private static AVLMap<Flag, AVLSet<Test>> unionPredicates (AVLMap<Flag, AVLSet<Test>> first,
        AVLMap<Flag, AVLSet<Test>> second) {
      Fn2<AVLSet<Test>, AVLSet<Test>, AVLSet<Test>> selector = new Fn2<AVLSet<Test>, AVLSet<Test>, AVLSet<Test>>() {
        @Override public AVLSet<Test> apply (AVLSet<Test> a, AVLSet<Test> b) {
          return a.union(b);
        }
      };
      return first.union(selector, second);
    }

    private static AVLMap<Flag, AVLSet<Test>> differencePredicates (AVLMap<Flag, AVLSet<Test>> first,
        AVLMap<Flag, AVLSet<Test>> second) {
      ThreeWaySplit<AVLMap<Flag, AVLSet<Test>>> split = splitPredicates(first, second);
      return unionPredicates(split.onlyInFirst(), split.inBothButDiffering());
    }

    private static ThreeWaySplit<AVLMap<Flag, AVLSet<Test>>> splitPredicates (AVLMap<Flag, AVLSet<Test>> first,
        AVLMap<Flag, AVLSet<Test>> second) {
      ThreeWaySplit<AVLMap<Flag, AVLSet<Test>>> split = first.split(second);
      AVLMap<Flag, AVLSet<Test>> onlyInFirst = split.onlyInFirst();
      AVLMap<Flag, AVLSet<Test>> onlyInSecond = split.onlyInSecond();
      AVLMap<Flag, AVLSet<Test>> inBothButDiffering = AVLMap.empty();
      for (P2<Flag, AVLSet<Test>> tuple : split.inBothButDiffering()) {
        Flag key = tuple._1();
        AVLSet<Test> values = tuple._2().difference(second.getOrNull(key));
        inBothButDiffering = inBothButDiffering.bind(key, values);
      }
      return ThreeWaySplit.make(onlyInFirst, inBothButDiffering, onlyInSecond);
    }

    public void intersect (MutableState other) {
      ThreeWaySplit<AVLMap<Flag, AVLSet<Test>>> split = splitPredicates(predicates, other.predicates);
      for (Flag flag : split.onlyInFirst().keys()) { // rebuild reverse map
        VarSet varsToRemove = getVariablesInPredicatesOf(flag);
        for (NumVar var : varsToRemove) {
          removeVariableOccurrenceInPredicatesOf(flag, var);
        }
      }
      predicates = differencePredicates(predicates, split.onlyInFirst());
      for (P2<Flag, AVLSet<Test>> element : split.inBothButDiffering()) {
        Flag flag = element._1();
        AVLSet<Test> inThis = element._2();
        AVLSet<Test> inOther = other.predicates.get(flag).get();
        AVLSet<Test> common = inThis.intersection(inOther);
        setPredicates(flag, common);
        VarSet varsToRemove = getVariablesInPredicates(inThis.difference(common));
        for (NumVar var : varsToRemove) { // rebuild reverse map
          removeVariableOccurrenceInPredicatesOf(flag, var);
        }
      }
    }

    public static <D extends FiniteDomain<D>> boolean isEntailed (MutableState otherState, MutableState inState,
        D inChildState) {
      // 3-way split does a simple entailment check where equality of predicates is the only syntactic entailment used
      ThreeWaySplit<AVLMap<Flag, AVLSet<Test>>> split = splitPredicates(otherState.predicates, inState.predicates);
      // if all the implications in state exist in the other state, then the state is entailed by the other state
      if (split.onlyInFirst().isEmpty() && split.inBothButDiffering().isEmpty())
        return true;
      // if not collect the ones not entailed by the split and do some more entailment checks on them
      // 1. perform a more sophisticated syntactic entailment for each left over implication
      // TODO: see how to perform the syntactic entailment in a scalable way.
      // 2. perform the semantic entailment for each left over implication
      AVLMap<Flag, AVLSet<Test>> leftOverImplications = unionPredicates(split.onlyInFirst(), split.inBothButDiffering());
      return Entailment.areAllImplicationsSemanticallyEntailed(leftOverImplications, inChildState);
    }

    public static <D extends FiniteDomain<D>> MutableState getEntailed (MutableState otherState, MutableState inState,
        D inChildState) {
      // 3-way split does a simple entailment check where equality of predicates is the only syntactic entailment used
      ThreeWaySplit<AVLMap<Flag, AVLSet<Test>>> split = splitPredicates(otherState.predicates, inState.predicates);
      // if all the implications in state exist in the other state, then the state is entailed by the other state
      if (split.onlyInFirst().isEmpty() && split.inBothButDiffering().isEmpty())
        return otherState;
      // if not collect the ones not entailed by the split and do some more entailment checks on them
      // 1. syntactic entailment in Finite is difficult because of wrapping, so only tests that are equal are used
      // 2. perform the semantic entailment for each left over implication
      AVLMap<Flag, AVLSet<Test>> leftOverImplications = unionPredicates(split.onlyInFirst(), split.inBothButDiffering());
      AVLMap<Flag, AVLSet<Test>> syntacticallyEntailed =
        differencePredicates(otherState.predicates, leftOverImplications);
      AVLMap<Flag, AVLSet<Test>> semanticallyEntailed =
        Entailment.getAllSemanticallyEntailedImplications(leftOverImplications, inChildState);
      AVLMap<Flag, AVLSet<Test>> entailed = unionPredicates(syntacticallyEntailed, semanticallyEntailed);
      MutableState result = PredicatesState.EMPTY.toMutable();
      for (P2<Flag, AVLSet<Test>> tuple : entailed) {
        result.addPredicates(tuple._1(), tuple._2());
      }
      return result;
    }
  }
}
