package rreil.disassembler.translators.common;

import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public interface RegisterTranslator {
  public int defaultArchitectureSize ();

  public LowLevelRReilOpnd translateRegister (final String name);

  public LowLevelRReilOpnd temporaryRegister (final TranslationCtx ctx, final OperandSize size);

  public LowLevelRReilOpnd temporaryRegister (final TranslationCtx ctx, Number size);
}
