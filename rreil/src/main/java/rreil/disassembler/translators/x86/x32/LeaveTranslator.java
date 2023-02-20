package rreil.disassembler.translators.x86.x32;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class LeaveTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();

    final LowLevelRReilOpnd esp = registerTranslator.translateRegister("esp");
    final LowLevelRReilOpnd ebp = registerTranslator.translateRegister("ebp");

    instructions.addAll(Arrays.asList(
        factory.MOV(env.getNextReilAddress(), esp, ebp),
        factory.LOAD(env.getNextReilAddress(), ebp, esp),
        factory.ADD(env.getNextReilAddress(), esp, esp, factory.immediate(OperandSize.DWORD, 4))));
  }
}
