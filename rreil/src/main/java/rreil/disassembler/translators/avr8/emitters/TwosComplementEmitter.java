package rreil.disassembler.translators.avr8.emitters;

import java.util.List;

import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class TwosComplementEmitter implements InsnEmitter {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static final TwosComplementEmitter $ = new TwosComplementEmitter();

  @Override public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
    instructions.add(factory.XOR(env.getNextReilAddress(), dst, src1, factory.immediate(dst.size(), (1 << dst.size()) - 1)));
    instructions.add(factory.ADD(env.getNextReilAddress(), dst, dst, factory.immediate(dst.size(), 1)));
  }

  public static int getNumberOfInstructions() {
    return 2;
  }
}
