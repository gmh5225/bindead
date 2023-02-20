package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8Helpers;
import rreil.disassembler.translators.avr8.common.AVR8RegRegWordTranslator;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class SubiwTranslator extends AVR8RegRegWordTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static class SubFlagEmitter implements InsnEmitter {
    public static InsnEmitter $ = new SubFlagEmitter();

    @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
      // Zero
      instructions.add(factory.CMPEQ(env.getNextReilAddress(), AVR8Helpers.Z_OPERAND, dst, factory.immediate(dst.size(), 0)));

      // Negative
      instructions.add(factory.CMPLTS(env.getNextReilAddress(), AVR8Helpers.N_OPERAND, dst, factory.immediate(dst.size(), 0)));

      // Carry
      instructions.add(factory.CMPLTU(env.getNextReilAddress(), AVR8Helpers.C_OPERAND, src1, src2));

      // LE : signed-less-than-or-equal Z+(N XOR V) = 1
      instructions.add(factory.CMPLES(env.getNextReilAddress(), AVR8Helpers.LE_OPERAND, src1, src2));

      // BE : unsigned-less-than-or-equal (C+Z=1)
      instructions.add(factory.CMPLEU(env.getNextReilAddress(), AVR8Helpers.BE_OPERAND, src1, src2));

      // B : unsigned-less-than (Carry)
      instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.B_OPERAND, AVR8Helpers.C_OPERAND));

      // L : signed-less-than ((N XOR V) = 1)
      instructions.add(factory.CMPLTS(env.getNextReilAddress(), AVR8Helpers.L_OPERAND, src1, src2));

      // Signed
      instructions.add(factory.XOR(env.getNextReilAddress(), AVR8Helpers.S_OPERAND, AVR8Helpers.N_OPERAND, AVR8Helpers.V_OPERAND));

      // Two's complement overflow indicator
      instructions.add(factory.XOR(env.getNextReilAddress(), AVR8Helpers.V_OPERAND, AVR8Helpers.L_OPERAND, AVR8Helpers.S_OPERAND));
    }
  }

  public SubiwTranslator () {
    super(new boolean[] { true, false });
  }

  @Override public void emit (final TranslationCtx env, LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    final LowLevelRReilOpnd t0 = env.temporaryRegister(src1.size());
    instructions.add(factory.CONVERT(env.getNextReilAddress(), t0, src2));

    if (dst == null)
      dst = env.temporaryRegister(src1.size());

    instructions.add(factory.SUB(env.getNextReilAddress(), dst, src1, t0));

    SubFlagEmitter.$.emit(env, dst, src1, t0, instructions);
  }
}
