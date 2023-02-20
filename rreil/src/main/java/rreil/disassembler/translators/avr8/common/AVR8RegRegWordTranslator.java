package rreil.disassembler.translators.avr8.common;

import java.util.Arrays;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationState;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.OperandSize;

/**
 * Generic wrapper for translating binary operations with register or immediate
 * arguments only.
 *
 * @author mb0
 */
public abstract class AVR8RegRegWordTranslator implements InsnTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();
  private final boolean[] wordArguments;

  public AVR8RegRegWordTranslator (boolean[] wordArguments) {
    this.wordArguments = wordArguments;
  }

  public abstract void emit (TranslationCtx ctx, LowLevelRReilOpnd dst, LowLevelRReilOpnd LowLevelRReilOpnd, LowLevelRReilOpnd LowLevelRReilOpnd0, List<LowLevelRReil> instructions);

  @Override public void translate (TranslationCtx ctx, Instruction insn, List<LowLevelRReil> instructions) {
    AVR8SregMemTranslator.loadSreg(ctx, instructions);

    int numberOfArguments = insn.operands().size();

    TranslationState[] opndStates = new TranslationState[numberOfArguments];

    for (int i = 0; i < opndStates.length; i++) {
      opndStates[i] = AVR8OperandTranslator.translateOperand(ctx, insn.operand(i));
    }

    for (int i = 0; i < opndStates.length; i++) {
      assert opndStates[i].getInstructionStack().isEmpty() : "non-empty instruction stack for first operand";
    }

    LowLevelRReilOpnd[] ops = new LowLevelRReilOpnd[numberOfArguments];
    for (int i = 0; i < ops.length; i++) {
      ops[i] = opndStates[i].getOperandStack().pop();
    }

    LowLevelRReilOpnd dst = ctx.temporaryRegister(OperandSize.WORD);
    LowLevelRReilOpnd t0 = ctx.temporaryRegister(dst.size());
    LowLevelRReilOpnd t1 = ctx.temporaryRegister(dst.size());

    LowLevelRReilOpnd[] args = new LowLevelRReilOpnd[numberOfArguments];
    LowLevelRReilOpnd[] opsHigh = new LowLevelRReilOpnd[numberOfArguments];

    for (int i = 0; i < args.length; i++) {
      if (wordArguments[i]) {
        args[i] = ctx.temporaryRegister(ops[i].size() * 2);

        opsHigh[i] = AVR8RegisterTranslator.$.getFollowingRegister(ops[i]);
        instructions.add(factory.CONVERT(ctx.getNextReilAddress(), t0, opsHigh[i]));
        instructions.add(factory.SHL(ctx.getNextReilAddress(), t0, t0, factory.immediate(t0.size(), ops[i].size())));
        instructions.add(factory.CONVERT(ctx.getNextReilAddress(), t1, ops[i]));
        instructions.add(factory.OR(ctx.getNextReilAddress(), args[i], t0, t1));
      } else
        args[i] = ops[i];
    }

    emit(ctx, dst, args.length >= 1 ? args[0] : null, args.length >= 2 ? args[1] : null, instructions);

    // Result write-back.
    instructions.addAll(Arrays.asList(factory.CONVERT(ctx.getNextReilAddress(), ops[0], dst)));
    instructions.addAll(Arrays.asList(factory.SHRS(ctx.getNextReilAddress(), dst, dst, factory.immediate(t0.size(), ops[0].size()))));
    instructions.addAll(Arrays.asList(factory.CONVERT(ctx.getNextReilAddress(), opsHigh[0], dst)));

    AVR8SregMemTranslator.storeSreg(ctx, instructions);
  }
}
