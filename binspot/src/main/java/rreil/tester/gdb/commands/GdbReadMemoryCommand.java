package rreil.tester.gdb.commands;

import java.io.PrintWriter;
import java.io.StringWriter;

public class GdbReadMemoryCommand implements IGdbCommand {
  public final int address;
  public final int length;
  
  public GdbReadMemoryCommand(int address, int length) {
    this.address = address;
    this.length = length;
  }
  
  @Override public String commandString () {
    StringWriter sW = new StringWriter();
    PrintWriter pW = new PrintWriter(sW);
    
    pW.printf("m%x,%x", address, length);
    
    return sW.toString();
  }

  @Override public GdbCommandType getType () {
    return GdbCommandType.ReadMemory;
  }
}
