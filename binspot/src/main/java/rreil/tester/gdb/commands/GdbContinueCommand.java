package rreil.tester.gdb.commands;

public class GdbContinueCommand implements IGdbCommand {

  @Override public String commandString () {
    return "c";
  }

  @Override public GdbCommandType getType () {
    return GdbCommandType.Continue;
  }
}
