package bindead.abstractsyntax.finite.util;

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
import bindead.data.VarSet;

/**
 * Retrieves all the variables that are used in a finite expression.
 *
 * @author Bogdan Mihaila
 */
public class VarExtractor {
  private static Collector collector = new Collector();

  public static VarSet get (Assign stmt) {
    VarSet data = VarSet.empty();
    data = data.add(stmt.getLhs().getId());
    data = stmt.getRhs().accept(collector, data);
    return data;
  }

  public static VarSet get (Test stmt) {
    return stmt.getVars();
  }

  public static VarSet get (Lhs expr) {
    VarSet data = VarSet.empty();
    return data.add(expr.getId());
  }

  public static VarSet get (Rhs expr) {
    return expr.accept(collector, VarSet.empty());
  }

  public static class Collector implements FiniteExprVisitor<VarSet, VarSet> {

    @Override public VarSet visit (Bin expr, VarSet data) {
      return expr.getLeft().accept(this, expr.getRight().accept(this, data));
    }

    @Override public VarSet visit (Cmp expr, VarSet data) {
      return expr.getLeft().accept(this, expr.getRight().accept(this, data));
    }


    @Override public VarSet visit (SignExtend expr, VarSet data) {
      return expr.getExpr().accept(this, data);
    }

    @Override public VarSet visit (Convert expr, VarSet data) {
      return expr.getExpr().accept(this, data);
    }

    @Override public VarSet visit (Rlin expr, VarSet data) {
      return data.union(expr.getLinearTerm().getVars());
    }

    @Override public VarSet visit (FiniteRangeRhs expr, VarSet data) {
      return data;
    }

  }

}
