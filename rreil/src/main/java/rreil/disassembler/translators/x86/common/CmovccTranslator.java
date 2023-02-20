package rreil.disassembler.translators.x86.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.CondEmitter;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.RReilAddr;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class CmovccTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private final CondEmitter emitter;
  private final InsnTranslator movTranslator = new MovTranslator();

  public CmovccTranslator (CondEmitter emitter) {
    this.emitter = emitter;
  }

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);

    final LowLevelRReilOpnd cond = emitter.emit(env, instructions);
    final long condOffset = env.getCurrentReilOffset();

    final RReilAddr addr1 = env.getNextReilAddress();
    final RReilAddr addr2 = env.getNextReilAddress();

    ArrayList<LowLevelRReil> movCode = new ArrayList<LowLevelRReil>();
    movTranslator.translate(env, instruction, movCode);

    final long baseOffset = env.getBaseAddress();
    final long takenOffset = condOffset + 2L;
    final long exitOffset = movCode.size() + takenOffset;

    instructions.addAll(Arrays.asList(
        factory.IFGOTORREIL(addr1, cond, baseOffset, takenOffset),
        factory.GOTORREIL(addr2, baseOffset, exitOffset)));
    instructions.addAll(movCode);
    instructions.add(factory.NOP(env.getNextReilAddress()));
  }
}
