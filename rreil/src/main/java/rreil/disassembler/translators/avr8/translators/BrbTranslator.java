package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8Helpers;
import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class BrbTranslator extends AVR8OperationTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private final boolean jumpSet;

  public BrbTranslator (final boolean jumpSet) {
    super(ReturnType.None);
    this.jumpSet = jumpSet;
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    final LowLevelRReilOpnd f1 = env.temporaryRegister(1);

    switch((Integer) src1.child().getData()) {
      case 0: {
        instructions.add(factory.MOV(env.getNextReilAddress(), f1, AVR8Helpers.C_OPERAND));
        break;
      }
      case 1: {
        instructions.add(factory.MOV(env.getNextReilAddress(), f1, AVR8Helpers.Z_OPERAND));
        break;
      }
      case 2: {
        instructions.add(factory.MOV(env.getNextReilAddress(), f1, AVR8Helpers.N_OPERAND));
        break;
      }
      case 3: {
        instructions.add(factory.MOV(env.getNextReilAddress(), f1, AVR8Helpers.V_OPERAND));
        break;
      }
      case 4: {
        instructions.add(factory.MOV(env.getNextReilAddress(), f1, AVR8Helpers.S_OPERAND));
        break;
      }
      case 5: {
        instructions.add(factory.MOV(env.getNextReilAddress(), f1, AVR8Helpers.H_OPERAND));
        break;
      }
      case 6: {
        instructions.add(factory.MOV(env.getNextReilAddress(), f1, AVR8Helpers.T_OPERAND));
        break;
      }
    }

    if (!this.jumpSet)
      instructions.add(factory.XOR(env.getNextReilAddress(), f1, f1, factory.immediate(f1.size(), (1 << f1.size()) - 1)));

    final LowLevelRReilOpnd t0 = env.temporaryRegister(src2.size());
    instructions.add(factory.ADD(env.getNextReilAddress(), t0, src2, factory.immediate(src2.size(), 2)));

    instructions.add(factory.IFGOTO(env.getNextReilAddress(), f1, t0));
  }
}
