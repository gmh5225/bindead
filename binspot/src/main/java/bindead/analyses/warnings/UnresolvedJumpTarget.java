package bindead.analyses.warnings;

import rreil.lang.RReil;
import bindead.domainnetwork.channels.WarningMessage;

/**
 * A warning issued by the disassembler algorithm indicating that some branch targets for a jump
 * could not be resolved.
 */
public class UnresolvedJumpTarget extends WarningMessage.StateRestrictionWarning {
  private static final String fmt = "Could not resolve the jump targets for instruction: %s %s.";
  private RReil insn;
  private String address;

  public UnresolvedJumpTarget (RReil insn) {
    this.insn = insn;
    this.address = insn.getRReilAddress().toShortStringWithHexPrefix();
  }

  @Override public String message () {
    return String.format(fmt, address, insn);
  }
}
