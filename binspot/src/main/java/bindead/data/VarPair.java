package bindead.data;

/**
 * A pair of variables used during folding and expanding of abstract states.
 *
 * @author Holger Siegel
 */
public class VarPair extends FoldPair<NumVar> {

  public VarPair (NumVar permanent, NumVar ephemeral) {
    super(permanent, ephemeral);
  }
}
