package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8Helpers;
import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator;
import rreil.disassembler.translators.avr8.emitters.GenericFlagEmitter;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.TranslationHelpers;

public class NegTranslator extends AVR8OperationTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static class NegFlagEmitter implements InsnEmitter {
    public static InsnEmitter $ = new NegFlagEmitter();

    @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
      env.temporaryRegister(1);
      final LowLevelRReilOpnd t2 = env.temporaryRegister(1);

      // Zero
      instructions.add(factory.CMPEQ(env.getNextReilAddress(), AVR8Helpers.Z_OPERAND, dst, factory.immediate(dst.size(), 0)));

      // Negative
      instructions.add(factory.CMPLTS(env.getNextReilAddress(), AVR8Helpers.N_OPERAND, dst, factory.immediate(dst.size(), 0)));

      // Carry
      instructions.add(factory.XOR(env.getNextReilAddress(), AVR8Helpers.C_OPERAND, AVR8Helpers.Z_OPERAND, factory.immediate(t2.size(), (1 << t2.size()) - 1)));

      // Half Carry
      instructions.add(factory.OR(env.getNextReilAddress(), AVR8Helpers.H_OPERAND, dst.withOffset(3, 1), src1.withOffset(3, 1)));

      // Two's complement overflow indicator
      instructions.add(factory.CMPEQ(env.getNextReilAddress(), AVR8Helpers.V_OPERAND, dst, factory.immediate(dst.size(), 0x80)));

      GenericFlagEmitter.$.emit(env, dst, src1, src2, instructions);
    }
  }

  public NegTranslator () {
    super(ReturnType.Register);
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    final OperandSize archDefaultSize = TranslationHelpers.architectures.get("AVR");

    instructions.add(factory.XOR(env.getNextReilAddress(), dst, src1, TranslationHelpers.getMaxImmediate(archDefaultSize.getSizeInBytes() * 8)));

    NegFlagEmitter.$.emit(env, dst, src1, src2, instructions);
  }

}
