package rreil.disassembler.translators.avr8.implementations;

public class AvrAtmega32L extends AvrImplementation {
  private static final int numIORegisters = 64;
  private static final int sRamSize = 2048;
  private static final int[] spAddress = new int[] { 0x5e, 0x5d };
  private static final int sregAddress = 0x5f;
  private static final int addressWidth = 16;

  @Override public int getNumIORegisters () {
    return numIORegisters;
  }

  @Override public int getSRamSize () {
    return sRamSize;
  }

  @Override public int[] getSpAddress () {
    return spAddress;
  }

  @Override public int getSregAddress () {
    return sregAddress;
  }
  
  @Override public int getAddressWidth () {
    return addressWidth;
  }
}
