package bindead.domains.predicates.finite;

import javalx.numeric.Bound;
import javalx.numeric.Interval;
import rreil.lang.BinOp;
import bindead.abstractsyntax.finite.Finite.Assign;
import bindead.abstractsyntax.finite.Finite.Bin;
import bindead.abstractsyntax.finite.Finite.Cmp;
import bindead.abstractsyntax.finite.Finite.Convert;
import bindead.abstractsyntax.finite.Finite.FiniteRangeRhs;
import bindead.abstractsyntax.finite.Finite.Lhs;
import bindead.abstractsyntax.finite.Finite.Rhs;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.Finite.SignExtend;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.abstractsyntax.finite.FiniteExprVisitor;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.domains.affine.Substitution;

/**
 * Transform the rhs expression of the statement and execute the side-effects on the builder.
 *
 * @author Bogdan Mihaila
 */
class PredicatesAssignmentVisitor {
  private static final FiniteFactory finite = FiniteFactory.getInstance();

  public static Assign run (Assign stmt, PredicatesStateBuilder builder) {
    Visitor vistor = new Visitor(stmt.getLhs());
    Rhs expr = stmt.getRhs().accept(vistor, builder);
    if (expr != stmt.getRhs())
      stmt = finite.assign(stmt.getLhs(), expr);
    return stmt;
  }

  static class Visitor implements FiniteExprVisitor<Rhs, PredicatesStateBuilder> {
    private static final FiniteRangeRhs TOP = finite.range(1, Interval.BOOLEANTOP);
    final Lhs lhs;

    public Visitor (Lhs lhs) {
      this.lhs = lhs;
    }

    @Override public Rhs visit (Cmp expr, PredicatesStateBuilder builder) {
      Test test = expr.asTest();
      builder.assignPredicateToFlag(lhs.getId(), test);
      return TOP; // test will be executed later on so we do not have a value for the rhs yet
    }

    @Override public Rhs visit (Bin expr, PredicatesStateBuilder builder) {
      switch (expr.getOp()) {
      case Add:
        return handleLinearArithmetic(expr, builder);
      case Sub:
        return handleLinearArithmetic(expr, builder);
      case Xor:
        return handleXor(expr, builder);
      default:
        builder.project(lhs.getId());
        return expr;
      }
    }

    private Rhs handleLinearArithmetic (Bin expr, PredicatesStateBuilder builder) {
      switch (expr.getOp()) {
      case Add:
        return expr.getLeft().add(expr.getRight()).accept(this, builder);
      case Sub:
        return expr.getLeft().sub(expr.getRight()).accept(this, builder);
      default:
        builder.project(lhs.getId());
        return expr;
      }
    }

    private Rhs handleXor (Bin expr, PredicatesStateBuilder builder) {
      Linear l = expr.getLeft().getLinearTerm();
      Linear r = expr.getRight().getLinearTerm();
      // check for semantic not; f1 := f0 xorb 1; f1 := 1 xorb f0;
      if (expr.getOp() == BinOp.Xor && lhs.getSize() == 1) {
        NumVar variable = null;
        if (l.isConstantOnly() && l.getConstant().isOne() && r.isSingleTerm()) {
          variable = r.getKey();
        } else if (r.isConstantOnly() && r.getConstant().isOne() && l.isSingleTerm()) {
          variable = l.getKey();
        }
        if (variable != null && builder.isFlag(variable)) {
          builder.assignNegatedCopy(lhs.getId(), variable);
          // XXX: add an equality (1 - exF = fTA) in the Affine?
          // How to do that? Would need to change the rhs of the assignment and execute that.
          return TOP; // tests will be executed later on so we do not have a value for the rhs yet
        } else {
          builder.project(lhs.getId());
          return expr;
        }
      }
      builder.project(lhs.getId());
      return expr;
    }

    @Override public Rhs visit (SignExtend expr, PredicatesStateBuilder builder) {
      builder.project(lhs.getId());
      return expr;
    }

    @Override public Rhs visit (Convert expr, PredicatesStateBuilder builder) {
      builder.project(lhs.getId());
      return expr;
    }

    @Override public Rhs visit (Rlin expr, PredicatesStateBuilder builder) {
      Linear rhs = expr.getLinearTerm();
      NumVar var = lhs.getId();
      if (builder.isPredicatesVar(var)) {
        if (rhs.contains(var)) { // invertible assignment
          Substitution substitution = Substitution.invertingSubstitution(rhs, Bound.ONE, var);
          builder.applyPredicatesSubstitution(substitution);
        } else {
          builder.project(var);
        }
      } else if (rhs.isSingleTerm()) {
        // check for assignments of the form f1 := a*f0 + c; with a=1 and c=0 and copy the associated predicates
        NumVar variable = rhs.getKey();
        if (builder.isFlag(variable) && rhs.getConstant().isZero() && rhs.getCoeff(variable).isOne())
          builder.assignCopy(var, variable);
        // by returning expr in this case instead of TOP we maintain the affine relation between f0 and f1
        else
          builder.project(var);
      } else {
        builder.project(var);
      }
      return expr;
    }

    @Override public Rhs visit (FiniteRangeRhs expr, PredicatesStateBuilder builder) {
      NumVar var = lhs.getId();
      builder.project(var);
      return expr;
    }
  }
}
