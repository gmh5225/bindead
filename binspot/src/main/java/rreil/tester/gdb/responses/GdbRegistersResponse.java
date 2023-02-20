package rreil.tester.gdb.responses;

import rreil.tester.gdb.GdbTools;

public class GdbRegistersResponse implements IGdbResponse {
  private final int[] generalPurposeRegisters = new int[32];

  private final int sreg;
  private final int sp;
  private final int pc;

  public int[] getGeneralPurposeRegisters() {
    return generalPurposeRegisters;
  }

  public int getSreg() {
    return sreg;
  }

  public int getSp() {
    return sp;
  }

  public int getPc() {
    return pc;
  }

  public GdbRegistersResponse (String data) {
    for(int i = 0; i < generalPurposeRegisters.length; i++)
      generalPurposeRegisters[i] = Integer.parseInt(data.substring(2*i, 2*i + 2), 16);
    sreg = Integer.parseInt(data.substring(2*32, 2*32 + 2), 16);
    sp = Integer.parseInt(data.substring(2*33, 2*33 + 4), 16);
    pc = Integer.parseInt(GdbTools.changeStringEndianess(data.substring(2*35, 2*35 + 4)), 16);
  }

  @Override public GdbResponseType getType () {
    return GdbResponseType.Registers;
  }

}
