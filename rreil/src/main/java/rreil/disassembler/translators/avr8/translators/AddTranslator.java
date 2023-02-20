package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8Helpers;
import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.TranslationHelpers;

/**
 * Add instruction emitter.
 *
 * @author mb0
 */
public class AddTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();
  public static class AddFlagEmitter implements InsnEmitter {
    public static InsnEmitter $ = new AddFlagEmitter();

    @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
      // Zero
      instructions.add(factory.CMPEQ(env.getNextReilAddress(), AVR8Helpers.Z_OPERAND, dst, factory.immediate(dst.size(), 0)));

      // Negative
      instructions.add(factory.CMPLTS(env.getNextReilAddress(), AVR8Helpers.N_OPERAND, dst, factory.immediate(dst.size(), 0)));

      // Carry
      final LowLevelRReilOpnd t1 = env.temporaryRegister(1);
      final LowLevelRReilOpnd t2 = env.temporaryRegister(1);

      instructions.add(factory.AND(env.getNextReilAddress(), t1, src1.withOffset(7, 1), src2.withOffset(7, 1)));
      instructions.add(factory.XOR(env.getNextReilAddress(), t2, dst.withOffset(7, 1), factory.immediate(t2.size(), (1 << t2.size()) - 1)));
      instructions.add(factory.AND(env.getNextReilAddress(), t2, src2.withOffset(7, 1), t2));
      instructions.add(factory.OR(env.getNextReilAddress(), t1, t1, t2));
      instructions.add(factory.XOR(env.getNextReilAddress(), t2, dst.withOffset(7, 1), factory.immediate(t2.size(), (1 << t2.size()) - 1)));
      instructions.add(factory.AND(env.getNextReilAddress(), t2, src1.withOffset(7, 1), t2));
      instructions.add(factory.OR(env.getNextReilAddress(), AVR8Helpers.C_OPERAND, t1, t2));

      // Half Carry
      instructions.add(factory.AND(env.getNextReilAddress(), t1, src1.withOffset(3, 1), src2.withOffset(3, 1)));

      //factory.immediate(t2.size(), (1 << t2.size()) - 1)
      instructions.add(factory.NOT(env.getNextReilAddress(), t2, dst.withOffset(3, 1)));
      instructions.add(factory.AND(env.getNextReilAddress(), t2, src2.withOffset(3, 1), t2));

      instructions.add(factory.OR(env.getNextReilAddress(), t1, t1, t2));

      instructions.add(factory.NOT(env.getNextReilAddress(), t2, dst.withOffset(3, 1)));
      instructions.add(factory.AND(env.getNextReilAddress(), t2, src1.withOffset(3, 1), t2));

      instructions.add(factory.OR(env.getNextReilAddress(), AVR8Helpers.H_OPERAND, t1, t2));

      final LowLevelRReilOpnd t4 = env.temporaryRegister(dst.size());
      final LowLevelRReilOpnd t5 = env.temporaryRegister(dst.size());
      final LowLevelRReilOpnd t6 = env.temporaryRegister(dst.size());
      final LowLevelRReilOpnd t7 = env.temporaryRegister(dst.size());
      final LowLevelRReilOpnd zero = factory.immediate(dst.size(), 0);
      final LowLevelRReilOpnd max = TranslationHelpers.getMaxImmediate(dst.size());

      instructions.add(factory.SUB(env.getNextReilAddress(), t4, max, src2));
      instructions.add(factory.XOR(env.getNextReilAddress(), t5, dst, src1));
      instructions.add(factory.XOR(env.getNextReilAddress(), t6, dst, src2));
      instructions.add(factory.AND(env.getNextReilAddress(), t7, t5, t6));

      // B : unsigned-less-than (Carry) [BELOW_FLAG_OPERAND]
      instructions.add(factory.CMPLTU(env.getNextReilAddress(), AVR8Helpers.B_OPERAND, t4, src1));

      // BE : unsigned-less-than-or-equal (C+Z=1) [BELOW_OR_EQUAL_FLAG_OPERAND]
      final LowLevelRReilOpnd t0 = env.temporaryRegister(1);
      instructions.add(factory.CMPEQ(env.getNextReilAddress(), t0, dst, zero));
      instructions.add(factory.OR(env.getNextReilAddress(), AVR8Helpers.BE_OPERAND, AVR8Helpers.B_OPERAND, t0));

      // Signed
      instructions.add(factory.CMPLTS(env.getNextReilAddress(), AVR8Helpers.S_OPERAND, dst, zero));

      // Two's complement overflow indicator
      instructions.add(factory.CMPLTS(env.getNextReilAddress(), AVR8Helpers.V_OPERAND, t7, zero));

      // L : signed-less-than ((N XOR V) = 1) [LESS_FLAG_OPERAND]
      instructions.add(factory.XOR(env.getNextReilAddress(), AVR8Helpers.L_OPERAND, AVR8Helpers.S_OPERAND, AVR8Helpers.V_OPERAND));

      // LE : signed-less-than-or-equal Z+(N XOR V) = 1
      // [LESS_OR_EQUAL_FLAG_OPERAND]
      instructions.add(factory.OR(env.getNextReilAddress(), AVR8Helpers.LE_OPERAND, AVR8Helpers.L_OPERAND, t0));
    }
  }

  public AddTranslator () {
    super(ReturnType.Register);
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    instructions.add(factory.ADD(env.getNextReilAddress(), dst, src1, src2));
    AddFlagEmitter.$.emit(env, dst, src1, src2, instructions);
  }
}
