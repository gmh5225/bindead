package rreil.disassembler.translators.common;

import java.util.List;

import rreil.disassembler.Instruction;
import rreil.lang.lowlevel.LowLevelRReil;

public interface InsnTranslator {
  public void translate (TranslationCtx ctx, Instruction insn, List<LowLevelRReil> instructions);
}
