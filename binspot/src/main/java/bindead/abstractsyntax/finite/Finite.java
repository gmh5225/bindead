package bindead.abstractsyntax.finite;

import rreil.lang.BinOp;
import rreil.lang.ComparisonOp;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;
import javalx.xml.XmlPrintable;
import bindead.data.FoldMap;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domains.affine.Substitution;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Definition of the grammar used by finite domains.
 */
public class Finite {

  public static final class Assign {
    private final Lhs lhs;
    private final Rhs rhs;

    Assign (Lhs lhs, Rhs rhs) {
      this.lhs = lhs;
      this.rhs = rhs;
    }

    public Lhs getLhs () {
      return lhs;
    }

    public Rhs getRhs () {
      return rhs;
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      lhs.asString(builder);
      builder.append(" =").append(lhs.getSize()).append(' ');
      rhs.asString(builder);
      return builder.toString();
    }

  }

  public static final class Lhs {
    private final int size;
    private final NumVar id;

    Lhs (int size, NumVar id) {
      assert size >= 0;
      this.size = size;
      this.id = id;
    }

    public NumVar getId () {
      return id;
    }

    public int getSize () {
      return size;
    }

    public StringBuilder asString (StringBuilder builder) {
      return builder.append(id.toString()).append(':').append(size);
    }

    @Override public String toString () {
      return asString(new StringBuilder()).toString();
    }
  }

  public static abstract class Rhs {
    @Override public String toString () {
      return asString(new StringBuilder()).toString();
    }

    public abstract <R, T> R accept (FiniteExprVisitor<R, T> visitor, T data);

    public abstract StringBuilder asString (StringBuilder builder);

    public abstract int getSize ();
  }

  public static final class SignExtend extends Rhs {
    private final Rlin expr;

    SignExtend (Rlin rval) {
      this.expr = rval;
    }

    @Override public <R, T> R accept (FiniteExprVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    public Rlin getExpr () {
      return expr;
    }

    @Override public StringBuilder asString (StringBuilder builder) {
      builder.append("sign-extend(");
      expr.asString(builder);
      builder.append(')');
      return builder;
    }

    @Override public int getSize () {
      return expr.getSize();
    }

  }

  public static final class Convert extends Rhs {
    private final Rlin expr;

    Convert (Rlin rval) {
      this.expr = rval;
    }

    @Override public <R, T> R accept (FiniteExprVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    public Rlin getExpr () {
      return expr;
    }

    @Override public StringBuilder asString (StringBuilder builder) {
      builder.append("convert(");
      expr.asString(builder);
      builder.append(')');
      return builder;
    }

    @Override public int getSize () {
      return expr.getSize();
    }

  }

  public static final class Bin extends Rhs {
    private final BinOp op;
    private final Rlin left;
    private final Rlin right;

    Bin (Rlin left, BinOp op, Rlin right) {
      this.op = op;
      this.left = left;
      this.right = right;
    }

    @Override public <R, T> R accept (FiniteExprVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    public BinOp getOp () {
      return op;
    }

    public Rlin getLeft () {
      return left;
    }

    public Rlin getRight () {
      return right;
    }

    @Override public StringBuilder asString (StringBuilder builder) {
      left.asString(builder).append(' ');
      builder.append(op.asInfix()).append(' ');
      right.asString(builder);
      return builder;
    }

    @Override public int getSize () {
      assert left.getSize() == right.getSize();
      return left.getSize();
    }

  }
  
//  public static abstract class Lin extends Rhs {
//  }
//
//  public static final class LinBin extends Lin {
//    private final LinBinOp op;
//    private final Lin left;
//    private final Lin right;
//
//    public LinBin (Lin left, LinBinOp op, Lin right) {
//      this.op = op;
//      this.left = left;
//      this.right = right;
//    }
//
//    public LinBinOp getOp () {
//      return op;
//    }
//
//    public Lin getLeft () {
//      return left;
//    }
//
//    public Lin getRight () {
//      return right;
//    }
//
//    @Override public <R, T> R accept (FiniteExprVisitor<R, T> visitor, T data) {
//      return visitor.visit(this, data);
//    }
//
//    @Override public StringBuilder asString (StringBuilder builder) {
//      left.asString(builder).append(' ');
//      builder.append(op.asInfix()).append(' ');
//      right.asString(builder);
//      return builder;
//    }
//
//    @Override public int getSize () {
//      assert left.getSize() == right.getSize();
//      return left.getSize();
//    }
//  }
//
//  public static final class LinScale extends Lin {
//    private final Lin opnd;
//    private final BigInt _const;
//
//    public LinScale (Lin opnd, BigInt _const) {
//      this.opnd = opnd;
//      this._const = _const;
//    }
//
//    public Lin getOpnd () {
//      return opnd;
//    }
//
//    public BigInt getConst () {
//      return _const;
//    }
//
//    @Override public <R, T> R accept (FiniteExprVisitor<R, T> visitor, T data) {
//      return visitor.visit(this, data);
//    }
//    
//    @Override public StringBuilder asString (StringBuilder builder) {
//      builder.append(_const).append("*");
//      opnd.asString(builder);
//      return builder;
//    }
//
//    @Override public int getSize () {
//      return opnd.getSize();
//    }
//  }
//  
//  public static final class LinRlit extends Lin {
//    private final Rlit lit;
//
//    public LinRlit (Rlit lit) {
//      this.lit = lit;
//    }
//
//    public Rlit getLit () {
//      return lit;
//    }
//
//    @Override public <R, T> R accept (FiniteExprVisitor<R, T> visitor, T data) {
//      return visitor.visit(this, data);
//    }
//    
//    @Override public StringBuilder asString (StringBuilder builder) {
//      builder.append(lit.toString());
//      return builder;
//    }
//
//    @Override public int getSize () {
//      return lit.getSize();
//    }
//  }
//  
//  public static final class LinRvar extends Lin {
//    private final Rvar var;
//
//    public LinRvar (Rvar var) {
//      this.var = var;
//    }
//
//    public Rvar getVar () {
//      return var;
//    }
//
//    @Override public <R, T> R accept (FiniteExprVisitor<R, T> visitor, T data) {
//      return visitor.visit(this, data);
//    }
//    
//    @Override public StringBuilder asString (StringBuilder builder) {
//      builder.append(var.toString());
//      return builder;
//    }
//
//    @Override public int getSize () {
//      return var.getSize();
//    }
//  }

  public static final class Cmp extends Rhs {
    private static final FiniteFactory finite = FiniteFactory.getInstance();
    private final ComparisonOp op;
    private final Rlin left;
    private final Rlin right;

    Cmp (Rlin left, ComparisonOp op, Rlin right) {
      this.op = op;
      this.left = left;
      this.right = right;
    }

    public ComparisonOp getOp () {
      return op;
    }

    public Rlin getLeft () {
      return left;
    }

    public Rlin getRight () {
      return right;
    }

    public Test asTest () {
      assert left.size == right.size;
      int size = getSize();
      Linear l = left.linearTerm;
      Linear r = right.linearTerm;
      Test test = null;
      switch (op) {
      case Cmpeq:
        test = finite.equalTo(size, l, r);
        break;
      case Cmpneq:
        test = finite.notEqualTo(size, l, r);
        break;
      case Cmples:
        test = finite.signedLessThanOrEqualTo(size, l, r);
        break;
      case Cmpleu:
        test = finite.unsignedLessThanOrEqualTo(size, l, r);
        break;
      case Cmplts:
        test = finite.signedLessThan(size, l, r);
        break;
      case Cmpltu:
        test = finite.unsignedLessThan(size, l, r);
        break;
      }
      return test;
    }

    @Override public <R, T> R accept (FiniteExprVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public StringBuilder asString (StringBuilder builder) {
      left.asString(builder).append(' ');
      builder.append(op.asInfix()).append(' ');
      right.asString(builder);
      return builder;
    }

    @Override public int getSize () {
      assert left.getSize() == right.getSize();
      return left.getSize();
    }

  }

  public static final class Rlin extends Rhs {
    private final Linear linearTerm;
    private final int size;

    Rlin (int size, Linear lin) {
      assert size >= 0;
      this.size = size;
      this.linearTerm = lin;
    }

    @Override public int getSize () {
      return size;
    }

    public Rlin add (Rlin other) {
      assert other.size == size;
      Linear res = linearTerm.add(other.linearTerm);
      return new Rlin(size, res);
    }

    public Rlin sub (Rlin other) {
      assert other.size == size;
      Linear res = linearTerm.sub(other.linearTerm);
      return new Rlin(size, res);
    }

    public Rlin smul (BigInt scalar) {
      return new Rlin(getSize(), getLinearTerm().smul(scalar));
    }

    public Linear getLinearTerm () {
      return linearTerm;
    }

    public VarSet getVars () {
      return linearTerm.getVars();
    }

    @Override public String toString () {
      return "{" + linearTerm + "}:" + size;
    }

    @Override public <R, T> R accept (FiniteExprVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public StringBuilder asString (StringBuilder builder) {
      return builder.append(toString());
    }

    @Override public boolean equals (Object o) {
      if (!(o instanceof Rlin))
        return false;
      Rlin other = (Rlin) o;
      return size == other.size && linearTerm.equals(other.linearTerm);
    }

    @Override public int hashCode () {
      // TODO implement following contract that a.equals(b) implies a.hashCode()==b.hashCode()
      throw new UnimplementedException();
    }
  }

  public static final class FiniteRangeRhs extends Rhs {
    private final Interval range;
    private final int size;

    public FiniteRangeRhs (int size, Interval range) {
      assert size >= 0;
      this.size = size;
      this.range = range;
    }

    public Interval getRange () {
      return range;
    }

    @Override public int getSize () {
      return size;
    }

    @Override public <R, T> R accept (FiniteExprVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public StringBuilder asString (StringBuilder builder) {
      builder.append(range).append(':').append(size);
      return builder;
    }

  }

  public enum TestOp {
    Equal("="),
    NotEqual("≠"),
    UnsignedLessThanOrEqual("⊴"),
    SignedLessThanOrEqual("≤"),
    UnsignedLessThan("⊲"),
    SignedLessThan("<");

    private final String opString;

    TestOp (String opString) {
      this.opString = opString;
    }

    @Override public String toString () {
      return opString;
    }

  }

  public static class Test implements Comparable<Test>, XmlPrintable {
    private final int size;
    private final Linear leftExpr;
    private final TestOp operator;
    private final Linear rightExpr;

    Test (int size, Linear leftExpr, TestOp operator, Linear rightExpr) {
      assert size >= 0;
      this.size = size;
      this.rightExpr = rightExpr;
      this.operator = operator;
      this.leftExpr = leftExpr;
    }

    public int getSize () {
      return size;
    }

    public Linear getLeftExpr () {
      return leftExpr;
    }

    public TestOp getOperator () {
      return operator;
    }

    public Linear getRightExpr () {
      return rightExpr;
    }

    /**
     * Transform this test into a linear by subtracting the rhs from the lhs.
     * Tests of the form {@code lhs op rhs} become the linear {@code lhs - rhs = 0}.
     */
    public Linear toLinear () {
      return getLeftExpr().sub(getRightExpr());
    }

    public Rlin toRLin () {
      return new Rlin(getSize(), toLinear());
    }

    /**
     * Negate the test operator.
     */
    public Test not () {
      switch (operator) {
      case Equal:
        return new Test(size, leftExpr, TestOp.NotEqual, rightExpr);
      case NotEqual:
        return new Test(size, leftExpr, TestOp.Equal, rightExpr);
      case SignedLessThan:
        return new Test(size, rightExpr, TestOp.SignedLessThanOrEqual, leftExpr);
      case SignedLessThanOrEqual:
        return new Test(size, rightExpr, TestOp.SignedLessThan, leftExpr);
      case UnsignedLessThan:
        return new Test(size, rightExpr, TestOp.UnsignedLessThanOrEqual, leftExpr);
      case UnsignedLessThanOrEqual:
        return new Test(size, rightExpr, TestOp.UnsignedLessThan, leftExpr);
      default:
        throw new IllegalStateException();
      }
    }

    /**
     * Build a test with the same operator but the given linear expressions for {@code lhs} and {@code rhs}.
     */
    public final Test build (Linear left, Linear right) {
      return new Test(size, left, operator, right);
    }

    /**
     * Build a test with the same operator but the given linear expressions for {@code lhs} and {@code rhs}.
     */
    public final Test build (int size, Linear left, Linear right) {
      return new Test(size, left, operator, right);
    }

    /**
     * Build a test with the same operator but the given linear expressions for {@code lhs} and 0 as {@code rhs}.
     */
    public final Test build (Linear left) {
      return build(left, Linear.ZERO);
    }

    /**
     * Apply the substitution on the left and right hand sides of the test.
     */
    public Test applySubstitution (Substitution sigma) {
      Linear left = leftExpr.applySubstitution(sigma);
      Linear right = rightExpr.applySubstitution(sigma);
      return build(left, right);
    }

    public Test renameVars (FoldMap pairs) {
      Linear left = leftExpr.renameVar(pairs);
      Linear right = rightExpr.renameVar(pairs);
      return build(left, right);
    }

    public VarSet getVars () {
      return leftExpr.getVars().union(rightExpr.getVars());
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
      result = prime * result + (leftExpr == null ? 0 : leftExpr.hashCode());
      result = prime * result + (operator == null ? 0 : operator.hashCode());
      result = prime * result + (rightExpr == null ? 0 : rightExpr.hashCode());
      result = prime * result + size;
      return result;
    }

    @Override public int compareTo (Test other) {
      int cmp = operator.compareTo(other.operator);
      if (cmp == 0)
        cmp = new Integer(size).compareTo(other.size);
      if (cmp == 0)
        cmp = leftExpr.compareTo(other.leftExpr);
      if (cmp == 0)
        cmp = rightExpr.compareTo(other.rightExpr);
      return cmp;
    }

    @Override public String toString () {
      return toPrettyString();
    }

    /**
     * Tries to print the test such that the leading variable on the left is only on one side of the operator
     * with positive coefficient.
     */
    public String toPrettyString () {
      if (getLeftExpr().isConstantOnly() && getRightExpr().isConstantOnly())
        return toSimpleString();
      if (getLeftExpr().isConstantOnly()) // switch lhs and rhs
        return new Test(size, rightExpr.negate(), operator, leftExpr.negate()).toPrettyString();
      Linear lhs = getLeftExpr();
      NumVar keyVar = lhs.getKey();
      BigInt keyCoeff = lhs.getCoeff(keyVar);
      Linear rhs = getRightExpr();
      if (keyCoeff.isNegative()) {
        lhs = lhs.smul(Bound.MINUSONE);
        rhs = rhs.smul(Bound.MINUSONE);
        if (getOperator() != TestOp.Equal && getOperator() != TestOp.NotEqual) {
          Linear swapTemp = rhs;
          rhs = lhs;
          lhs = swapTemp;
        }
      }
      StringBuilder builder = new StringBuilder();
      builder.append(lhs);
      builder.append(getOperator());
      builder.append(rhs);
      return builder.toString();
    }

    public String toSimpleString () {
      return leftExpr.toTermsString() + " " + operator + ":" + size + " " + rightExpr.toTermsString();
    }

    @Override public XMLBuilder toXML (XMLBuilder builder) {
      // as rhs is 0 the divisor can actually be neglected.
      builder = builder.e("Equation").a("side", "left");
      builder = leftExpr.toXML(builder);
      builder = builder.up();
      builder = builder.e("Operator").t(operator.toString()).up();
      builder = builder.e("Size").t(String.valueOf(size)).up();
      builder = builder.e("Equation").a("side", "right");
      builder = rightExpr.toXML(builder);
      builder = builder.up();
      return builder;
    }

  }

}
