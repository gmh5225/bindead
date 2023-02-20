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
import rreil.lang.lowlevel.TranslationHelpers;

public class ImulTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);

    switch (instruction.operands().size()) {
      case 1:
        translate1(env, instruction, instructions);
        break;
      case 2:
        translate2(env, instruction, instructions);
        break;
      default:
        translate3(env, instruction, instructions);
        break;
    }
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

    instructions.addAll(Arrays.asList(
        factory.SIGNEXTEND(env.getNextReilAddress(), t, actual),
        factory.CMPEQ(env.getNextReilAddress(), X86Helpers.BELOW_FLAG_OPERAND, precise, t),
        factory.CMPEQ(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND, precise, t)));
  }

  private static void translate1 (TranslationCtx env, Instruction instruction, List<LowLevelRReil> instructions) {
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();

    final List<? extends OperandTree> operands = instruction.operands();
    final OperandTree operand = operands.get(0);

    final TranslationState opnd =
        X86OperandTranslator.translateOperand(env, operand);

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

        instructions.addAll(Arrays.asList(
            factory.SIGNEXTEND(env.getNextReilAddress(), op1, al),
            factory.SIGNEXTEND(env.getNextReilAddress(), op2, src),
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

        instructions.addAll(Arrays.asList(
            factory.SIGNEXTEND(env.getNextReilAddress(), op1, ax),
            factory.SIGNEXTEND(env.getNextReilAddress(), op2, src),
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

        instructions.addAll(Arrays.asList(
            factory.SIGNEXTEND(env.getNextReilAddress(), op1, eax),
            factory.SIGNEXTEND(env.getNextReilAddress(), op2, src),
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

        instructions.addAll(Arrays.asList(
            factory.SIGNEXTEND(env.getNextReilAddress(), op1, rax),
            factory.SIGNEXTEND(env.getNextReilAddress(), op2, src),
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

  private static void translate2 (TranslationCtx env, Instruction instruction, List<LowLevelRReil> instructions) {
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();

    final List<? extends OperandTree> operands = instruction.operands();
    final OperandTree operand1 = operands.get(0);
    final OperandTree operand2 = operands.get(1);

    final TranslationState opnd1 =
        X86OperandTranslator.translateOperand(env, operand1);

    final TranslationState opnd2 =
        X86OperandTranslator.translateOperand(env, operand2);

    instructions.addAll(opnd1.getInstructionStack());
    instructions.addAll(opnd2.getInstructionStack());

    final LowLevelRReilOpnd dst = opnd1.getOperandStack().peek();
    final int preciseSize = TranslationHelpers.getNextSize(dst.size());

    final LowLevelRReilOpnd src1 = opnd1.getOperandStack().pop();
    final LowLevelRReilOpnd src2 = opnd2.getOperandStack().pop();
    final LowLevelRReilOpnd preciseSrc1 = registerTranslator.temporaryRegister(env, preciseSize);
    final LowLevelRReilOpnd preciseSrc2 = registerTranslator.temporaryRegister(env, preciseSize);
    final LowLevelRReilOpnd precise = registerTranslator.temporaryRegister(env, preciseSize);

    instructions.addAll(Arrays.asList(
        factory.SIGNEXTEND(env.getNextReilAddress(), preciseSrc1, src1),
        factory.SIGNEXTEND(env.getNextReilAddress(), preciseSrc2, src2),
        factory.MUL(env.getNextReilAddress(), precise, preciseSrc1, preciseSrc2) //MUL(env.getNextReilAddress(), dst, src1, src2)
        ));
    // TODO: Check implicit zero extension!
    X86Helpers.emitWritebackAndMaybeZeroExtend(env, dst, precise.withSize(dst.size()), instructions);

    emitFlags(env, precise, dst, instructions);
    emitUndefineFlags(env, instructions);
  }

  private static void translate3 (TranslationCtx env, Instruction instruction, List<LowLevelRReil> instructions) {
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();

    final List<? extends OperandTree> operands = instruction.operands();
    final OperandTree operand1 = operands.get(0);
    final OperandTree operand2 = operands.get(1);
    final OperandTree operand3 = operands.get(2);

    final TranslationState opnd1 =
        X86OperandTranslator.translateOperand(env, operand1);

    final TranslationState opnd2 =
        X86OperandTranslator.translateOperand(env, operand2);

    final TranslationState opnd3 =
        X86OperandTranslator.translateOperand(env, operand3);

    instructions.addAll(opnd1.getInstructionStack());
    instructions.addAll(opnd2.getInstructionStack());
    instructions.addAll(opnd3.getInstructionStack());

    final LowLevelRReilOpnd dst = opnd1.getOperandStack().peek();
    final int preciseSize = TranslationHelpers.getNextSize(dst.size());

    final LowLevelRReilOpnd src1 = opnd2.getOperandStack().pop();
    final LowLevelRReilOpnd src2 = opnd3.getOperandStack().pop();
    final LowLevelRReilOpnd preciseSrc1 = registerTranslator.temporaryRegister(env, preciseSize);
    final LowLevelRReilOpnd preciseSrc2 = registerTranslator.temporaryRegister(env, preciseSize);
    final LowLevelRReilOpnd precise = registerTranslator.temporaryRegister(env, preciseSize);

    instructions.addAll(Arrays.asList(
        factory.SIGNEXTEND(env.getNextReilAddress(), preciseSrc1, src1),
        factory.SIGNEXTEND(env.getNextReilAddress(), preciseSrc2, src2),
        factory.MUL(env.getNextReilAddress(), precise, preciseSrc1, preciseSrc2)));
    // TODO: Check implicit zero extension!
    X86Helpers.emitWritebackAndMaybeZeroExtend(env, dst, precise.withSize(dst.size()), instructions);

    emitFlags(env, precise, dst, instructions);
    emitUndefineFlags(env, instructions);
  }
}
