package rreil.disassembler.translators.common;

import java.util.Stack;

import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class TranslationState {
  /** The RREIL code that was created during the partial translation */
  private final Stack<LowLevelRReil> instructionStack = new Stack<LowLevelRReil>();
  /** The operand stack used during translation. */
  private final Stack<LowLevelRReilOpnd> operandStack = new Stack<LowLevelRReilOpnd>();

  public Stack<LowLevelRReil> getInstructionStack () {
    return instructionStack;
  }

  public Stack<LowLevelRReilOpnd> getOperandStack () {
    return operandStack;
  }
}
