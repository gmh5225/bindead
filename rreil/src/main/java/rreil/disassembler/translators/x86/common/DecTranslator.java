package rreil.disassembler.translators.x86.common;

import static rreil.disassembler.translators.x86.common.X86Helpers.emitWritebackAndMaybeZeroExtend;

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

public class DecTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private static final InsnEmitter emitter = new X86FlagHelpers.DecFlagEmitter();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    RegisterTranslator registerTranslator = env.getRegisterTranslator();

    final List<OperandTree> operands = instruction.operands();
    final OperandTree operand = operands.get(0);
    final TranslationState opnd = X86OperandTranslator.translateOperand(env, operand);

    instructions.addAll(opnd.getInstructionStack());

    if (opnd.getOperandStack().size() > 1) {
      // Left hand side operand was a memory dereference.
      // OperandStack size greater than one means the operand contained a memory dereference.
      // Since `opnd` is also the destination operand on x86 we have to take care of this
      // when writing back the result.

      final LowLevelRReilOpnd src = opnd.getOperandStack().pop();
      final LowLevelRReilOpnd addr = opnd.getOperandStack().pop();
      final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, src.size());
      final LowLevelRReilOpnd one = factory.immediate(src.size(), 1);

      instructions.add(factory.SUB(env.getNextReilAddress(), t, src, one));
      emitter.emit(env, t, src, one, instructions);
      instructions.add(factory.STORE(env.getNextReilAddress(), addr, t));

    } else {
      //	No memory dereference in `opnd`.

      final LowLevelRReilOpnd src = opnd.getOperandStack().pop();
      final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, src.size());
      final LowLevelRReilOpnd one = factory.immediate(src.size(), 1);

      instructions.add(factory.SUB(env.getNextReilAddress(), t, src, one));
      emitter.emit(env, t, src, one, instructions);
      emitWritebackAndMaybeZeroExtend(env, src, t, instructions);
    }
  }
}
