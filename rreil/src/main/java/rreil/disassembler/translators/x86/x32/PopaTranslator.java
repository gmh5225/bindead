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

public class PopaTranslator implements InsnTranslator {
  @Override public void translate (TranslationCtx env, Instruction instruction, List<LowLevelRReil> instructions) {
    // Popa is 32bit only!
    RegisterTranslator registerTranslator = env.getRegisterTranslator();
    env.setCurrentInstruction(instruction);

    LowLevelRReilOpnd edi = registerTranslator.translateRegister("edi");
    LowLevelRReilOpnd esi = registerTranslator.translateRegister("esi");
    LowLevelRReilOpnd ebp = registerTranslator.translateRegister("ebp");
    LowLevelRReilOpnd ebx = registerTranslator.translateRegister("ebx");
    LowLevelRReilOpnd edx = registerTranslator.translateRegister("edx");
    LowLevelRReilOpnd ecx = registerTranslator.translateRegister("ecx");
    LowLevelRReilOpnd eax = registerTranslator.translateRegister("eax");
    LowLevelRReilOpnd tmp = registerTranslator.temporaryRegister(env, OperandSize.DWORD);

    _POP(env, edi, instructions);
    _POP(env, esi, instructions);
    _POP(env, ebp, instructions);
    _POP(env, tmp, instructions);
    _POP(env, ebx, instructions);
    _POP(env, edx, instructions);
    _POP(env, ecx, instructions);
    _POP(env, eax, instructions);
  }
}
