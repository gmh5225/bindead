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

public class X86RegTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private final InsnEmitter emitter;

  public X86RegTranslator (InsnEmitter emitter) {
    this.emitter = emitter;
  }

  public static class BswapEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      RegisterTranslator registerTranslator = env.getRegisterTranslator();

      if (dst.size() < 32) {
        instructions.add(factory.UNDEF(env.getNextReilAddress(), dst));
        return;
      }

      final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, dst.size());

      if (dst.size() == 32) {
        final LowLevelRReilOpnd b1 = t.withOffset(0, OperandSize.BYTE);
        final LowLevelRReilOpnd b2 = t.withOffset(8, OperandSize.BYTE);
        final LowLevelRReilOpnd b3 = t.withOffset(16, OperandSize.BYTE);
        final LowLevelRReilOpnd b4 = t.withOffset(24, OperandSize.BYTE);

        instructions.addAll(Arrays.asList(
            factory.MOV(env.getNextReilAddress(), b1, src1.withOffset(24, OperandSize.BYTE)),
            factory.MOV(env.getNextReilAddress(), b2, src1.withOffset(16, OperandSize.BYTE)),
            factory.MOV(env.getNextReilAddress(), b3, src1.withOffset(8, OperandSize.BYTE)),
            factory.MOV(env.getNextReilAddress(), b4, src1.withOffset(0, OperandSize.BYTE)),
            factory.MOV(env.getNextReilAddress(), dst, t)));

        return;
      }

      if (dst.size() == 64) {
        final LowLevelRReilOpnd b1 = t.withOffset(0, OperandSize.BYTE);
        final LowLevelRReilOpnd b2 = t.withOffset(8, OperandSize.BYTE);
        final LowLevelRReilOpnd b3 = t.withOffset(16, OperandSize.BYTE);
        final LowLevelRReilOpnd b4 = t.withOffset(24, OperandSize.BYTE);
        final LowLevelRReilOpnd b5 = t.withOffset(32, OperandSize.BYTE);
        final LowLevelRReilOpnd b6 = t.withOffset(40, OperandSize.BYTE);
        final LowLevelRReilOpnd b7 = t.withOffset(48, OperandSize.BYTE);
        final LowLevelRReilOpnd b8 = t.withOffset(56, OperandSize.BYTE);

        instructions.addAll(Arrays.asList(
            factory.MOV(env.getNextReilAddress(), b1, src1.withOffset(56, OperandSize.BYTE)),
            factory.MOV(env.getNextReilAddress(), b2, src1.withOffset(48, OperandSize.BYTE)),
            factory.MOV(env.getNextReilAddress(), b3, src1.withOffset(40, OperandSize.BYTE)),
            factory.MOV(env.getNextReilAddress(), b4, src1.withOffset(32, OperandSize.BYTE)),
            factory.MOV(env.getNextReilAddress(), b5, src1.withOffset(24, OperandSize.BYTE)),
            factory.MOV(env.getNextReilAddress(), b6, src1.withOffset(16, OperandSize.BYTE)),
            factory.MOV(env.getNextReilAddress(), b7, src1.withOffset(8, OperandSize.BYTE)),
            factory.MOV(env.getNextReilAddress(), b8, src1.withOffset(0, OperandSize.BYTE)),
            factory.MOV(env.getNextReilAddress(), dst, t)));

        return;
      }

      assert false;
    }
  }

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    final List<? extends OperandTree> operands = instruction.operands();
    final OperandTree targetOperand = operands.get(0);
    final TranslationState opnd1 = X86OperandTranslator.translateOperand(env, targetOperand);

    instructions.addAll(opnd1.getInstructionStack());

    assert opnd1.getOperandStack().size() == 1 : "Source/Destination operand must be a register";

    final LowLevelRReilOpnd dst = opnd1.getOperandStack().pop();
    final LowLevelRReilOpnd tmp = registerTranslator.temporaryRegister(env, dst.size());

    emitter.emit(env, tmp, dst, null, instructions);
    emitWritebackAndMaybeZeroExtend(env, dst, tmp, instructions);
  }
}
