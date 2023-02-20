package rreil.disassembler.translators.x86.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.RReilAddr;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class RepneTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private final InsnTranslator translator;

  public RepneTranslator (InsnTranslator translator) {
    this.translator = translator;
  }

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    final LowLevelRReilOpnd cx = registerTranslator.translateRegister("ecx").withSize(env.getDefaultArchitectureSize());
    final LowLevelRReilOpnd cxZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
    final LowLevelRReilOpnd zfZero = registerTranslator.temporaryRegister(env, OperandSize.BIT);
    final LowLevelRReilOpnd zero = factory.immediate(cx.size(), 0);
    final LowLevelRReilOpnd one = factory.immediate(cx.size(), 1);

    ArrayList<LowLevelRReil> inner = new ArrayList<LowLevelRReil>();

    // Reserve the first two addresses for the loop exit condition code
    final RReilAddr addr1 = env.getNextReilAddress();
    final RReilAddr addr2 = env.getNextReilAddress();

    translator.translate(env, instruction, inner);

    final long base = env.getBaseAddress();
    final long innerLen = inner.size();
    final long done = 2L + 3L + innerLen;

    instructions.addAll(Arrays.asList(
        factory.CMPEQ(addr1, cxZero, cx, zero),
        factory.IFGOTORREIL(addr2, cxZero, base, done)));

    instructions.addAll(inner);

    instructions.addAll(Arrays.asList(
        factory.SUB(env.getNextReilAddress(), cx, cx, one),
        factory.NOT(env.getNextReilAddress(), zfZero, X86Helpers.EQUAL_FLAG_OPERAND),
        factory.IFGOTORREIL(env.getNextReilAddress(), zfZero, base, 0),
        // Done:
        factory.NOP(env.getNextReilAddress())));
  }
}
