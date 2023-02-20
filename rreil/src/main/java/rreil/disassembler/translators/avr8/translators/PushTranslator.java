package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator;
import rreil.disassembler.translators.avr8.emitters.StackPointerBuilder;
import rreil.disassembler.translators.avr8.emitters.StackPointerWritebackEmitter;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.OperandSize;

public class PushTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public PushTranslator () {
    super(ReturnType.None);
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    final LowLevelRReilOpnd sp = env.temporaryRegister(OperandSize.WORD);
    StackPointerBuilder.$.emit(env, sp, null, null, instructions);

    instructions.add(factory.STORE(env.getNextReilAddress(), sp, src1));

    instructions.add(factory.SUB(env.getNextReilAddress(), sp, sp, factory.immediate(sp.size(), 1)));

    StackPointerWritebackEmitter.$.emit(env, null, sp, null, instructions);
  }
}
