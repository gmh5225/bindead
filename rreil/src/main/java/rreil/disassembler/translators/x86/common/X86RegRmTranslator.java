package rreil.disassembler.translators.x86.common;

import static rreil.disassembler.translators.x86.common.X86Helpers.AUXILIARY_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.BELOW_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.BELOW_OR_EQUAL_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.EQUAL_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.LESS_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.LESS_OR_EQUAL_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.OVERFLOW_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.PARITY_FLAG_OPERAND;
import static rreil.disassembler.translators.x86.common.X86Helpers.SIGN_FLAG_OPERAND;
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

public class X86RegRmTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private final InsnEmitter emitter;

  public X86RegRmTranslator (InsnEmitter emitter) {
    this.emitter = emitter;
  }

  public static class MovsxEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      instructions.add(factory.SIGNEXTEND(env.getNextReilAddress(), dst, src1));
    }
  }

  public static class MovzxEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      instructions.add(factory.CONVERT(env.getNextReilAddress(), dst, src1));
    }
  }

  public static class BsfEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd tmp, LowLevelRReilOpnd src1, LowLevelRReilOpnd _null, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd zero = factory.immediate(src1.size(), 0);
      final LowLevelRReilOpnd one = factory.immediate(OperandSize.BIT, 1);
      final LowLevelRReilOpnd srcNonZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd src = registerTranslator.temporaryRegister(env, src1.size());
      final LowLevelRReilOpnd bitIsSet = registerTranslator.temporaryRegister(env, OperandSize.BIT);

      final long base = env.getBaseAddress();
      final long reilBase = env.getCurrentReilOffset();
      final long loopInit = reilBase + 5L;
      final long loopBody = loopInit + 3L;
      final long done = loopBody + 5L;

      instructions.addAll(Arrays.asList(
          factory.CMPLTU(env.getNextReilAddress(), srcNonZero, zero, src1),
          factory.IFGOTORREIL(env.getNextReilAddress(), srcNonZero, base, loopInit),
          factory.UNDEF(env.getNextReilAddress(), tmp),
          factory.MOV(env.getNextReilAddress(), EQUAL_FLAG_OPERAND, one),
          factory.GOTORREIL(env.getNextReilAddress(), base, done),
          // LoopInit:
          factory.MOV(env.getNextReilAddress(), EQUAL_FLAG_OPERAND, zero.withSize(OperandSize.BIT)),
          factory.MOV(env.getNextReilAddress(), tmp, zero),
          factory.MOV(env.getNextReilAddress(), src, src1),
          // LoopBody:
          factory.CMPEQ(env.getNextReilAddress(), bitIsSet, src.withSize(OperandSize.BIT), one),
          factory.IFGOTORREIL(env.getNextReilAddress(), bitIsSet, base, done),
          factory.ADD(env.getNextReilAddress(), tmp, tmp, factory.immediate(tmp.size(), 1)),
          factory.SHRU(env.getNextReilAddress(), src, src, factory.immediate(src.size(), 1)),
          factory.GOTORREIL(env.getNextReilAddress(), base, loopBody),
          // Done:
          factory.UNDEF(env.getNextReilAddress(), AUXILIARY_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), PARITY_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), SIGN_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), BELOW_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), BELOW_OR_EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), LESS_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), LESS_OR_EQUAL_FLAG_OPERAND)));
    }
  }

  public static class BsrEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd tmp, LowLevelRReilOpnd src1, LowLevelRReilOpnd _null, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd zero = factory.immediate(src1.size(), 0);
      final LowLevelRReilOpnd one = factory.immediate(OperandSize.BIT, 1);
      final LowLevelRReilOpnd srcNonZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd src = registerTranslator.temporaryRegister(env, src1.size());
      final LowLevelRReilOpnd bitIsSet = registerTranslator.temporaryRegister(env, OperandSize.BIT);

      final long base = env.getBaseAddress();
      final long reilBase = env.getCurrentReilOffset();
      final long loopInit = reilBase + 5L;
      final long loopBody = loopInit + 3L;
      final long done = loopBody + 5L;

      instructions.addAll(
          Arrays.asList(
              factory.CMPLTU(env.getNextReilAddress(), srcNonZero, zero, src1),
              factory.IFGOTORREIL(env.getNextReilAddress(), srcNonZero, base, loopInit),
              factory.UNDEF(env.getNextReilAddress(), tmp),
              factory.MOV(env.getNextReilAddress(), EQUAL_FLAG_OPERAND, one),
              factory.GOTORREIL(env.getNextReilAddress(), base, done),
          // LoopInit:
              factory.MOV(env.getNextReilAddress(), EQUAL_FLAG_OPERAND, zero.withSize(OperandSize.BIT)),
              factory.MOV(env.getNextReilAddress(), tmp, factory.immediate(tmp.size(), tmp.size() - 1)),
              factory.MOV(env.getNextReilAddress(), src, src1),
          // LoopBody:
              factory.CMPEQ(env.getNextReilAddress(), bitIsSet, src.withOffset(src.size() - 1, OperandSize.BIT), one),
              factory.IFGOTORREIL(env.getNextReilAddress(), bitIsSet, base, done),
              factory.SUB(env.getNextReilAddress(), tmp, tmp, factory.immediate(tmp.size(), 1)),
              factory.SHL(env.getNextReilAddress(), src, src, factory.immediate(src.size(), 1)),
              factory.GOTORREIL(env.getNextReilAddress(), base, loopBody),
          // Done:
              factory.UNDEF(env.getNextReilAddress(), AUXILIARY_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), PARITY_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), SIGN_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), BELOW_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), BELOW_OR_EQUAL_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), LESS_FLAG_OPERAND),
              factory.UNDEF(env.getNextReilAddress(), LESS_OR_EQUAL_FLAG_OPERAND)));
    }
  }

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    final List<? extends OperandTree> operands = instruction.operands();
    final OperandTree targetOperand = operands.get(0);
    final OperandTree sourceOperand = operands.get(1);
    final TranslationState opnd1 = X86OperandTranslator.translateOperand(env, targetOperand);
    final TranslationState opnd2 = X86OperandTranslator.translateOperand(env, sourceOperand);

    instructions.addAll(opnd1.getInstructionStack());
    instructions.addAll(opnd2.getInstructionStack());

    assert (opnd1.getOperandStack().size() == 1) : "Destination operand must be a register";

    final LowLevelRReilOpnd dst = opnd1.getOperandStack().pop();
    final LowLevelRReilOpnd src = opnd2.getOperandStack().pop();
    final LowLevelRReilOpnd tmp = registerTranslator.temporaryRegister(env, dst.size());

    emitter.emit(env, tmp, src, null, instructions);
    emitWritebackAndMaybeZeroExtend(env, dst, tmp, instructions);
  }
}
