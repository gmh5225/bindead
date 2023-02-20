package rreil.disassembler.translators.avr8.emitters;

import java.util.List;

import rreil.disassembler.translators.avr8.implementations.AvrImplementation;
import rreil.disassembler.translators.common.InsnEmitter;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.OperandSize;

public class StackPointerWritebackEmitter implements InsnEmitter {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static final InsnEmitter $ = new StackPointerWritebackEmitter();

  @Override public void emit (final TranslationCtx env, final LowLevelRReilOpnd dst, final LowLevelRReilOpnd src1, final LowLevelRReilOpnd src2, final List<LowLevelRReil> instructions) {
    final LowLevelRReilOpnd sphAddr = factory.immediate(OperandSize.BYTE, AvrImplementation.$ATMEGA32L.getSpAddress()[0]);
    final LowLevelRReilOpnd splAddr = factory.immediate(OperandSize.BYTE, AvrImplementation.$ATMEGA32L.getSpAddress()[1]);
    final LowLevelRReilOpnd sph = env.temporaryRegister(OperandSize.BYTE);
    final LowLevelRReilOpnd spl = env.temporaryRegister(OperandSize.BYTE);

    instructions.add(factory.CONVERT(env.getNextReilAddress(), spl, src1));
    final LowLevelRReilOpnd t1 = env.temporaryRegister(OperandSize.WORD);
    instructions.add(factory.SHRS(env.getNextReilAddress(), t1, src1, factory.immediate(src1.size(), spl.size())));
    instructions.add(factory.CONVERT(env.getNextReilAddress(), sph, t1));
    instructions.add(factory.STORE(env.getNextReilAddress(), sphAddr, sph));
    instructions.add(factory.STORE(env.getNextReilAddress(), splAddr, spl));
  }

}
