package rreil.disassembler.translators.avr8.emitters;

import java.util.List;

import rreil.disassembler.translators.avr8.translators.PushTranslator;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.OperandSize;

public class PushPCEmitter implements InsnEmitter {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private final int instructionSize;

  public PushPCEmitter(int instructionSize) {
    this.instructionSize = instructionSize;
  }

  @Override public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
    final LowLevelRReilOpnd t0 = env.temporaryRegister(OperandSize.WORD);
    instructions.add(factory.ADD(env.getNextReilAddress(), t0, factory.immediate(OperandSize.WORD, env.getBaseAddress()), factory.immediate(OperandSize.WORD, instructionSize)));

    instructions.add(factory.SHRS(env.getNextReilAddress(), t0, t0, factory.immediate(t0.size(), 1)));

    new PushTranslator().emit(env, null, t0.withOffset(0, 8), null, instructions);
    new PushTranslator().emit(env, null, t0.withOffset(8, 8), null, instructions);
  }

}
