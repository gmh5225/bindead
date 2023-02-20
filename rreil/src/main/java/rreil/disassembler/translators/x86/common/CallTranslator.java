package rreil.disassembler.translators.x86.common;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.OperandTree;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class CallTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override
  public void translate (TranslationCtx env, Instruction instruction, List<LowLevelRReil> instructions) {

    //env.setCurrentInstruction(instruction);
    final RegisterTranslator registerTranslator = env.getRegisterTranslator();

    final List<OperandTree> operands = instruction.operands();
    final OperandTree targetOperand = operands.get(0);
    final TranslationState opnd = X86OperandTranslator.translateOperand(env, targetOperand);

    final LowLevelRReilOpnd target = opnd.getOperandStack().pop();

    final LowLevelRReilOpnd sp = registerTranslator.translateRegister("esp").withSize(env.getDefaultArchitectureSize());
    final LowLevelRReilOpnd retAddr = factory.immediate(sp.size(), env.getFollowupAddress(instruction));

    final long inc = sp.size() / 8;

    instructions.addAll(opnd.getInstructionStack());

    instructions.addAll(Arrays.asList(
        factory.SUB(env.getNextReilAddress(), sp, sp, factory.immediate(sp.size(), inc)),
        factory.STORE(env.getNextReilAddress(), sp, retAddr),
        factory.CALL(env.getNextReilAddress(), target)));
  }
}
