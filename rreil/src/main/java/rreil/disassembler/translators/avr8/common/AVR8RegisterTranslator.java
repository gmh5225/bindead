package rreil.disassembler.translators.avr8.common;

import java.util.HashMap;

import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.OperandSize;

public class AVR8RegisterTranslator implements RegisterTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static final AVR8RegisterTranslator $ = new AVR8RegisterTranslator();
  private static final HashMap<String, LowLevelRReilOpnd> registers = new HashMap<String, LowLevelRReilOpnd>();
  private static final HashMap<LowLevelRReilOpnd, Integer> registerNumbers = new HashMap<LowLevelRReilOpnd, Integer>();
  public static final String X = "X";
  public static final String Y = "Y";
  public static final String Z = "Z";
  public static final String R0 = "r0";
  public static final String R1 = "r1";
  public static final String R2 = "r2";
  public static final String R3 = "r3";
  public static final String R4 = "r4";
  public static final String R5 = "r5";
  public static final String R6 = "r6";
  public static final String R7 = "r7";
  public static final String R8 = "r8";
  public static final String R9 = "r9";
  public static final String R10 = "r10";
  public static final String R11 = "r11";
  public static final String R12 = "r12";
  public static final String R13 = "r13";
  public static final String R14 = "r14";
  public static final String R15 = "r15";
  public static final String R16 = "r16";
  public static final String R17 = "r17";
  public static final String R18 = "r18";
  public static final String R19 = "r19";
  public static final String R20 = "r20";
  public static final String R21 = "r21";
  public static final String R22 = "r22";
  public static final String R23 = "r23";
  public static final String R24 = "r24";
  public static final String R25 = "r25";
  public static final String R26 = "r26";
  public static final String R27 = "r27";
  public static final String R28 = "r28";
  public static final String R29 = "r29";
  public static final String R30 = "r30";
  public static final String R31 = "r31";
  public static final String RAMPTR = "ramptr";
  public static final String RAM = "ram";
  private static int regNum = -8;

  private static int nextReg () {
    regNum += 8;
    return regNum;
  }

  static {
    registers.put(R0, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R1, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R2, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R3, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R4, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R5, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R6, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R7, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R8, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R9, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R10, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R11, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R12, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R13, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R14, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R15, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R16, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R17, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R18, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R19, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R20, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R21, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R22, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R23, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R24, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R25, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R26, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R27, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R28, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R29, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R30, makeRegister(OperandSize.BYTE, nextReg()));
    registers.put(R31, makeRegister(OperandSize.BYTE, nextReg()));

    registers.put(X, makeRegister(OperandSize.WORD, registers.get(R26).getOffsetOrZero()));
    registers.put(Y, makeRegister(OperandSize.WORD, registers.get(R28).getOffsetOrZero()));
    registers.put(Z, makeRegister(OperandSize.WORD, registers.get(R30).getOffsetOrZero()));

    registers.put("C", AVR8Helpers.C_OPERAND);
    registers.put("V", AVR8Helpers.V_OPERAND);
    registers.put("N", AVR8Helpers.N_OPERAND);
    registers.put("I", AVR8Helpers.I_OPERAND);
    registers.put("T", AVR8Helpers.T_OPERAND);
    registers.put("BE", AVR8Helpers.BE_OPERAND);
    registers.put("B", AVR8Helpers.B_OPERAND);
    registers.put("H", AVR8Helpers.H_OPERAND);
    registers.put("LE", AVR8Helpers.LE_OPERAND);
    registers.put("L", AVR8Helpers.L_OPERAND);
    registers.put("S", AVR8Helpers.S_OPERAND);

    // registers.put(R26, makeRegister(OperandSize.BYTE, X));
    // registers.put(R27, makeRegister(OperandSize.BYTE, X).withOffset(8,
    // OperandSize.BYTE));
    // registers.put(R28, makeRegister(OperandSize.BYTE, Y));
    // registers.put(R29, makeRegister(OperandSize.BYTE, Y).withOffset(8,
    // OperandSize.BYTE));
    // registers.put(R30, makeRegister(OperandSize.BYTE, Z));
    // registers.put(R31, makeRegister(OperandSize.BYTE, Z).withOffset(8,
    // OperandSize.BYTE));

    registerNumbers.put(registers.get(R0), 0);
    registerNumbers.put(registers.get(R1), 1);
    registerNumbers.put(registers.get(R2), 2);
    registerNumbers.put(registers.get(R3), 3);
    registerNumbers.put(registers.get(R4), 4);
    registerNumbers.put(registers.get(R5), 5);
    registerNumbers.put(registers.get(R6), 6);
    registerNumbers.put(registers.get(R7), 7);
    registerNumbers.put(registers.get(R8), 8);
    registerNumbers.put(registers.get(R9), 9);
    registerNumbers.put(registers.get(R10), 10);
    registerNumbers.put(registers.get(R11), 11);
    registerNumbers.put(registers.get(R12), 12);
    registerNumbers.put(registers.get(R13), 13);
    registerNumbers.put(registers.get(R14), 14);
    registerNumbers.put(registers.get(R15), 15);
    registerNumbers.put(registers.get(R16), 16);
    registerNumbers.put(registers.get(R17), 17);
    registerNumbers.put(registers.get(R18), 18);
    registerNumbers.put(registers.get(R19), 19);
    registerNumbers.put(registers.get(R20), 20);
    registerNumbers.put(registers.get(R21), 21);
    registerNumbers.put(registers.get(R22), 22);
    registerNumbers.put(registers.get(R23), 23);
    registerNumbers.put(registers.get(R24), 24);
    registerNumbers.put(registers.get(R25), 25);
    registerNumbers.put(registers.get(R26), 26);
    registerNumbers.put(registers.get(R27), 27);
    registerNumbers.put(registers.get(R28), 28);
    registerNumbers.put(registers.get(R29), 29);
    registerNumbers.put(registers.get(R30), 30);
    registerNumbers.put(registers.get(R31), 31);
  }

  public static LowLevelRReilOpnd makeRegister (OperandSize size, int offset) {
    return factory.variable(size, RAM).withOffset(offset, size.getBits());
  }

  private AVR8RegisterTranslator () {
  }

  @Override public LowLevelRReilOpnd translateRegister (String name) {
    if (registers.containsKey(name))
      return registers.get(name);
    throw new RuntimeException("Invalid register '" + name + "'");
  }

  @Override public LowLevelRReilOpnd temporaryRegister (final TranslationCtx env, final OperandSize size) {
    return factory.variable(size, env.getNextVariableString());
  }

  @Override public LowLevelRReilOpnd temporaryRegister (final TranslationCtx env, final Number size) {
    return factory.variable(size, env.getNextVariableString());
  }

  @Override public int defaultArchitectureSize () {
    return 8;
  }

  public LowLevelRReilOpnd getFollowingRegister (LowLevelRReilOpnd register) {
    return registers.get("r" + (registerNumbers.get(register) + 1));
  }
}
