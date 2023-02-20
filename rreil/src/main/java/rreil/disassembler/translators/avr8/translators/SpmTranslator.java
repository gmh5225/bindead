package rreil.disassembler.translators.avr8.translators;

import java.util.List;

import javalx.exceptions.UnimplementedException;
import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class SpmTranslator extends AVR8OperationTranslator {

  public SpmTranslator () {
    super(ReturnType.None);
  }

  @Override public void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions) {
    throw new UnimplementedException();
  }

}
