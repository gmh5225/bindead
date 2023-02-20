package rreil.disassembler.translators.avr8.implementations;

public abstract class AvrImplementation {
  public static final AvrImplementation $ATMEGA32L = new AvrAtmega32L();
  private static final int numRegisters = 32;

  public abstract int getNumIORegisters ();

  public abstract int getSRamSize ();

  public abstract int getSregAddress ();

  public abstract int[] getSpAddress ();

  public abstract int getAddressWidth ();

  public int getNumRegisters () {
    return numRegisters;
  }

  public int getRegistersOffset () {
    return 0;
  }

  public int getIORegistersOffset () {
    return getNumRegisters();
  }

  public int getSRamOffset () {
    return getIORegistersOffset() + getNumIORegisters();
  }

  public int getDataSpaceSize () {
    return getNumRegisters() + getNumIORegisters() + getSRamSize();
  }
}
