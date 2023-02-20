package bindead.domainnetwork.interfaces;

import java.util.Collection;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLSet;
import rreil.lang.util.Type;
import bindead.abstractsyntax.finite.Finite.Assign;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarSet;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.exceptions.Unreachable;

/**
 * Interface for {@code finite} domains, that are domains handling fixed-size arithmetic.
 *
 * @param <D> The self-type for binary operations
 */
public interface FiniteDomain<D extends FiniteDomain<D>>
    extends SemiLattice<D>, QueryChannel, PrettyDomain {

  /* == Domain transfer functions == */

  public abstract D eval (Test test) throws Unreachable;

  public abstract D eval (Assign stmt);


  /*
   * make an exact copy of a variable
   * the difference to eval(Assign) is that it also works for summary elements
   */
  public abstract D copyVariable (NumVar to, NumVar from);

  /**
   * Dereference the given pointer. This function partitions the state space
   * such that in each partition, the pointer points to the returned address.
   * In one element of the list, the returned address may be {@code null} indicating that the pointer merely contains
   * values.
   *
   * @param ptr the pointer expression
   * @return a list of addresses and the state in which the pointer points to this address
   * @throws Unreachable if the pointer only points to the sum of several pointers
   */
  // still used in old Root domain :(
  @Deprecated public abstract List<P2<AddrVar, D>> deprecatedDeref (Rlin ptr, VarSet summaries) throws Unreachable;


  /**
   * The returned state is clean in the sense that states with illegal pointers are cut off
   * and turned into the respective warnings
   *
   * @return set of possible addresses (null address is implicit) and a state
   *         with sumOfFlags and all flags in [0,1], together with proper warnings
   * */
  public abstract P2<AVLSet<AddrVar>, D> deref (Rlin ptr) throws Unreachable;

  /**
   * Introduces a numeric variable with the given type and an (optional) initial value.
   *
   * @param variable A fresh numeric-variable identifier.
   * @param type The type of the numeric-variable
   * @param value An optional initial value for the variable, otherwise the variable will be TOP
   * @return The updated domain-state.
   */
  public abstract D introduce (NumVar variable, Type type, Option<BigInt> value);

  /**
   * Drop (project-out) {@code variable} from this domain's support set.
   *
   * @param variable
   * @return The updated domain-state.
   */
  public abstract D project (NumVar variable);

  /**
   * Substitute all occurrences of {@code x} with {@code y}.
   *
   * @param from the variable to replace
   * @param to the variable to add
   * @return The updated domain-state.
   */
  public abstract D substitute (NumVar from, NumVar to);

  /**
   * @param reference the variable that has to point to target
   * @param target the address that is pointed to (or NULL to force scalar value)
   * @param contents the numeric contents of the pointed-to region that have to be concretized (that is, ghost-nodes and
   *          other summary-related stuff is deleted from the points-to sets)
   * */
  public abstract D assumePointsToAndConcretize (Rlin reference, AddrVar target, VarSet contents);

  /**
   * Returns a set of possible (abstract) target addresses without restricting the numeric state.
   */
  public abstract Collection<AddrVar> findPossiblePointerTargets (NumVar id);

  public abstract Range queryEdgeFlag (NumVar src, AddrVar tgt);

  public abstract D assumeEdgeFlag (NumVar refVar, AddrVar target, BigInt value);

  public abstract D assumeEdgeNG (Rlin pointerVar, AddrVar targetAddr);

  /**
   * Duplicate the information stored on the permanent variables of the
   * variable pairs.
   *
   * Semantically, this operation corresponds to adding all ephemeral variables
   * in {@code vars} to the domain. Then copy the
   * domain D to D' and swap the variables of each pair. Then perform a meet on
   * D and D' and return the result.
   *
   * @param pairs a list of variable pairs in which each permanent variable is
   *          to be expanded to the ephemeral variable in that pair
   * @return the new domain in which the given variables are duplicated
   */
  public abstract D expandNG (ListVarPair nvps);

  public abstract D expandNG (AddrVar p, AddrVar e, ListVarPair nvps);

  /**
   * Merge the information stored in the two elements of each variable tuple.
   *
   * Semantically, this operation corresponds to copying this domain D to D',
   * swapping the persistent variable of each variable pair to its ephemeral one in D,
   * perform the join of D and D' and project out the ephemeral variables from the result.
   *
   * @param nvps a list of variable pairs where each pair is to be summarized
   * @return the new domain that does not contain the ephemeral variable of each pair
   */
  public abstract D foldNG (ListVarPair nvps);

  public abstract D foldNG (AddrVar p, AddrVar e, ListVarPair nvps);

  public abstract D concretizeAndDisconnectNG (AddrVar summary, VarSet concreteVars);

  public abstract D bendBackGhostEdgesNG (AddrVar summary, AddrVar concrete, VarSet svs, VarSet cvs, VarSet pts, VarSet ptc);

  public abstract D bendGhostEdgesNG (AddrVar summary, AddrVar concrete, VarSet svs, VarSet cvs, VarSet pts, VarSet ptc);

  /**
   * Copy the state stored for the given variables in the /from/ domain into {@code this} domain.
   *
   * @param vars the variables that are to be copied to this domain (the
   *          support set is extended by these variables, i.e. the variables must not be present in this state)
   * @param from the domain from which to copy the state (the support set of
   *          the /from/ domain must include /vars/)
   * @return the new domain that holds information over /vars/
   */
  public abstract D copyAndPaste (VarSet vars, D from);

  /**
   * Inform the domain that the variable is going to be treated as a concrete non-summarized value.
   *
   * @param var the variable whose summarization metadata may be cleaned
   */
  public abstract D assumeConcrete (NumVar var);

  public abstract D assumeVarsAreEqual(int size, NumVar fst, NumVar snd);

}
