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
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class PopTranslator implements InsnTranslator {
  private final InsnEmitter emitter;
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();


  public PopTranslator (InsnEmitter emitter) {
    this.emitter = emitter;
  }

  public static class PopEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd _null1, LowLevelRReilOpnd _null2, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd esp = registerTranslator.translateRegister("esp").withSize(env.getDefaultArchitectureSize());
      final LowLevelRReilOpnd inc = factory.immediate(esp.size(), dst.size() / 8); // TODO: Check division of size annotation.

      instructions.addAll(Arrays.asList(
          factory.LOAD(env.getNextReilAddress(), dst, esp),
          factory.ADD(env.getNextReilAddress(), esp, esp, inc)));
    }
  }

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    final List<? extends OperandTree> operands = instruction.operands();
    final OperandTree operand = operands.get(0);
    final TranslationState opnd = X86OperandTranslator.translateOperand(env, operand, false);
    instructions.addAll(opnd.getInstructionStack());

    if (opnd.getOperandStack().size() > 1) {
      // Left hand side operand was a memory dereference.
      // OperandStack size greater than one means the operand contained a memory dereference.
      // Since `opnd` is also the destination operand on x86 we have to take care of this
      // when writing back the result.

      final LowLevelRReilOpnd dummy = opnd.getOperandStack().pop();
      final LowLevelRReilOpnd addr = opnd.getOperandStack().pop();

      emitter.emit(env, dummy, null, null, instructions);
      instructions.add(factory.STORE(env.getNextReilAddress(), addr, dummy));

    } else {
      //	No memory dereference in `opnd`.

      final LowLevelRReilOpnd dst = opnd.getOperandStack().pop();
      final LowLevelRReilOpnd tmp = registerTranslator.temporaryRegister(env, dst.size());

      emitter.emit(env, tmp, null, null, instructions);
      emitWritebackAndMaybeZeroExtend(env, dst, tmp, instructions);
    }
  }
}
