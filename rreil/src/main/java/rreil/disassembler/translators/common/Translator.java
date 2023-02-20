package rreil.disassembler.translators.common;

import java.util.List;

import rreil.disassembler.Instruction;
import rreil.lang.lowlevel.LowLevelRReil;

/**
 *
 * @author mb0
 */
public interface Translator {
  public List<LowLevelRReil> translate (TranslationCtx ctx, Instruction insn) throws TranslationException;
}
