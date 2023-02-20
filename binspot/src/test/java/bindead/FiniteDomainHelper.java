package bindead;

import static bindead.data.Linear.linear;
import static bindead.data.Linear.num;
import static bindead.data.Linear.term;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static rreil.lang.util.Type.Zeno;
import javalx.data.Option;
import javalx.numeric.BigInt;
import javalx.numeric.Congruence;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import rreil.lang.BinOp;
import bindead.abstractsyntax.finite.Finite.Bin;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.Linear;
import bindead.data.Linear.Term;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.interfaces.FiniteDomain;

/**
 * Collection of methods to ease the manipulation of the finite domain for tests.
 *
 * @author Bogdan Mihaila
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class FiniteDomainHelper {
  private static final FiniteFactory finite = FiniteFactory.getInstance();

  /**
   * Wrap a finite domain instance such that all operations on variables will assume a 32 bit variable size.
   */
  public static RichFiniteDomain for32bitVars (FiniteDomain domain) {
    return new RichFiniteDomain(domain, 32);
  }

  /**
   * Wrap a finite domain instance such that all operations on variables will assume a 64 bit variable size.
   */
  public static RichFiniteDomain for64bitVars (FiniteDomain domain) {
    return new RichFiniteDomain(domain, 64);
  }

  /**
   * Wrap a finite domain instance such that all operations on variables will assume the given variables size in bits.
   */
  public static RichFiniteDomain forNbitVars (FiniteDomain domain, int variablesSize) {
    return new RichFiniteDomain(domain, variablesSize);
  }

  public static class RichFiniteDomain {
    private final FiniteDomain wrappedDomain;
    private final int variablesSize;

    private RichFiniteDomain (FiniteDomain wrappedDomain, int variablesSize) {
      this.wrappedDomain = wrappedDomain;
      this.variablesSize = variablesSize;
    }

    private RichFiniteDomain build (FiniteDomain domainToWrap) {
      return new RichFiniteDomain(domainToWrap, variablesSize);
    }

    public FiniteDomain getWrappedDomain () {
      return wrappedDomain;
    }

    public RichFiniteDomain join (RichFiniteDomain other) {
      assert this.variablesSize == other.variablesSize : "Joined domains do not have variables of the same bit size.";
      return build((FiniteDomain) wrappedDomain.join(other.wrappedDomain));
    }

    public RichFiniteDomain widen (RichFiniteDomain other) {
      assert this.variablesSize == other.variablesSize : "Joined domains do not have variables of the same bit size.";
      return build((FiniteDomain) wrappedDomain.widen(other.wrappedDomain));
    }

    public boolean subsetOrEqual (RichFiniteDomain other) {
      return wrappedDomain.subsetOrEqual(other.wrappedDomain);
    }

    public RichFiniteDomain fold (ListVarPair pairs) {
      return build(wrappedDomain.foldNG(pairs));
    }

    public RichFiniteDomain expandNG (ListVarPair pairs) {
      return build(wrappedDomain.expandNG(pairs));
    }

    /**
     * Copy and paste the state of variables from another domain to this domain.
     * @param copyVars Variables to be copied to this domain.
     * @param from The state from which the variables should be copied
     */
    public RichFiniteDomain copyAndPaste (VarSet copyVars, RichFiniteDomain from) {
      return build(wrappedDomain.copyAndPaste(copyVars, from.wrappedDomain));
    }

    /**
     * Assign the division of two variables to a variable. {@code lhs = dividend / divisor}
     */
    public RichFiniteDomain assignDivision (NumVar lhs, NumVar divident, NumVar divisor) {
      Bin rhs = finite.binary(finite.linear(variablesSize, divident), BinOp.Divs, finite.linear(variablesSize, divisor));
      return build(wrappedDomain.eval( finite.assign(finite.variable(variablesSize, lhs), rhs)));
    }

    /**
     * Assign a sum of variables (linear term) to a variable. {@code y = x1 + ... + xn}
     */
    public RichFiniteDomain assign (NumVar variable, NumVar... variables) {
      assert variables.length > 0 : "Right hand side of assignment " + variable + " = ... must not be empty.";
      Term[] variablesTerms = new Term[variables.length];
      for (int i = 0; i < variables.length; i++) {
        variablesTerms[i] = term(variables[i]);
      }
      return build(wrappedDomain.eval(
          finite.assign(finite.variable(variablesSize, variable),
              finite.linear(variablesSize, linear(num(0), variablesTerms)))));
    }

    /**
     * Assign a variable with a coefficient to a variable. {@code y = c*x}
     */
    public RichFiniteDomain assign (NumVar lhsVariable, long coefficient, NumVar rhsVariable) {
      return build(wrappedDomain.eval(
          finite.assign(finite.variable(variablesSize, lhsVariable),
              finite.linear(variablesSize, linear(term(num(coefficient), rhsVariable))))));
    }

    /**
     * Assign a value to a variable. {@code y = c}
     */
    public RichFiniteDomain assign (NumVar lhsVariable, long value) {
      return build(wrappedDomain.eval(
          finite.assign(finite.variable(variablesSize, lhsVariable),
              finite.linear(variablesSize, linear(num(value))))));
    }

    /**
     * Assign an interval to a variable.
     */
    public RichFiniteDomain assign (NumVar variable, Interval value) {
      return build(wrappedDomain.eval(
          finite.assign(finite.variable(variablesSize, variable), finite.range(variablesSize, value))));
    }

    /**
     * Assign a linear term to a variable. {@code y = c0 + c1*x1 + ... + cn*xn}
     */
    public RichFiniteDomain assign (NumVar lhsVariable, Linear linear) {
      return build(wrappedDomain.eval(
          finite.assign(finite.variable(variablesSize, lhsVariable), finite.linear(variablesSize, linear))));
    }

    /**
     * Introduce a variable without an initial value.
     */
    public RichFiniteDomain introduce (NumVar variable) {
      return build(wrappedDomain.introduce(variable, Zeno, Option.none()));
    }

    /**
     * Introduce a variable with an initial value.
     */
    public RichFiniteDomain introduce (NumVar variable, long value) {
      return build(wrappedDomain.introduce(variable, Zeno, Option.some(BigInt.of(value))));
    }

    /**
     * Introduce a variable with an initial value.
     */
    public RichFiniteDomain introduce (NumVar variable, BigInt value) {
      return build(wrappedDomain.introduce(variable, Zeno, Option.fromNullable(value)));
    }

    /**
     * Introduce a variable with an initial value. The variable is first introduced with TOP and then assigned the value.
     */
    public RichFiniteDomain introduce (NumVar variable, Interval value) {
      return introduce(variable).assign(variable, value);
    }

    public RichFiniteDomain project (NumVar variable) {
      return build(wrappedDomain.project(variable));
    }

    public RichFiniteDomain substitute (NumVar from, NumVar to) {
      return build(wrappedDomain.substitute(from, to));
    }

    /**
     * Evaluate a test of the form {@code c1 = c2}. It compares two constants to each it other. Useful to test certain
     * code paths in your domains that deal with constants.
     */
    public RichFiniteDomain equalTo (long value1, long value2) {
      return build(wrappedDomain.eval(finite.equalTo(variablesSize, linear(num(value1)), linear(num(value2)))));
    }

    /**
     * Evaluate a test of the form {@code x = c}
     */
    public RichFiniteDomain equalTo (NumVar variable, long value) {
      return build(wrappedDomain.eval(finite.equalTo(variablesSize, linear(variable), linear(num(value)))));
    }

    /**
     * Evaluate a test of the form {@code y = x1 + ... + xn}
     */
    public RichFiniteDomain equalTo (NumVar variable, NumVar... variables) {
      assert variables.length > 0;
      Term[] variablesTerms = new Term[variables.length];
      for (int i = 0; i < variables.length; i++) {
        variablesTerms[i] = term(variables[i]);
      }
      return build(wrappedDomain.eval(finite.equalTo(variablesSize, linear(variable), linear(num(0), variablesTerms))));
    }

    /**
     * Evaluate a test of the form {@code x != c}
     */
    public RichFiniteDomain notEqualTo (NumVar variable, long value) {
      return build(wrappedDomain.eval(finite.notEqualTo(variablesSize, linear(variable), linear(num(value)))));
    }

    /**
     * Evaluate a test of the form {@code y != x1 + ... + xn}
     */
    public RichFiniteDomain notEqualTo (NumVar variable, NumVar... variables) {
      assert variables.length > 0;
      Term[] variablesTerms = new Term[variables.length];
      for (int i = 0; i < variables.length; i++) {
        variablesTerms[i] = term(variables[i]);
      }
      return build(wrappedDomain.eval(
          finite.notEqualTo(variablesSize, linear(variable), linear(num(0), variablesTerms))));
    }

    /**
     * Evaluate a test of the form {@code c <= x}. Note that the comparison is signed!
     */
    public RichFiniteDomain lessOrEqualTo (long value, NumVar variable) {
      return build(wrappedDomain.eval(
          finite.signedLessThanOrEqualTo(variablesSize, linear(num(value)), linear(variable))));
    }

    /**
     * Evaluate a test of the form {@code x <= c}. Note that the comparison is signed!
     */
    public RichFiniteDomain lessOrEqualTo (NumVar variable, long value) {
      return build(wrappedDomain.eval(
          finite.signedLessThanOrEqualTo(variablesSize, linear(variable), linear(num(value)))));
    }

    /**
     * Evaluate a test of the form {@code c * x <= d}. Note that the comparison is signed!
     */
    public RichFiniteDomain lessOrEqualTo (long coefficient, NumVar variable, long value) {
      return build(wrappedDomain.eval(
          finite.signedLessThanOrEqualTo(variablesSize, linear(num(coefficient), variable), linear(num(value)))));
    }

    /**
     * Evaluate a test of the form {@code y <= x1 + ... + xn}. Note that the comparison is signed!
     */
    public RichFiniteDomain lessOrEqualTo (NumVar variable, NumVar... variables) {
      assert variables.length > 0;
      Term[] variablesTerms = new Term[variables.length];
      for (int i = 0; i < variables.length; i++) {
        variablesTerms[i] = term(variables[i]);
      }
      return build(wrappedDomain.eval(
          finite.signedLessThanOrEqualTo(variablesSize, linear(variable), linear(num(0), variablesTerms))));
    }

    /**
     * Assert that a variable has a certain value.
     */
    public void assertValueIs (NumVar variable, long value) {
      Range range = wrappedDomain.queryRange(linear(term(variable)));
      assertThat("The tested value is not " + value + " but " + range, range.isConstant(), is(true));
      assertThat(range.getConstantOrNull(), is(BigInt.of(value)));
    }

    /**
     * Assert that a variable has a certain value.
     */
    public void assertValueIs (NumVar variable, Interval value) {
      assertThat(wrappedDomain.queryRange(linear(term(variable))).convexHull(), is(value));
    }

    /**
     * Assert that a variable has a certain congruence.
     */
    public void assertValueIsCongruent (NumVar variable, Congruence congruence) {
      assertThat(wrappedDomain.queryRange(linear(term(variable))).getCongruence(), is(congruence));
    }

    /**
     * Assert that a variable has a linear equalities relation to another variable.
     */
    public void assertEqualityRelationExists (NumVar x1, NumVar x2) {
      VarSet varsInRelation = VarSet.empty();
      for (Linear equality : wrappedDomain.queryEqualities(x1)) {
        varsInRelation = varsInRelation.union(equality.getVars());
      }
      assertThat(varsInRelation.contains(x2), is(true));
    }

    @Override public String toString () {
      return wrappedDomain.toString();
    }
  }
}
