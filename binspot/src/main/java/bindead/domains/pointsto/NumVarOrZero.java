package bindead.domains.pointsto;

import bindead.data.Linear;
import bindead.data.NumVar;

class NumVarOrZero {
  final NumVar numVar;

  final private static NumVarOrZero zero = new NumVarOrZero();

  NumVarOrZero (NumVar v) {
    assert v != null;
    numVar = v;
  }

  private NumVarOrZero () {
    numVar = null;
  }

  public boolean isConstantZero () {
    return numVar == null;
  }

  static NumVarOrZero zero () {
    return zero;
  }

  public Linear getLinear () {
    if (isConstantZero())
      return Linear.ZERO;
    else
      return Linear.linear(numVar);
  }

  @Override public String toString () {
    if (isConstantZero())
      return "#0";
    else
      return numVar.toString();
  }

  @Override public boolean equals (Object other) {
    if (!(other instanceof NumVarOrZero))
      return false;
    NumVarOrZero o = (NumVarOrZero) other;
    if (isConstantZero())
      return o.isConstantZero();
    if (o.isConstantZero())
      return false;
    return numVar == o.numVar;
  }
}
