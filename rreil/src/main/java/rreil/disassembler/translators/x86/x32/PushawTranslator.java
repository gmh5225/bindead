package rreil.disassembler.translators.x86.x32;

import static rreil.disassembler.translators.x86.common.X86Helpers._PUSH;

import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class PushawTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    // Pusha is 32bit only

    env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();

    final LowLevelRReilOpnd sp = registerTranslator.translateRegister("sp");
    final LowLevelRReilOpnd di = registerTranslator.translateRegister("di");
    final LowLevelRReilOpnd si = registerTranslator.translateRegister("si");
    final LowLevelRReilOpnd bp = registerTranslator.translateRegister("bp");
    final LowLevelRReilOpnd bx = registerTranslator.translateRegister("bx");
    final LowLevelRReilOpnd dx = registerTranslator.translateRegister("dx");
    final LowLevelRReilOpnd cx = registerTranslator.translateRegister("cx");
    final LowLevelRReilOpnd ea = registerTranslator.translateRegister("ax");
    final LowLevelRReilOpnd tmp = registerTranslator.temporaryRegister(env, OperandSize.WORD);

    instructions.add(factory.MOV(env.getNextReilAddress(), tmp, sp));
    _PUSH(env, ea, instructions);
    _PUSH(env, cx, instructions);
    _PUSH(env, dx, instructions);
    _PUSH(env, bx, instructions);
    _PUSH(env, tmp, instructions);
    _PUSH(env, bp, instructions);
    _PUSH(env, si, instructions);
    _PUSH(env, di, instructions);
  }
}
