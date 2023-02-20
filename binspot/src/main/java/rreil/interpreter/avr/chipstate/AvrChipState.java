package rreil.interpreter.avr.chipstate;

import java.io.PrintWriter;
import java.io.StringWriter;

import javalx.numeric.BigInt;
import rreil.disassembler.translators.avr8.implementations.AvrImplementation;
import rreil.interpreter.InterpCtx;
import rreil.tester.gdb.responses.GdbRegistersResponse;

public class AvrChipState {
  private final byte[] dataSpace;

  public byte[] getGeneralPurposeRegisters () {
    byte[] gPRegisters = new byte[AvrImplementation.$ATMEGA32L.getNumRegisters()];
    int gPOffset = AvrImplementation.$ATMEGA32L.getRegistersOffset();

    for(int i = 0; i < gPRegisters.length; i++)
      gPRegisters[i] = dataSpace[gPOffset + i];

    return gPRegisters;
  }

  public int getSreg () {
    return dataSpace[AvrImplementation.$ATMEGA32L.getSregAddress()] & 0xff;
  }

  public void setSreg (int sreg) {
    dataSpace[AvrImplementation.$ATMEGA32L.getSregAddress()] = (byte)sreg;
  }

  public int getSp () {
    return (dataSpace[AvrImplementation.$ATMEGA32L.getSpAddress()[0]] & 0xff) << 8 + (dataSpace[AvrImplementation.$ATMEGA32L.getSpAddress()[1]] & 0xff);
  }

  public void setSp (int sp) {
    dataSpace[AvrImplementation.$ATMEGA32L.getSpAddress()[0]] = (byte)(sp >> 8);
    dataSpace[AvrImplementation.$ATMEGA32L.getSpAddress()[1]] = (byte)sp;
  }

//  public int getPc () {
//    return pc;
//  }

  public AvrChipStateDelta changed (AvrChipState chipState) {
    boolean[] dataSpaceChanged = new boolean[dataSpace.length];
    for(int i = 0; i < dataSpaceChanged.length; i++)
      dataSpaceChanged[i] = this.dataSpace[i] != chipState.dataSpace[i];
    return new AvrChipStateDelta(dataSpaceChanged);
  }

  public void toAvrInterpCtx (InterpCtx avrInterpCtx) {
    int numGpRegisters = AvrImplementation.$ATMEGA32L.getNumRegisters();
    int gpOffset = AvrImplementation.$ATMEGA32L.getRegistersOffset();
    for(int i = 0; i < numGpRegisters; i++)
      avrInterpCtx.set("r" + i, BigInt.of(dataSpace[gpOffset + i]));

//    int sreg = getSreg();
//
//    avrInterpCtx.setBool("H", BigInt.valueOf(sreg >> 5 & 0x01));
//    avrInterpCtx.setBool("S", BigInt.valueOf(sreg >> 4 & 0x01));
//    avrInterpCtx.setBool("V", BigInt.valueOf(sreg >> 3 & 0x01));
//    avrInterpCtx.setBool("N", BigInt.valueOf(sreg >> 2 & 0x01));
//    avrInterpCtx.setBool("Z", BigInt.valueOf(sreg >> 1 & 0x01));
//    avrInterpCtx.setBool("C", BigInt.valueOf(sreg >> 0 & 0x01));

    for(int i = 0; i < dataSpace.length; i++)
      avrInterpCtx.store(8, i, BigInt.of(dataSpace[i]));
  }

  public AvrChipState (byte[] dataSpace) {
    this.dataSpace = dataSpace;
//    this.generalPurposeRegisters = generalPurposeRegisters;
//    this.sreg = sreg & 0x3f;
//    this.sp = sp & 0x00;
//    this.pc = pc & 0x00;
  }

  public AvrChipState() {
    this.dataSpace = new byte[AvrImplementation.$ATMEGA32L.getDataSpaceSize()];
  }

  public static AvrChipState fromGdbRegistersResponse (GdbRegistersResponse response) {
    AvrChipState state = new AvrChipState();

    int gpOffset = AvrImplementation.$ATMEGA32L.getRegistersOffset();

    int[] registers = response.getGeneralPurposeRegisters();
    for(int i = 0; i < registers.length; i++)
      state.dataSpace[gpOffset + i] = (byte)registers[i];

    state.setSreg(response.getSreg());
    state.setSp(response.getSp());

    return state;
  }

  public static AvrChipState fromGdbMemoryDump (byte[] bytes, int offset, int size) {
    AvrChipState state = new AvrChipState();

    for(int i = 0; i < size; i++)
      state.dataSpace[i + offset] = bytes[i];

    return state;
  }

  public static AvrChipState fromAvrInterpCtx (InterpCtx avrInterpCtx) {
    AvrChipState state = new AvrChipState();

    for(int i = 0; i < state.dataSpace.length; i++)
      state.dataSpace[i] = avrInterpCtx.load(8, i).bigIntegerValue().byteValue();

    int gpOffset = AvrImplementation.$ATMEGA32L.getRegistersOffset();
    int numGpRegisters = AvrImplementation.$ATMEGA32L.getNumRegisters();

    for(int i = 0; i < numGpRegisters; i++)
      state.dataSpace[gpOffset + i] = (byte)avrInterpCtx.get("r" + i).intValue();

//    int sreg = (avrInterpCtx.getBool("H").intValue() << 5) | (avrInterpCtx.getBool("S").intValue() << 4) | (avrInterpCtx.getBool("V").intValue() << 3) | (avrInterpCtx.getBool("N").intValue() << 2) | (avrInterpCtx.getBool("Z").intValue() << 1)
//        | (avrInterpCtx.getBool("C").intValue() << 0);
//
//    state.setSreg(sreg);

    return state;
  }

  @Override public String toString () {
    StringWriter sW = new StringWriter();
    PrintWriter pW = new PrintWriter(sW);

    pW.println("AvrChipState {");

    pW.append(getRegistersString());
    pW.println("\t-----------");
    pW.append(getIOString());
//    pW.println("\t-----------");
//    pW.append(getSramString());

//    pW.printf("\tSreg:\t0x%02x\n", sreg);
//    pW.printf("\tSp:\t0x%04x\n", sp);
//    pW.printf("\tPc:\t0x%04x\n", pc);
    pW.println("}");

    return sW.toString();
  }

  private String getPartString(String format, int offset, int size) {
    StringWriter sW = new StringWriter();
    PrintWriter pW = new PrintWriter(sW);

    pW.println("{");

    for(int i = 0; i < size; i++)
        pW.printf(format, i, dataSpace[offset + i]);

    pW.println("}");

    return sW.toString();
  }

  public String getRegistersString() {
    int registersOffset = AvrImplementation.$ATMEGA32L.getRegistersOffset();

    return getPartString("\tr%d\t=\t0x%02x\n", registersOffset, AvrImplementation.$ATMEGA32L.getNumRegisters());
  }

  public String getIOString() {
    int ioOffset = AvrImplementation.$ATMEGA32L.getIORegistersOffset();

    return getPartString("\tio[%02d]\t=\t0x%02x\n", ioOffset, AvrImplementation.$ATMEGA32L.getNumIORegisters());
  }

  public String getSramString() {
    int sramOffset = AvrImplementation.$ATMEGA32L.getSRamOffset();

    return getPartString("\tsram[%05d]\t=\t0x%02x\n", sramOffset, AvrImplementation.$ATMEGA32L.getSRamSize());
  }

  public String getJavaArrayString() {
    StringWriter sW = new StringWriter();
    PrintWriter pW = new PrintWriter(sW);

    pW.printf("{");
    for(int i = 0; i < dataSpace.length; i++) {
      pW.printf(" (byte)0x%02x", dataSpace[i]);
      if(i + 1 < dataSpace.length)
        pW.printf(",");
    }
    pW.printf(" }");

    return sW.toString();
  }

  public String diff(AvrChipState other, AvrChipStateDelta changes) {
    StringWriter sW = new StringWriter();
    PrintWriter pW = new PrintWriter(sW);

    pW.println("{");
    boolean[] changedDataSpace = changes.getChangedDataSpace();
    int registersOffset = AvrImplementation.$ATMEGA32L.getRegistersOffset();
    int ioOffset = AvrImplementation.$ATMEGA32L.getIORegistersOffset();
    int sramOffset = AvrImplementation.$ATMEGA32L.getSRamOffset();

    for(int i = 0; i < AvrImplementation.$ATMEGA32L.getNumRegisters(); i++)
      if(changedDataSpace[registersOffset + i])
        pW.printf("\t* r%d\t=\t0x%02x <- 0x%02x\n", i, dataSpace[registersOffset + i], other.dataSpace[registersOffset + i]);
//      else
//        pW.printf("\t- r%d\t=\t0x%02x\n", i, dataSpace[registersOffset + i]);

    pW.println("\t-----------");

    for(int i = 0; i < AvrImplementation.$ATMEGA32L.getNumIORegisters(); i++)
      if(changedDataSpace[ioOffset + i])
        pW.printf("\t* io[%02d]\t=\t0x%02x <- 0x%02x\n", i, dataSpace[ioOffset + i], other.dataSpace[ioOffset + i]);

    pW.println("\t-----------");

    for(int i = 0; i < AvrImplementation.$ATMEGA32L.getSRamSize(); i++)
      if(changedDataSpace[sramOffset + i])
        pW.printf("\t* sram[%05d]\t=\t0x%02x <- 0x%02x\n", i, dataSpace[sramOffset + i], other.dataSpace[sramOffset + i]);

    pW.println("}");

    return sW.toString();
  }

  public String diff(AvrChipState other) {
    return diff(other, changed(other));
  }

  // FIXME define hashCode that respects contract
  @Override public boolean equals(Object other) {
    AvrChipState otherState = (AvrChipState)other;
    for(int i = 0; i < dataSpace.length; i++)
      if(dataSpace[i] != otherState.dataSpace[i])
        return false;
    return true;
  }
}
