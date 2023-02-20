package bindead.domains.pointsto;

import javalx.data.products.P2;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.abstractsyntax.finite.util.FiniteTestSimplifier;
import bindead.data.Linear;
import bindead.data.NumVar.AddrVar;
import bindead.domainnetwork.channels.WarningsContainer;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.exceptions.Unreachable;

/*
 * new implementation
 * */
class TestTranslator<D extends FiniteDomain<D>> {
  private final FiniteFactory fin = FiniteFactory.getInstance();
  private final boolean DEBUG = PointsToProperties.INSTANCE.debugOther.isTrue();

  private final PointsToStateBuilder<D> builder;
  private final WarningsContainer warningsChannel;

  TestTranslator (PointsToStateBuilder<D> builder, WarningsContainer wc) {
    assert builder != null;
    this.builder = builder;
    this.warningsChannel = wc;
  }

  @SuppressWarnings("unused") private void msg (String s) {
    if (DEBUG) {
      System.out.println("TestTranslator: " + s);
    }
  }

  D resulting = null;

  D run (D remaining, Test test, HyperPointsToSet left, HyperPointsToSet right) {
    Linear leftOffset = left.getLinearOffset().getLinearTerm();
    Linear rightOffset = right.getLinearOffset().getLinearTerm();

    resulting = null;
    remaining = splitoffNegativeFlags(left, remaining);
    remaining = splitoffNegativeFlags(right, remaining);

    Linear allFlagSum = left.sumOfFlags.add(right.sumOfFlags);
    D scalarState = statesWithZeroFlagsOnly(remaining, allFlagSum);
    //msg("state only with zero flags " + scalarState);
    Test wrappingTest = test.build(leftOffset, rightOffset);
    //msg("testing " + wrappingTest);
    scalarState = runTest(wrappingTest, scalarState);
    //msg("scalarstate " + scalarState);
    Test flagsTest = fin.notEqualToZero(fin.linear(0, allFlagSum));
    D nonscalarState = null;
    nonscalarState = runTest(flagsTest, remaining);
    //msg("nonscalarstate " + nonscalarState);
    if (nonscalarState != null) {
      Rlin difference = fin.linear(test.getSize(), test.toLinear());
      HyperPointsToSet hpts = builder.translate(test.getSize(), difference, warningsChannel);
      for (P2<AddrVar, Linear> p : hpts.coefficients) {
        Linear x = p._2();
        resulting = maybeJoin(resulting, runTest(fin.notEqualToZero(fin.linear(0, x)), nonscalarState));
        nonscalarState = statesWithZeroFlagsOnly(nonscalarState, x);
      }
      nonscalarState = runTest(test.build(0, leftOffset, rightOffset), nonscalarState);
    }
    resulting = maybeJoin(resulting, scalarState);
    resulting = maybeJoin(resulting, nonscalarState);
    return resulting;
  }

  private D statesWithZeroFlagsOnly (D s, Linear l) {
    D s2 = runTest(fin.equalToZero(fin.linear(0, l)), s);
    // we might improve the approximation by also testing flags == 0
    return s2;
  }

  private D splitoffNegativeFlags (HyperPointsToSet hpts, D s) {
    //msg("bogoize " + hpts);
    for (P2<AddrVar, Linear> p : hpts.getTerms()) {
      Linear x = p._2();
      s = runTest(fin.unsignedLessThanOrEqualTo(0, Linear.ZERO, x), s);
      D bogo = runTest(fin.unsignedLessThan(0, x, Linear.ZERO), s);
      resulting = maybeJoin(resulting, bogo);
    }
    return s;
  }

  private D runTest (Test test, D state) {
    if (state == null)
      return null;
    try {
      if (!FiniteTestSimplifier.isTautologyReportUnreachable(test))
        return state.eval(test);
    } catch (Unreachable u) {
      return null;
    }
    return state;
  }

  D maybeJoin (D a, D b) {
    if (a == null)
      return b;
    else if (b == null)
      return a;
    else
      return a.join(b);
  }
}
