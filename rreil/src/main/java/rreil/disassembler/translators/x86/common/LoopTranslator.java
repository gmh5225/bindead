package rreil.disassembler.translators.x86.common;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.OperandTree;
import rreil.disassembler.translators.common.CondEmitter;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class LoopTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static final CondEmitter LOOP = new LoopEmitter();
  public static final CondEmitter LOOPE = new LoopeEmitter();
  public static final CondEmitter LOOPNE = new LoopneEmitter();
  private final CondEmitter emitter;

  public LoopTranslator (CondEmitter emitter) {
    this.emitter = emitter;
  }

  public static class LoopEmitter implements CondEmitter {
    @Override
    public LowLevelRReilOpnd emit (TranslationCtx env, List<LowLevelRReil> insns) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd cx = registerTranslator.translateRegister("ecx").withSize(env.getDefaultArchitectureSize());
      final LowLevelRReilOpnd zero = factory.immediate(cx.size(), 0);
      final LowLevelRReilOpnd cxZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cxNZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);

      insns.addAll(Arrays.asList(
          factory.CMPEQ(env.getNextReilAddress(), cxZero, cx, zero),
          factory.NOT(env.getNextReilAddress(), cxNZero, cxZero)));
      return cxNZero;
    }
  }

  public static class LoopeEmitter implements CondEmitter {
    @Override
    public LowLevelRReilOpnd emit (TranslationCtx env, List<LowLevelRReil> insns) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd cx = registerTranslator.translateRegister("ecx").withSize(env.getDefaultArchitectureSize());
      final LowLevelRReilOpnd zero = factory.immediate(cx.size(), 0);
      final LowLevelRReilOpnd cxZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cxNZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cond = registerTranslator.temporaryRegister(env, OperandSize.BIT);

      insns.addAll(Arrays.asList(
          factory.CMPEQ(env.getNextReilAddress(), cxZero, cx, zero),
          factory.NOT(env.getNextReilAddress(), cxNZero, cxZero),
          factory.AND(env.getNextReilAddress(), cond, cxNZero, X86Helpers.EQUAL_FLAG_OPERAND)));
      return cond;
    }
  }

  public static class LoopneEmitter implements CondEmitter {
    @Override
    public LowLevelRReilOpnd emit (TranslationCtx env, List<LowLevelRReil> insns) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd cx = registerTranslator.translateRegister("ecx").withSize(env.getDefaultArchitectureSize());
      final LowLevelRReilOpnd zero = factory.immediate(cx.size(), 0);
      final LowLevelRReilOpnd cxZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cxNZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd ne = registerTranslator.temporaryRegister(env, OperandSize.BIT);
      final LowLevelRReilOpnd cond = registerTranslator.temporaryRegister(env, OperandSize.BIT);

      insns.addAll(Arrays.asList(
          factory.CMPEQ(env.getNextReilAddress(), cxZero, cx, zero),
          factory.NOT(env.getNextReilAddress(), cxNZero, cxZero),
          factory.NOT(env.getNextReilAddress(), ne, X86Helpers.EQUAL_FLAG_OPERAND),
          factory.AND(env.getNextReilAddress(), cond, cxNZero, ne)));
      return cond;
    }
  }

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    env.setCurrentInstruction(instruction);
    final List<? extends OperandTree> operands = instruction.operands();
    final OperandTree targetOperand = operands.get(0);
    final TranslationState opnd1 = X86OperandTranslator.translateOperand(env, targetOperand);
    final LowLevelRReilOpnd target = opnd1.getOperandStack().pop();
    // decrements either 'ecx' or 'rax'
    // TODO: 16bit decrements
    final LowLevelRReilOpnd cx = registerTranslator.translateRegister("ecx").withSize(env.getDefaultArchitectureSize());
    // cx--
    instructions.add(factory.SUB(env.getNextReilAddress(), cx, cx, factory.immediate(cx.size(), 1)));
    final LowLevelRReilOpnd cond = emitter.emit(env, instructions);
    instructions.add(factory.IFGOTO(env.getNextReilAddress(), cond, target));
  }
}
