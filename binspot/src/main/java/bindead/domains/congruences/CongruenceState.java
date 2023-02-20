package bindead.domains.congruences;

import java.util.Iterator;
import java.util.Set;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Congruence;
import javalx.persistentcollections.AVLMap;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.debug.PrettyDomain;
import bindead.debug.StringHelpers;
import bindead.domainnetwork.interfaces.FunctorState;

import com.jamesmurty.utils.XMLBuilder;

public class CongruenceState extends FunctorState {
  public static final CongruenceState EMPTY = new CongruenceState(AVLMap.<NumVar, Congruence>empty());
  protected final AVLMap<NumVar, Congruence> congruences;

  public CongruenceState (AVLMap<NumVar, Congruence> congruence) {
    this.congruences = congruence;
  }

  /**
   * Insert all relevant congruences into the given expression.
   *
   * @param con the expression
   * @param d the divisor with which the expression was scaled down (may be {@code null})
   * @return a new expression that contains scaled variables according to the congruences
   */
  public Linear inlineIntoLinear (Linear con, Linear.Divisor d) {
    return inlineIntoLinear(con, d, congruences);
  }

  /**
   * Insert all relevant congruences into the given expression.
   *
   * @param linear the expression
   * @param d the divisor with which the expression was scaled down (may be {@code null})
   * @param congruences the map of congruences that are to be inlined
   * @return a new expression that contains scaled variables according to the congruences
   */
  public static Linear inlineIntoLinear (Linear linear, Linear.Divisor d, AVLMap<NumVar, Congruence> congruences) {
    Linear result = linear;
    for (NumVar x : linear.getVars()) {
      Option<Congruence> c = congruences.get(x);
      // replace x with bx+a => add a*coeff(x) to the linear expression and multiply the coefficient of x with b
      if (c.isSome()) {
        Congruence congruence = c.get();
        BigInt coeff = result.getCoeff(x);
        result = result.dropTerm(x);
        result = result.add(congruence.getOffset().mul(coeff));
        if (!congruence.getScale().isZero())
          result = result.addTerm(coeff.mul(congruence.getScale()), x);
      }
    }
    if (d != null)
      result = result.lowestForm(d);
    return result;
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    builder = builder.e(Congruences.NAME);
    for (P2<NumVar, Congruence> varAndValue : congruences) {
      Congruence value = varAndValue._2();
      builder = builder.e("Entry")
          .a("type", "congruence")
          .e("Variable")
          .t(varAndValue._1().toString())
          .up()
          .e("Value")
          .e("multiplier")
          .t(value.getScale().toString())
          .up()
          .e("offset")
          .t(value.getOffset().toString())
          .up()
          .up()
          .up();
    }
    return builder.up();
  }

  @Override public String toString () {
    return "#" + congruences.size() + " " + contentToString();
  }

  private String contentToString () {
    Set<NumVar> sorted = StringHelpers.sortLexically(congruences.keys());
    Iterator<NumVar> iterator = sorted.iterator();
    StringBuilder builder = new StringBuilder();
    builder.append('{');
    while (iterator.hasNext()) {
      NumVar key = iterator.next();
      Congruence value = congruences.getOrNull(key);
      builder.append(key);
      if (value.isConstantOnly()) {
        builder.append("=");
        builder.append(value.getOffset());
      } else {
        builder.append(value);
      }
      if (iterator.hasNext())
        builder.append(", ");
    }
    return builder.append('}').toString();
  }

  @Override public void toCompactString (String domainName, StringBuilder builder, PrettyDomain childDomain) {
    // nop
  }

  public void appendVar (NumVar var, StringBuilder builder, PrettyDomain childDomain) {
    Congruence value = congruences.getOrNull(var);
    // if (value.getScale().isOne() && value.getOffset().isZero())
    // continue;
    if (value.isConstantOnly()) {
      builder.append(value.getOffset());
    } else {
      childDomain.varToCompactString(builder, var);
      if (!value.getScale().isOne() || !value.getOffset().isZero())
        builder.append(value);
    }
  }
}
