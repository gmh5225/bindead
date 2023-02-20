package bindead.domains.fields;

import bindead.data.NumVar;

/**
 * Maintain the context in which a (finite) numeric variable is used.
 */
public class VariableCtx {
  // hsi: size is also implicit in fields tree.
  private final int size;
  private final NumVar variable;

  public VariableCtx (int size, NumVar variable) {
    assert variable != null : "invalid variable context";
    this.size = size;
    this.variable = variable;
  }

  public int getSize () {
    return size;
  }

  public NumVar getVariable () {
    return variable;
  }

  @Override public String toString () {
    return variable.toString() + ':' + size;
  }

  @Override public boolean equals (Object obj) {
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final VariableCtx other = (VariableCtx) obj;
    if (this.size != other.size)
      return false;
    if (this.variable != other.variable && (this.variable == null || !this.variable.equalTo(other.variable)))
      return false;
    return true;
  }

  @Override public int hashCode () {
    int hash = 7;
    hash = 97 * hash + this.size;
    hash = 97 * hash + (this.variable != null ? this.variable.hashCode() : 0);
    return hash;
  }
}
