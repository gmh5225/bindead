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

public class LsrTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static class LsrFlagEmitter implements InsnEmitter {
    public static InsnEmitter $ = new LsrFlagEmitter();

    @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
      // Zero
      instructions.add(factory.CMPEQ(env.getNextReilAddress(), AVR8Helpers.Z_OPERAND, dst, factory.immediate(dst.size(), 0)));

      // Negative
      instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.N_OPERAND, factory.immediate(dst.size(), 0)));

      // Carry
      instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.C_OPERAND, src1.withOffset(0, 1)));

      // Two's complement overflow indicator
      instructions.add(factory.XOR(env.getNextReilAddress(), AVR8Helpers.V_OPERAND, AVR8Helpers.N_OPERAND, AVR8Helpers.C_OPERAND));

      GenericFlagEmitter.$.emit(env, dst, src1, src2, instructions);
    }
  }

  public LsrTranslator () {
    super(ReturnType.Register);
    // TODO Auto-generated constructor stub
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    instructions.add(factory.SHRS(env.getNextReilAddress(), dst, src1, factory.immediate(dst.size(), 1)));

    LsrFlagEmitter.$.emit(env, dst, src1, src2, instructions);
  }

}
