package rreil.disassembler.translators.avr8.emitters;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8Helpers;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class CarryShiftFlagEmitter implements InsnEmitter {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static InsnEmitter $ = new CarryShiftFlagEmitter();

  @Override public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
    // Zero
    instructions.addAll(Arrays.asList(factory.CMPEQ(env.getNextReilAddress(), AVR8Helpers.Z_OPERAND, dst, factory.immediate(dst.size(), 0))));

    // Negative
    instructions.addAll(Arrays.asList(factory.CMPLES(env.getNextReilAddress(), AVR8Helpers.N_OPERAND, dst, factory.immediate(dst.size(), 0))));

    // Carry
    instructions.addAll(Arrays.asList(factory.CONVERT(env.getNextReilAddress(), AVR8Helpers.C_OPERAND, src1)));

    // Two's complement overflow indicator
    instructions.addAll(Arrays.asList(factory.XOR(env.getNextReilAddress(), AVR8Helpers.V_OPERAND, AVR8Helpers.N_OPERAND, AVR8Helpers.C_OPERAND)));

    GenericFlagEmitter.$.emit(env, dst, src1, src2, instructions);
  }

}
