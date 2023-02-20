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

public class X86RmRegBitmodTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private final InsnEmitter emitter;

  public X86RmRegBitmodTranslator (final InsnEmitter emitter) {
    this.emitter = emitter;
  }

  public static class BtcEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd r, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      RegisterTranslator registerTranslator = env.getRegisterTranslator();

      final LowLevelRReilOpnd i = registerTranslator.temporaryRegister(env, src2.size());
      final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, src1.size());
      final LowLevelRReilOpnd m = registerTranslator.temporaryRegister(env, t.size());

      instructions.addAll(
          Arrays.asList(
              factory.AND(env.getNextReilAddress(), i, src2, TranslationHelpers.getMaxImmediate(src2.size())),
              factory.SHRU(env.getNextReilAddress(), t, src1, i),
              factory.MOV(env.getNextReilAddress(), X86Helpers.BELOW_FLAG_OPERAND, t.withSize(OperandSize.BIT)),
              factory.SHL(env.getNextReilAddress(), m, factory.immediate(m.size(), 1), i),
              factory.XOR(env.getNextReilAddress(), r, src1, m),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.EQUAL_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.SIGN_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.BELOW_OR_EQUAL_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_OR_EQUAL_FLAG_OPERAND)));
    }
  }

  public static class BtrEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd r, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      RegisterTranslator registerTranslator = env.getRegisterTranslator();

      final LowLevelRReilOpnd i = registerTranslator.temporaryRegister(env, src2.size());
      final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, src1.size());
      final LowLevelRReilOpnd m1 = registerTranslator.temporaryRegister(env, t.size());
      final LowLevelRReilOpnd m2 = registerTranslator.temporaryRegister(env, t.size());

      instructions.addAll(
          Arrays.asList(
              factory.AND(env.getNextReilAddress(), i, src2, TranslationHelpers.getMaxImmediate(src2.size())),
              factory.SHRU(env.getNextReilAddress(), t, src1, i),
              factory.MOV(env.getNextReilAddress(), X86Helpers.BELOW_FLAG_OPERAND, t.withSize(OperandSize.BIT)),
              factory.SHL(env.getNextReilAddress(), m1, factory.immediate(m1.size(), 1), i),
              factory.XOR(env.getNextReilAddress(), m2, m1, TranslationHelpers.getMaxImmediate(m1.size())),
              factory.AND(env.getNextReilAddress(), r, src1, m2),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.EQUAL_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.SIGN_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.BELOW_OR_EQUAL_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_OR_EQUAL_FLAG_OPERAND)));
    }
  }

  public static class BtsEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd r, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      RegisterTranslator registerTranslator = env.getRegisterTranslator();

      final LowLevelRReilOpnd i = registerTranslator.temporaryRegister(env, src2.size());
      final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, src1.size());
      final LowLevelRReilOpnd m = registerTranslator.temporaryRegister(env, t.size());

      instructions.addAll(
          Arrays.asList(
              factory.AND(env.getNextReilAddress(), i, src2, TranslationHelpers.getMaxImmediate(src2.size())),
              factory.SHRU(env.getNextReilAddress(), t, src1, i),
              factory.MOV(env.getNextReilAddress(), X86Helpers.BELOW_FLAG_OPERAND, t.withSize(OperandSize.BIT)),
              factory.SHL(env.getNextReilAddress(), m, factory.immediate(m.size(), 1), i),
              factory.OR(env.getNextReilAddress(), r, src1, m),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.EQUAL_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.SIGN_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.BELOW_OR_EQUAL_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_OR_EQUAL_FLAG_OPERAND)));
    }
  }

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    RegisterTranslator registerTranslator = env.getRegisterTranslator();
    final List<? extends OperandTree> operands = instruction.operands();
    final OperandTree targetOperand = operands.get(0);
    final OperandTree sourceOperand = operands.get(1);
    final TranslationState opnd1 = X86OperandTranslator.translateOperand(env, targetOperand);
    final TranslationState opnd2 = X86OperandTranslator.translateOperand(env, sourceOperand);

    instructions.addAll(opnd1.getInstructionStack());
    instructions.addAll(opnd2.getInstructionStack());

    final LowLevelRReilOpnd src1 = opnd1.getOperandStack().pop();
    final LowLevelRReilOpnd src2 = opnd2.getOperandStack().pop();
    final LowLevelRReilOpnd dst = registerTranslator.temporaryRegister(env, src1.size());

    emitter.emit(env, dst, src1, src2, instructions);

    if (opnd1.getOperandStack().size() >= 1) {
      // Left hand side operand was a memory dereference.

      final LowLevelRReilOpnd addr = opnd1.getOperandStack().pop();

      instructions.add(factory.STORE(env.getNextReilAddress(), addr, dst));
    } else
      // TODO: Check if implicit zero extension is needed (x86_64)
      instructions.add(factory.MOV(env.getNextReilAddress(), src1, dst));
  }
}
