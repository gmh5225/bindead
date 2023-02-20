package rreil.disassembler.translators.x86.common;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.OperandTree;
import rreil.disassembler.OperandTree.Type;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class RetnTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  @Override public void translate (final TranslationCtx env, final Instruction instruction, final List<LowLevelRReil> instructions) {
    env.setCurrentInstruction(instruction);
    RegisterTranslator registerTranslator = env.getRegisterTranslator();
    List<? extends OperandTree> operands = instruction.operands();
    long stackOffset = 0;
    if (operands.size() == 1) {
      OperandTree targetOperand = operands.get(0);
      TranslationState opnd = X86OperandTranslator.translateOperand(env, targetOperand);
      LowLevelRReilOpnd imm = opnd.getOperandStack().pop();
      assert imm.getRoot().getChildren().get(0).getType() == Type.Immi;
      stackOffset = ((Number) imm.getRoot().getChildren().get(0).getData()).longValue();
    }
    LowLevelRReilOpnd sp = registerTranslator.translateRegister("esp").withSize(env.getDefaultArchitectureSize());
    LowLevelRReilOpnd retAddr = registerTranslator.temporaryRegister(env, sp.size());
    long inc = sp.size() / 8; // TODO: Check division of size annotation
    stackOffset += inc;
    instructions.addAll(
        Arrays.asList(
            factory.LOAD(env.getNextReilAddress(), retAddr, sp),
            factory.ADD(env.getNextReilAddress(), sp, sp, factory.immediate(sp.size(), stackOffset)),
            factory.RETURN(env.getNextReilAddress(), retAddr)));
  }
}
