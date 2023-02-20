/**
 * A pair of variables used during folding and expanding of abstract states.
 */
package bindead.data;

import rreil.lang.MemVar;

/**
 * A pair of variables used during folding and expanding of abstract states.
 *
 * @author Holger Siegel
 *
 */
public class MemVarPair extends FoldPair<MemVar> {

  public MemVarPair (MemVar permanent, MemVar ephemeral) {
    super(permanent, ephemeral);
  }
}
