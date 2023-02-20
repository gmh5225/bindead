package rreil.tester.gdb.commands;

public class GdbReadRegistersCommand implements IGdbCommand {

  @Override public String commandString () {
    return "g";
  }

  @Override public GdbCommandType getType () {
    return GdbCommandType.ReadRegisters;
  }

}
