package rreil.disassembler.translators.x86.common;

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

public class IdivTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private static final InsnEmitter emitter = new X86FlagHelpers.UndefineFlagEmitter();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    RegisterTranslator registerTranslator = env.getRegisterTranslator();

    final List<? extends OperandTree> operands = instruction.operands();
    final OperandTree operand = operands.get(0);
    final TranslationState opnd = X86OperandTranslator.translateOperand(env, operand);

    instructions.addAll(opnd.getInstructionStack());

    final LowLevelRReilOpnd divisor = opnd.getOperandStack().pop();
    final int size = divisor.size();

    switch (size) {
      case 8: {
        // AX
        final LowLevelRReilOpnd ax = registerTranslator.translateRegister("ax");
        final LowLevelRReilOpnd ah = registerTranslator.translateRegister("ah");
        final LowLevelRReilOpnd al = registerTranslator.translateRegister("al");
        final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, OperandSize.WORD);
        final LowLevelRReilOpnd q = registerTranslator.temporaryRegister(env, OperandSize.WORD);
        final LowLevelRReilOpnd r = registerTranslator.temporaryRegister(env, OperandSize.WORD);

        instructions.addAll(Arrays.asList(
            factory.SIGNEXTEND(env.getNextReilAddress(), t, divisor),
            factory.DIVS(env.getNextReilAddress(), q, ax, t),
            factory.MOD(env.getNextReilAddress(), r, ax, t),
            factory.MOV(env.getNextReilAddress(), ah, r.withSize(OperandSize.BYTE)),
            factory.MOV(env.getNextReilAddress(), al, q.withSize(OperandSize.BYTE))));
      }
      break;
      case 16: {
        // DX:AX
        final LowLevelRReilOpnd dx = registerTranslator.translateRegister("dx");
        final LowLevelRReilOpnd ax = registerTranslator.translateRegister("ax");
        final LowLevelRReilOpnd src = registerTranslator.temporaryRegister(env, OperandSize.DWORD);
        final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, OperandSize.DWORD);
        final LowLevelRReilOpnd q = registerTranslator.temporaryRegister(env, OperandSize.DWORD);
        final LowLevelRReilOpnd r = registerTranslator.temporaryRegister(env, OperandSize.DWORD);

        instructions.addAll(Arrays.asList(
            factory.SIGNEXTEND(env.getNextReilAddress(), t, divisor),
            factory.MOV(env.getNextReilAddress(), src.withSize(OperandSize.WORD), ax),
            factory.MOV(env.getNextReilAddress(), src.withOffset(16, OperandSize.WORD), dx),
            factory.DIVS(env.getNextReilAddress(), q, src, t),
            factory.MOD(env.getNextReilAddress(), r, src, t),
            factory.MOV(env.getNextReilAddress(), dx, r.withSize(OperandSize.WORD)),
            factory.MOV(env.getNextReilAddress(), ax, q.withSize(OperandSize.WORD))));
      }
      break;
      case 32: {
        // EDX:EAX
        final LowLevelRReilOpnd edx = registerTranslator.translateRegister("edx");
        final LowLevelRReilOpnd eax = registerTranslator.translateRegister("eax");
        final LowLevelRReilOpnd src = registerTranslator.temporaryRegister(env, OperandSize.QWORD);
        final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, OperandSize.QWORD);
        final LowLevelRReilOpnd q = registerTranslator.temporaryRegister(env, OperandSize.QWORD);
        final LowLevelRReilOpnd r = registerTranslator.temporaryRegister(env, OperandSize.QWORD);

        instructions.addAll(Arrays.asList(
            factory.SIGNEXTEND(env.getNextReilAddress(), t, divisor),
            factory.MOV(env.getNextReilAddress(), src.withSize(OperandSize.DWORD), eax),
            factory.MOV(env.getNextReilAddress(), src.withOffset(32, OperandSize.DWORD), edx),
            factory.DIVS(env.getNextReilAddress(), q, src, t),
            factory.MOD(env.getNextReilAddress(), r, src, t)));

        // TODO: Check implicit zero extension!
        X86Helpers.emitWritebackAndMaybeZeroExtend(env, eax, q.withSize(OperandSize.DWORD), instructions);
        X86Helpers.emitWritebackAndMaybeZeroExtend(env, edx, r.withSize(OperandSize.DWORD), instructions);
      }
      break;
      case 64: {
        // RDX:RAX
        final LowLevelRReilOpnd rdx = registerTranslator.translateRegister("rdx");
        final LowLevelRReilOpnd rax = registerTranslator.translateRegister("rax");
        final LowLevelRReilOpnd src = registerTranslator.temporaryRegister(env, OperandSize.OWORD);
        final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, OperandSize.OWORD);
        final LowLevelRReilOpnd q = registerTranslator.temporaryRegister(env, OperandSize.OWORD);
        final LowLevelRReilOpnd r = registerTranslator.temporaryRegister(env, OperandSize.OWORD);

        instructions.addAll(Arrays.asList(
            factory.SIGNEXTEND(env.getNextReilAddress(), t, divisor),
            factory.MOV(env.getNextReilAddress(), src.withSize(OperandSize.QWORD), rax),
            factory.MOV(env.getNextReilAddress(), src.withOffset(64, OperandSize.QWORD), rdx),
            factory.DIVS(env.getNextReilAddress(), q, src, t),
            factory.MOD(env.getNextReilAddress(), r, src, t),
            factory.MOV(env.getNextReilAddress(), rdx, r.withSize(OperandSize.QWORD)),
            factory.MOV(env.getNextReilAddress(), rax, q.withSize(OperandSize.QWORD))));
      }
      break;
      default:
        throw new RuntimeException("Error: Invalid operand size");
    }

    // Undefine all flags.
    emitter.emit(env, null, null, null, instructions);
  }
}
