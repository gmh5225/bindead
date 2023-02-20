package bindead.analyses.systems.natives;

public class DefinitionId {
  private final String base;
  
  private DefinitionId (String base) {
    this.base = base;
  }
  
  public static DefinitionId valueOf(String base) {
    return new DefinitionId(base);
  }
  
  public String getBase () {
    return base;
  }
  
  @Override public int hashCode () {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((base == null) ? 0 : base.hashCode());
    return result;
  }
  
  @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    DefinitionId other = (DefinitionId) obj;
    if (base == null) {
      if (other.base != null)
        return false;
    } else if (!base.equals(other.base))
      return false;
    return true;
  }

  @Override public String toString () {
    return "DefId: " + base;
  }
}
