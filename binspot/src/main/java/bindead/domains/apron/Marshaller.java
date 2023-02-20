package bindead.domains.apron;

import gmp.Mpfr;
import gmp.Mpq;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javalx.data.Option;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import javalx.persistentcollections.BiMap;
import apron.DoubleScalar;
import apron.Environment;
import apron.Linexpr1;
import apron.Linterm1;
import apron.MpfrScalar;
import apron.MpqScalar;
import apron.Scalar;
import apron.Texpr1BinNode;
import apron.Texpr1CstNode;
import apron.Texpr1Intern;
import apron.Texpr1Node;
import bindead.abstractsyntax.zeno.Zeno.Bin;
import bindead.abstractsyntax.zeno.Zeno.Rhs;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.ZenoRhsVisitorSkeleton;
import bindead.data.Linear;
import bindead.data.Linear.Term;
import bindead.data.NumVar;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.exceptions.DomainStateException;
import bindead.exceptions.DomainStateException.InvariantViolationException;
import bindead.exceptions.Unreachable;

/**
 * Helper class that translates between Bindead and Apron datatypes and vice versa.
 *
 * @author Bogdan Mihaila
 */
class Marshaller {
  private final static ScalarMarshaller scalarMarshaller = new DoubleMarshaller();

  private abstract static class ScalarMarshaller {
    abstract Scalar makeScalar ();

    abstract Scalar makeScalar (BigInteger bigIntegerValue);

    abstract BigInt fromApronScalarNoRounding (Scalar value);

    abstract BigInt fromApronScalarRoundUp (Scalar value);

    abstract BigInt fromApronScalarRoundDown (Scalar value);
  }

  static class MpqMarshaller extends ScalarMarshaller {
    @Override Scalar makeScalar () {
      return new MpqScalar();
    }

    @Override Scalar makeScalar (BigInteger bigIntegerValue) {
      return new MpqScalar(bigIntegerValue);
    }

    @Override BigInt fromApronScalarNoRounding (Scalar value) {
      Mpq mpq = toMpq(value);
      if (!mpq.getDen().bigIntegerValue().equals(BigInteger.ONE))
        throw new InvariantViolationException("no fractions allowed here");
      return BigInt.of(mpq.getNum().bigIntegerValue());
    }

    @Override BigInt fromApronScalarRoundUp (Scalar value) {
      Mpq mpq = toMpq(value);
      if (mpq.getDen().bigIntegerValue().equals(BigInteger.ONE)) {
        return BigInt.of(mpq.getNum().bigIntegerValue());
      } else {
        BigInt numerator = BigInt.of(mpq.getNum().bigIntegerValue());
        BigInt denominator = BigInt.of(mpq.getDen().bigIntegerValue());
        return numerator.divRoundUp(denominator);
      }
    }

    @Override BigInt fromApronScalarRoundDown (Scalar value) {
      Mpq mpq = toMpq(value);
      if (mpq.getDen().bigIntegerValue().equals(BigInteger.ONE)) {
        return BigInt.of(mpq.getNum().bigIntegerValue());
      } else {
        BigInt numerator = BigInt.of(mpq.getNum().bigIntegerValue());
        BigInt denominator = BigInt.of(mpq.getDen().bigIntegerValue());
        return numerator.divRoundDown(denominator);
      }
    }

    private Mpq toMpq (Scalar value) {
      Mpq mpq = new Mpq();
      if (value.isInfty() != 0)
        throw new IllegalArgumentException("Value is infinity: " + value);
      if (value.toMpq(mpq, 0) != 0)
        throw new InvariantViolationException(); // the result was rounded. We do not want to handle fractions
      return mpq;
    }
  }

  static class MpfrMarshaller extends ScalarMarshaller {
    @Override Scalar makeScalar () {
      return new MpfrScalar();
    }

    @Override Scalar makeScalar (BigInteger bigIntegerValue) {
      // hsi: I hope there will be no overflows...
      return new MpfrScalar(bigIntegerValue.intValue());
    }

    @Override BigInt fromApronScalarNoRounding (Scalar value) {
      Mpfr mpfr = toMpfr(value);
      // hsi: we allow
      if (mpfr.isInteger())
        throw new InvariantViolationException("no fractions allowed here");
      return BigInt.of(mpfr.MpzValue(0).bigIntegerValue());
    }

    @Override BigInt fromApronScalarRoundUp (Scalar value) {
      Mpfr mpfr = toMpfr(value);
      return BigInt.of(mpfr.MpzValue(2).bigIntegerValue());
    }

    @Override BigInt fromApronScalarRoundDown (Scalar value) {
      Mpfr mpfr = toMpfr(value);
      return BigInt.of(mpfr.MpzValue(3).bigIntegerValue());
    }

    private Mpfr toMpfr (Scalar value) {
      Mpfr mpfr = new Mpfr();
      if (value.isInfty() != 0)
        throw new IllegalArgumentException("Value is infinity: " + value);
      if (value.toMpfr(mpfr, 0) != 0)
        throw new InvariantViolationException(); // the result was rounded. We do not want to handle fractions
      return mpfr;
    }
  }

  static class FastDoubleMarshaller extends ScalarMarshaller {
    @Override Scalar makeScalar () {
      return new DoubleScalar();
    }

    @Override Scalar makeScalar (BigInteger bigIntegerValue) {
      return new DoubleScalar(bigIntegerValue.doubleValue());
    }

    @Override BigInt fromApronScalarNoRounding (Scalar rawValue) {
      DoubleScalar value = (DoubleScalar) rawValue;
      double doubleValue = value.val;
      double roundedValue = Math.ceil(doubleValue);
      if (roundedValue > doubleValue)
        throw new InvariantViolationException("no fractions allowed here");
      return BigInt.of(BigDecimal.valueOf(roundedValue).toBigInteger());
    }

    @Override BigInt fromApronScalarRoundUp (Scalar rawValue) {
      DoubleScalar value = (DoubleScalar) rawValue;
      double doubleValue = value.val;
      double roundedValue = Math.ceil(doubleValue);
      return BigInt.of(BigDecimal.valueOf(roundedValue).toBigInteger());
    }

    @Override BigInt fromApronScalarRoundDown (Scalar rawValue) {
      DoubleScalar value = (DoubleScalar) rawValue;
      double doubleValue = value.val;
      double roundedValue = Math.floor(doubleValue);
      return BigInt.of(BigDecimal.valueOf(roundedValue).toBigInteger());
    }
  }

  static class DoubleMarshaller extends ScalarMarshaller {
    @Override Scalar makeScalar () {
      return new DoubleScalar();
    }

    @Override Scalar makeScalar (BigInteger bigIntegerValue) {
      return new DoubleScalar(bigIntegerValue.doubleValue());
    }

    @Override BigInt fromApronScalarNoRounding (Scalar value) {
      double[] mpd = new double[1];
      int rounded = value.toDouble(mpd, 0);
      if (rounded != 0)
        throw new InvariantViolationException("no fractions allowed here");
      double doubleValue = mpd[0];
      double roundedValue = Math.ceil(doubleValue);
      if (roundedValue > doubleValue)
        throw new InvariantViolationException("no fractions allowed here");
      return BigInt.of(BigDecimal.valueOf(roundedValue).toBigInteger());
    }

    @Override BigInt fromApronScalarRoundUp (Scalar value) {
      double[] mpd = new double[1];
      value.toDouble(mpd, 2);
      double doubleValue = mpd[0];
      double roundedValue = Math.ceil(doubleValue);
      return BigInt.of(BigDecimal.valueOf(roundedValue).toBigInteger());
    }

    @Override BigInt fromApronScalarRoundDown (Scalar value) {
      double[] mpd = new double[1];
      value.toDouble(mpd, 3);
      double doubleValue = mpd[0];
      double roundedValue = Math.floor(doubleValue);
      return BigInt.of(BigDecimal.valueOf(roundedValue).toBigInteger());
    }
  }

  public static BigInt fromApronScalarNoRounding (Scalar value) {
    return scalarMarshaller.fromApronScalarNoRounding(value);
  }

  public static BigInt fromApronScalarRoundDown (Scalar cst) {
    return scalarMarshaller.fromApronScalarRoundDown(cst);
  }

  public static apron.Interval toApronInterval (Interval interval) {
    apron.Interval apronInterval = new apron.Interval(0, 0);
    if (!interval.low().isFinite() && !interval.high().isFinite()) {
      apronInterval.setTop();
      return apronInterval;
    }
    if (!interval.low().isFinite()) {
      BigInteger upper = interval.high().asInteger().bigIntegerValue();
      Scalar lowerBound = scalarMarshaller.makeScalar();
      lowerBound.setInfty(-1);
      apronInterval.setInf(lowerBound);
      Scalar upperBound = scalarMarshaller.makeScalar(upper);
      apronInterval.setSup(upperBound);
      return apronInterval;
    }
    if (!interval.high().isFinite()) {
      BigInteger lower = interval.low().asInteger().bigIntegerValue();
      Scalar lowerBound = scalarMarshaller.makeScalar(lower);
      apronInterval.setInf(lowerBound);
      Scalar upperBound = scalarMarshaller.makeScalar();
      upperBound.setInfty(1);
      apronInterval.setSup(upperBound);
      return apronInterval;
    }
    BigInteger lower = interval.low().asInteger().bigIntegerValue();
    Scalar lowerBound = scalarMarshaller.makeScalar(lower);
    BigInteger upper = interval.high().asInteger().bigIntegerValue();
    Scalar upperBound = scalarMarshaller.makeScalar(upper);
    apronInterval.setInf(lowerBound);
    apronInterval.setSup(upperBound);
    return apronInterval;
  }

  public static Interval fromApronInterval (apron.Interval interval) {
    if (interval.isBottom()) // as a safeguard here, we do not have bottom and cannot continue computation with it
      throw new Unreachable();
    if (interval.isTop())
      return Interval.TOP;
    if (interval.inf().isInfty() == -1) {
      BigInt upperBound = scalarMarshaller.fromApronScalarRoundDown(interval.sup());
      return Interval.downFrom(upperBound);
    }
    if (interval.sup().isInfty() == 1) {
      BigInt lowerBound = scalarMarshaller.fromApronScalarRoundUp(interval.inf());
      return Interval.upFrom(lowerBound);
    }
    BigInt lowerBound = scalarMarshaller.fromApronScalarRoundUp(interval.inf());
    BigInt upperBound = scalarMarshaller.fromApronScalarRoundDown(interval.sup());
    if (lowerBound.isGreaterThan(upperBound))
      throw new Unreachable(); // not well-formed interval
    return Interval.of(lowerBound, upperBound);
  }

  public static Scalar toApronScalar (BigInt value) {
    BigInteger bigIntegerValue = value.bigIntegerValue();
    return scalarMarshaller.makeScalar(bigIntegerValue);
  }

  /**
   * Translates a linear expression to an Apron linear expression.
   *
   * @param expr the linear expression to be translated
   * @param env the environment managing the variables in Apron
   * @param variablesMapping a mapping between our variables and their name in Apron
   * @return An Apron linear expression or {@code DomainStateException.VariableSupportSetException} if a variable was not
   *         found in {@code variablesMapping}.
   */
  public static Linexpr1 toApronLinear (Linear expr, Environment env, BiMap<NumVar, String> variablesMapping) {
    List<Linterm1> terms = new ArrayList<Linterm1>();
    for (Term term : expr) {
      Scalar coefficient = toApronScalar(term.getCoeff());
      Option<String> variable = variablesMapping.get(term.getId());
      if (variable.isNone()) // Apron would not know about this variable and throw an exception, so fail fast
        throw new DomainStateException.VariableSupportSetException();
      terms.add(new Linterm1(variable.get(), coefficient));
    }
    Scalar constant = toApronScalar(expr.getConstant());
    return new Linexpr1(env, terms.toArray(new Linterm1[0]), constant);
  }

  /**
   * Translates from an Apron linear to our linear expression.
   *
   * @param expr the Apron linear expression to be translated
   * @param variablesMapping a mapping between our variables and their name in Apron
   * @return A linear expression or {@code DomainStateException.VariableSupportSetException} if a variable was not
   *         found in {@code variablesMapping}.
   */
  public static Linear fromApronLinear (Linexpr1 expr, BiMap<NumVar, String> variablesMapping) {
    List<Term> terms = new ArrayList<Term>();
    for (Linterm1 term : expr.getLinterms()) {
      Option<NumVar> variable = variablesMapping.getKey(term.var);
      if (variable.isNone()) // we do not know about this variable
        throw new DomainStateException.VariableSupportSetException();
      if (!term.coeff.isScalar())
        throw new IllegalArgumentException("The coefficient " + term.coeff + " is not a scalar value and can thus "
          + "not be represented with our linear coefficients.");
      BigInt coefficient = scalarMarshaller.fromApronScalarNoRounding(term.coeff.inf());
      terms.add(Linear.term(coefficient, variable.get()));
    }
    if (!expr.getCst().isScalar())
      throw new IllegalArgumentException("The constant coefficient " + expr.getCst() + " is not a scalar value and "
        + "can thus not be represented with our linear coefficients.");
    BigInt constant = scalarMarshaller.fromApronScalarNoRounding(expr.getCst().inf());
    return Linear.linear(terms.toArray(new Term[terms.size()])).add(constant);
  }

  /**
   * Translates a Zeno right-hand-side expression to an Apron tree-expression.
   *
   * @param expr the linear expression to be translated
   * @param env the environment managing the variables in Apron
   * @param variablesMapping a mapping between our variables and their name in Apron
   * @return An Apron linear expression or {@code DomainStateException.VariableSupportSetException} if a variable was not
   *         found in {@code variablesMapping}.
   */
  public static Texpr1Intern toApronExpr (Rhs expr, Environment env, BiMap<NumVar, String> variablesMapping,
      QueryChannel domain) {
    ToApronRhsTranslator rhsTranslator = new ToApronRhsTranslator(env, variablesMapping, domain);
    return expr.accept(rhsTranslator, null);
  }

  /**
   * Translates a Zeno linear expression (can contain a divisor) to an Apron tree-expression.
   *
   * @param expr the linear expression to be translated
   * @param env the environment managing the variables in Apron
   * @param variablesMapping a mapping between our variables and their name in Apron
   * @return An Apron linear expression or {@code DomainStateException.VariableSupportSetException} if a variable was not
   *         found in {@code variablesMapping}.
   */
  public static Texpr1Intern toApronExpr (Rlin expr, Environment env, BiMap<NumVar, String> variablesMapping) {
    Linexpr1 linear = toApronLinear(expr.getLinearTerm(), env, variablesMapping);
    Texpr1Node apronExpr;
    if (expr.getDivisor().isOne()) { // adding "1" as divisor seems to produce rounding errors due to over-approx
      apronExpr = Texpr1Node.fromLinexpr1(linear);
    } else {
      Texpr1CstNode divisor = new Texpr1CstNode(toApronScalar(expr.getDivisor()));
      apronExpr = new Texpr1BinNode(Texpr1BinNode.OP_DIV, Texpr1BinNode.RTYPE_REAL, Texpr1BinNode.RDIR_ZERO,
        Texpr1Node.fromLinexpr1(linear), divisor);
    }
    return new Texpr1Intern(env, apronExpr);
  }

  private static class ToApronRhsTranslator extends ZenoRhsVisitorSkeleton<Texpr1Intern, Void> {
    private final Environment env;
    private final BiMap<NumVar, String> variablesMapping;
    private final QueryChannel domain;

    public ToApronRhsTranslator (Environment env, BiMap<NumVar, String> variablesMapping, QueryChannel domain) {
      this.env = env;
      this.variablesMapping = variablesMapping;
      this.domain = domain;
    }

    @Override public Texpr1Intern visit (Bin expr, Void data) {
      Texpr1Node apronExpr = null;
      Texpr1Node left = toApronExpr(expr.getLeft(), env, variablesMapping).toTexpr1Node();
      Texpr1Node right = toApronExpr(expr.getRight(), env, variablesMapping).toTexpr1Node();
      switch (expr.getOp()) {
      case Mul:
        apronExpr = new Texpr1BinNode(Texpr1BinNode.OP_MUL, left, right);
        break;
      case Div: {
        Range divisor = domain.queryRange(expr.getRight().getLinearTerm());
        if (divisor.isZero()) {
          // ARM semantics say division by zero results in zero
          apronExpr = new Texpr1CstNode(toApronScalar(Bound.ZERO));
        } else if (divisor.contains(Bound.ZERO)) {
          // XXX: to implement the ARM semantics, we would need to assign the < 0 part to a temporary and the 0 part
          // and the > 0 part and perform an assignment with each expression and join the result.
          throw new DomainStateException.UnimplementedMethodException();
        } else {
          apronExpr =
            new Texpr1BinNode(Texpr1BinNode.OP_DIV, Texpr1BinNode.RTYPE_INT, Texpr1BinNode.RDIR_ZERO, left, right);
        }
        break;
      }
      case Mod:
        apronExpr =
          new Texpr1BinNode(Texpr1BinNode.OP_MOD, Texpr1BinNode.RTYPE_INT, Texpr1BinNode.RDIR_ZERO, left, right);
        break;
      case Shl: {
        Texpr1BinNode shiftAmount =
          new Texpr1BinNode(Texpr1BinNode.OP_POW, new Texpr1CstNode(toApronScalar(Bound.TWO)), right);
        apronExpr = new Texpr1BinNode(Texpr1BinNode.OP_MUL, left, shiftAmount);
        break;
      }
      case Shr: {
        Texpr1BinNode shiftAmount =
          new Texpr1BinNode(Texpr1BinNode.OP_POW, new Texpr1CstNode(toApronScalar(Bound.TWO)), right);
        apronExpr =
          new Texpr1BinNode(Texpr1BinNode.OP_DIV, Texpr1BinNode.RTYPE_INT, Texpr1BinNode.RDIR_DOWN, left, shiftAmount);
        break;
      }
      default:
        throw new InvariantViolationException();
      }
      return new Texpr1Intern(env, apronExpr);
    }

    @Override public Texpr1Intern visit (Rlin expr, Void data) {
      return toApronExpr(expr, env, variablesMapping);
    }

    @Override public Texpr1Intern visit (bindead.abstractsyntax.zeno.Zeno.RangeRhs expr, Void data) {
      apron.Interval interval = toApronInterval(expr.getRange().convexHull());
      return new Texpr1Intern(env, new Texpr1CstNode(interval));
    }

  }

}
