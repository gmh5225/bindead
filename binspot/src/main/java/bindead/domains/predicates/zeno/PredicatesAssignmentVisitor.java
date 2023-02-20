package bindead.domains.predicates.zeno;

import javalx.numeric.Bound;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Bin;
import bindead.abstractsyntax.zeno.Zeno.Lhs;
import bindead.abstractsyntax.zeno.Zeno.RangeRhs;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.ZenoVisitor;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.domains.affine.Substitution;

/**
 * Execute the side-effects of an assignment on the Predicates domain.
 *
 * @author Bogdan Mihaila
 */
class PredicatesAssignmentVisitor {

  public static void run (Assign assign, PredicatesStateBuilder builder) {
    // * remove predicates when overwriting a variable with a binop or a range value
    // * transform predicates on linear transformations of a variable
    Visitor visitor = new Visitor(assign.getLhs().getId());
    assign.getRhs().accept(visitor, builder);
  }

  private static class Visitor implements ZenoVisitor<Void, PredicatesStateBuilder> {
    private final NumVar lhsVariable;

    public Visitor (NumVar lhsVariable) {
      this.lhsVariable = lhsVariable;
    }

    @Override public Void visit (Assign stmt, PredicatesStateBuilder builder) {
      throw new IllegalStateException();
    }

    @Override public Void visit (Lhs variable, PredicatesStateBuilder builder) {
      throw new IllegalStateException();
    }

    @Override public Void visit (Bin expr, PredicatesStateBuilder builder) {
      builder.project(lhsVariable);
      return null;
    }

    @Override public Void visit (RangeRhs range, PredicatesStateBuilder builder) {
      builder.project(lhsVariable);
      return null;
    }

    @Override public Void visit (Rlin lin, PredicatesStateBuilder builder) {
      if (!builder.contains(lhsVariable))
        return null;
      if (!lin.getDivisor().isOne()) {
        builder.project(lhsVariable);
        return null;
      }
      Linear rhs = lin.getLinearTerm();
      if (rhs.contains(lhsVariable)) { // invertible assignment
        Substitution substitution = Substitution.invertingSubstitution(rhs, Bound.ONE, lhsVariable);
        builder.substitute(substitution);
      } else {
        builder.project(lhsVariable);
      }
      return null;
    }

  }
}
