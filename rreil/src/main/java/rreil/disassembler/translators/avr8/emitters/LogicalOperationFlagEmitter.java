package rreil.disassembler.translators.avr8.emitters;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8Helpers;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class LogicalOperationFlagEmitter implements InsnEmitter {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static final InsnEmitter $ = new LogicalOperationFlagEmitter();

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    // Zero
    instructions.addAll(Arrays.asList(factory.CMPEQ(env.getNextReilAddress(), AVR8Helpers.Z_OPERAND, dst, factory.immediate(dst.size(), 0))));

    // Negative
    instructions.addAll(Arrays.asList(factory.CMPLES(env.getNextReilAddress(), AVR8Helpers.N_OPERAND, dst, factory.immediate(dst.size(), 0))));

    // Two's complement overflow indicator
    instructions.addAll(Arrays.asList(factory.MOV(env.getNextReilAddress(), AVR8Helpers.V_OPERAND, factory.immediate(1, 0))));

    GenericFlagEmitter.$.emit(env, dst, src1, src2, instructions);
  }

}
