package bindead.domains.pointsto;

import javalx.numeric.BigInt;
import bindead.abstractsyntax.finite.Finite.Bin;
import bindead.abstractsyntax.finite.Finite.Cmp;
import bindead.abstractsyntax.finite.Finite.Convert;
import bindead.abstractsyntax.finite.Finite.FiniteRangeRhs;
import bindead.abstractsyntax.finite.Finite.Rhs;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.Finite.SignExtend;
import bindead.abstractsyntax.finite.FiniteExprVisitor;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.Linear;
import bindead.data.Linear.Term;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.domainnetwork.channels.WarningsContainer;
import bindead.domains.pointsto.PointsToSet.PointsToEntry;

class RhsTranslator implements
    FiniteExprVisitor<HyperPointsToSet, Void> {

  final private PointsToState state;

  final private WarningsContainer warnings;

  private final FiniteFactory fin = FiniteFactory.getInstance();

  final private boolean DEBUG = PointsToProperties.INSTANCE.debugOther.isTrue();

  private final int lhsSize;

  RhsTranslator (int lhsSize, PointsToState pb, WarningsContainer w) {
    this.lhsSize = lhsSize;
    this.state = pb;
    this.warnings = w;
  }

  HyperPointsToSet run (Rhs rhs) {
    return rhs.accept(this, null);
  }

  private void msg (String s) {
    if (DEBUG) {
      System.out.println("\nRhsTranslator: " + s);
    }
  }

  @Override public HyperPointsToSet visit (Bin expr, Void data) {
    HyperPointsToSet l = visit(expr.getLeft(), data);
    HyperPointsToSet r = visit(expr.getRight(), data);
    if (l.isScalar() && r.isScalar()) {
      l.offset = expr;
      return l;
    }
    switch (expr.getOp()) {
    case Add:
      l.add(r);
      return l;
    case Sub:
      l.sub(r);
      return l;
    case And:
      Linear rightLin = expr.getRight().getLinearTerm();
      if (rightLin.isConstantOnly()) {
        BigInt c = rightLin.getConstant();
        if (isPointerAlignmentMask(c)) {
          warnings.addWarning(new PointerAlignmentWarning(expr.getLeft(), expr.getRight()));
          return l;
        }
      }
    default:
      msg("operation " + expr.getOp() + " not supported yet");
      l.setToTop();
      return l;
    }
  }

  private static final BigInt $PointerAlignmentMask32Bit = BigInt.of("fffffff0", 16);
  private static final BigInt $PointerAlignmentMask64Bit = BigInt.of("fffffffffffffff0", 16);
  private static final BigInt $PointerAlignmentMaskSigned = BigInt.of(-16);

  private static boolean isPointerAlignmentMask (BigInt c) {
    if (c == null)
      return false;
    return c.isEqualTo($PointerAlignmentMask32Bit) ||
      c.isEqualTo($PointerAlignmentMask64Bit) || c.isEqualTo($PointerAlignmentMaskSigned);
  }

  @Override public HyperPointsToSet visit (Cmp expr, Void data) {
    HyperPointsToSet l = visit(expr.getLeft(), data);
    HyperPointsToSet r = visit(expr.getRight(), data);
    if (l.isScalar() && r.isScalar()) {
      l.offset = fin.comparison((Rlin) l.offset, expr.getOp(), (Rlin) r.offset);
    } else {
      l.setToTop();
    }
    return l;
  }

  @Override public HyperPointsToSet visit (SignExtend expr, Void data) {
    HyperPointsToSet h = this.visit(expr.getExpr(), data);
    if (isPtsAgnostic(h, expr.getSize())) {
      h.offset = fin.signExtend((Rlin) h.offset);
    } else {
      h.setToTop();
    }
    return h;
  }

  @Override public HyperPointsToSet visit (Convert expr, Void data) {
    Rlin linExpr = expr.getExpr();
    HyperPointsToSet h = visit(linExpr, data);
    if (isPtsAgnostic(h, expr.getSize())) {
      h.offset = fin.convert((Rlin) h.offset);
    } else {
      h.setToTop();
    }
    return h;
  }

  private boolean isPtsAgnostic (HyperPointsToSet h, int rhsSize) {
    return h.isScalar() || rhsSize <= lhsSize;
  }

  @Override public HyperPointsToSet visit (Rlin rlin, Void data) {
    return visitLinear(rlin.getLinearTerm(), rlin.getSize());
  }

//XXX make private
  HyperPointsToSet visitLinear (Linear linearTerm, int size) {
    HyperPointsToSet result = new HyperPointsToSet(size);
    result.addOffset(size, linearTerm.getConstant());
    for (Term t : linearTerm.getTerms()) {
      result.add(visitVar(size, t.getId()).mul(t.getCoeff()));
    }
    return result;
  }

//XXX make private
  HyperPointsToSet visitVar (int size, NumVar termVar) {
    // XXX test ghost nodes when applying edgenodes???
    // assert !state.hasPts(termVar) || !state.getPts(termVar).hasGhostNode();
    HyperPointsToSet result = new HyperPointsToSet(size);
    if (termVar.isAddress()) {
      result.addCoefficient((AddrVar) termVar, Linear.ONE);
      // XXX hsi: we assume that the address does not point to a summary
      // otherwise we would have to increase the sum by more than one
      // fix: create domain operation evalAssignAddress(Lhs, AddrVar) and
      // assume that normal assignments do not contain addresses
      result.addSumOfFlags(Linear.ONE);
    } else {
      PointsToSet pts = state.getPts(termVar);
      result.addOffset(fin.linear(size, Linear.linear(termVar)));
      if (!pts.sumOfFlags.isConstantZero())
        result.addSumOfFlags(pts.sumOfFlags.getLinear());
      for (PointsToEntry entry : pts) {
        result.addCoefficient(entry.address, Linear.linear(entry.flag));
      }
    }
    return result;
  }


  @Override public HyperPointsToSet visit (FiniteRangeRhs range, Void data) {
    return new HyperPointsToSet(range);
  }

  @Override public String toString () {
    return "RhsTranslator(...)";
  }
}
