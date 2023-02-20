package rreil.disassembler.translators.x86.common;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class CmpsTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private static final InsnEmitter subFlagEmitter = new X86FlagHelpers.SubFlagEmitter();
  private final OperandSize elementSize;

  public CmpsTranslator (OperandSize elementSize) {
    this.elementSize = elementSize;
  }

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();

    final LowLevelRReilOpnd si = registerTranslator.translateRegister("esi").withSize(env.getDefaultArchitectureSize());
    final LowLevelRReilOpnd di = registerTranslator.translateRegister("edi").withSize(env.getDefaultArchitectureSize());

    final LowLevelRReilOpnd t1 = registerTranslator.temporaryRegister(env, elementSize);
    final LowLevelRReilOpnd t2 = registerTranslator.temporaryRegister(env, elementSize);
    final LowLevelRReilOpnd idx = factory.immediate(si.size(), elementSize.getSizeInBytes());
    final LowLevelRReilOpnd tmp = registerTranslator.temporaryRegister(env, elementSize);

    instructions.addAll(
        Arrays.asList(
        // t1:=[esi] and t2:=[edi]
            factory.LOAD(env.getNextReilAddress(), t1, si),
            factory.LOAD(env.getNextReilAddress(), t2, di)));

    final long base = env.getBaseAddress();
    final long reilBase = env.getCurrentReilOffset();

    final long doDecrement = 4L + reilBase;
    final long doFlags = 2L + doDecrement;

    instructions.addAll(Arrays.asList(
        factory.IFGOTORREIL(env.getNextReilAddress(), X86Helpers.DIRECTION_FLAG_OPERAND, base, doDecrement),
        // DF==0 => si++, di++
        factory.ADD(env.getNextReilAddress(), si, si, idx),
        factory.ADD(env.getNextReilAddress(), di, di, idx),
        factory.GOTORREIL(env.getNextReilAddress(), base, doFlags),
        // DF==1 => si--, di--
        factory.SUB(env.getNextReilAddress(), si, si, idx),
        factory.SUB(env.getNextReilAddress(), di, di, idx),
        // doFlags
        factory.SUB(env.getNextReilAddress(), tmp, t1, t2)));

    subFlagEmitter.emit(env, tmp, t1, t2, instructions);
  }
}
