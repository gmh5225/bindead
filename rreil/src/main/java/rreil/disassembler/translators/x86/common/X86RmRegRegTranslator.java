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
import rreil.lang.lowlevel.TranslationHelpers;

public class X86RmRegRegTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private final InsnEmitter emitter;

  public X86RmRegRegTranslator (InsnEmitter emitter) {
    this.emitter = emitter;
  }

  public static class ShldEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, LowLevelRReilOpnd src3, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final int opndSz = src1.size();
      final int preciseSz = TranslationHelpers.getNextSize(opndSz);
      final LowLevelRReilOpnd a = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd b = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd c = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd r = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd r2 = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd c2 = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, opndSz);
      final LowLevelRReilOpnd cntZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cntOutOfRange = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cntOne = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd bits = factory.immediate(preciseSz, opndSz);
      final LowLevelRReilOpnd zero = factory.immediate(preciseSz, 0);
      final LowLevelRReilOpnd one = factory.immediate(preciseSz, 1);

      instructions.addAll(Arrays.asList(
          factory.CONVERT(env.getNextReilAddress(), a, src1),
          factory.CONVERT(env.getNextReilAddress(), b, src2),
          factory.CONVERT(env.getNextReilAddress(), c, src3)));

      LowLevelRReilOpnd maxCnt;
      if (env.getDefaultArchitectureSize() == 64) {
        if (opndSz <= 32)
          maxCnt = bits;
        else
          maxCnt = factory.immediate(preciseSz, 64);
        instructions.add(factory.MOD(env.getNextReilAddress(), c, c, maxCnt));
      } else
        maxCnt = bits;

      final long base = env.getBaseAddress();
      final long reilBase = env.getCurrentReilOffset();
      final long done = reilBase + 39L;
      final long error = reilBase + 29L;
      final long setOf = reilBase + 16L;
      final long flags = setOf + 6L;

      instructions.addAll(Arrays.asList(
          //cnt==0?
          factory.CMPEQ(env.getNextReilAddress(), cntZero, c, zero),
          factory.IFGOTORREIL(env.getNextReilAddress(), cntZero, base, done),
          //cnt>maxCnt?
          factory.CMPLTU(env.getNextReilAddress(), cntOutOfRange, maxCnt, c),
          factory.IFGOTORREIL(env.getNextReilAddress(), cntOutOfRange, base, error),
          // Doit:
          factory.SHL(env.getNextReilAddress(), a, a, bits),
          factory.OR(env.getNextReilAddress(), a, a, b),
          factory.SHL(env.getNextReilAddress(), r, a, c),
          factory.SHRU(env.getNextReilAddress(), r, r, bits),
          // Overflow flag
          //cnt==1?
          factory.CMPEQ(env.getNextReilAddress(), cntOne, c, one),
          factory.IFGOTORREIL(env.getNextReilAddress(), cntOne, base, setOf),
          factory.UNDEF(env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), LESS_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), LESS_OR_EQUAL_FLAG_OPERAND),
          factory.CMPLTS(env.getNextReilAddress(), SIGN_FLAG_OPERAND, r.withSize(opndSz), zero.withSize(opndSz)),
          factory.CMPEQ(env.getNextReilAddress(), EQUAL_FLAG_OPERAND, r.withSize(opndSz), zero.withSize(opndSz)),
          factory.GOTORREIL(env.getNextReilAddress(), base, flags),
          //SetOf:
          factory.XOR(env.getNextReilAddress(), t, src1, r.withSize(opndSz)),
          factory.MOV(
          env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND, t.withOffset(opndSz - 1, OperandSize.BIT)),
          factory.CMPEQ(env.getNextReilAddress(), EQUAL_FLAG_OPERAND, r.withSize(opndSz), zero.withSize(opndSz)),
          factory.CMPLTS(env.getNextReilAddress(), SIGN_FLAG_OPERAND, r.withSize(opndSz), zero.withSize(opndSz)),
          factory.XOR(env.getNextReilAddress(), LESS_FLAG_OPERAND, SIGN_FLAG_OPERAND, OVERFLOW_FLAG_OPERAND),
          factory.OR(env.getNextReilAddress(), LESS_OR_EQUAL_FLAG_OPERAND, LESS_FLAG_OPERAND, EQUAL_FLAG_OPERAND),
          // Flags:
          // Carry flag
          factory.SUB(env.getNextReilAddress(), c2, c, factory.immediate(preciseSz, 1)),
          factory.SHL(env.getNextReilAddress(), r2, a, c2),
          factory.MOV(env.getNextReilAddress(), BELOW_FLAG_OPERAND, r2.withOffset(preciseSz - 1, 1)),
          // Writeback
          factory.MOV(env.getNextReilAddress(), src1, r.withSize(opndSz)),
          // Other flags
          factory.OR(env.getNextReilAddress(), BELOW_OR_EQUAL_FLAG_OPERAND, BELOW_FLAG_OPERAND, EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), AUXILIARY_FLAG_OPERAND),
          factory.GOTORREIL(env.getNextReilAddress(), base, done),
          // Error: Invalid parameters
          factory.UNDEF(env.getNextReilAddress(), AUXILIARY_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), PARITY_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), SIGN_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), BELOW_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), BELOW_OR_EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), LESS_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), LESS_OR_EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), src1),
          // Done:
          factory.NOP(env.getNextReilAddress())));
    }
  }

  public static class ShrdEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, LowLevelRReilOpnd src3, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final int opndSz = src1.size();
      final int preciseSz = TranslationHelpers.getNextSize(opndSz);
      final LowLevelRReilOpnd a = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd b = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd c = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd r = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd r2 = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd c2 = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd t = registerTranslator.temporaryRegister(env, opndSz);
      final LowLevelRReilOpnd cntZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cntOutOfRange = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cntOne = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd bits = factory.immediate(preciseSz, opndSz);
      final LowLevelRReilOpnd zero = factory.immediate(preciseSz, 0);
      final LowLevelRReilOpnd one = factory.immediate(preciseSz, 1);

      instructions.addAll(
          Arrays.asList(
              factory.CONVERT(env.getNextReilAddress(), a, src1),
          factory.CONVERT(env.getNextReilAddress(), b, src2),
          factory.CONVERT(env.getNextReilAddress(), c, src3)));

      LowLevelRReilOpnd maxCnt;
      if (env.getDefaultArchitectureSize() == 64) {
        if (opndSz <= 32)
          maxCnt = bits;
        else
          maxCnt = factory.immediate(preciseSz, 64);
        instructions.add(factory.MOD(env.getNextReilAddress(), c, c, maxCnt));
      } else
        maxCnt = bits;

      final long base = env.getBaseAddress();
      final long reilBase = env.getCurrentReilOffset();
      final long done = reilBase + 38L;
      final long error = reilBase + 28L;
      final long setOf = reilBase + 15L;
      final long flags = setOf + 6L;

      instructions.addAll(Arrays.asList(
          //cnt==0?
          factory.CMPEQ(env.getNextReilAddress(), cntZero, c, zero),
          factory.IFGOTORREIL(env.getNextReilAddress(), cntZero, base, done),
          //cnt>maxCnt?
          factory.CMPLTU(env.getNextReilAddress(), cntOutOfRange, maxCnt, c),
          factory.IFGOTORREIL(env.getNextReilAddress(), cntOutOfRange, base, error),
          // Doit:
          factory.SHL(env.getNextReilAddress(), b, b, bits),
          factory.OR(env.getNextReilAddress(), a, a, b),
          factory.SHRU(env.getNextReilAddress(), r, a, c),
          //SLR(env.getNextReilAddress(), r, r, bits),

          // Overflow flag
          //cnt==1?
          factory.CMPEQ(env.getNextReilAddress(), cntOne, c, one),
          factory.IFGOTORREIL(env.getNextReilAddress(), cntOne, base, setOf),
          factory.UNDEF(env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), LESS_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), LESS_OR_EQUAL_FLAG_OPERAND),
          factory.CMPLTS(env.getNextReilAddress(), SIGN_FLAG_OPERAND, r.withSize(opndSz), zero.withSize(opndSz)),
          factory.CMPEQ(env.getNextReilAddress(), EQUAL_FLAG_OPERAND, r.withSize(opndSz), zero.withSize(opndSz)),
          factory.GOTORREIL(env.getNextReilAddress(), base, flags),
          //SetOf:
          factory.XOR(env.getNextReilAddress(), t, src1, r.withSize(opndSz)),
          factory.MOV(
          env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND, t.withOffset(opndSz - 1, OperandSize.BIT)),
          factory.CMPEQ(env.getNextReilAddress(), EQUAL_FLAG_OPERAND, r.withSize(opndSz), zero.withSize(opndSz)),
          factory.CMPLTS(env.getNextReilAddress(), SIGN_FLAG_OPERAND, r.withSize(opndSz), zero.withSize(opndSz)),
          factory.XOR(env.getNextReilAddress(), LESS_FLAG_OPERAND, SIGN_FLAG_OPERAND, OVERFLOW_FLAG_OPERAND),
          factory.OR(env.getNextReilAddress(), LESS_OR_EQUAL_FLAG_OPERAND, LESS_FLAG_OPERAND, EQUAL_FLAG_OPERAND),
          // Flags:
          // Carry flag
          factory.SUB(env.getNextReilAddress(), c2, c, factory.immediate(preciseSz, 1)),
          factory.SHRU(env.getNextReilAddress(), r2, a, c2),
          factory.MOV(env.getNextReilAddress(), BELOW_FLAG_OPERAND, r2.withSize(OperandSize.BIT)),
          // Writeback
          factory.MOV(env.getNextReilAddress(), src1, r.withSize(opndSz)),
          // Other flags
          factory.OR(env.getNextReilAddress(), BELOW_OR_EQUAL_FLAG_OPERAND, BELOW_FLAG_OPERAND, EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), AUXILIARY_FLAG_OPERAND),
          factory.GOTORREIL(env.getNextReilAddress(), base, done),
          // Error: Invalid parameters
          factory.UNDEF(env.getNextReilAddress(), AUXILIARY_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), PARITY_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), SIGN_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), OVERFLOW_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), BELOW_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), BELOW_OR_EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), LESS_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), LESS_OR_EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), src1),
          // Done:
          factory.NOP(env.getNextReilAddress())));
    }
  }

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    final List<? extends OperandTree> operands = instruction.operands();
    final OperandTree operand1 = operands.get(0);
    final OperandTree operand2 = operands.get(1);
    final OperandTree operand3 = operands.get(2);

    final TranslationState opnd1 = X86OperandTranslator.translateOperand(env, operand1);
    final TranslationState opnd2 = X86OperandTranslator.translateOperand(env, operand2);
    final TranslationState opnd3 = X86OperandTranslator.translateOperand(env, operand3);

    instructions.addAll(opnd1.getInstructionStack());

    final LowLevelRReilOpnd src1 = opnd1.getOperandStack().pop();
    final LowLevelRReilOpnd src2 = opnd2.getOperandStack().pop();
    final LowLevelRReilOpnd src3 = opnd3.getOperandStack().pop();

    final LowLevelRReilOpnd tmp = registerTranslator.temporaryRegister(env, src1.size());

    instructions.add(factory.MOV(env.getNextReilAddress(), tmp, src1));

    emitter.emit(env, tmp, src2, src3, instructions);

    if (opnd1.getOperandStack().size() >= 1) {
      // Left hand side operand was a memory dereference.

      final LowLevelRReilOpnd addr = opnd1.getOperandStack().pop();
      instructions.add(factory.STORE(env.getNextReilAddress(), addr, tmp));
    } else
      //	No memory dereference in `opnd1`.
      emitWritebackAndMaybeZeroExtend(env, src1, tmp, instructions);
  }
}
