package rreil.disassembler.translators.x86.common;

import static rreil.disassembler.translators.x86.common.X86Helpers.emitWritebackAndMaybeZeroExtend;

import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.OperandTree;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class LeaTranslator implements InsnTranslator {
  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final List<? extends OperandTree> operands = instruction.operands();
    final OperandTree targetOperand = operands.get(0);
    final OperandTree sourceOperand = operands.get(1);
    final TranslationState opnd1 = X86OperandTranslator.translateOperand(env, targetOperand, false);
    final TranslationState opnd2 = X86OperandTranslator.translateOperand(env, sourceOperand, false);

    instructions.addAll(opnd1.getInstructionStack());
    instructions.addAll(opnd2.getInstructionStack());

    assert opnd1.getOperandStack().size() == 1 : "Destination operand must be a register";

    final LowLevelRReilOpnd dst = opnd1.getOperandStack().pop();

    // pop dummy value
    opnd2.getOperandStack().pop();
    final LowLevelRReilOpnd addr = opnd2.getOperandStack().pop();

    emitWritebackAndMaybeZeroExtend(env, dst, addr.withSize(dst.size()), instructions);
  }
}
