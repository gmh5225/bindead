package rreil.tester.gdb.commands;

import java.io.PrintWriter;
import java.io.StringWriter;

public class GdbInsertBreakOrWatchPointCommand implements IGdbCommand {
  public final int address;
  
  public GdbInsertBreakOrWatchPointCommand(int address) {
    this.address = address;
  }

  @Override public String commandString () {
    StringWriter sW = new StringWriter();
    PrintWriter pW = new PrintWriter(sW);
    
    pW.printf("Z0,%x,2", address);
    
    return sW.toString();
  }

  @Override public GdbCommandType getType () {
    return GdbCommandType.InsertBreakOrWatchPoint;
  }

}
