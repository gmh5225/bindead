package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import rreil.disassembler.translators.avr8.common.AVR8Helpers;
import rreil.disassembler.translators.avr8.common.AVR8RegRegWordHWordHwordTranslator;
import rreil.disassembler.translators.avr8.emitters.GenericFlagEmitter;
import rreil.disassembler.translators.avr8.emitters.TwosComplementEmitter;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class MulTranslator extends AVR8RegRegWordHWordHwordTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static class FmulFlagEmitter implements InsnEmitter {
    public static InsnEmitter $ = new FmulFlagEmitter();

    @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
      // Zero
      instructions.add(factory.CMPEQ(env.getNextReilAddress(), AVR8Helpers.Z_OPERAND, dst, factory.immediate(dst.size(), 0)));

      // Carry
      instructions.add(factory.CMPLTS(env.getNextReilAddress(), AVR8Helpers.C_OPERAND, dst, factory.immediate(dst.size(), 0)));

      GenericFlagEmitter.$.emit(env, dst, src1, src2, instructions);
    }
  }

  private final boolean fractional;
  private final boolean firstOperandSigned;

  private final boolean secondOperandSigned;

  public MulTranslator (final boolean fractional, final boolean firstOperandSigned, final boolean secondOperandSigned, final LowLevelRReilOpnd dstHigh, final LowLevelRReilOpnd dstLow) {
    super(dstHigh, dstLow);

    this.fractional = fractional;
    this.firstOperandSigned = firstOperandSigned;
    this.secondOperandSigned = secondOperandSigned;
  }

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    final LowLevelRReilOpnd t0 = env.temporaryRegister(dst.size());
    final LowLevelRReilOpnd t1 = env.temporaryRegister(dst.size());

    instructions.add(factory.SIGNEXTEND(env.getNextReilAddress(), t0, src1));
    instructions.add(factory.SIGNEXTEND(env.getNextReilAddress(), t1, src2));

    LowLevelRReilOpnd t0t = null;
    if (this.firstOperandSigned) {
      t0t = env.temporaryRegister(dst.size());
      instructions.add(factory.SHRS(env.getNextReilAddress(), t0t, t0, factory.immediate(t0t.size(), t0.size() - 1)));
    }
    LowLevelRReilOpnd t1t = null;
    if (this.secondOperandSigned) {
      t1t = env.temporaryRegister(dst.size());
      instructions.add(factory.SHRS(env.getNextReilAddress(), t1t, t1, factory.immediate(t1t.size(), t1.size() - 1)));
    }

    LowLevelRReilOpnd t0s = null;
    if (this.firstOperandSigned) {
      t0s = env.temporaryRegister(1);
      instructions.add(factory.CONVERT(env.getNextReilAddress(), t0s, t0t));
    }
    LowLevelRReilOpnd t1s = null;
    if (this.secondOperandSigned) {
      t1s = env.temporaryRegister(1);
      instructions.add(factory.CONVERT(env.getNextReilAddress(), t1s, t1t));
    }

    if (this.firstOperandSigned) {
      instructions.add(factory.XOR(env.getNextReilAddress(), t0s, t0s, factory.immediate(1, 1)));
      instructions.add(factory.IFGOTORREIL(env.getNextReilAddress(), t0s, env.getBaseAddress(), env.getCurrentReilOffset() + TwosComplementEmitter.getNumberOfInstructions()));
      TwosComplementEmitter.$.emit(env, t0, t0, null, instructions);
    }

    if (this.secondOperandSigned) {
      instructions.add(factory.XOR(env.getNextReilAddress(), t1s, t1s, factory.immediate(1, 1)));
      instructions.add(factory.IFGOTORREIL(env.getNextReilAddress(), t1s, env.getBaseAddress(), env.getCurrentReilOffset() + TwosComplementEmitter.getNumberOfInstructions()));
      TwosComplementEmitter.$.emit(env, t1, t1, null, instructions);
    }

    LowLevelRReilOpnd dsts = null;
    if (this.firstOperandSigned && this.secondOperandSigned) {
      dsts = env.temporaryRegister(1);
      instructions.add(factory.XOR(env.getNextReilAddress(), dsts, t0s, t1s));
    } else if (this.firstOperandSigned)
      dsts = t0s;
    else if (this.secondOperandSigned)
      dsts = t1s;

    instructions.add(factory.MUL(env.getNextReilAddress(), dst, t0, t1));

    if (this.firstOperandSigned || this.secondOperandSigned) {
      instructions.add(factory.XOR(env.getNextReilAddress(), dsts, dsts, factory.immediate(1, 1)));
      instructions.add(factory.IFGOTORREIL(env.getNextReilAddress(), dsts, env.getBaseAddress(), env.getCurrentReilOffset() + TwosComplementEmitter.getNumberOfInstructions()));
      TwosComplementEmitter.$.emit(env, dst, dst, null, instructions);
    }

    if (this.fractional)
      instructions.add(factory.SHL(env.getNextReilAddress(), dst, dst, factory.immediate(dst.size(), 1)));

    FmulFlagEmitter.$.emit(env, dst, src1, src2, instructions);
  }
}
