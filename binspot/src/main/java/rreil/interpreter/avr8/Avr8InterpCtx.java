package rreil.interpreter.avr8;

import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.interpreter.InterpCtx;
import rreil.interpreter.MemoryModel;

public class Avr8InterpCtx extends InterpCtx {

  public Avr8InterpCtx (int baseSize, RegisterTranslator registerTranslator, MemoryModel memory) {
    super(baseSize, registerTranslator, memory);
  }

}
