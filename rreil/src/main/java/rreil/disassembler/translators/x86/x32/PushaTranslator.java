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


public class PushaTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    // Pusha is 32bit only

    final RegisterTranslator registerTranslator = env.getRegisterTranslator();
    env.setCurrentInstruction(instruction);

    final LowLevelRReilOpnd esp = registerTranslator.translateRegister("esp");
    final LowLevelRReilOpnd edi = registerTranslator.translateRegister("edi");
    final LowLevelRReilOpnd esi = registerTranslator.translateRegister("esi");
    final LowLevelRReilOpnd ebp = registerTranslator.translateRegister("ebp");
    final LowLevelRReilOpnd ebx = registerTranslator.translateRegister("ebx");
    final LowLevelRReilOpnd edx = registerTranslator.translateRegister("edx");
    final LowLevelRReilOpnd ecx = registerTranslator.translateRegister("ecx");
    final LowLevelRReilOpnd eax = registerTranslator.translateRegister("eax");
    final LowLevelRReilOpnd tmp = registerTranslator.temporaryRegister(env, OperandSize.DWORD);

    instructions.add(factory.MOV(env.getNextReilAddress(), tmp, esp));
    _PUSH(env, eax, instructions);
    _PUSH(env, ecx, instructions);
    _PUSH(env, edx, instructions);
    _PUSH(env, ebx, instructions);
    _PUSH(env, tmp, instructions);
    _PUSH(env, ebp, instructions);
    _PUSH(env, esi, instructions);
    _PUSH(env, edi, instructions);
  }
}
