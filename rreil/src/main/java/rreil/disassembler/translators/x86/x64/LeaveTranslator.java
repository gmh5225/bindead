package rreil.disassembler.translators.x86.x64;

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
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    env.setCurrentInstruction(instruction);

    final LowLevelRReilOpnd rsp = registerTranslator.translateRegister("rsp");
    final LowLevelRReilOpnd rbp = registerTranslator.translateRegister("rbp");

    instructions.addAll(Arrays.asList(
        factory.MOV(env.getNextReilAddress(), rsp, rbp),
        factory.LOAD(env.getNextReilAddress(), rbp, rsp),
        factory.ADD(env.getNextReilAddress(), rsp, rsp, factory.immediate(OperandSize.QWORD, 8))));
  }
}
