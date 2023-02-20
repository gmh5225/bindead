package bindead.domainnetwork.interfaces;

import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.numeric.BigInt;
import javalx.numeric.FiniteRange;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLSet;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Lin;
import rreil.lang.Rhs.Rval;
import rreil.lang.Test;
import rreil.lang.util.Type;
import bindead.abstractsyntax.finite.Finite;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.abstractsyntax.memderef.AbstractPointer;
import bindead.data.MemVarPair;
import bindead.data.MemVarSet;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarSet;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.domainnetwork.channels.RootQueryChannel;
import bindead.domains.segments.heap.PathString;
import bindead.exceptions.DomainStateException;
import bindead.exceptions.Unreachable;

public interface MemoryDomain<D extends MemoryDomain<D>>
    extends RootQueryChannel, SemiLattice<D>, QueryChannel, PrettyDomain {

  /* == Domain transfer functions == */

  public abstract D evalAssign (Lhs lhs, Rhs rhs);

  public abstract D evalLoad (Lhs value, AbstractMemPointer location);

  public abstract D evalStore (AbstractMemPointer location, Lin rhs);

  public abstract D eval (Test test) throws Unreachable;

  public abstract D eval (Finite.Test test) throws Unreachable;

  public abstract D evalFiniteAssign (Finite.Lhs lhs, Finite.Rhs rhs);

  /**
   * Dereference the given pointer. This function partitions the state space
   * such that in each partition, the pointer points to the returned address.
   * In one element of the list, the returned address may be {@code null} indicating that the pointer merely contains
   * values. Each tuple contains a linear expression that can be queried to obtain the offset of the pointer.
   *
   * @param size the size of the pointer in bits
   * @param ptr the pointer expression
   *
   * @return a list of addresses and the state in which the pointer points to this address
   * @throws Unreachable if the pointer only points to the sum of several pointers
   *
   *           deprecated: use {@link #findPointerTargets(Rval)} instead.
   */
  // still used in old Root domain :(
  @Deprecated public abstract List<P2<AbstractPointer, D>> deprecatedDeref (int size, Rval ptr, VarSet summaries)
      throws Unreachable;


  /**
   * Returns a set of possible (abstract) target addresses without restricting the numeric state.
   */
  public abstract List<P2<PathString, AddrVar>> findPossiblePointerTargets (MemVar id) throws Unreachable;

  /**
   * Returns a set of possible (abstract) target addresses and a numeric state.
   * The numeric state is restricted so that ptr is either a scalar (NULL+offset) or a valid pointer.
   * Configurations where ptr contains an invalid pointer are removed from the numeric state and a warning is emitted.
   */
  // XXX hsi: this is problematic, since it exports NumVars to where only MemVars should be known.
  public abstract P3<AVLSet<AddrVar>, D, Rlin> findPointerTargets (Lin ptr) throws Unreachable;

  /* == Domain support functions == */

  /**
   * Substitute all occurrences of {@code x} with {@code y}.
   *
   * @param from the variable to replace
   * @param to the variable to add
   * @return the updated domain state.
   */
  public abstract D substitute (NumVar from, NumVar to);

  /**
   * Introduces a fresh region with identifier {@code region} using the given context.
   *
   * @see {@link RootDomain#introduceRegion(MemVar, RegionCtx)}
   * @param region
   * @param ctx
   * @return The updated domain-state.
   * @throws DomainStateException if region was already introduced in this.
   */
  public abstract D introduceRegion (MemVar region, RegionCtx ctx);

  /**
   * Rename the region "x" to the region "y".
   *
   * @param from the region that is removed
   * @param to the region that is added
   * @return the updated domain state.
   */
  public abstract D substituteRegion (MemVar from, MemVar to);


  /**
   * Remove the given region from the state.
   */
  public abstract D projectRegion (MemVar region);

  /**
   * Introduces a numeric variable with the given type and an (optional) initial value.
   *
   * @param numericVariable A fresh numeric-variable identifier.
   * @param type
   * @return The updated domain-state.
   */
  public abstract D introduce (NumVar numericVariable, Type type, Option<BigInt> value);

  /**
   * Project (remove) the given numeric variable from the state.
   *
   * @param numericVariable
   * @return The updated domain-state.
   */
  public abstract D project (NumVar numericVariable);

  /**
   * Merge the information stored in the two elements of each variable tuple.
   *
   * Semantically, this operation corresponds to adding the ephemeral variables of
   * each tuple in the list of variable pairs, copying this domain D to D',
   * swapping the persistent variable of each variable pair to its ephemeral one in D,
   * perform the join of D and D' and project out the ephemeral variables from the result.
   *
   * @param pairs a list of variable pairs where each pair is to be summarized
   *
   * @param permanent
   *
   * @param ephemeral
   *
   * @return the new domain that does not contain the ephemeral variable of each pair
   */
  //public abstract D fold (FoldMap numVars);


  /**
   * Assign to a variable the given address such that the field denoted by {@code var} "points-to" the symbolic
   * address variable.
   *
   * @param var A left-hand side variable.
   * @param symbolicAddress A symbolic address variable.
   * @return The updated state.
   */
  public abstract D assignSymbolicAddressOf (Lhs var, NumVar symbolicAddress);

  /**
   * if target is an address, pointervalue is assumed to be a pointer to this address.
   * (also turn this region from a summary into a concrete region)
   *
   * if target is null, pointervalue is asumed to be a scalar.
   */
  public abstract D assumePointsToAndConcretize (Lin pointerValue, AddrVar target, MemVar region);

  public abstract D copyMemRegion (MemVar fromVar, MemVar toVar);

  public abstract D copyAndPaste (MemVarSet vars, D other);

  /**
   * Picks the NumVar that holds the valuation of a specific interval, gently
   * ignoring any overlappings. Note that this does _not_ create a field if one cannot
   * be found for the given access.
   */
  public abstract Option<NumVar> pickSpecificField (MemVar region, FiniteRange access);

  public abstract MemVarSet getSupportSet ();

  public abstract D assumeEdgeNG (Lin fieldThatPoints, AddrVar address);

  public abstract D expandNG (List<MemVarPair> mvps);

  public abstract D expandNG (AddrVar address, AddrVar address2, List<MemVarPair> mvps);

  public abstract D foldNG (List<MemVarPair> mvps);

  public abstract D foldNG (AddrVar address, AddrVar address2, List<MemVarPair> mvps);

  public abstract D bendGhostEdgesNG (AddrVar summary, AddrVar concrete, MemVarSet sContents, MemVarSet cContents, MemVarSet pointingToSummary, MemVarSet pointingToConcrete);

  public abstract D bendBackGhostEdgesNG (AddrVar summary, AddrVar concrete, MemVarSet sContents, MemVarSet cContents, MemVarSet pointingToSummary, MemVarSet pointingToConcrete);

  public abstract D concretizeAndDisconnectNG (AddrVar summary, AVLSet<MemVar> concreteNodes);

  public abstract Range queryPtsEdge (MemVar from, BigInt prefix, int size, AddrVar to);

  public abstract D assumeRegionsAreEqual (MemVar first, MemVar second);
}
