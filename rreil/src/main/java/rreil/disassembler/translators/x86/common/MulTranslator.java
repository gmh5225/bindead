package rreil.disassembler.translators.x86.common;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.OperandTree;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class MulTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    final List<? extends OperandTree> operands = instruction.operands();
    final OperandTree operand = operands.get(0);
    final TranslationState opnd = X86OperandTranslator.translateOperand(env, operand);
    instructions.addAll(opnd.getInstructionStack());
    final LowLevelRReilOpnd src = opnd.getOperandStack().pop();
    final int size = src.size();

    switch (size) {
      case 8: {
        // AX := AL * src
        final LowLevelRReilOpnd ax = registerTranslator.translateRegister("ax");
        final LowLevelRReilOpnd al = registerTranslator.translateRegister("al");
        final LowLevelRReilOpnd op1 = registerTranslator.temporaryRegister(env, OperandSize.WORD);
        final LowLevelRReilOpnd op2 = registerTranslator.temporaryRegister(env, OperandSize.WORD);

        instructions.addAll(
            Arrays.asList(
                factory.CONVERT(env.getNextReilAddress(), op1, al),
                factory.CONVERT(env.getNextReilAddress(), op2, src),
                factory.MUL(env.getNextReilAddress(), ax, op1, op2)));

        emitFlags(env, ax, al, instructions);
      }
      break;
      case 16: {
        // DX:AX := AX * src
        final LowLevelRReilOpnd dx = registerTranslator.translateRegister("dx");
        final LowLevelRReilOpnd ax = registerTranslator.translateRegister("ax");
        final LowLevelRReilOpnd op1 = registerTranslator.temporaryRegister(env, OperandSize.DWORD);
        final LowLevelRReilOpnd op2 = registerTranslator.temporaryRegister(env, OperandSize.DWORD);
        final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, OperandSize.DWORD);

        instructions.addAll(
            Arrays.asList(
                factory.CONVERT(env.getNextReilAddress(), op1, ax),
                factory.CONVERT(env.getNextReilAddress(), op2, src),
                factory.MUL(env.getNextReilAddress(), t, op1, op2),
                factory.MOV(env.getNextReilAddress(), ax, t.withSize(OperandSize.WORD)),
                factory.MOV(env.getNextReilAddress(), dx, t.withOffset(16, OperandSize.WORD))));

        emitFlags(env, t, ax, instructions);
      }
      break;
      case 32: {
        // EDX:EAX := AX * src
        final LowLevelRReilOpnd edx = registerTranslator.translateRegister("edx");
        final LowLevelRReilOpnd eax = registerTranslator.translateRegister("eax");
        final LowLevelRReilOpnd op1 = registerTranslator.temporaryRegister(env, OperandSize.QWORD);
        final LowLevelRReilOpnd op2 = registerTranslator.temporaryRegister(env, OperandSize.QWORD);
        final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, OperandSize.QWORD);

        instructions.addAll(
            Arrays.asList(
                factory.CONVERT(env.getNextReilAddress(), op1, eax),
                factory.CONVERT(env.getNextReilAddress(), op2, src),
                factory.MUL(env.getNextReilAddress(), t, op1, op2)));
        // TODO: Check implicit zero extension!
        X86Helpers.emitWritebackAndMaybeZeroExtend(env, eax, t.withSize(OperandSize.DWORD), instructions);
        X86Helpers.emitWritebackAndMaybeZeroExtend(env, edx, t.withOffset(32, OperandSize.DWORD), instructions);

        emitFlags(env, t, eax, instructions);
      }
      break;
      case 64: {
        // RDX:RAX := RAX * src
        final LowLevelRReilOpnd rdx = registerTranslator.translateRegister("rdx");
        final LowLevelRReilOpnd rax = registerTranslator.translateRegister("rax");
        final LowLevelRReilOpnd op1 = registerTranslator.temporaryRegister(env, OperandSize.OWORD);
        final LowLevelRReilOpnd op2 = registerTranslator.temporaryRegister(env, OperandSize.OWORD);
        final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, OperandSize.OWORD);

        instructions.addAll(
            Arrays.asList(
                factory.CONVERT(env.getNextReilAddress(), op1, rax),
                factory.CONVERT(env.getNextReilAddress(), op2, src),
                factory.MUL(env.getNextReilAddress(), t, op1, op2),
                factory.MOV(env.getNextReilAddress(), rax, t.withSize(OperandSize.QWORD)),
                factory.MOV(env.getNextReilAddress(), rdx, t.withOffset(64, OperandSize.QWORD))));

        emitFlags(env, t, rax, instructions);
      }
      break;
      default:
        throw new RuntimeException("Error: Invalid operand size");
    }

    emitUndefineFlags(env, instructions);
  }

  private static void emitUndefineFlags (TranslationCtx env, List<LowLevelRReil> instructions) {
    instructions.addAll(
        Arrays.asList(
            factory.UNDEF(env.getNextReilAddress(), X86Helpers.SIGN_FLAG_OPERAND),
            factory.UNDEF(env.getNextReilAddress(), X86Helpers.EQUAL_FLAG_OPERAND),
            factory.UNDEF(env.getNextReilAddress(), X86Helpers.PARITY_FLAG_OPERAND),
            factory.UNDEF(env.getNextReilAddress(), X86Helpers.AUXILIARY_FLAG_OPERAND),
            factory.UNDEF(env.getNextReilAddress(), X86Helpers.BELOW_OR_EQUAL_FLAG_OPERAND),
            factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_FLAG_OPERAND),
            factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_OR_EQUAL_FLAG_OPERAND)));
  }

  private static void emitFlags (
      TranslationCtx env, LowLevelRReilOpnd precise, LowLevelRReilOpnd actual, List<LowLevelRReil> instructions) {
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, precise.size());

    instructions.addAll(
        Arrays.asList(
            factory.CONVERT(env.getNextReilAddress(), t, actual),
            factory.CMPEQ(env.getNextReilAddress(), X86Helpers.BELOW_FLAG_OPERAND, precise, t),
            factory.CMPEQ(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND, precise, t)));
  }
}
