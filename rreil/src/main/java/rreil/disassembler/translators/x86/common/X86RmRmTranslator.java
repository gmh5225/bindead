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
import rreil.lang.lowlevel.TranslationHelpers;

public class X86RmRmTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  protected final InsnEmitter emitter;

  public X86RmRmTranslator (InsnEmitter emitter) {
    this.emitter = emitter;
  }

  public static class AddEmitter implements InsnEmitter {
    private static final InsnEmitter flags = new X86FlagHelpers.AddFlagEmitter();

    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      instructions.add(factory.ADD(env.getNextReilAddress(), dst, src1, src2));
      flags.emit(env, dst, src1, src2, instructions);
    }
  }

  public static class XaddEmitter implements InsnEmitter {
    private static final InsnEmitter flags = new X86FlagHelpers.AddFlagEmitter();

    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      instructions.add(factory.ADD(env.getNextReilAddress(), dst, src1, src2));
      flags.emit(env, dst, src1, src2, instructions);
      instructions.add(factory.MOV(env.getNextReilAddress(), src2, src1));
    }
  }

  public static class XchgEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd tmp, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      instructions.add(factory.MOV(env.getNextReilAddress(), tmp, src2));
      instructions.add(factory.MOV(env.getNextReilAddress(), src2, src1));
    }
  }

  public static class AdcEmitter implements InsnEmitter {
    private static final InsnEmitter flags = new X86FlagHelpers.ZeroSignFlagEmitter();

    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd CF = registerTranslator.temporaryRegister(env, dst.size());
      final LowLevelRReilOpnd t1 = registerTranslator.temporaryRegister(env, dst.size());
      final LowLevelRReilOpnd cf = X86Helpers.BELOW_FLAG_OPERAND;
      final LowLevelRReilOpnd of = X86Helpers.OVERFLOW_FLAG_OPERAND;

      final int preciseSz = TranslationHelpers.getNextSize(dst.size());
      final LowLevelRReilOpnd psrc1 = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd psrc2 = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd pCF = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd pt1 = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd pt2 = registerTranslator.temporaryRegister(env, preciseSz);

      final LowLevelRReilOpnd t2 = registerTranslator.temporaryRegister(env, dst.size());
      final LowLevelRReilOpnd t3 = registerTranslator.temporaryRegister(env, dst.size());
      final LowLevelRReilOpnd t4 = registerTranslator.temporaryRegister(env, dst.size());

      instructions.addAll(
          Arrays.asList(
              factory.CONVERT(env.getNextReilAddress(), CF, cf),
              factory.ADD(env.getNextReilAddress(), t1, src1, src2),
              factory.ADD(env.getNextReilAddress(), dst, t1, CF),
          /*
          // Signed overflow using higher precision	arithmetic
          MOVZX(env.getNextReilAddress(), pCF, cf),
          MOVSX(env.getNextReilAddress(), psrc1, src1),
          MOVSX(env.getNextReilAddress(), psrc2, src2),
          ADD(env.getNextReilAddress(), pt1, psrc1, psrc2),
          ADD(env.getNextReilAddress(), pt2, pt1, pCF),
          CMPLTS(env.getNextReilAddress(), f1, pt2, getMinImmediateSigned(dst.size()).withSize(preciseSz)),
          CMPLTS(env.getNextReilAddress(), f2, getMaxImmediateSigned(dst.size()).withSize(preciseSz), pt2),
          OR(env.getNextReilAddress(), of, f1, f2),
           */
          // Hackers Delight p27
              factory.XOR(env.getNextReilAddress(), t2, dst, src1),
              factory.XOR(env.getNextReilAddress(), t3, dst, src2),
              factory.AND(env.getNextReilAddress(), t4, t2, t3),
              factory.MOV(env.getNextReilAddress(), of, t4.withOffset(t4.size() - 1, cf.size())),
          // Unsigned overflow using higher precision arithmetic
              factory.CONVERT(env.getNextReilAddress(), pCF, cf),
              factory.CONVERT(env.getNextReilAddress(), psrc1, src1),
              factory.CONVERT(env.getNextReilAddress(), psrc2, src2),
              factory.ADD(env.getNextReilAddress(), pt1, psrc1, psrc2),
              factory.ADD(env.getNextReilAddress(), pt2, pt1, pCF),
              factory.CMPLTU(env.getNextReilAddress(), cf, TranslationHelpers.getMaxImmediate(dst.size()).withSize(preciseSz), pt2)));

      flags.emit(env, dst, null, null, instructions);
    }
  }

  public static class SbbEmitter implements InsnEmitter {
    private static final InsnEmitter flags = new X86FlagHelpers.ZeroSignFlagEmitter();

    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd CF = registerTranslator.temporaryRegister(env, dst.size());
      final LowLevelRReilOpnd t1 = registerTranslator.temporaryRegister(env, dst.size());
      final LowLevelRReilOpnd cf = X86Helpers.BELOW_FLAG_OPERAND;
      final LowLevelRReilOpnd of = X86Helpers.OVERFLOW_FLAG_OPERAND;

      final int preciseSz = TranslationHelpers.getNextSize(dst.size());
      final LowLevelRReilOpnd psrc1 = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd psrc2 = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd pCF = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd pt1 = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd pt2 = registerTranslator.temporaryRegister(env, preciseSz);

      final LowLevelRReilOpnd t4 = registerTranslator.temporaryRegister(env, dst.size());
      final LowLevelRReilOpnd t5 = registerTranslator.temporaryRegister(env, dst.size());
      final LowLevelRReilOpnd t6 = registerTranslator.temporaryRegister(env, dst.size());

      instructions.addAll(
          Arrays.asList(
              factory.CONVERT(env.getNextReilAddress(), CF, cf),
              factory.SUB(env.getNextReilAddress(), t1, src1, src2),
              factory.SUB(env.getNextReilAddress(), dst, t1, CF),
          /*
          // Signed overflow
          MOVZX(env.getNextReilAddress(), pCF, cf),
          MOVSX(env.getNextReilAddress(), psrc1, src1),
          MOVSX(env.getNextReilAddress(), psrc2, src2),
          SUB(env.getNextReilAddress(), pt1, psrc1, psrc2),
          SUB(env.getNextReilAddress(), pt2, pt1, pCF),
          CMPLTS(env.getNextReilAddress(), f1, pt2, getMinImmediateSigned(dst.size()).withSize(preciseSz)),
          CMPLTS(env.getNextReilAddress(), f2, getMaxImmediateSigned(dst.size()).withSize(preciseSz), pt2),
          OR(env.getNextReilAddress(), of, f1, f2),
           */
          // Hackers Delight p27
              factory.XOR(env.getNextReilAddress(), t4, src1, src2),
              factory.XOR(env.getNextReilAddress(), t5, dst, src1),
              factory.AND(env.getNextReilAddress(), t6, t4, t5),
              factory.MOV(env.getNextReilAddress(), of, t6.withOffset(t6.size() - 1, cf.size())),
          // Unsigned overflow
              factory.CONVERT(env.getNextReilAddress(), pCF, cf),
              factory.CONVERT(env.getNextReilAddress(), psrc1, src1),
              factory.CONVERT(env.getNextReilAddress(), psrc2, src2),
              factory.SUB(env.getNextReilAddress(), pt1, psrc1, psrc2),
              factory.SUB(env.getNextReilAddress(), pt2, pt1, pCF),
              factory.CMPLTU(env.getNextReilAddress(), cf, TranslationHelpers.getMaxImmediate(dst.size()).withSize(preciseSz), pt2)));

      flags.emit(env, dst, null, null, instructions);
    }
  }

  public static class AndEmitter implements InsnEmitter {
    private static final InsnEmitter flags = new X86FlagHelpers.LogicFlagEmitter();

    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      instructions.add(factory.AND(env.getNextReilAddress(), dst, src1, src2));
      flags.emit(env, dst, src1, src2, instructions);
    }
  }

  public static class OrEmitter implements InsnEmitter {
    private static final InsnEmitter flags = new X86FlagHelpers.LogicFlagEmitter();

    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      instructions.add(factory.OR(env.getNextReilAddress(), dst, src1, src2));
      flags.emit(env, dst, src1, src2, instructions);
    }
  }

  public static class SubEmitter implements InsnEmitter {
    private static final InsnEmitter flags = new X86FlagHelpers.SubFlagEmitter();

    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      instructions.add(factory.SUB(env.getNextReilAddress(), dst, src1, src2));
      flags.emit(env, dst, src1, src2, instructions);
    }
  }

  public static class XorEmitter implements InsnEmitter {
    private static final InsnEmitter flags = new X86FlagHelpers.LogicFlagEmitter();

    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      instructions.add(factory.XOR(env.getNextReilAddress(), dst, src1, src2));
      flags.emit(env, dst, src1, src2, instructions);
    }
  }

  public static class RolEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final int opndSz = dst.size();
      final LowLevelRReilOpnd cnt = registerTranslator.temporaryRegister(env, opndSz);
      final LowLevelRReilOpnd mod = factory.immediate(opndSz, opndSz);
      final int bits = dst.size() <= 32 ? 31 : 63;
      final LowLevelRReilOpnd mask = factory.immediate(opndSz, bits);
      final LowLevelRReilOpnd cxZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cxOne = registerTranslator.temporaryRegister(env, OperandSize.BIT);

      final LowLevelRReilOpnd t1 = registerTranslator.temporaryRegister(env, opndSz);
      final LowLevelRReilOpnd t2 = registerTranslator.temporaryRegister(env, opndSz);
      final LowLevelRReilOpnd t3 = registerTranslator.temporaryRegister(env, opndSz);

      instructions.addAll(Arrays.asList(
          factory.CONVERT(env.getNextReilAddress(), cnt, src2),
          factory.AND(env.getNextReilAddress(), cnt, cnt, mask),
          factory.MOD(env.getNextReilAddress(), cnt, cnt, mod)));

      final long base = env.getBaseAddress();
      final long reilBase = env.getCurrentReilOffset();

      final long done = reilBase + 12L;
      final long setOF = reilBase + 11L;

      instructions.addAll(Arrays.asList(
          // cnt==0
          factory.CMPEQ(env.getNextReilAddress(), cxZero, cnt, factory.immediate(cnt.size(), 0)),
          factory.IFGOTORREIL(env.getNextReilAddress(), cxZero, base, done),
          //ROL(env.getNextReilAddress(), dst, src1, cnt),

          // Hackers Delight p34
          factory.SUB(env.getNextReilAddress(), t1, factory.immediate(opndSz, opndSz), cnt),
          factory.SHL(env.getNextReilAddress(), t2, src1, cnt),
          factory.SHRU(env.getNextReilAddress(), t3, src1, t1),
          factory.OR(env.getNextReilAddress(), dst, t2, t3),
          // cf:=lsb(dst)
          factory.MOV(env.getNextReilAddress(), X86Helpers.BELOW_FLAG_OPERAND, dst.withSize(OperandSize.BIT)),
          // cnt==1
          factory.CMPEQ(env.getNextReilAddress(), cxOne, cnt, factory.immediate(cnt.size(), 1)),
          factory.IFGOTORREIL(env.getNextReilAddress(), cxOne, base, setOF),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND),
          factory.GOTORREIL(env.getNextReilAddress(), base, done),
          // SetOF:
          factory.XOR(
          env.getNextReilAddress(),
          X86Helpers.OVERFLOW_FLAG_OPERAND,
          X86Helpers.BELOW_FLAG_OPERAND,
          dst.withOffset(dst.size() - 1, OperandSize.BIT)),
          // Done:
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.BELOW_OR_EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_OR_EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_FLAG_OPERAND)));
    }
  }

  public static class RorEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final int opndSz = dst.size();
      final LowLevelRReilOpnd cnt = registerTranslator.temporaryRegister(env, opndSz);
      final LowLevelRReilOpnd mod = factory.immediate(opndSz, opndSz);
      final int bits = dst.size() <= 32 ? 31 : 63;
      final LowLevelRReilOpnd mask = factory.immediate(opndSz, bits);
      final LowLevelRReilOpnd cxZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cxOne = registerTranslator.temporaryRegister(env, OperandSize.BIT);

      final LowLevelRReilOpnd t1 = registerTranslator.temporaryRegister(env, opndSz);
      final LowLevelRReilOpnd t2 = registerTranslator.temporaryRegister(env, opndSz);
      final LowLevelRReilOpnd t3 = registerTranslator.temporaryRegister(env, opndSz);

      instructions.addAll(Arrays.asList(
          factory.CONVERT(env.getNextReilAddress(), cnt, src2),
          factory.AND(env.getNextReilAddress(), cnt, cnt, mask),
          factory.MOD(env.getNextReilAddress(), cnt, cnt, mod)));

      final long base = env.getBaseAddress();
      final long reilBase = env.getCurrentReilOffset();

      final long done = reilBase + 12L;
      final long setOF = reilBase + 11L;

      instructions.addAll(
          Arrays.asList(
          // cnt==0
              factory.CMPEQ(env.getNextReilAddress(), cxZero, cnt, factory.immediate(cnt.size(), 0)),
              factory.IFGOTORREIL(env.getNextReilAddress(), cxZero, base, done),
          //ROR(env.getNextReilAddress(), dst, src1, cnt),

          // Hackers Delight p34
              factory.SUB(env.getNextReilAddress(), t1, factory.immediate(opndSz, opndSz), cnt),
              factory.SHRU(env.getNextReilAddress(), t2, src1, cnt),
              factory.SHL(env.getNextReilAddress(), t3, src1, t1),
              factory.OR(env.getNextReilAddress(), dst, t2, t3),
          // cf:=msb(dst)
              factory.MOV(
          env.getNextReilAddress(),
          X86Helpers.BELOW_FLAG_OPERAND,
          dst.withOffset(dst.size() - 1, OperandSize.BIT)),
          // cnt==1
          factory.CMPEQ(env.getNextReilAddress(), cxOne, cnt, factory.immediate(cnt.size(), 1)),
          factory.IFGOTORREIL(env.getNextReilAddress(), cxOne, base, setOF),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND),
          factory.GOTORREIL(env.getNextReilAddress(), base, done),
          // SetOF:
          // XOR of the two most significant bits of the result
          factory.XOR(
          env.getNextReilAddress(),
          X86Helpers.OVERFLOW_FLAG_OPERAND,
          dst.withOffset(dst.size() - 1, OperandSize.BIT),
          dst.withOffset(dst.size() - 2, OperandSize.BIT)),
          // Done:
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.BELOW_OR_EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_OR_EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_FLAG_OPERAND)));
    }
  }

  public static class RclEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final int opndSz = dst.size();
      final int preciseSz = TranslationHelpers.getNextSize(opndSz);
      final LowLevelRReilOpnd n = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd x = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd mod = factory.immediate(preciseSz, opndSz + 1);
      final int bits = dst.size() <= 32 ? 31 : 63;
      final LowLevelRReilOpnd mask = factory.immediate(preciseSz, bits);
      final LowLevelRReilOpnd cxZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cxOne = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cf = X86Helpers.BELOW_FLAG_OPERAND;
      final LowLevelRReilOpnd a = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd b = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd c = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd y = registerTranslator.temporaryRegister(env, preciseSz);

      instructions.addAll(Arrays.asList(
          factory.CONVERT(env.getNextReilAddress(), n, src2),
          factory.AND(env.getNextReilAddress(), n, n, mask),
          factory.CONVERT(env.getNextReilAddress(), x, src1),
          factory.MOV(env.getNextReilAddress(), x.withOffset(opndSz, OperandSize.BIT), cf)));

      if (opndSz < 32)
        instructions.add(factory.MOD(env.getNextReilAddress(), n, n, mod));

      final long base = env.getBaseAddress();
      final long reilBase = env.getCurrentReilOffset();

      final long done = reilBase + 13L;
      final long setOF = reilBase + 12L;

      instructions.addAll(
          Arrays.asList(
          // n==0?
              factory.CMPEQ(env.getNextReilAddress(), cxZero, n, factory.immediate(n.size(), 0)),
              factory.IFGOTORREIL(env.getNextReilAddress(), cxZero, base, done),
          // Doit:
              factory.SUB(env.getNextReilAddress(), c, factory.immediate(n.size(), opndSz + 1), n),
              factory.SHL(env.getNextReilAddress(), a, x, n),
              factory.SHRU(env.getNextReilAddress(), b, x, c),
              factory.OR(env.getNextReilAddress(), y, a, b),
              factory.MOV(env.getNextReilAddress(), cf, y.withOffset(opndSz + 1, OperandSize.BIT)),
              factory.MOV(env.getNextReilAddress(), dst, y.withSize(opndSz)),
          // n==1?
              factory.CMPEQ(env.getNextReilAddress(), cxOne, n, factory.immediate(n.size(), 1)),
              factory.IFGOTORREIL(env.getNextReilAddress(), cxOne, base, setOF),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND),
              factory.GOTORREIL(env.getNextReilAddress(), base, done),
          // SetOF:
              factory.XOR(
          env.getNextReilAddress(),
          X86Helpers.OVERFLOW_FLAG_OPERAND,
          cf,
          dst.withOffset(dst.size() - 1, OperandSize.BIT)),
          // Done:
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.BELOW_OR_EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_OR_EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_FLAG_OPERAND)));
    }
  }

  public static class RcrEmitter implements InsnEmitter {
    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final int opndSz = dst.size();
      final int preciseSz = TranslationHelpers.getNextSize(opndSz);
      final LowLevelRReilOpnd n = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd x = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd mod = factory.immediate(preciseSz, opndSz + 1);
      final int bits = dst.size() <= 32 ? 31 : 63;
      final LowLevelRReilOpnd mask = factory.immediate(preciseSz, bits);
      final LowLevelRReilOpnd cxZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cxOne = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cf = X86Helpers.BELOW_FLAG_OPERAND;
      final LowLevelRReilOpnd a = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd b = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd c = registerTranslator.temporaryRegister(env, preciseSz);
      final LowLevelRReilOpnd y = registerTranslator.temporaryRegister(env, preciseSz);

      instructions.addAll(Arrays.asList(
          factory.CONVERT(env.getNextReilAddress(), n, src2),
          factory.AND(env.getNextReilAddress(), n, n, mask),
          factory.CONVERT(env.getNextReilAddress(), x, src1),
          factory.SHL(env.getNextReilAddress(), x, x, factory.immediate(x.size(), 1)),
          factory.MOV(env.getNextReilAddress(), x.withSize(OperandSize.BIT), cf)));

      if (opndSz < 32)
        instructions.add(factory.MOD(env.getNextReilAddress(), n, n, mod));

      final long base = env.getBaseAddress();
      final long reilBase = env.getCurrentReilOffset();

      final long done = reilBase + 13L;
      final long doit = reilBase + 7L;
      final long setOF = reilBase + 6L;

      instructions.addAll(
          Arrays.asList(
          // n==0?
              factory.CMPEQ(env.getNextReilAddress(), cxZero, n, factory.immediate(n.size(), 0)),
              factory.IFGOTORREIL(env.getNextReilAddress(), cxZero, base, done),
          // n==1?
              factory.CMPEQ(env.getNextReilAddress(), cxOne, n, factory.immediate(n.size(), 1)),
              factory.IFGOTORREIL(env.getNextReilAddress(), cxOne, base, setOF),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND),
              factory.GOTORREIL(env.getNextReilAddress(), base, doit),
          // SetOF:
              factory.XOR(
          env.getNextReilAddress(),
          X86Helpers.OVERFLOW_FLAG_OPERAND,
          cf,
          src1.withOffset(dst.size() - 1, OperandSize.BIT)),
          // Doit:
          factory.SUB(env.getNextReilAddress(), c, factory.immediate(n.size(), opndSz + 1), n),
          factory.SHRU(env.getNextReilAddress(), a, x, n),
          factory.SHL(env.getNextReilAddress(), b, x, c),
          factory.OR(env.getNextReilAddress(), y, a, b),
          factory.MOV(env.getNextReilAddress(), cf, y.withSize(OperandSize.BIT)),
          factory.MOV(env.getNextReilAddress(), dst, y.withOffset(1, opndSz)),
          // Done:
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.BELOW_OR_EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_OR_EQUAL_FLAG_OPERAND),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.LESS_FLAG_OPERAND)));

    }
  }

  public static class ShlEmitter implements InsnEmitter {
    private static final InsnEmitter flags = new X86FlagHelpers.ZeroSignFlagEmitter();

    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final int opndSz = dst.size();
      final LowLevelRReilOpnd cnt = registerTranslator.temporaryRegister(env, opndSz);
      final int bits = opndSz <= 32 ? 31 : 63;
      final LowLevelRReilOpnd mask = factory.immediate(opndSz, bits);
      final LowLevelRReilOpnd cxZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cxOne = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cf = X86Helpers.BELOW_FLAG_OPERAND;
      final LowLevelRReilOpnd cfIdx = registerTranslator.temporaryRegister(env, cnt.size());

      instructions.addAll(Arrays.asList(
          factory.CONVERT(env.getNextReilAddress(), cnt, src2),
          factory.AND(env.getNextReilAddress(), cnt, cnt, mask),
          factory.MOV(env.getNextReilAddress(), dst, src1)));

      final long base = env.getBaseAddress();
      final long reilBase = env.getCurrentReilOffset();

      final long done = reilBase + 11L;
      final long setOF = reilBase + 10L;

      instructions.addAll(Arrays.asList(
          // cnt==0
          factory.CMPEQ(env.getNextReilAddress(), cxZero, cnt, factory.immediate(cnt.size(), 0)),
          factory.IFGOTORREIL(env.getNextReilAddress(), cxZero, base, done),
          factory.SUB(env.getNextReilAddress(), cfIdx, cnt, factory.immediate(cfIdx.size(), 1)),
          factory.SHL(env.getNextReilAddress(), cfIdx, src1, cfIdx),
          factory.MOV(env.getNextReilAddress(), cf, cfIdx.withOffset(bits, OperandSize.BIT)),
          factory.SHL(env.getNextReilAddress(), dst, src1, cnt),
          //cnt==1
          factory.CMPEQ(env.getNextReilAddress(), cxOne, cnt, factory.immediate(cnt.size(), 1)),
          factory.IFGOTORREIL(env.getNextReilAddress(), cxOne, base, setOF),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND),
          factory.GOTORREIL(env.getNextReilAddress(), base, done),
          // SetOF:
          factory.XOR(
          env.getNextReilAddress(),
          X86Helpers.OVERFLOW_FLAG_OPERAND,
          cf,
          dst.withOffset(dst.size() - 1, OperandSize.BIT)),
          // Done:
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.AUXILIARY_FLAG_OPERAND)));

      flags.emit(env, dst, null, null, instructions);
    }
  }

  public static class ShrEmitter implements InsnEmitter {
    private static final InsnEmitter flags = new X86FlagHelpers.ZeroSignFlagEmitter();

    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final int opndSz = dst.size();
      final LowLevelRReilOpnd cnt = registerTranslator.temporaryRegister(env, opndSz);
      final int bits = opndSz <= 32 ? 31 : 63;
      final LowLevelRReilOpnd mask = factory.immediate(opndSz, bits);
      final LowLevelRReilOpnd cxZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cxOne = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cf = X86Helpers.BELOW_FLAG_OPERAND;
      final LowLevelRReilOpnd cfIdx = registerTranslator.temporaryRegister(env, cnt.size());

      instructions.addAll(
          Arrays.asList(
              factory.CONVERT(env.getNextReilAddress(), cnt, src2),
              factory.AND(env.getNextReilAddress(), cnt, cnt, mask),
              factory.MOV(env.getNextReilAddress(), dst, src1)));

      final long base = env.getBaseAddress();
      final long reilBase = env.getCurrentReilOffset();

      final long done = reilBase + 11L;
      final long setOF = reilBase + 10L;

      instructions.addAll(Arrays.asList(
          // cnt==0
          factory.CMPEQ(env.getNextReilAddress(), cxZero, cnt, factory.immediate(cnt.size(), 0)),
          factory.IFGOTORREIL(env.getNextReilAddress(), cxZero, base, done),
          factory.SUB(env.getNextReilAddress(), cfIdx, cnt, factory.immediate(cfIdx.size(), 1)),
          factory.SHRU(env.getNextReilAddress(), cfIdx, src1, cfIdx),
          factory.MOV(env.getNextReilAddress(), cf, cfIdx.withSize(OperandSize.BIT)),
          factory.SHRU(env.getNextReilAddress(), dst, src1, cnt),
          //cnt==1
          factory.CMPEQ(env.getNextReilAddress(), cxOne, cnt, factory.immediate(cnt.size(), 1)),
          factory.IFGOTORREIL(env.getNextReilAddress(), cxOne, base, setOF),
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND),
          factory.GOTORREIL(env.getNextReilAddress(), base, done),
          // SetOF:
          factory.MOV(
          env.getNextReilAddress(),
          X86Helpers.OVERFLOW_FLAG_OPERAND,
          src1.withOffset(src1.size() - 1, OperandSize.BIT)),
          // Done:
          factory.UNDEF(env.getNextReilAddress(), X86Helpers.AUXILIARY_FLAG_OPERAND)));

      flags.emit(env, dst, null, null, instructions);
    }
  }

  public static class SarEmitter implements InsnEmitter {
    private static final InsnEmitter flags = new X86FlagHelpers.ZeroSignFlagEmitter();

    @Override
    public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final int opndSz = dst.size();
      final LowLevelRReilOpnd cnt = registerTranslator.temporaryRegister(env, opndSz);
      final int bits = opndSz <= 32 ? 31 : 63;
      final LowLevelRReilOpnd mask = factory.immediate(opndSz, bits);
      final LowLevelRReilOpnd lowestBitMask = factory.immediate(opndSz, 1);
      final LowLevelRReilOpnd cxZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cxOne = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cf = X86Helpers.BELOW_FLAG_OPERAND;
      final LowLevelRReilOpnd cfIdx = registerTranslator.temporaryRegister(env, cnt.size());

      instructions.addAll(Arrays.asList(
          factory.CONVERT(env.getNextReilAddress(), cnt, src2),
          factory.AND(env.getNextReilAddress(), cnt, cnt, mask),
          factory.MOV(env.getNextReilAddress(), dst, src1)));

      final long base = env.getBaseAddress();
      final long reilBase = env.getCurrentReilOffset();

      final long done = reilBase + 11L;
      final long setOF = reilBase + 10L;

      instructions.addAll(
          Arrays.asList(
          // cnt==0?
              factory.CMPEQ(env.getNextReilAddress(), cxZero, cnt, factory.immediate(cnt.size(), 0)),
              factory.IFGOTORREIL(env.getNextReilAddress(), cxZero, base, done),
              factory.SUB(env.getNextReilAddress(), cfIdx, cnt, factory.immediate(cfIdx.size(), 1)),
              factory.SHRU(env.getNextReilAddress(), cfIdx, src1, cfIdx),
              factory.AND(env.getNextReilAddress(), cfIdx, cfIdx, lowestBitMask),
              factory.MOV(env.getNextReilAddress(), cf, cfIdx.withSize(OperandSize.BIT)),
          /*
          // Compute signed shift right using unsigned shift right
          ADD(env.getNextReilAddress(), t1, src1, signMask),
          SHR(env.getNextReilAddress(), t2, t1, cnt),
          SHR(env.getNextReilAddress(), t3, signMask, cnt),
          SUB(env.getNextReilAddress(), dst, t2, t3),
           */
              factory.SHRS(env.getNextReilAddress(), dst, src1, cnt),
          //cnt==1?
              factory.CMPEQ(env.getNextReilAddress(), cxOne, cnt, factory.immediate(cnt.size(), 1)),
              factory.IFGOTORREIL(env.getNextReilAddress(), cxOne, base, setOF),
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND),
              factory.GOTORREIL(env.getNextReilAddress(), base, done),
          // SetOF:
              factory.MOV(env.getNextReilAddress(), X86Helpers.OVERFLOW_FLAG_OPERAND, factory.immediate(OperandSize.BIT, 0)),
          // Done:
              factory.UNDEF(env.getNextReilAddress(), X86Helpers.AUXILIARY_FLAG_OPERAND)));

      flags.emit(env, dst, null, null, instructions);
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
      //	No memory dereference in `opnd1`.
      emitWritebackAndMaybeZeroExtend(env, src1, tmp, instructions);
  }
}
