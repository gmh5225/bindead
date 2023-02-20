package bindead.domains.phased;

import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.exceptions.UnimplementedException;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.AVLSet;
import rreil.lang.MemVar;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.util.ZenoTestHelper;
import bindead.data.NumVar;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.interfaces.AnalysisCtx;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.domains.decisiontree.DecisionTree;
import bindead.exceptions.Unreachable;

/**
 * A domain that creates a tree of child domains by observing unsatisfiable tests.
 *
 * @author Axel Simon
 */
public class Phased<D extends ZenoDomain<D>> extends DecisionTree<D> {
  public static final String NAME = "PHASED";

  public Phased (D child) {
    this(Option.<D>some(child), DecisionTree.<D>noChild(), null);
  }

  private Phased (Option<D> state, AVLMap<Test, DecisionTree<D>> children, AnalysisCtx ctx) {
    super(NAME, state, children, ctx);
  }

  @Override protected Phased<D> build (Option<D> state, AVLMap<Test, DecisionTree<D>> children) {
    return new Phased<D>(state, children, getContext());
  }

  @Override public Phased<D> setContext (AnalysisCtx ctx) {
    return new Phased<D>(state, children, ctx);
  }

  @Override public DecisionTree<D> eval (final Test test) throws Unreachable {
    DecisionTree<D> result = accept(new UnaryVisitor<D>() {
      @Override public Option<D> visit (D state) {
        try {
          return Option.<D>some(state.eval(test));
        } catch (Unreachable _) {
          return Option.<D>none();
        }
      }

      @Override public AVLSet<Test> newTests (D state) {
        if (ZenoTestHelper.isTautology(test))
          return AVLSet.empty();
        AVLSet<Test> unsatisfiable = AVLSet.empty();
        AVLSet<Test> candidateTests = AVLSet.empty();
        if (test.getOperator() == ZenoTestOp.NotEqualToZero || test.getOperator() == ZenoTestOp.EqualToZero) {
          P2<Test, Test> split = test.splitEquality();
          Test negatedTest1 = split._1().not();
          Test negatedTest2 = split._2().not();
          candidateTests = candidateTests.add(negatedTest1);
          candidateTests = candidateTests.add(negatedTest2);
        } else {
          Test negatedTest = test.not();
          candidateTests = candidateTests.add(negatedTest);
        }
        for (Test test : candidateTests) {
          if (isUnsatisfiable(test, state))
            unsatisfiable = unsatisfiable.add(test);
        }
        return unsatisfiable;
      }

      private boolean isUnsatisfiable (Test test, D state) {
        try {
          state.eval(test);
        } catch (Unreachable _) {
          return true;
        }
        return false;
      }
    });

    if (!result.isReachable())
      throw new Unreachable();
    return result;
  }

  @Override public SetOfEquations queryEqualities (NumVar variable) {
    return SetOfEquations.empty();
  }

  @Override public void varToCompactString (StringBuilder builder, NumVar var) {
    assert false : "implement in PrettyDomain";
  }

  @Override public void toCompactString (StringBuilder builder) {
    assert false : "implement in PrettyDomain";
  }

  @Override public List<DecisionTree<D>> enumerateAlternatives () {
    // TODO implement in SemiLattice<DecisionTree<D>>
    throw new UnimplementedException();

  }

  @Override public void memVarToCompactString (StringBuilder builder, MemVar var) {
    // TODO implement in PrettyDomain
    throw new UnimplementedException();

  }
}
