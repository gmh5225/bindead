package rreil.disassembler.translators.avr8.emitters;

import java.util.List;

import rreil.disassembler.translators.avr8.implementations.AvrImplementation;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.OperandSize;

public class StackPointerBuilder implements InsnEmitter {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static final InsnEmitter $ = new StackPointerBuilder();

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    final LowLevelRReilOpnd sphAddr = factory.immediate(OperandSize.BYTE, AvrImplementation.$ATMEGA32L.getSpAddress()[0]);
    final LowLevelRReilOpnd splAddr = factory.immediate(OperandSize.BYTE, AvrImplementation.$ATMEGA32L.getSpAddress()[1]);
    final LowLevelRReilOpnd sph = env.temporaryRegister(OperandSize.BYTE);
    final LowLevelRReilOpnd spl = env.temporaryRegister(OperandSize.BYTE);
    instructions.add(factory.LOAD(env.getNextReilAddress(), sph, sphAddr));
    instructions.add(factory.LOAD(env.getNextReilAddress(), spl, splAddr));

    final LowLevelRReilOpnd t0 = env.temporaryRegister(OperandSize.WORD);
    instructions.add(factory.CONVERT(env.getNextReilAddress(), t0, spl));

    instructions.add(factory.CONVERT(env.getNextReilAddress(), dst, sph));
    instructions.add(factory.SHL(env.getNextReilAddress(), dst, dst, factory.immediate(dst.size(), sph.size())));
    instructions.add(factory.OR(env.getNextReilAddress(), dst, dst, t0));
  }

}
