package rreil.disassembler.translators.x86.common;

import static rreil.disassembler.translators.x86.common.X86Helpers.emitWritebackAndMaybeZeroExtend;

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

public class X86RmTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private final InsnEmitter emitter;

  public X86RmTranslator (InsnEmitter emitter) {
    this.emitter = emitter;
  }

  public static class NegEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src, LowLevelRReilOpnd _null, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd zxCond = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, src.size());
      final LowLevelRReilOpnd zero = factory.immediate(src.size(), 0);
      final LowLevelRReilOpnd f1 = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd f2 = registerTranslator.temporaryRegister(env, OperandSize.BIT);

      instructions.addAll(Arrays.asList(
          factory.CMPEQ(env.getNextReilAddress(), zxCond, src, factory.immediate(src.size(), 0)),
          factory.NOT(env.getNextReilAddress(), X86Helpers.BELOW_FLAG_OPERAND, zxCond),
          factory.SIGNEXTEND(env.getNextReilAddress(), t, src),
          factory.MUL(env.getNextReilAddress(), dst, t, TranslationHelpers.getMaxImmediate(t.size())),
          factory.CMPEQ(env.getNextReilAddress(), X86Helpers.EQUAL_FLAG_OPERAND, dst, zero),
          factory.CMPLTS(env.getNextReilAddress(), X86Helpers.SIGN_FLAG_OPERAND, dst, zero),
          // The OF is set if the original value was the lowest negative value of the target
          // Example: EAX => OF is set if value was 0x80000000
          // If that happens, the MSB of the operand and the result must both be set.
          factory.CMPLES(env.getNextReilAddress(), f1, src, TranslationHelpers.getMinImmediateSigned(dst.size())),
          factory.CMPLES(env.getNextReilAddress(), f2, dst, TranslationHelpers.getMinImmediateSigned(dst.size())),
          factory.AND(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND, f1, f2)));
    }
  }

  public static class NotEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src, LowLevelRReilOpnd _null, List<LowLevelRReil> instructions) {
      instructions.add(factory.XOR(env.getNextReilAddress(), dst, src, TranslationHelpers.getMaxImmediate(src.size())));
    }
  }

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    final List<? extends OperandTree> operands = instruction.operands();
    final OperandTree operand = operands.get(0);
    final TranslationState opnd = X86OperandTranslator.translateOperand(env, operand);
    instructions.addAll(opnd.getInstructionStack());
    final LowLevelRReilOpnd src = opnd.getOperandStack().pop();
    final LowLevelRReilOpnd tmp = registerTranslator.temporaryRegister(env, src.size());

    emitter.emit(env, tmp, src, null, instructions);

    if (opnd.getOperandStack().size() >= 1) {
      // Left hand side operand was a memory dereference.
      // OperandStack size greater than one means the operand contained a memory dereference.
      // Since `opnd` is also the destination operand on x86 we have to take care of this
      // when writing back the result.

      final LowLevelRReilOpnd addr = opnd.getOperandStack().pop();
      instructions.add(factory.STORE(env.getNextReilAddress(), addr, tmp));
    } else
      //	No memory dereference in `opnd`.
      emitWritebackAndMaybeZeroExtend(env, src, tmp, instructions);
  }
}
