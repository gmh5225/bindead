package bindead.domainnetwork.interfaces;

import bindead.data.FoldMap;
import bindead.data.NumVar;
import bindead.data.VarSet;

/**
 * Summarization for numeric domains. An interface that provides the fold and expand function necessary to
 * summarize the content of several memory cells.
 *
 * @author Axel Simon
 */
public interface Summarization<D extends Summarization<D>> {

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
  public D expand (FoldMap pairs);

  /**
   * Merge the information stored in the two elements of each variable tuple.
   *
   * Semantically, this operation corresponds to copying this domain D to D',
   * swapping the persistent variable of each variable pair to its ephemeral one in D,
   * perform the join of D and D' and project out the ephemeral variables from the result.
   *
   * @param pairs a list of variable pairs where each pair is to be summarized
   * @return the new domain that does not contain the ephemeral variable of each pair
   */
  public D fold (FoldMap pairs);

  /**
   * Copy the state stored for the given variables in the /from/ domain into {@code this} domain.
   *
   * @param vars the variables that are to be copied to this domain (the
   *          support set is extended by these variables, i.e. the variables must not be present in this state)
   * @param from the domain from which to copy the state (the support set of
   *          the /from/ domain must include /vars/)
   * @return the new domain that holds information over /vars/
   */
  public D copyAndPaste (VarSet vars, D from);

  /**
   * Inform the domain that the variable is going to be treated as a concrete non-summarized value.
   *
   * @param var the variable whose summarization metadata may be cleaned
   */
  public D assumeConcrete (NumVar var);


}
