package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8Helpers;
import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class BclssrTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private final int bitValue;

  public BclssrTranslator (final int bitValue) {
    super(ReturnType.None);
    this.bitValue = bitValue;
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    switch((Integer) src1.child().getData()) {
      case 0: {
        instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.C_OPERAND, factory.immediate(1, this.bitValue)));
        break;
      }
      case 1: {
        instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.Z_OPERAND, factory.immediate(1, this.bitValue)));
        break;
      }
      case 2: {
        instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.N_OPERAND, factory.immediate(1, this.bitValue)));
        break;
      }
      case 3: {
        instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.V_OPERAND, factory.immediate(1, this.bitValue)));
        break;
      }
      case 4: {
        instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.S_OPERAND, factory.immediate(1, this.bitValue)));
        break;
      }
      case 5: {
        instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.H_OPERAND, factory.immediate(1, this.bitValue)));
        break;
      }
      case 6: {
        instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.T_OPERAND, factory.immediate(1, this.bitValue)));
        break;
      }
      case 7: {
        instructions.add(factory.MOV(env.getNextReilAddress(), AVR8Helpers.I_OPERAND, factory.immediate(1, this.bitValue)));
        break;
      }
    }
  }

}
