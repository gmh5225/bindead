package bindead.analyses.systems;

import bindead.environment.abi.ABI;

/**
 * Dummy class for RREIL assembler code.
 *
 * @author Bogdan Mihaila
 */
public class RReilSystemModel extends GenericSystemModel {

  public RReilSystemModel (ABI abi) {
    this(ESystemType.RREIL, abi);
  }

  public RReilSystemModel (ESystemType type, ABI abi) {
    super(type, abi);
  }

}
