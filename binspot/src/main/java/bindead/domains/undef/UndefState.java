package bindead.domains.undef;

import javalx.data.products.P2;
import javalx.persistentcollections.AVLMap;
import bindead.data.NumVar;
import bindead.data.NumVar.FlagVar;
import bindead.data.VarSet;
import bindead.debug.PrettyDomain;
import bindead.debug.XmlPrintHelpers;
import bindead.domainnetwork.interfaces.FunctorState;

import com.jamesmurty.utils.XMLBuilder;

class UndefState extends FunctorState {
  private static final UndefState EMPTY = new UndefState();
  protected final VarSet undefined; // variables that are undefined, i.e. the associated flag is false
  protected final AVLMap<FlagVar, VarSet> partitions;
  protected final AVLMap<NumVar, FlagVar> reverse;

  private UndefState () {
    this(VarSet.empty(), AVLMap.<FlagVar, VarSet>empty(),
        AVLMap.<NumVar, FlagVar>empty());
  }

  protected UndefState (VarSet undefined, AVLMap<FlagVar, VarSet> partitions, AVLMap<NumVar, FlagVar> reverse) {
    this.undefined = undefined;
    this.partitions = partitions;
    this.reverse = reverse;
    assert isConsistent();
  }

  public static UndefState empty () {
    return EMPTY;
  }

  /**
   * Only for debugging purpose. Can be inserted in assertions to check if the mappings in this state are consistent.
   */
  private boolean isConsistent () {
    for (P2<FlagVar, VarSet> tuple : partitions) {
      FlagVar flag = tuple._1();
      VarSet variables = tuple._2();
      if (undefined.containsAll(variables))
        return false;
      for (NumVar variable : variables) {
        if (!reverse.contains(variable))
          return false;
        if (!reverse.get(variable).get().equalTo(flag))
          return false;
      }
    }
    for (P2<NumVar, FlagVar> tuple : reverse) {
      NumVar variable = tuple._1();
      FlagVar flag = tuple._2();
      if (undefined.contains(variable))
        return false;
      if (!partitions.contains(flag))
        return false;
      if (!partitions.get(flag).get().contains(variable))
        return false;
    }
    return true;
  }

  /**
   * Return from the given variables the ones being owned by this domain, i.e. the intersection of this domain's flags
   * and the passed in variables.
   *
   * @param vars the variables to be filtered
   * @return the subset of {@code vars} that are flags in this domain
   */
  public VarSet getFlagVars (VarSet vars) {
    VarSet res = VarSet.empty();
    for (NumVar var : vars)
      if (var instanceof FlagVar && partitions.contains((FlagVar) var))
        res = res.add(var);
    return res;
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    builder = builder.e(Undef.NAME);
    if (!undefined.isEmpty()) {
      builder = builder.e("Entry").a("type", "Always");
      builder = XmlPrintHelpers.variableSet(builder, undefined);
      builder = builder.up();
    }
    for (P2<FlagVar, VarSet> binding : partitions) {
      builder = builder.e("Entry").a("type", "Conditionally");
      builder = builder.e("Flag").t(binding._1().toString()).up();
      builder = XmlPrintHelpers.variableSet(builder, binding._2());
      builder = builder.up();
    }
    builder = builder.up();
    return builder;
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("#");
    if (!undefined.isEmpty())
      builder.append(partitions.size() + 1);
    else
      builder.append(partitions.size());
    builder.append(" ");
    builder.append("{");
    String separator = "";
    if (!undefined.isEmpty()) {
      builder.append("always: ");
      builder.append(undefined);
      separator = ", ";
    }
    for (P2<FlagVar, VarSet> binding : partitions) {
      builder.append(separator);
      builder.append("if ");
      builder.append(binding._1());
      builder.append(": ");
      builder.append(binding._2());
      separator = ", ";
    }
    builder.append("}");
    return builder.toString();
  }

  @Override public void toCompactString (String domainName, StringBuilder builder, PrettyDomain childDomain) {
  }
}
