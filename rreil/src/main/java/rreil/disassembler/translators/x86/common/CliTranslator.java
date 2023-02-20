package rreil.disassembler.translators.x86.common;

import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;

public class CliTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {

    env.setCurrentInstruction(instruction);

    instructions.add(
        // According to the intel spec, this is only correct when protected-mode virtual interrupts are not enabled.
        factory.MOV(env.getNextReilAddress(), X86Helpers.INTERRUPT_FLAG_OPERAND, factory.immediate(OperandSize.BIT, 0)));
  }
}
