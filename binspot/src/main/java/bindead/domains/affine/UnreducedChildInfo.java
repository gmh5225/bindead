package bindead.domains.affine;

import javalx.numeric.Range;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.domainnetwork.channels.WarningMessage;

/**
 * A warning issued by the {@link Affine} domain indicating earlier precision loss in child domains.
 */
public class UnreducedChildInfo extends WarningMessage.Info {
  private static final String fmt = "Reducing child with test %s yielded ⊥ as value in child is %s";
  private final Test reductionTest;
  private final Range childValue;

  public UnreducedChildInfo (Test reductionTest, Range childValue) {
    this.reductionTest = reductionTest;
    this.childValue = childValue;
  }

  @Override public String message () {
    return String.format(fmt, reductionTest.toSimpleString(), childValue);
  }
}
