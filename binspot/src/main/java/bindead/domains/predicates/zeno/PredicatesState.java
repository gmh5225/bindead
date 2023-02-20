package bindead.domains.predicates.zeno;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javalx.data.products.P2;
import javalx.persistentcollections.AVLSet;
import javalx.persistentcollections.MultiMap;
import javalx.persistentcollections.ThreeWaySplit;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.util.ZenoTestHelper;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.debug.PrettyDomain;
import bindead.debug.StringHelpers;
import bindead.debug.XmlPrintHelpers;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.domains.affine.Substitution;

import com.jamesmurty.utils.XMLBuilder;

/**
 * The state of the Predicates Domain. It maps predicates (tests) to other predicates,
 * as implications. That is, one predicate implies the other predicate.
 *
 * @author Bogdan Mihaila
 */
class PredicatesState extends FunctorState {
  private final MultiMap<Test, Test> forward;
  private final MultiMap<Test, Test> backward;
  private final MultiMap<NumVar, Test> occurences;
  public static final PredicatesState EMPTY = new PredicatesState();
  // due to transformations predicates can become constant, mostly for the rhs of an implication there is no need to track them
  // FIXME: think we need to distinguish between rhs and lhs tautologies, but that is not yet needed
  private static final boolean removePredicateTautologies = true;
  // due to transformations implications can become tautologous (e.g. a -> a). No need to track such tautologies.
  private static final boolean removeImplicationTautologies = true;

  private PredicatesState (MultiMap<Test, Test> forward, MultiMap<Test, Test> backward,
      MultiMap<NumVar, Test> occurrences) {
    this.forward = forward;
    this.backward = backward;
    this.occurences = occurrences;
    assert isConsistent(); // quite expensive operation so would be wise to disable it when not needed anymore
  }

  private PredicatesState () {
    this(MultiMap.<Test, Test>empty(), MultiMap.<Test, Test>empty(), MultiMap.<NumVar, Test>empty());
  }

  /**
   * Test that all the mappings and reverse mappings are consistent.
   * Use this in assertions to catch errors after state modifications through transfer functions.
   * As it is costly it should not be used other than in assertion code.
   */
  private boolean isConsistent () {
    // p -> p should not exist
    for (P2<Test, Test> tuple : forward) {
      Test lhs = tuple._1();
      Test rhs = tuple._2();
      if (!backward.get(rhs).contains(lhs))
        return false;
      for (NumVar variable : lhs.getVars()) {
        if (!occurences.get(variable).contains(lhs))
          return false;
      }
    }
    for (P2<Test, Test> tuple : backward) {
      Test rhs = tuple._1();
      Test lhs = tuple._2();
      if (!forward.get(lhs).contains(rhs))
        return false;
      for (NumVar variable : rhs.getVars()) {
        if (!occurences.get(variable).contains(rhs))
          return false;
      }
    }
    for (P2<NumVar, Test> tuple : occurences) {
      NumVar variable = tuple._1();
      Test test = tuple._2();
      if (!test.getVars().contains(variable))
        return false;
      if (!forward.contains(test) && !backward.contains(test))
        return false;
    }
    return true;
  }

  @Override public String toString () {
    return "#" + size() + " " + contentToString();
  }

  @Override public void toCompactString (String domainName, StringBuilder builder, PrettyDomain childDomain) {
    builder.append(domainName + ": " + toString());
    builder.append('\n');
  }

  /**
   * Returns the number of implications tracked by this state.
   */
  private int size () {
    // very slow to iterate over all implications but this should be only used for the debug printing
    int count = 0;
    Iterator<P2<Test, Test>> iterator = forward.iterator();
    while (iterator.hasNext()) {
      count = count + 1;
      iterator.next();
    }
    return count;
  }

  private String contentToString () {
    StringBuilder builder = new StringBuilder();
    builder.append("{");
    Set<Test> sorted = StringHelpers.sortLexically(forward.keys());
    Iterator<Test> iterator = sorted.iterator();
    while (iterator.hasNext()) {
      Test premise = iterator.next();
      AVLSet<Test> implications = forward.get(premise);
      for (Test implication : implications) {
        builder.append(premise);
        builder.append(" → ");
        builder.append(implication);
        builder.append(", ");
      }
      if (!iterator.hasNext())
        builder.setLength(builder.length() - 2); // remove last comma separator
    }
    builder.append("}");
    return builder.toString();
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    builder = builder.e(XmlPrintHelpers.sanitize(Predicates.NAME));
    for (P2<Test, Test> implication : forward) {
      builder = builder.e("Entry").a("type", "Implication");
      builder = builder.e("Premise");
      builder = implication._1().toXML(builder);
      builder = builder.up(); // Premise
      builder = builder.e("Consequence");
      builder = implication._2().toXML(builder);
      builder = builder.up(); // Consequence
      builder = builder.up(); // Entry
    }
    builder = builder.up();
    return builder;
  }

  public MutableState toMutable () {
    return new MutableState(forward, backward, occurences);
  }

  static class MutableState {
    private MultiMap<Test, Test> forward;
    private MultiMap<Test, Test> backward;
    private MultiMap<NumVar, Test> occurrences;

    private MutableState (MultiMap<Test, Test> forward, MultiMap<Test, Test> backward, MultiMap<NumVar, Test> occurrences) {
      this.forward = forward;
      this.backward = backward;
      this.occurrences = occurrences;
    }

    public PredicatesState toImmutable () {
      return new PredicatesState(forward, backward, occurrences);
    }

    @Override public String toString () {
      return toImmutable().toString();
    }

    public boolean contains (NumVar variable) {
      return occurrences.contains(variable);
    }

    public void addImplication (Test premise, Test consequence) {
      forward = forward.add(premise, consequence);
      backward = backward.add(consequence, premise);
      addOccurrencesFor(premise);
      addOccurrencesFor(consequence);
    }

    public Iterable<P2<Test, Test>> getImplications () {
      return forward;
    }

    public AVLSet<Test> getConsequences (Test predicate) {
      return forward.get(predicate);
    }

    public AVLSet<Test> getPremises (Test predicate) {
      return backward.get(predicate);
    }

    public Iterable<Test> getAllLhs () {
      return forward.keys();
    }

    public Iterable<Test> getAllLhsContaining (VarSet vars) {
      AVLSet<Test> lhs = AVLSet.empty();
      for (NumVar var : vars) {
        for (Test predicate : occurrences.get(var)) {
          if (isLhs(predicate))
            lhs = lhs.add(predicate);
        }
      }
      return lhs;
    }

    public Iterable<Test> getAllRhs () {
      return backward.keys();
    }

    public AVLSet<Test> getTestsContainingOnly (VarSet variables) {
      AVLSet<Test> result = AVLSet.empty();
      for (NumVar variable : variables) {
        result = result.union(occurrences.get(variable));
      }
      for (Test test : result) {
        if (!variables.containsAll(test.getVars()))
          result = result.remove(test);
      }
      return result;
    }

    public Iterable<Test> getAllRhsContaining (VarSet vars) {
      AVLSet<Test> rhs = AVLSet.empty();
      for (NumVar var : vars) {
        for (Test predicate : occurrences.get(var)) {
          if (isRhs(predicate))
            rhs = rhs.add(predicate);
        }
      }
      return rhs;
    }

    public void removePredicatesContaining (NumVar variable) {
      AVLSet<Test> occs = occurrences.get(variable);
      if (occs.isEmpty())
        return;
      for (Test test : occs) {
        removePredicate(test);
      }
    }

    public void removePredicatesNotContaining (VarSet variables) {
      AVLSet<Test> toBeKept = getTestsContainingOnly(variables);
      MultiMap<Test, Test> newForward = MultiMap.empty();
      MultiMap<Test, Test> newBackward = MultiMap.empty();
      MultiMap<NumVar, Test> newOccurrences = MultiMap.empty();
      for (Test test : toBeKept) {
        if (isLhs(test)) {
          AVLSet<Test> oldValues = forward.get(test);
          AVLSet<Test> newValues = oldValues.intersection(toBeKept);
          newForward = newForward.add(test, newValues);
        }
        if (isRhs(test)) {
          AVLSet<Test> oldValues = backward.get(test);
          AVLSet<Test> newValues = oldValues.intersection(toBeKept);
          newBackward = newBackward.add(test, newValues);
        }
        if (newForward.contains(test) || newBackward.contains(test)) {
          // the test might not be kept if its premise or implication does not contain any of the variables
          for (NumVar variable : test.getVars()) {
            newOccurrences = newOccurrences.add(variable, test);
          }
        }
      }
      forward = newForward;
      backward = newBackward;
      occurrences = newOccurrences;
    }

    private void removePredicate (Test predicate) {
      removeOccurencesOf(predicate);
      if (isLhs(predicate)) {
        for (Test rhs : forward.get(predicate)) {
          backward = backward.remove(rhs, predicate);
          if (!forward.contains(rhs) && !backward.contains(rhs))
            removeOccurencesOf(rhs);
        }
        forward = forward.remove(predicate);
      }
      if (isRhs(predicate)) {
        for (Test lhs : backward.get(predicate)) {
          forward = forward.remove(lhs, predicate);
          if (!forward.contains(lhs) && !backward.contains(lhs))
            removeOccurencesOf(lhs);
        }
        backward = backward.remove(predicate);
      }
    }

    public void substituteInPredicates (Substitution substitution) {
      NumVar substitutedVar = substitution.getVar();
      if (substitution.isSimple() && substitutedVar.equalTo(substitution.getExpr().getSingleVarOrNull()))
        return; // substituting x with x is avoided and triggers a bug in the worklist algorithm below
      AVLSet<Test> originalPredicates = occurrences.get(substitutedVar);
      if (originalPredicates.isEmpty())
        return;

      // performing the substitutions on a temporary collection is a fix to a possible bug
      // when having a clash of predicates during the substitution
      // An example: original predicates to be replaced are {x<0, x<1} with substitution [x/x-1]
      // then it depends on the order the substitutions are processed. If the first one replaced is x<0
      // then it will become x<1 and thus the same as the second one. We get a clash and lose one of the predicates.
      List<P2<Test, Test>> originalImplications = toImplicationTuples(originalPredicates);
      List<P2<Test, Test>> substitutedImplications = new LinkedList<P2<Test, Test>>();
      removePredicatesContaining(substitutedVar);
      for (P2<Test, Test> implication : originalImplications) {
        Test originalPremise = implication._1();
        Test substitutedPremise = originalPremise.applySubstitution(substitution);
        Test originalConsequence = implication._2();
        Test substitutedConsequence = originalConsequence.applySubstitution(substitution);
        if (removeImplicationTautologies && substitutedPremise.equals(substitutedConsequence))
          continue;
        if (removePredicateTautologies && ZenoTestHelper.isTautology(substitutedPremise)
          || ZenoTestHelper.isTautology(substitutedConsequence))
          continue;
        substitutedImplications.add(P2.tuple2(substitutedPremise, substitutedConsequence));
      }
      for (P2<Test, Test> implication : substitutedImplications) {
        addImplication(implication._1(), implication._2());
      }
    }

    private List<P2<Test, Test>> toImplicationTuples (AVLSet<Test> predicates) {
      List<P2<Test, Test>> implications = new LinkedList<P2<Test, Test>>();
      for (Test predicate : predicates) {
        if (isLhs(predicate)) {
          for (Test rhs : forward.get(predicate)) {
            implications.add(P2.tuple2(predicate, rhs));
          }
        }
        if (isRhs(predicate)) {
          for (Test lhs : backward.get(predicate)) {
            implications.add(P2.tuple2(lhs, predicate));
          }
        }
      }
      return implications;
    }

    private boolean isLhs (Test predicate) {
      return forward.contains(predicate);
    }

    private boolean isRhs (Test predicate) {
      return backward.contains(predicate);
    }

    private void addOccurrencesFor (Test predicate) {
      for (NumVar variable : predicate.getVars()) {
        occurrences = occurrences.add(variable, predicate);
      }
    }

    private void removeOccurencesOf (Test predicate) {
      for (NumVar variable : predicate.getVars()) {
        occurrences = occurrences.remove(variable, predicate);
      }
    }

    public void union (MutableState otherState) {
      forward = forward.union(otherState.forward);
      backward = backward.union(otherState.backward);
      occurrences = occurrences.union(otherState.occurrences);
    }

    public void intersect (MutableState otherState) {
      forward = forward.intersection(otherState.forward);
      backward = backward.intersection(otherState.backward);
      // need to rebuild the occurrences map as intersecting it is not right
      occurrences = MultiMap.empty();
      for (P2<Test, Test> implication : forward) {
        addOccurrencesFor(implication._1());
        addOccurrencesFor(implication._2());
      }
    }

    public static <D extends ZenoDomain<D>> boolean isEntailed (MutableState otherState, MutableState inState,
        D inChildState) {
      // 3-way split does a simple entailment check where equality of predicates is the only syntactic entailment used
      ThreeWaySplit<MultiMap<Test, Test>> split = otherState.forward.splitWithDifference(inState.forward);
      // if all the implications in state exist in the other state, then the state is entailed by the other state
      if (split.onlyInFirst().isEmpty() && split.inBothButDiffering().isEmpty())
        return true;
      // if not collect the ones not entailed by the split and do some more entailment checks on them
      // 1. perform a more sophisticated syntactic entailment for each left over implication
      // TODO: see how to perform the syntactic entailment in a scalable way.
      // 2. perform the semantic entailment for each left over implication
      Iterable<P2<Test, Test>> leftOverImplications = split.onlyInFirst().union(split.inBothButDiffering());
      return Entailment.areAllImplicationsSemanticallyEntailed(leftOverImplications, inChildState);
    }

    public static <D extends ZenoDomain<D>> MutableState getEntailed (MutableState otherState, MutableState inState,
        D inChildState) {
      // 3-way split does a simple entailment check where equality of predicates is the only syntactic entailment used
      ThreeWaySplit<MultiMap<Test, Test>> split = otherState.forward.splitWithDifference(inState.forward);
      // if all the implications in state exist in the other state, then the state is entailed by the other state
      if (split.onlyInFirst().isEmpty() && split.inBothButDiffering().isEmpty())
        return otherState;
      // if not collect the ones not entailed by the split and do some more entailment checks on them
      // 1. perform a more sophisticated syntactic entailment for each left over implication
      // TODO: see how to perform the syntactic entailment in a scalable way.
      // 2. perform the semantic entailment for each left over implication
      MultiMap<Test, Test> leftOverImplications = split.onlyInFirst().union(split.inBothButDiffering());
      MultiMap<Test, Test> syntacticallyEntailed = otherState.forward.difference(leftOverImplications);
      MultiMap<Test, Test> semanticallyEntailed =
        Entailment.getAllSemanticallyEntailedImplications(leftOverImplications, inChildState);
      MultiMap<Test, Test> entailed = MultiMap.empty();
      entailed = entailed.union(syntacticallyEntailed);
      entailed = entailed.union(semanticallyEntailed);
      MutableState result = PredicatesState.EMPTY.toMutable();
      for (P2<Test, Test> implication : entailed) {
        Test lhs = implication._1();
        Test rhs = implication._2();
        result.addImplication(lhs, rhs);
      }
      return result;
    }

  }
}
