package rreil.disassembler.translators.x86.x32;

import static rreil.disassembler.translators.x86.common.X86Helpers._POP;

import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class PopawTranslator implements InsnTranslator {
  @Override public void translate (TranslationCtx env, Instruction instruction, List<LowLevelRReil> instructions) {
    // Popaw is 32bit only!
    RegisterTranslator registerTranslator = env.getRegisterTranslator();
    env.setCurrentInstruction(instruction);
    LowLevelRReilOpnd di = registerTranslator.translateRegister("di");
    LowLevelRReilOpnd si = registerTranslator.translateRegister("si");
    LowLevelRReilOpnd bp = registerTranslator.translateRegister("bp");
    LowLevelRReilOpnd bx = registerTranslator.translateRegister("bx");
    LowLevelRReilOpnd dx = registerTranslator.translateRegister("dx");
    LowLevelRReilOpnd cx = registerTranslator.translateRegister("cx");
    LowLevelRReilOpnd ax = registerTranslator.translateRegister("ax");
    LowLevelRReilOpnd tmp = registerTranslator.temporaryRegister(env, OperandSize.WORD);

    _POP(env, di, instructions);
    _POP(env, si, instructions);
    _POP(env, bp, instructions);
    _POP(env, tmp, instructions);
    _POP(env, bx, instructions);
    _POP(env, dx, instructions);
    _POP(env, cx, instructions);
    _POP(env, ax, instructions);
  }
}
