package rreil.tester.gdb.commands;

public class GdbStepCommand implements IGdbCommand {

  @Override public String commandString () {
    return "s";
  }

  @Override public GdbCommandType getType () {
    return GdbCommandType.Step;
  }

}
