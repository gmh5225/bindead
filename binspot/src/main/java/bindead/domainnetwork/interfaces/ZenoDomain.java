package bindead.domainnetwork.interfaces;

import javalx.data.Option;
import javalx.numeric.BigInt;
import rreil.lang.util.Type;
import bindead.abstractsyntax.zeno.Zeno;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.exceptions.Unreachable;

public interface ZenoDomain<D extends ZenoDomain<D>>
    extends SemiLattice<D>, Summarization<D>, QueryChannel, PrettyDomain {

  public abstract D eval (Zeno.Assign stmt);

  public abstract D eval (Zeno.Test test) throws Unreachable;

  /**
   * Introduces a numeric variable with the given type and an (optional) initial value.
   * The variable must not exist already in the domain.
   *
   * @param numericVariable A fresh numeric-variable identifier.
   * @param type
   * @return The updated domain-state.
   */
  public abstract D introduce (NumVar variable, Type type, Option<BigInt> value);

  /**
   * Drop (project-out) {@code variable} from this domains support set.
   *
   * @param variable
   * @return The updated domain-state.
   */
  public abstract D project (VarSet vars);

  /**
   * Substitute all occurrences of {@code x} with {@code y}. The variable {@code y} must not exist already in the domain.
   *
   * @param x the variable to replace
   * @param y the variable to add
   * @return The updated domain-state.
   */
  public abstract D substitute (NumVar x, NumVar y);
}
