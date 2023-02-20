package rreil.disassembler.translators.avr8.common;

import java.util.List;

import rreil.disassembler.translators.avr8.implementations.AvrImplementation;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.OperandSize;

public class AVR8SregMemTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static void storeSreg(TranslationCtx env, List<LowLevelRReil> instructions) {
    //    int sreg = (avrInterpCtx.getBool("H").intValue() << 5) | (avrInterpCtx.getBool("S").intValue() << 4) | (avrInterpCtx.getBool("V").intValue() << 3) | (avrInterpCtx.getBool("N").intValue() << 2) | (avrInterpCtx.getBool("Z").intValue() << 1)
    //| (avrInterpCtx.getBool("C").intValue() << 0);
    LowLevelRReilOpnd sreg = env.temporaryRegister(OperandSize.BYTE);
    instructions.add(factory.MOV(env.getNextReilAddress(), sreg, factory.immediate(sreg.size(), 0)));
    instructions.add(factory.MOV(env.getNextReilAddress(), sreg.withOffset(5, 1), AVR8Helpers.H_OPERAND));
    instructions.add(factory.MOV(env.getNextReilAddress(), sreg.withOffset(4, 1), AVR8Helpers.S_OPERAND));
    instructions.add(factory.MOV(env.getNextReilAddress(), sreg.withOffset(3, 1), AVR8Helpers.V_OPERAND));
    instructions.add(factory.MOV(env.getNextReilAddress(), sreg.withOffset(2, 1), AVR8Helpers.N_OPERAND));
    instructions.add(factory.MOV(env.getNextReilAddress(), sreg.withOffset(1, 1), AVR8Helpers.Z_OPERAND));
    instructions.add(factory.MOV(env.getNextReilAddress(), sreg.withOffset(0, 1), AVR8Helpers.C_OPERAND));
    instructions.add(factory.STORE(env.getNextReilAddress(), factory.immediate(OperandSize.WORD, AvrImplementation.$ATMEGA32L.getSregAddress()), sreg));
  }

  public static void loadSreg(TranslationCtx env, List<LowLevelRReil> instructions) {
    LowLevelRReilOpnd sreg = env.temporaryRegister(OperandSize.BYTE);
    instructions.add(factory.LOAD(env.getNextReilAddress(), sreg, factory.immediate(OperandSize.WORD, AvrImplementation.$ATMEGA32L.getSregAddress())));
    instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.H_OPERAND, sreg.withOffset(5, 1)));
    instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.S_OPERAND, sreg.withOffset(4, 1)));
    instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.V_OPERAND, sreg.withOffset(3, 1)));
    instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.N_OPERAND, sreg.withOffset(2, 1)));
    instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.Z_OPERAND, sreg.withOffset(1, 1)));
    instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.C_OPERAND, sreg.withOffset(0, 1)));
  }
}
