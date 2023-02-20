package rreil.tester.gdb.commands;

public interface IGdbCommand {
  public String commandString();
  public GdbCommandType getType();
}
