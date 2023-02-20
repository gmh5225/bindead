package rreil.disassembler.translators.x86.common;

import static rreil.disassembler.translators.x86.common.X86Helpers.emitWritebackAndMaybeZeroExtend;

import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.OperandTree;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class MovTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override public void translate (TranslationCtx env, Instruction instruction, List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    List<? extends OperandTree> operands = instruction.operands();
    OperandTree targetOperand = operands.get(0);
    OperandTree sourceOperand = operands.get(1);
    TranslationState opnd1 = X86OperandTranslator.translateOperand(env, targetOperand, false);
    TranslationState opnd2 = X86OperandTranslator.translateOperand(env, sourceOperand);

    instructions.addAll(opnd1.getInstructionStack());
    instructions.addAll(opnd2.getInstructionStack());

    if (opnd1.getOperandStack().size() > 1) {
      // Left hand side operand was a memory dereference.
      // OperandStack size greater than one means the operand contained a memory dereference.
      // Since `opnd1` is also the destination operand on x86 we have to take care of this
      // when writing back the result.
      LowLevelRReilOpnd dst = opnd1.getOperandStack().pop(); // Unused, just to consume all parameters from the stack
      LowLevelRReilOpnd addr = opnd1.getOperandStack().pop();
      LowLevelRReilOpnd src = opnd2.getOperandStack().pop();
      instructions.add(factory.STORE(env.getNextReilAddress(), addr, factory.immediateSizeFixup(dst.size(), src)));
    } else {
      // No memory dereference in `opnd1`.
      LowLevelRReilOpnd dst = opnd1.getOperandStack().pop();
      LowLevelRReilOpnd src = factory.immediateSizeFixup(dst.size(), opnd2.getOperandStack().pop());  // XXX: signedImmediateSize
      emitWritebackAndMaybeZeroExtend(env, dst, src, instructions);
    }
  }
}
