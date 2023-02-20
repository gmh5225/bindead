package bindead.abstractsyntax.zeno;

import static bindead.data.Linear.linear;
import static bindead.data.Linear.term;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;
import javalx.xml.XmlPrintable;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domains.affine.Substitution;
import bindead.exceptions.DomainStateException;
import bindead.exceptions.DomainStateException.InvariantViolationException;

import com.jamesmurty.utils.XMLBuilder;

/**
 * The grammar definition used in the zeno domain stage.
 */
public abstract class Zeno {
  public abstract <R, T> R accept (ZenoVisitor<R, T> visitor, T data);

  @Override public abstract String toString ();

  public static enum ZenoBinOp {
    Mul("*"),
    Div("/"),
    Shl("<<"),
    Shr(">>"),
    Mod("%");
    private final String infixSymbol;

    ZenoBinOp (final String infixSymbol) {
      this.infixSymbol = infixSymbol;
    }

    @Override public String toString () {
      return infixSymbol;
    }
  }

  public static enum ZenoTestOp {
    EqualToZero("="),
    NotEqualToZero("≠"),
    LessThanOrEqualToZero("≤");

    private final String pretty;

    ZenoTestOp (String pretty) {
      this.pretty = pretty;
    }

    @Override public String toString () {
      return pretty + " 0";
    }

    public String getOpString () {
      return pretty;
    }
  }

  public static final class Assign extends Zeno {
    private final Lhs lhs;
    private final Rhs rhs;

    public Assign (Lhs lhs, Rhs rhs) {
      this.lhs = lhs;
      this.rhs = rhs;
    }

    public Lhs getLhs () {
      return lhs;
    }

    public Rhs getRhs () {
      return rhs;
    }

    @Override public <R, T> R accept (ZenoVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public String toString () {
      return lhs + " = " + rhs;
    }
  }

  /**
   * Variables used in left-hand-side positions, that is as destination of assignments.
   */
  public static final class Lhs {
    private final NumVar id;

    public Lhs (NumVar id) {
      this.id = id;
    }

    public NumVar getId () {
      return id;
    }

    public <R, T> R accept (ZenoVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public String toString () {
      return id.toString();
    }
  }

  public abstract static class Rhs {

    public abstract <R, T> R accept (ZenoVisitor<R, T> visitor, T data);

    public abstract VarSet getVars ();

    @Override public abstract String toString ();
  }

  public static final class Bin extends Rhs {
    private final ZenoBinOp op;
    private final Rlin left;
    private final Rlin right;

    public Bin (Rlin left, ZenoBinOp op, Rlin right) {
      this.op = op;
      this.left = left;
      this.right = right;
    }

    public ZenoBinOp getOp () {
      return op;
    }

    public Rlin getLeft () {
      return left;
    }

    public Rlin getRight () {
      return right;
    }

    @Override public VarSet getVars () {
      return left.getVars().union(right.getVars());
    }

    @Override public <R, T> R accept (ZenoVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public String toString () {
      return left + " " + op + " " + right;
    }
  }

  public static final class Test implements Comparable<Test>, XmlPrintable {
    private static final ZenoFactory zeno = ZenoFactory.getInstance();

    private final ZenoTestOp op;
    private final Linear expr;

    Test (Linear expr, ZenoTestOp op) {
      this.op = op;
      this.expr = expr;
    }

    public ZenoTestOp getOperator () {
      return op;
    }

    /**
     * Build a test with the same operator but the given linear expression.
     */
    public Test withExpr (Linear lin) {
      return zeno.comparison(lin, op);
    }

    public Linear getExpr () {
      return expr;
    }

    public VarSet getVars () {
      return expr.getVars();
    }

    public Test applySubstitution (Substitution sigma) {
      if (expr.getVars().contains(sigma.getVar()))
        return zeno.comparison(expr.applySubstitution(sigma), op);
      else
        return this;
    }

    /**
     * Negate the test to its opposite.
     *
     * @return the opposite test.
     */
    public Test not () {
      Test test = null;
      switch (getOperator()) {
      case EqualToZero:
        test = zeno.comparison(getExpr(), ZenoTestOp.NotEqualToZero);
        break;
      case NotEqualToZero:
        test = zeno.comparison(getExpr(), ZenoTestOp.EqualToZero);
        break;
      case LessThanOrEqualToZero: {
        Linear lin = Linear.ONE.sub(getExpr());
        test = zeno.comparison(zeno.linear(lin), ZenoTestOp.LessThanOrEqualToZero);
        break;
      }
      }
      return test;
    }

    /**
     * Split an equality or disequality test into two lower-or-equal tests that mean the same.
     *
     * @return two lower or equal tests for the (dis-)equality
     */
    public P2<Test, Test> splitEquality () {
      assert (op == ZenoTestOp.NotEqualToZero || op == ZenoTestOp.EqualToZero);
      Linear lin = expr;
      BigInt ofs = (op == ZenoTestOp.NotEqualToZero ? Bound.ONE : Bound.ZERO);
      Test less = zeno.comparison(lin.add(ofs), ZenoTestOp.LessThanOrEqualToZero);
      Test greater = zeno.comparison(zeno.linear(lin.negate().add(ofs)), ZenoTestOp.LessThanOrEqualToZero);
      return P2.tuple2(less, greater);
    }

    @Override public int compareTo (Test other) {
      int cmp = op.compareTo(other.op);
      if (cmp == 0)
        return expr.compareTo(other.expr);
      return cmp;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof Test))
        return false;
      Test other = (Test) obj;
      return compareTo(other) == 0;
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((expr == null) ? 0 : expr.hashCode());
      result = prime * result + ((op == null) ? 0 : op.hashCode());
      return result;
    }

    @Override public XMLBuilder toXML (XMLBuilder builder) {
      Linear equation = expr;
      builder = builder.e("Equation");
      builder = equation.toXML(builder);
      builder = builder.up();
      builder = builder.e("Operator").t(op.toString()).up();
      return builder;
    }

    /**
     * Format tests such that the leading variable is alone and positive as lhs or rhs and the
     * rest of the test is a linear on the other side of the operator.
     */
    @Override public String toString () {
      if (expr.isConstantOnly())
        return expr.toString() + " " + op;
      Linear lhs = expr;
      NumVar keyVar = lhs.getKey();
      BigInt keyCoeff = lhs.getCoeff(keyVar);
      Linear rhs = lhs.dropTerm(keyVar).negate();
      lhs = linear(term(keyCoeff, keyVar));
      if (keyCoeff.isNegative()) {
        lhs = lhs.smul(Bound.MINUSONE);
        rhs = rhs.smul(Bound.MINUSONE);
        if (getOperator() == ZenoTestOp.LessThanOrEqualToZero) {
          Linear swapTemp = rhs;
          rhs = lhs;
          lhs = swapTemp;
        }
      }
      StringBuilder builder = new StringBuilder();
      builder.append(lhs);
      builder.append(getOperator().getOpString());
      builder.append(rhs);
      return builder.toString();
    }

    public String toSimpleString () {
      StringBuilder builder = new StringBuilder();
      builder.append(getExpr());
      builder.append(getOperator().getOpString());
      builder.append("0");
      return builder.toString();
    }

  }

  public static final class Rlin extends Rhs implements Comparable<Rlin> {
    private final Linear linearTerm;
    private final BigInt divisor;

    public Rlin (Linear linearTerm) {
      this.linearTerm = linearTerm;
      this.divisor = Bound.ONE;
    }

    /**
     * Build a new right-hand-side wrapper for linear in the Zeno domain.
     *
     * @param linearTerm The linear term to be wrapped as an right-hand-side linear
     * @param divisor The amount by which the expression above is to be divided, this is necessary for the affine domain
     *          when an equality like 4x = y is inlined into an assignment r := x which becomes r := y/4. The divisor
     *          is always greater than zero.
     */
    public Rlin (Linear linearTerm, BigInt divisor) {
      assert !divisor.isZero();
      if (!divisor.isNegative()) {
        this.linearTerm = linearTerm;
        this.divisor = divisor;
      } else {
        this.linearTerm = linearTerm.negate();
        this.divisor = divisor.negate();
      }
    }

    @Override public <R, T> R accept (ZenoVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    public Linear getLinearTerm () {
      return linearTerm;
    }

    public BigInt getDivisor () {
      return divisor;
    }

    /**
     * Check if this linear expression is a constant and return it.
     *
     * @return {@code null} if the linear expression is not constant
     * @throws DomainStateException if it was a constant but the divisor is not one.
     */
    public BigInt getConstantOrNull () throws DomainStateException {
      if (isConstantOnly())
        return linearTerm.getConstant();
      return null;
    }

    /**
     * Check if this constraint only consists of a constant and no variable.
     *
     * @return {@code true} if there are no variables in this linear expression
     */
    public boolean isConstantOnly () {
      if (linearTerm.isConstantOnly()) {
        if (!divisor.isOne())
          throw new InvariantViolationException();
      }
      return linearTerm.isConstantOnly();
    }

    @Override public VarSet getVars () {
      return linearTerm.getVars();
    }

    public Rlin add (final Rlin other) {
      Linear res = Linear.mulAdd(other.divisor, this.linearTerm, this.divisor, other.linearTerm);
      return new Rlin(res, Bound.ONE);
    }

    public Rlin sub (final Rlin other) {
      return add(new Rlin(other.getLinearTerm().negate()));
    }

    public Rlin smul (final BigInt c) {
      assert divisor.isOne();
      return new Rlin(linearTerm.smul(c), divisor);
    }

    @Override public int compareTo (Rlin other) {
      int cmp = divisor.compareTo(other.divisor);
      if (cmp == 0)
        return linearTerm.compareTo(other.linearTerm);
      return cmp;
    }

    @Override public String toString () {
      boolean hasDivisor = !divisor.isOne();
      StringBuilder builder = new StringBuilder();
      if (hasDivisor)
        builder.append("(");
      builder.append(linearTerm);
      if (hasDivisor)
        builder.append(")/" + divisor);
      return builder.toString();
    }

    @Override public boolean equals (Object obj) {
      if (obj == null)
        return false;
      if (!(obj instanceof Rlin))
        return false;
      final Rlin other = (Rlin) obj;
      return compareTo(other) == 0;
    }

    @Override public int hashCode () {
      int hash = 5;
      hash = 67 * hash + (this.linearTerm != null ? this.linearTerm.hashCode() : 0);
      hash = 67 * hash + (this.divisor != null ? this.divisor.hashCode() : 0);
      return hash;
    }

  }

  public static final class RangeRhs extends Rhs {
    private final javalx.numeric.Range range;

    public RangeRhs (javalx.numeric.Range range) {
      this.range = range;
    }

    public RangeRhs (Interval range) {
      this.range = javalx.numeric.Range.from(range);
    }

    public javalx.numeric.Range getRange () {
      return range;
    }

    @Override public VarSet getVars () {
      return VarSet.empty();
    }

    @Override public <R, T> R accept (ZenoVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public String toString () {
      return range.toString();
    }
  }
}
