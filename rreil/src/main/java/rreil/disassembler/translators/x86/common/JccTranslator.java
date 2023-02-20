package rreil.disassembler.translators.x86.common;

import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.OperandTree;
import rreil.disassembler.translators.common.CondEmitter;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class JccTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private final CondEmitter emitter;

  public JccTranslator (CondEmitter emitter) {
    this.emitter = emitter;
  }

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final List<? extends OperandTree> operands = instruction.operands();
    final OperandTree operand = operands.get(0);
    final TranslationState opnd = X86OperandTranslator.translateOperand(env, operand);

    instructions.addAll(opnd.getInstructionStack());

    final LowLevelRReilOpnd cond = emitter.emit(env, instructions);
    final LowLevelRReilOpnd target = opnd.getOperandStack().pop();

    instructions.add(factory.IFGOTO(env.getNextReilAddress(), cond, target));
  }
}
