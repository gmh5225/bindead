package rreil.disassembler.translators.common;

import rreil.disassembler.Instruction;
import rreil.lang.RReilAddr;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

/**
 */
public final class TranslationCtx {
  private final RegisterTranslator registerTranslator;
  private int currentRReilOffset = 0;
  private int nextVariable = 0;
  private final int defaultArchitectureSize;
  private final long baseAddress;
  private Instruction currentInstruction;

  public TranslationCtx (long baseAddress, RegisterTranslator translator, int defaultArchitectureSize) {
    this.baseAddress = baseAddress;
    this.registerTranslator = translator;
    this.defaultArchitectureSize = defaultArchitectureSize;
  }

  public String getNextVariableString () {
    return String.format("t%d", generateNextVariable());
  }

  public int generateNextVariable () {
    return nextVariable++;
  }

  public RReilAddr getNextReilAddress () {
    return new RReilAddr(baseAddress, currentRReilOffset++);
  }

  public long getCurrentReilOffset () {
    return currentRReilOffset;
  }

  public RegisterTranslator getRegisterTranslator () {
    return registerTranslator;
  }

  public int getDefaultArchitectureSize () {
    return defaultArchitectureSize;
  }

  public long getBaseAddress () {
    return baseAddress;
  }

  public long getFollowupAddress (final Instruction insn) {
    return baseAddress + insn.length();
  }

  public void setCurrentInstruction (final Instruction insn) {
    this.currentInstruction = insn;
  }

  public Instruction getCurrentInstruction () {
    return currentInstruction;
  }

  public LowLevelRReilOpnd translateRegister (String name) {
    return registerTranslator.translateRegister(name);
  }

  public LowLevelRReilOpnd temporaryRegister (Number size) {
    return registerTranslator.temporaryRegister(this, size);
  }

  public LowLevelRReilOpnd temporaryRegister (OperandSize size) {
    return registerTranslator.temporaryRegister(this, size);
  }
}
