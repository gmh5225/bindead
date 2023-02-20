package rreil.disassembler.translators.x86.common;

import java.util.Stack;

import rreil.disassembler.OperandPostorderIterator;
import rreil.disassembler.OperandTree;
import rreil.disassembler.OperandTree.Node;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public final class X86OperandTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static TranslationState translateOperand (final TranslationCtx env, final OperandTree opnd) {
    return translateOperand(env, opnd, true);
  }

  public static TranslationState translateOperand (final TranslationCtx env, final OperandTree opnd, final boolean load) {
    final OperandPostorderIterator it = new OperandPostorderIterator(opnd.getRoot());
    final TranslationState state = new TranslationState();

    while (it.next()) {
      final Node t = it.current();

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
          // A memory dereference always pushed two operands onto the operand stack.
          // If `load` is false, a dummy temporary variable is pushed ontop. Otherwise
          // the operand stack contains both the address as well as the dereferenced value.
          if (load)
            translateMemoryDereferenceAndLoad(env, state, it);
          else
            translateMemoryDereferenceNoLoad(env, state, it);
          break;
        case Size:
          break;
        default:
          throw new RuntimeException(
              "Error: Unhandled expression type during operand translation: " + t.getType());
      }
    }

    return state;
  }

  private static void translateRegister (final TranslationCtx env, final TranslationState state, final OperandPostorderIterator it) {

    final Node t = it.current();
    final LowLevelRReilOpnd reg = env.getRegisterTranslator().translateRegister((String) t.getData());

    // TODO: Check size conversion of segment registers
    if (it.currentSizeScope().intValue() != reg.size())
      if (isSegment((String) t.getData())) {
        state.getOperandStack().push(reg);
        state.getOperandStack().push(reg.withSize(it.currentSizeScope().intValue()));
      } else
        throw new RuntimeException(
            "Error: Register accessed with different size than expected (eg. byte eax)");
    else
      state.getOperandStack().push(reg);
  }

  private static boolean isSegment (final String value) {
    return value.equals("cs") ||
        value.equals("ds") ||
        value.equals("es") ||
        value.equals("fs") ||
        value.equals("gs") ||
        value.equals("ss");
  }

  private static void translateImmediate (final TranslationCtx env, final TranslationState state, final OperandPostorderIterator it) {
    // TODO: handle size of immediate
    final Node t = it.current();
    state.getOperandStack().push(factory.immediate(it.currentSizeScope(), (Number) t.getData()));
  }

  private static void translateOperator (final TranslationCtx env, final TranslationState state, final OperandPostorderIterator it) {
    final Node t = it.current();
    final String operator = (String) t.getData();

    // Binary operators

    if (operator.equals("+"))
      translatePlusOperator(env, state, it);
    else if (operator.equals("*"))
      translateMulOperator(env, state, it);
    // Unary operators
    else if (operator.endsWith(":"))
      translateSizePrefixOperator(env, state, it);
    else
      throw new RuntimeException(
          "Encountered invalid operator during operand translation '" + operator + "'");
  }

  private static void translatePlusOperator (final TranslationCtx env, final TranslationState state, final OperandPostorderIterator it) {
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();

    final LowLevelRReilOpnd opnd1 = state.getOperandStack().pop();
    final LowLevelRReilOpnd opnd2 = state.getOperandStack().pop();

    assert opnd1.size() == opnd2.size() : "Invalid operand sizes";

    final LowLevelRReilOpnd target = registerTranslator.temporaryRegister(env, opnd1.size());
    final LowLevelRReil insn = factory.ADD(env.getNextReilAddress(), target, opnd1, opnd2);
    state.getInstructionStack().push(insn);
    state.getOperandStack().push(target);
  }

  private static void translateMulOperator (final TranslationCtx env, final TranslationState state, final OperandPostorderIterator it) {
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    final LowLevelRReilOpnd opnd1 = state.getOperandStack().pop();
    final LowLevelRReilOpnd opnd2 = state.getOperandStack().pop();

    assert opnd1.size() == opnd2.size() : "Invalid operand sizes";

    final LowLevelRReilOpnd target = registerTranslator.temporaryRegister(env, opnd1.size());
    final LowLevelRReil insn = factory.MUL(env.getNextReilAddress(), target, opnd1, opnd2);
    state.getInstructionStack().push(insn);
    state.getOperandStack().push(target);
  }

  @SuppressWarnings("unused")
  private static void translateSizePrefixOperator (final TranslationCtx env, final TranslationState state, final OperandPostorderIterator it) {
    // TODO: Log ignoring size prefix operator
  }

  private static void translateMemoryDereferenceAndLoad (final TranslationCtx env, final TranslationState state, final OperandPostorderIterator it) {
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();

    Stack<Number> stack = it.getOperandSizeStack();
    stack.pop();
    final Number derefSz = stack.pop();

    final LowLevelRReilOpnd address = state.getOperandStack().peek();
    final LowLevelRReilOpnd target = registerTranslator.temporaryRegister(env, derefSz);
    final LowLevelRReil insn = factory.LOAD(env.getNextReilAddress(), target, address);

    state.getInstructionStack().push(insn);
    state.getOperandStack().push(target);
  }

  private static void translateMemoryDereferenceNoLoad (final TranslationCtx env, final TranslationState state, final OperandPostorderIterator it) {
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();

    Stack<Number> stack = it.getOperandSizeStack();
    stack.pop();
    final Number derefSz = stack.pop();

    state.getOperandStack().push(registerTranslator.temporaryRegister(env, derefSz));
  }
}
