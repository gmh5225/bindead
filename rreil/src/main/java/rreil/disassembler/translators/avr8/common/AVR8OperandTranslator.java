package rreil.disassembler.translators.avr8.common;

import java.util.Stack;

import rreil.disassembler.OperandPostorderIterator;
import rreil.disassembler.OperandTree;
import rreil.disassembler.OperandTree.Node;
import rreil.disassembler.translators.avr8.implementations.AvrImplementation;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.OperandSize;

public final class AVR8OperandTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static TranslationState translateOperand (TranslationCtx env, OperandTree opnd) {
    return translateOperand(env, opnd, true);
  }

  public static TranslationState translateOperand (TranslationCtx env, OperandTree opnd, boolean load) {
    return translateOperand(env, opnd, load, false);
  }

  public static TranslationState translateOperand (TranslationCtx env, OperandTree opnd, boolean load, boolean ioAddresses) {
    OperandPostorderIterator it = new OperandPostorderIterator(opnd.getRoot());
    TranslationState state = new TranslationState();
    while (it.next()) {
      Node t = it.current();
      switch (t.getType()) {
        case Sym:
          translateRegister(env, state, it);
          break;
        case Immi:
          translateImmediate(env, state, it);
          break;
        case Op:
          translateOperator(env, state, it);
          break;
        case Mem:
          // A memory dereference always pushed two operands onto the operand
          // stack.
          // If `load` is false, a dummy temporary variable is pushed ontop.
          // Otherwise
          // the operand stack contains both the address as well as the
          // dereferenced value.
          if (load)
            translateMemoryDereferenceAndLoad(env, state, it, ioAddresses);
          else
            translateMemoryDereferenceNoLoad(env, state, it);
          break;
        case Size:
          break;
        case Immf:
          throw new RuntimeException("Error: Unhandled expression type during operand translation: " + t.getType());
      }
    }
    return state;
  }

  private static void translateRegister (TranslationCtx env, TranslationState state, OperandPostorderIterator it) {
    Node t = it.current();
    String registerName = (String) t.getData();
    LowLevelRReilOpnd reg = env.getRegisterTranslator().translateRegister(registerName);
    // TODO: Check size conversion of segment registers
    if (it.currentSizeScope().intValue() != reg.size())
      throw new RuntimeException("Error: Register accessed with different size than expected (eg. byte eax)");
    else
      state.getOperandStack().push(reg);
  }

  private static void translateImmediate (TranslationCtx env, TranslationState state, OperandPostorderIterator it) {
    // TODO: handle size of immediate
    Node t = it.current();
    state.getOperandStack().push(factory.immediate(it.currentSizeScope(), (Number) t.getData()));
  }

  private static int translateOperator (TranslationCtx env, TranslationState state, OperandPostorderIterator it) {
    Node t = it.current();
    String operator = (String) t.getData();
    // Binary operators
    if (operator.equals("+"))
      return translatePlusOperator(env, state, null);
    else if (operator.equals("*"))
      return translateMulOperator(env, state, null);
    // Unary operators
    else if (operator.endsWith("++"))
      return translateSizePrefixPostIncOperator(env, state, null);
    else if (operator.endsWith("--"))
      return translateSizePrefixPreDecOperator(env, state, null);
    else
      throw new RuntimeException("Encountered invalid operator during operand translation '" + operator + "'");
  }

  private static int translatePlusOperator (TranslationCtx env, TranslationState state, OperandPostorderIterator it) {
    RegisterTranslator registerTranslator = env.getRegisterTranslator();
    LowLevelRReilOpnd opnd1 = state.getOperandStack().pop();
    LowLevelRReilOpnd opnd2 = state.getOperandStack().pop();
    assert opnd1.size() == opnd2.size() : "Invalid operand sizes";
    LowLevelRReilOpnd target = registerTranslator.temporaryRegister(env, opnd1.size());
    LowLevelRReil insn = factory.ADD(env.getNextReilAddress(), target, opnd1, opnd2);
    state.getInstructionStack().push(insn);
    state.getOperandStack().push(target);
    return 1;
  }

  private static int translateMulOperator (TranslationCtx env, TranslationState state, OperandPostorderIterator it) {
    RegisterTranslator registerTranslator = env.getRegisterTranslator();
    LowLevelRReilOpnd opnd1 = state.getOperandStack().pop();
    LowLevelRReilOpnd opnd2 = state.getOperandStack().pop();
    assert opnd1.size() == opnd2.size() : "Invalid operand sizes";
    LowLevelRReilOpnd target = registerTranslator.temporaryRegister(env, opnd1.size());
    LowLevelRReil insn = factory.MUL(env.getNextReilAddress(), target, opnd1, opnd2);
    state.getInstructionStack().push(insn);
    state.getOperandStack().push(target);
    return 1;
  }

  private static int translateSizePrefixPostIncOperator (TranslationCtx env, TranslationState state, OperandPostorderIterator it) {
    LowLevelRReilOpnd opnd1 = state.getOperandStack().pop();

    LowLevelRReilOpnd t0 = env.temporaryRegister(opnd1.size());
    state.getInstructionStack().push(factory.MOV(env.getNextReilAddress(), t0, opnd1));

    LowLevelRReilOpnd opnd2 = factory.immediate(opnd1.size(), 1);
    LowLevelRReil insn = factory.ADD(env.getNextReilAddress(), opnd1, opnd1, opnd2);
    state.getInstructionStack().push(insn);
    state.getOperandStack().push(t0);
    return 1;
  }

  private static int translateSizePrefixPreDecOperator (TranslationCtx env, TranslationState state, OperandPostorderIterator it) {
    LowLevelRReilOpnd opnd1 = state.getOperandStack().pop();
    LowLevelRReilOpnd opnd2 = factory.immediate(opnd1.size(), 1);
    LowLevelRReil insn = factory.SUB(env.getNextReilAddress(), opnd1, opnd1, opnd2);
    state.getInstructionStack().push(insn);
    state.getOperandStack().push(opnd1);
    return 1;
  }

  private static int translateMemoryDereferenceAndLoad (TranslationCtx env, TranslationState state, OperandPostorderIterator it, boolean ioAddresses) {
    RegisterTranslator registerTranslator = env.getRegisterTranslator();
    Stack<Number> stack = it.getOperandSizeStack();
    stack.pop();
    Number derefSz = stack.pop();
    LowLevelRReilOpnd address = state.getOperandStack().peek();

    LowLevelRReilOpnd t0 = env.temporaryRegister(OperandSize.WORD);
    state.getInstructionStack().push(factory.CONVERT(env.getNextReilAddress(), t0, address));
    if (ioAddresses)
      state.getInstructionStack().push(factory.ADD(env.getNextReilAddress(), t0, t0, factory.immediate(t0.size(), AvrImplementation.$ATMEGA32L.getIORegistersOffset())));

    LowLevelRReilOpnd target = registerTranslator.temporaryRegister(env, derefSz);
    LowLevelRReil insn = factory.LOAD(env.getNextReilAddress(), target, t0);
    state.getInstructionStack().push(insn);
    state.getOperandStack().push(target);
    return 1;
  }

  private static int translateMemoryDereferenceNoLoad (TranslationCtx env, TranslationState state, OperandPostorderIterator it) {
    RegisterTranslator registerTranslator = env.getRegisterTranslator();
    Stack<Number> stack = it.getOperandSizeStack();
    stack.pop();
    Number derefSz = stack.pop();
    state.getOperandStack().push(registerTranslator.temporaryRegister(env, derefSz));
    return 1;
  }
}