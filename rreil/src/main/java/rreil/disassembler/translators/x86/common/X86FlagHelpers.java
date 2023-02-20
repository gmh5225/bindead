package rreil.disassembler.translators.x86.common;

import static rreil.disassembler.translators.x86.common.X86Helpers.AUXILIARY_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.BELOW_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.BELOW_OR_EQUAL_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.EQUAL_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.LESS_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.LESS_OR_EQUAL_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.OVERFLOW_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.PARITY_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.SIGN_FLAG_OPERAND;
import static rreil.lang.lowlevel.TranslationHelpers.getNextSize;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.TranslationHelpers;

public class X86FlagHelpers {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static class UndefineFlagEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      instructions.addAll(Arrays.asList(
          factory.UNDEF(env.getNextReilAddress(), AUXILIARY_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), PARITY_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), SIGN_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), BELOW_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), BELOW_OR_EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), LESS_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), LESS_OR_EQUAL_FLAG_OPERAND)));
    }
  }

  public static class ZeroSignFlagEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd unused1, LowLevelRReilOpnd unused2, List<LowLevelRReil> instructions) {
      final LowLevelRReilOpnd zero = factory.immediate(dst.size(), 0);
      instructions.addAll(Arrays.asList(
          factory.CMPEQ(env.getNextReilAddress(), EQUAL_FLAG_OPERAND, dst, zero),
          factory.CMPLTS(env.getNextReilAddress(), SIGN_FLAG_OPERAND, dst, zero),
          factory.OR(env.getNextReilAddress(), BELOW_OR_EQUAL_FLAG_OPERAND, BELOW_FLAG_OPERAND, EQUAL_FLAG_OPERAND),
          factory.XOR(env.getNextReilAddress(), LESS_FLAG_OPERAND, SIGN_FLAG_OPERAND, OVERFLOW_FLAG_OPERAND),
          factory.OR(env.getNextReilAddress(), LESS_OR_EQUAL_FLAG_OPERAND, LESS_FLAG_OPERAND, EQUAL_FLAG_OPERAND)));
    }
  }

  public static class AddFlagEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd tmp1 = registerTranslator.temporaryRegister(env, dst.size());
      final LowLevelRReilOpnd tmp2 = registerTranslator.temporaryRegister(env, dst.size());
      final LowLevelRReilOpnd tmp3 = registerTranslator.temporaryRegister(env, dst.size());
      final LowLevelRReilOpnd tmp4 = registerTranslator.temporaryRegister(env, dst.size());
      final LowLevelRReilOpnd zero = factory.immediate(dst.size(), 0);
      final LowLevelRReilOpnd max = factory.immediate(dst.size(), -1);
//      final LowLevelRReilOpnd max = getMaxImmediate(dst.size());

      instructions.addAll(
          Arrays.asList(
              factory.SUB(env.getNextReilAddress(), tmp1, max, src2),
              factory.CMPLTU(env.getNextReilAddress(), BELOW_FLAG_OPERAND, tmp1, src1),
              factory.CMPEQ(env.getNextReilAddress(), EQUAL_FLAG_OPERAND, dst, zero),
              factory.CMPLTS(env.getNextReilAddress(), SIGN_FLAG_OPERAND, dst, zero),
          /*
          // Higher precision arithmetic
          MOVSX(env.getNextReilAddress(), tmp2, src1),
          MOVSX(env.getNextReilAddress(), tmp3, src2),
          ADD(env.getNextReilAddress(), tmp4, tmp2, tmp3),
          CMPLTS(env.getNextReilAddress(), f1, maxs, tmp4),
          CMPLTS(env.getNextReilAddress(), f2, tmp4, mins),
          OR(env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND, f1, f2),
           */
          // Hackers Delight p27
              factory.XOR(env.getNextReilAddress(), tmp2, dst, src1),
              factory.XOR(env.getNextReilAddress(), tmp3, dst, src2),
              factory.AND(env.getNextReilAddress(), tmp4, tmp2, tmp3),
          //MOV(env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND, tmp4.withOffset(tmp4.size().getBits() - 1, OperandSize.BIT)),
              factory.CMPLTS(env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND, tmp4, zero),
              factory.OR(env.getNextReilAddress(), BELOW_OR_EQUAL_FLAG_OPERAND, BELOW_FLAG_OPERAND, EQUAL_FLAG_OPERAND),
              factory.XOR(env.getNextReilAddress(), LESS_FLAG_OPERAND, SIGN_FLAG_OPERAND, OVERFLOW_FLAG_OPERAND),
              factory.OR(env.getNextReilAddress(), LESS_OR_EQUAL_FLAG_OPERAND, LESS_FLAG_OPERAND, EQUAL_FLAG_OPERAND)));
    }
  }

  public static class SubFlagEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      instructions.addAll(Arrays.asList(
          factory.CMPLTU(env.getNextReilAddress(), BELOW_FLAG_OPERAND, src1, src2),
          factory.CMPLEU(env.getNextReilAddress(), BELOW_OR_EQUAL_FLAG_OPERAND, src1, src2),
          factory.CMPLTS(env.getNextReilAddress(), LESS_FLAG_OPERAND, src1, src2),
          factory.CMPLES(env.getNextReilAddress(), LESS_OR_EQUAL_FLAG_OPERAND, src1, src2),
          factory.CMPEQ(env.getNextReilAddress(), EQUAL_FLAG_OPERAND, src1, src2),
          factory.CMPLTS(env.getNextReilAddress(), SIGN_FLAG_OPERAND, dst, factory.immediate(dst.size(), 0)),
          factory.XOR(env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND, LESS_FLAG_OPERAND, SIGN_FLAG_OPERAND)));
    }
  }

  public static class DecFlagEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      instructions.addAll(Arrays.asList(
          factory.CMPLTS(env.getNextReilAddress(), LESS_FLAG_OPERAND, src1, src2),
          factory.CMPLES(env.getNextReilAddress(), LESS_OR_EQUAL_FLAG_OPERAND, src1, src2),
          factory.CMPEQ(env.getNextReilAddress(), EQUAL_FLAG_OPERAND, src1, src2),
          factory.CMPLTS(env.getNextReilAddress(), SIGN_FLAG_OPERAND, dst, factory.immediate(dst.size(), 0)),
          factory.XOR(env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND, LESS_FLAG_OPERAND, SIGN_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), BELOW_OR_EQUAL_FLAG_OPERAND)));
    }
  }

  public static class IncFlagEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final int preciseSz = getNextSize(dst.size());
      final LowLevelRReilOpnd t1 = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd t2 = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd t3 = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd f1 = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd f2 = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd zero = factory.immediate(dst.size(), 0);
      final LowLevelRReilOpnd max = TranslationHelpers.getMaxImmediateSigned(dst.size()).withSize(preciseSz);
      final LowLevelRReilOpnd min = TranslationHelpers.getMinImmediateSigned(dst.size()).withSize(preciseSz);

      instructions.addAll(
          Arrays.asList(
              factory.CMPEQ(env.getNextReilAddress(), EQUAL_FLAG_OPERAND, dst, zero),
              factory.CMPLTS(env.getNextReilAddress(), SIGN_FLAG_OPERAND, dst, zero),
              factory.SIGNEXTEND(env.getNextReilAddress(), t1, src1),
              factory.SIGNEXTEND(env.getNextReilAddress(), t2, src2),
              factory.ADD(env.getNextReilAddress(), t3, t1, t2),
              factory.CMPLTS(env.getNextReilAddress(), f1, max, t3),
              factory.CMPLTS(env.getNextReilAddress(), f2, t3, min),
              factory.OR(env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND, f1, f2),
              factory.XOR(env.getNextReilAddress(), LESS_FLAG_OPERAND, SIGN_FLAG_OPERAND, OVERFLOW_FLAG_OPERAND),
              factory.OR(env.getNextReilAddress(), LESS_OR_EQUAL_FLAG_OPERAND, LESS_FLAG_OPERAND, EQUAL_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), BELOW_OR_EQUAL_FLAG_OPERAND)));
    }
  }

  public static class LogicFlagEmitter implements InsnEmitter {
    @Override
    public void emit (
        TranslationCtx env,
        LowLevelRReilOpnd dst,
        LowLevelRReilOpnd src1,
        LowLevelRReilOpnd src2,
        List<LowLevelRReil> instructions) {
      final LowLevelRReilOpnd zero = factory.immediate(dst.size(), 0);
      final LowLevelRReilOpnd zeroFlag = factory.immediate(BELOW_OR_EQUAL_FLAG_OPERAND.size(), 0);

      instructions.addAll(
          Arrays.asList(
              factory.MOV(env.getNextReilAddress(), BELOW_FLAG_OPERAND, zeroFlag),
              factory.MOV(env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND, zeroFlag),
              factory.CMPEQ(env.getNextReilAddress(), EQUAL_FLAG_OPERAND, dst, zero),
              factory.CMPLTS(env.getNextReilAddress(), SIGN_FLAG_OPERAND, dst, zero),
              factory.MOV(env.getNextReilAddress(), BELOW_OR_EQUAL_FLAG_OPERAND, EQUAL_FLAG_OPERAND),
              factory.MOV(env.getNextReilAddress(), LESS_FLAG_OPERAND, SIGN_FLAG_OPERAND),
              factory.OR(env.getNextReilAddress(), LESS_OR_EQUAL_FLAG_OPERAND, SIGN_FLAG_OPERAND, EQUAL_FLAG_OPERAND)));
    }
  }
}
