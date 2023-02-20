package rreil.interpreter.avr8;

import bindis.Disassembler;
import rreil.interpreter.RReilMachine;

public class Avr8RReilMachine extends RReilMachine {

  public Avr8RReilMachine (Disassembler dis) {
    super(dis);
  }

  public Avr8RReilMachine (Disassembler dis, int pcBitWidth) {
     super(dis, pcBitWidth);
  }

  private void init() {

  }
}
