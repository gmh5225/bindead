package rreil.disassembler.translators.avr8.common;

import rreil.disassembler.translators.avr8.implementations.AvrImplementation;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.OperandSize;

/**
 *
 * @author mb0
 */
public class AVR8Helpers {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static final LowLevelRReilOpnd C_OPERAND = AVR8RegisterTranslator.makeRegister(OperandSize.BIT, AvrImplementation.$ATMEGA32L.getSregAddress()*8 + 0);
  public static final LowLevelRReilOpnd Z_OPERAND = AVR8RegisterTranslator.makeRegister(OperandSize.BIT, AvrImplementation.$ATMEGA32L.getSregAddress()*8 + 1);
  public static final LowLevelRReilOpnd N_OPERAND = AVR8RegisterTranslator.makeRegister(OperandSize.BIT, AvrImplementation.$ATMEGA32L.getSregAddress()*8 + 2);
  public static final LowLevelRReilOpnd V_OPERAND = AVR8RegisterTranslator.makeRegister(OperandSize.BIT, AvrImplementation.$ATMEGA32L.getSregAddress()*8 + 3);
  public static final LowLevelRReilOpnd S_OPERAND = AVR8RegisterTranslator.makeRegister(OperandSize.BIT, AvrImplementation.$ATMEGA32L.getSregAddress()*8 + 4);
  public static final LowLevelRReilOpnd H_OPERAND = AVR8RegisterTranslator.makeRegister(OperandSize.BIT, AvrImplementation.$ATMEGA32L.getSregAddress()*8 + 5);
  public static final LowLevelRReilOpnd T_OPERAND = AVR8RegisterTranslator.makeRegister(OperandSize.BIT, AvrImplementation.$ATMEGA32L.getSregAddress()*8 + 6);
  public static final LowLevelRReilOpnd I_OPERAND = AVR8RegisterTranslator.makeRegister(OperandSize.BIT, AvrImplementation.$ATMEGA32L.getSregAddress()*8 + 7);

  public static final String LE = "LE";
  public static final LowLevelRReilOpnd LE_OPERAND = factory.variable(OperandSize.BIT, LE);
  public static final String BE = "BE";
  public static final LowLevelRReilOpnd BE_OPERAND = factory.variable(OperandSize.BIT, BE);
  public static final LowLevelRReilOpnd B_OPERAND = C_OPERAND;
  public static final String L = "L";
  public static final LowLevelRReilOpnd L_OPERAND = factory.variable(OperandSize.BIT, L);
  public static final LowLevelRReilOpnd RAMPTR = factory.variable(OperandSize.WORD, AVR8RegisterTranslator.RAMPTR);
}
