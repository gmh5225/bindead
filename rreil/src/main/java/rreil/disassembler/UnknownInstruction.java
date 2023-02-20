package rreil.disassembler;

import java.util.Collections;
import java.util.List;

import rreil.disassembler.translators.common.TranslationException;
import rreil.lang.RReilAddr;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;

/**
 * Represents byte sequences that are unknown and cannot be disassembled to an instruction.
 *
 * @author Bogdan Mihaila
 */
public class UnknownInstruction extends Instruction {
  private static final String mnemonic = "<unknown>";
  private static final LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public UnknownInstruction (long address, byte[] opcode) {
    super(address, opcode, mnemonic, Collections.<OperandTree>emptyList());
  }

  @Override public List<LowLevelRReil> toRReilInstructions () throws TranslationException {
    LowLevelRReil insn = factory.NATIVE(RReilAddr.valueOf(baseAddress()), this);
    return Collections.singletonList(insn);
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append(String.format("%-6s", mnemonic()));
    builder.append(" ");
    return builder.toString();
  }

}
