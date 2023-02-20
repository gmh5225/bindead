package rreil.disassembler.translators.x86.common;

import static rreil.disassembler.translators.x86.common.X86Helpers.emitWritebackAndMaybeZeroExtend;

import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.OperandTree;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class SarTranslator extends X86RmRmTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public SarTranslator () {
    super(new X86RmRmTranslator.SarEmitter());
  }

  @Override public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    final List<? extends OperandTree> operands = instruction.operands();

    final OperandTree targetOperand = operands.get(0);
    final OperandTree sourceOperand = operands.get(1);

    final TranslationState opnd1 = X86OperandTranslator.translateOperand(env, targetOperand);
    final TranslationState opnd2 = X86OperandTranslator.translateOperand(env, sourceOperand);

    instructions.addAll(opnd1.getInstructionStack());
    instructions.addAll(opnd2.getInstructionStack());

    final LowLevelRReilOpnd src1 = opnd1.getOperandStack().pop();
    final LowLevelRReilOpnd src2 = factory.immediateSizeFixup(src1.size(), opnd2.getOperandStack().pop());  // XXX: signedImmediateSize
    final LowLevelRReilOpnd tmp = registerTranslator.temporaryRegister(env, src1.size());

    emitter.emit(env, tmp, src1, src2, instructions);

    if (opnd1.getOperandStack().size() >= 1) {
      // Left hand side operand was a memory dereference.
      // OperandStack size greater than one means the operand contained a memory dereference.
      // Since `opnd1` is also the destination operand on x86 we have to take care of this
      // when writing back the result.

      final LowLevelRReilOpnd addr = opnd1.getOperandStack().pop();
      instructions.add(factory.STORE(env.getNextReilAddress(), addr, tmp));
    } else
      //  No memory dereference in `opnd1`.
      emitWritebackAndMaybeZeroExtend(env, src1, tmp, instructions);
  }
}
