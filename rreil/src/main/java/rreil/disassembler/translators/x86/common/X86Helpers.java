package rreil.disassembler.translators.x86.common;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

/**
 * Collection of helper functions for translating x86 code to RREIL code.
 *
 * @author sp
 */
public class X86Helpers {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  /* == Real processor flags (native flags) == */
  public static final String AUXILIARY_FLAG = "AF";
  public static final String CARRY_FLAG = "CF";
  public static final String DIRECTION_FLAG = "DF";
  public static final String INTERRUPT_FLAG = "IF";
  public static final String OVERFLOW_FLAG = "OF";
  public static final String PARITY_FLAG = "PF";
  public static final String SIGN_FLAG = "SF";
  public static final String ZERO_FLAG = "ZF";

  /* == Virtual flags == */
  /** (Virtual) Below or equal flag, defined as (CF or ZF) */
  public static final String BELOW_OR_EQUAL_FLAG = "BE";
  /** (Virtual) Less than flag, defined as (SF xor OF) */
  public static final String LESS_FLAG = "LT";
  /** (Virtual) Less than or equal flag, defined as ((SF xor OF) or ZF) */
  public static final String LESS_OR_EQUAL_FLAG = "LE";
  public static final LowLevelRReilOpnd AUXILIARY_FLAG_OPERAND = factory.flag(AUXILIARY_FLAG);
  public static final LowLevelRReilOpnd DIRECTION_FLAG_OPERAND = factory.flag(DIRECTION_FLAG);
  public static final LowLevelRReilOpnd OVERFLOW_FLAG_OPERAND = factory.flag(OVERFLOW_FLAG);
  public static final LowLevelRReilOpnd SIGN_FLAG_OPERAND = factory.flag(SIGN_FLAG);
  public static final LowLevelRReilOpnd INTERRUPT_FLAG_OPERAND = factory.flag(INTERRUPT_FLAG);
  public static final LowLevelRReilOpnd PARITY_FLAG_OPERAND = factory.flag(PARITY_FLAG);
  public static final LowLevelRReilOpnd BELOW_FLAG_OPERAND = factory.flag(CARRY_FLAG);
  public static final LowLevelRReilOpnd BELOW_OR_EQUAL_FLAG_OPERAND = factory.flag(BELOW_OR_EQUAL_FLAG);
  public static final LowLevelRReilOpnd EQUAL_FLAG_OPERAND = factory.flag(ZERO_FLAG);
  public static final LowLevelRReilOpnd LESS_FLAG_OPERAND = factory.flag(LESS_FLAG);
  public static final LowLevelRReilOpnd LESS_OR_EQUAL_FLAG_OPERAND = factory.flag(LESS_OR_EQUAL_FLAG);

  public static void emitWritebackAndMaybeZeroExtend (
      TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src, List<LowLevelRReil> insns) {
    if (env.getDefaultArchitectureSize() == 64 && dst.size() == 32) {
      // Handel X86_64's 32bit to 64bit implicit zero-extension.
      insns.add(factory.MOV(env.getNextReilAddress(), dst, src));
      insns.add(
          factory.MOV(
          env.getNextReilAddress(), dst.withOffset(32, OperandSize.DWORD.getBits()), factory.immediate(dst.size(), 0)));
    } else
      insns.add(factory.MOV(env.getNextReilAddress(), dst, src));
  }

  public static void _POP (final TranslationCtx env, final LowLevelRReilOpnd dst, List<LowLevelRReil> insns) {
    final LowLevelRReilOpnd esp = env.getRegisterTranslator().translateRegister("esp").withSize(env.getDefaultArchitectureSize());
    final LowLevelRReilOpnd inc = factory.immediate(esp.size(), dst.size() / 8); // TODO: Check division of size annotation.

    insns.addAll(Arrays.asList(
        factory.LOAD(env.getNextReilAddress(), dst, esp),
        factory.ADD(env.getNextReilAddress(), esp, esp, inc)));
  }

  public static void _PUSH (final TranslationCtx env, final LowLevelRReilOpnd src, List<LowLevelRReil> insns) {
    final LowLevelRReilOpnd esp = env.getRegisterTranslator().translateRegister("esp").withSize(env.getDefaultArchitectureSize());
    final LowLevelRReilOpnd dec = factory.immediate(esp.size(), src.size() / 8); // TODO: Check division of size annotation.
    final LowLevelRReilOpnd tmp = env.getRegisterTranslator().temporaryRegister(env, src.size());
    insns.addAll(Arrays.asList(
        factory.MOV(env.getNextReilAddress(), tmp, src), // handle the case where src==esp
        factory.SUB(env.getNextReilAddress(), esp, esp, dec),
        factory.STORE(env.getNextReilAddress(), esp, tmp)));
  }
}
