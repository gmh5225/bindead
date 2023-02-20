package rreil.disassembler.translators.common;

import java.util.List;

import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public interface CondEmitter {
  public LowLevelRReilOpnd emit (TranslationCtx env, List<LowLevelRReil> insns);
}
