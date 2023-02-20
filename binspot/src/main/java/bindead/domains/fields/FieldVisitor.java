package bindead.domains.fields;

import rreil.RReilGrammarException;
import rreil.lang.Rhs.Address;
import rreil.lang.Rhs.Bin;
import rreil.lang.Rhs.Cmp;
import rreil.lang.Rhs.Convert;
import rreil.lang.Rhs.LinBin;
import rreil.lang.Rhs.LinRval;
import rreil.lang.Rhs.LinScale;
import rreil.lang.Rhs.RangeRhs;
import rreil.lang.Rhs.Rlit;
import rreil.lang.Rhs.Rvar;
import rreil.lang.Rhs.SignExtend;
import rreil.lang.util.RhsVisitor;
import bindead.abstractsyntax.finite.Finite;
import bindead.abstractsyntax.finite.Finite.Rhs;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.domainnetwork.interfaces.FiniteDomain;

/**
 * Transforms an RREIL term to a finite term by resolving variables.
 */
class FieldVisitor<D extends FiniteDomain<D>> implements RhsVisitor<Finite.Rhs, Void> {
  private static final FiniteFactory fin =  FiniteFactory.getInstance();
  private final FieldStateBuilder<D> builder;

  protected FieldVisitor (FieldStateBuilder<D> builder) {
    this.builder = builder;
  }

  @Override public Finite.Rhs visit (Bin expr, Void _) {
    Finite.Rhs l = expr.getLeft().accept(this, _);
    Finite.Rhs r = expr.getRight().accept(this, _);
    if (l instanceof Finite.Rlin && r instanceof Finite.Rlin)
      return fin.binary((Finite.Rlin) l, expr.getOp(), (Finite.Rlin) r);
    return null;
  }

  @Override public Rhs visit (LinBin expr, Void _) {
    Finite.Rhs l = expr.getLeft().accept(this, _);
    Finite.Rhs r = expr.getRight().accept(this, _);
    if (l instanceof Finite.Rlin && r instanceof Finite.Rlin) {
      Finite.Rlin llin = (Finite.Rlin) l;
      Finite.Rlin rlin = (Finite.Rlin) r;
      switch (expr.getOp()) {
      case Add:
        return llin.add(rlin);
      case Sub:
        return llin.sub(rlin);
      default:
        throw new RReilGrammarException();
      }
    }
    return null;
  }

  @Override public Rhs visit (LinScale expr, Void _) {
    Finite.Rhs opnd = expr.getOpnd().accept(this, _);
    if (opnd instanceof Finite.Rlin)
      return ((Finite.Rlin) opnd).smul(expr.getConst());
    return null;
  }

  @Override public Rhs visit (LinRval expr, Void _) {
    Finite.Rhs opnd = expr.getRval().accept(this, _);
    return opnd;
  }


  @Override public Finite.Rhs visit (Cmp expr, Void _) {
    Finite.Rhs l = expr.getLeft().accept(this, _);
    Finite.Rhs r = expr.getRight().accept(this, _);
    if (l instanceof Finite.Rlin && r instanceof Finite.Rlin)
      return fin.comparison((Finite.Rlin) l, expr.getOp(), (Finite.Rlin) r);
    return null;
  }

  @Override public Finite.Rhs visit (SignExtend expr, Void _) {
    return fin.signExtend((Finite.Rlin) expr.getRhs().accept(this, _));
  }

  @Override public Finite.Rhs visit (Convert expr, Void _) {
    return fin.convert((Finite.Rlin) expr.getRhs().accept(this, _));
  }

  @Override public Finite.Rlin visit (Rvar variable, Void _) {
    return builder.resolve(variable);
  }

  @Override public Finite.Rlin visit (Rlit literal, Void _) {
    return fin.literal(literal.getSize(), literal.getValue());
  }

  @Override public Rhs visit (Address expr, Void _) {
    throw new RReilGrammarException("RReil jump target addresses should not appear on this level anymore.");
  }

  @Override public Finite.Rhs visit (RangeRhs range, Void _) {
    return fin.range(range.getSize(), range.getRange());
  }
}