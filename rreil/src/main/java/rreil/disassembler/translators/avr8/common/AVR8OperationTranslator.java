package rreil.disassembler.translators.avr8.common;

import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.avr8.implementations.AvrImplementation;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.OperandSize;

/**
 * Generic wrapper for translating operations with register or immediate arguments only.
 *
 * @author mb0
 */
public abstract class AVR8OperationTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static enum ReturnType {
    None, Register, Memory
  }
  private final ReturnType returnType;
  private final boolean loadFromMemory;
  private final boolean ioAddresses;

  public AVR8OperationTranslator (ReturnType returnType) {
    this(returnType, true, false);
  }

  public AVR8OperationTranslator (ReturnType returnType, boolean loadFromMemory) {
    this(returnType, loadFromMemory, false);
  }

  public AVR8OperationTranslator (ReturnType returnType, boolean loadFromMemory, boolean ioAddresses) {
    this.returnType = returnType;
    this.loadFromMemory = loadFromMemory;
    this.ioAddresses = ioAddresses;
  }

  public abstract void emit (TranslationCtx env, LowLevelRReilOpnd dst, LowLevelRReilOpnd src1, LowLevelRReilOpnd src2, List<LowLevelRReil> instructions);

  @Override public void translate (TranslationCtx ctx, Instruction insn, List<LowLevelRReil> instructions) {
    int numerOfArguments = insn.operands().size();

    TranslationState[] opndStates = new TranslationState[numerOfArguments];
    for (int i = 0; i < opndStates.length; i++) {
      opndStates[i] = AVR8OperandTranslator.translateOperand(ctx, insn.operand(i), loadFromMemory, ioAddresses);
    }
    //for (int i = 0; i < opndStates.length; i++) {
    //  assert opndStates[i].getInstructionStack().isEmpty() : "non-empty instruction stack for first operand";
    //}
    LowLevelRReilOpnd[] LowLevelRReilOpnds = new LowLevelRReilOpnd[numerOfArguments];
    LowLevelRReilOpnd address = null;
    for (int i = 0; i < LowLevelRReilOpnds.length; i++) {
      LowLevelRReilOpnds[i] = opndStates[i].getOperandStack().pop();
      if (!opndStates[i].getOperandStack().isEmpty())
        address = opndStates[i].getOperandStack().pop();
    }

    LowLevelRReilOpnd dst = null;
    if (returnType != ReturnType.None)
      dst = ctx.temporaryRegister(LowLevelRReilOpnds[0].size());

    for (int i = 0; i < opndStates.length; i++) {
      for(int j = 0; j < opndStates[i].getInstructionStack().size(); j++)
        instructions.add(opndStates[i].getInstructionStack().get(j));

      opndStates[i].getInstructionStack().clear();

//      while (!opndStates[i].getInstructionStack().isEmpty()) {
//        instructions.add(opndStates[i].getInstructionStack().pop());
//      }
    }

    emit(ctx, dst, numerOfArguments >= 1 ? LowLevelRReilOpnds[0] : null, numerOfArguments >= 2 ? LowLevelRReilOpnds[1] : null, instructions);
    // Result write-back.
    switch (returnType) {
      case None: {
        break;
      }
      case Register: {
        instructions.add(factory.MOV(ctx.getNextReilAddress(), LowLevelRReilOpnds[0], dst));
        break;
      }
      case Memory: {
        LowLevelRReilOpnd t0 = ctx.temporaryRegister(OperandSize.WORD);
        instructions.add(factory.CONVERT(ctx.getNextReilAddress(), t0, address));
        if (ioAddresses)
          instructions.add(factory.ADD(ctx.getNextReilAddress(), t0, t0, factory.immediate(t0.size(), AvrImplementation.$ATMEGA32L.getIORegistersOffset())));

        instructions.add(factory.STORE(ctx.getNextReilAddress(), t0, dst));
        break;
      }
    }
  }
}
