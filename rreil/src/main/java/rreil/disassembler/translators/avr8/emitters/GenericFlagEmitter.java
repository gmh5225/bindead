package rreil.disassembler.translators.avr8.emitters;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8Helpers;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class GenericFlagEmitter implements InsnEmitter {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static InsnEmitter $ = new GenericFlagEmitter();

  @Override public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
    // LE : signed-less-than-or-equal Z+(N XOR V) = 1
    final LowLevelRReilOpnd t0 = env.temporaryRegister(1);
    instructions.add(factory.XOR(env.getNextReilAddress(), t0, AVR8Helpers.N_OPERAND, AVR8Helpers.V_OPERAND));
    instructions.add(factory.OR(env.getNextReilAddress(), AVR8Helpers.LE_OPERAND, AVR8Helpers.Z_OPERAND, t0));

    // BE : unsigned-less-than-or-equal (C+Z=1)
    instructions.add(factory.OR(env.getNextReilAddress(), AVR8Helpers.BE_OPERAND, AVR8Helpers.C_OPERAND, AVR8Helpers.Z_OPERAND));

    // B : unsigned-less-than (Carry)
    instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.B_OPERAND, AVR8Helpers.C_OPERAND));

    // L : signed-less-than ((N XOR V) = 1)
    instructions.add(factory.XOR(env.getNextReilAddress(), AVR8Helpers.L_OPERAND, AVR8Helpers.N_OPERAND, AVR8Helpers.V_OPERAND));

    // Signed
    instructions.add(factory.XOR(env.getNextReilAddress(), AVR8Helpers.S_OPERAND, AVR8Helpers.N_OPERAND, AVR8Helpers.V_OPERAND));
  }
}
