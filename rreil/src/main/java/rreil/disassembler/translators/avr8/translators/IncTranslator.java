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

public class IncTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static class IncFlagEmitter implements InsnEmitter {
    public static InsnEmitter $ = new IncFlagEmitter();

    @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
      // Zero
      instructions.add(factory.CMPEQ(env.getNextReilAddress(), AVR8Helpers.Z_OPERAND, dst, factory.immediate(dst.size(), 0)));

      // Negative
      instructions.add(factory.CMPLTS(env.getNextReilAddress(), AVR8Helpers.N_OPERAND, dst, factory.immediate(dst.size(), 0)));

      // Two's complement overflow indicator
      instructions.add(factory.CMPEQ(env.getNextReilAddress(), AVR8Helpers.V_OPERAND, dst, factory.immediate(dst.size(), 128)));

      GenericFlagEmitter.$.emit(env, dst, src1, src2, instructions);
    }
  }

  public IncTranslator () {
    super(ReturnType.Register);
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    instructions.add(factory.ADD(env.getNextReilAddress(), dst, src1, factory.immediate(dst.size(), 1)));

    IncFlagEmitter.$.emit(env, dst, src1, src2, instructions);
  }
}
