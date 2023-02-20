package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8Helpers;
import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class SubcTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static class SubcFlagEmitter implements InsnEmitter {
    public static InsnEmitter $ = new SubcFlagEmitter();

    @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
      final LowLevelRReilOpnd se1 = env.temporaryRegister(src1.size() + 1);
      instructions.add(factory.SIGNEXTEND(env.getNextReilAddress(), se1, src1));

      final LowLevelRReilOpnd se2 = env.temporaryRegister(src2.size() + 1);
      instructions.add(factory.SIGNEXTEND(env.getNextReilAddress(), se2, src2));

      final LowLevelRReilOpnd long_carry = env.temporaryRegister(se2.size());
      instructions.add(factory.CONVERT(env.getNextReilAddress(), long_carry, AVR8Helpers.C_OPERAND));

      instructions.add(factory.ADD(env.getNextReilAddress(), se2, se2, long_carry));

      final LowLevelRReilOpnd de = env.temporaryRegister(dst.size() + 1);
      instructions.add(factory.SIGNEXTEND(env.getNextReilAddress(), de, dst));

      // Zero
      final LowLevelRReilOpnd ztemp = env.temporaryRegister(1);
      instructions.add(factory.CMPEQ(env.getNextReilAddress(), ztemp, dst, factory.immediate(dst.size(), 0)));
      instructions.add(factory.AND(env.getNextReilAddress(), AVR8Helpers.Z_OPERAND, AVR8Helpers.Z_OPERAND, ztemp));

      // Negative
      instructions.add(factory.CMPLTS(env.getNextReilAddress(), AVR8Helpers.N_OPERAND, dst, factory.immediate(dst.size(), 0)));

      // Half Carry
      final LowLevelRReilOpnd t0 = env.temporaryRegister(1);

      instructions.add(factory.XOR(env.getNextReilAddress(), t0, src1.withOffset(3, 1), factory.immediate(t0.size(), (1 << t0.size()) - 1)));
      instructions.add(factory.AND(env.getNextReilAddress(), t0, t0, src2.withOffset(3, 1)));

      final LowLevelRReilOpnd t1 = env.temporaryRegister(1);

      instructions.add(factory.AND(env.getNextReilAddress(), t1, src2.withOffset(3, 1), dst.withOffset(3, 1)));

      instructions.add(factory.OR(env.getNextReilAddress(), t0, t0, t1));

      instructions.add(factory.XOR(env.getNextReilAddress(), t1, src1.withOffset(3, 1), factory.immediate(t0.size(), (1 << t0.size()) - 1)));
      instructions.add(factory.AND(env.getNextReilAddress(), t1, dst.withOffset(3, 1), t1));

      instructions.add(factory.OR(env.getNextReilAddress(), AVR8Helpers.H_OPERAND, t0, t1));

      // Carry
      instructions.add(factory.CMPLTU(env.getNextReilAddress(), AVR8Helpers.C_OPERAND, se1, se2));

      // LE : signed-less-than-or-equal Z+(N XOR V) = 1
      instructions.add(factory.CMPLES(env.getNextReilAddress(), AVR8Helpers.LE_OPERAND, se1, se2));

      // BE : unsigned-less-than-or-equal (C+Z=1)
      instructions.add(factory.CMPLEU(env.getNextReilAddress(), AVR8Helpers.BE_OPERAND, se1, se2));

      // B : unsigned-less-than (Carry)
      instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.B_OPERAND, AVR8Helpers.C_OPERAND));

      // L : signed-less-than ((N XOR V) = 1)
      instructions.add(factory.CMPLTS(env.getNextReilAddress(), AVR8Helpers.L_OPERAND, se1, se2));

      // Signed
      instructions.add(factory.XOR(env.getNextReilAddress(), AVR8Helpers.S_OPERAND, AVR8Helpers.N_OPERAND, AVR8Helpers.V_OPERAND));

      // Two's complement overflow indicator
      instructions.add(factory.OR(env.getNextReilAddress(), AVR8Helpers.V_OPERAND, t0, t1));
    }
  }

  public SubcTranslator (final ReturnType returnType) {
    super(returnType);
  }

  @Override public void emit (final TranslationCtx env, LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    if (dst == null)
      dst = env.temporaryRegister(src1.size());

    final LowLevelRReilOpnd t0 = env.temporaryRegister(src2.size());
    instructions.add(factory.CONVERT(env.getNextReilAddress(), t0, AVR8Helpers.C_OPERAND));
    instructions.add(factory.ADD(env.getNextReilAddress(), t0, t0, src2));

    instructions.add(factory.SUB(env.getNextReilAddress(), dst, src1, t0));

    SubcFlagEmitter.$.emit(env, dst, src1, t0, instructions);
  }
}
