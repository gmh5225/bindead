package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8Helpers;
import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator;
import rreil.disassembler.translators.avr8.emitters.GenericFlagEmitter;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class ComTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static class ComFlagEmitter implements InsnEmitter {
    public static InsnEmitter $ = new ComFlagEmitter();

    @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
      // Zero
      instructions.add(factory.CMPEQ(env.getNextReilAddress(), AVR8Helpers.Z_OPERAND, dst, factory.immediate(dst.size(), 0)));

      // Negative
      instructions.add(factory.CMPLTS(env.getNextReilAddress(), AVR8Helpers.N_OPERAND, dst, factory.immediate(dst.size(), 0)));

      // Two's complement overflow indicator
      instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.V_OPERAND, factory.immediate(1, 0)));

      // Carry
      instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.C_OPERAND, factory.immediate(1, 1)));

      GenericFlagEmitter.$.emit(env, dst, src1, src2, instructions);
    }
  }

  public ComTranslator () {
    super(ReturnType.Register);
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    instructions.add(factory.XOR(env.getNextReilAddress(), dst, src1, factory.immediate(dst.size(), (1 << dst.size()) - 1)));

    ComFlagEmitter.$.emit(env, dst, src1, src2, instructions);
  }
}
