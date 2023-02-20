package bindead.analyses.warnings;

import rreil.disassembler.Instruction;
import bindead.domainnetwork.channels.WarningMessage;

/**
 * A warning issued by the disassembler indicating that some byte sequence could not be disassembled
 * to a known instruction.
 */
public class UnknownInstructionWarning extends WarningMessage.StateRestrictionWarning {
  private static final String fmt = "Could not disassemble the byte sequence: \"%s\" at address %s.";
  private Instruction insn;
  private String address;

  public UnknownInstructionWarning (Instruction insn) {
    this.insn = insn;
    this.address = insn.address().toShortStringWithHexPrefix();
  }

  @Override public String message () {
    StringBuilder builder = new StringBuilder();
    insn.opcode(builder);
    return String.format(fmt, builder.toString(),  address);
  }
}
