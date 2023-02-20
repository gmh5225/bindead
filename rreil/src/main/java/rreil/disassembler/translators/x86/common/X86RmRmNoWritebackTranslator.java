package rreil.disassembler.translators.x86.common;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.OperandTree;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.TranslationHelpers;

public class X86RmRmNoWritebackTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private final InsnEmitter emitter;

  public X86RmRmNoWritebackTranslator (InsnEmitter emitter) {
    this.emitter = emitter;
  }

  public static class CmpEmitter implements InsnEmitter {
    private static final InsnEmitter flags = new X86FlagHelpers.SubFlagEmitter();

    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd tmp = registerTranslator.temporaryRegister(env, src2.size());
      instructions.add(factory.SUB(env.getNextReilAddress(), tmp, src1, src2));
      flags.emit(env, tmp, src1, src2, instructions);
    }
  }

  public static class BtEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd i = registerTranslator.temporaryRegister(env, src2.size());
      final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, src1.size());

      instructions.addAll(Arrays.asList(
          factory.AND(env.getNextReilAddress(), i, src2, TranslationHelpers.getMaxImmediate(src2.size())),
          factory.SHRU(env.getNextReilAddress(), t, src1, i),
          factory.MOV(env.getNextReilAddress(), X86Helpers.BELOW_FLAG_OPERAND, t.withSize(OperandSize.BIT)),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.SIGN_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.BELOW_OR_EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_OR_EQUAL_FLAG_OPERAND)));
    }
  }

  public static class TestEmitter implements InsnEmitter {
    private static final InsnEmitter emitter = new X86FlagHelpers.LogicFlagEmitter();

    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd _null, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd tmp = registerTranslator.temporaryRegister(env, src1.size());
      instructions.add(factory.AND(env.getNextReilAddress(), tmp, src1, src2));
      emitter.emit(env, tmp, src1, src2, instructions);
    }
  }

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final List<? extends OperandTree> operands = instruction.operands();

    final OperandTree targetOperand = operands.get(0);
    final OperandTree sourceOperand = operands.get(1);

    final TranslationState opnd1 = X86OperandTranslator.translateOperand(env, targetOperand);
    final TranslationState opnd2 = X86OperandTranslator.translateOperand(env, sourceOperand);

    instructions.addAll(opnd1.getInstructionStack());
    instructions.addAll(opnd2.getInstructionStack());

    final LowLevelRReilOpnd src1 = opnd1.getOperandStack().pop();
    final LowLevelRReilOpnd src2 = factory.immediateSizeFixup(src1.size(), opnd2.getOperandStack().pop());

    emitter.emit(env, null, src1, src2, instructions);
  }
}
