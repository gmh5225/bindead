package rreil.disassembler.translators.x86.x64;

import java.util.HashMap;

import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class X64RegisterTranslator implements RegisterTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static final X64RegisterTranslator $ = new X64RegisterTranslator();
  public static final HashMap<String, LowLevelRReilOpnd> registers = new HashMap<String, LowLevelRReilOpnd>();
  public static final String REG_RAX = "rax";
  public static final String REG_RBX = "rbx";
  public static final String REG_RCX = "rcx";
  public static final String REG_RDX = "rdx";
  public static final String REG_RDI = "rdi";
  public static final String REG_RSI = "rsi";
  public static final String REG_RBP = "rbp";
  public static final String REG_RSP = "rsp";
  public static final String REG_R8 = "r8";
  public static final String REG_R9 = "r9";
  public static final String REG_R10 = "r10";
  public static final String REG_R11 = "r11";
  public static final String REG_R12 = "r12";
  public static final String REG_R13 = "r13";
  public static final String REG_R14 = "r14";
  public static final String REG_R15 = "r15";
  public static final String REG_RIP = "rip";
  public static final String PREFIX_CS = "cs";
  public static final String PREFIX_DS = "ds";
  public static final String PREFIX_ES = "es";
  public static final String PREFIX_FS = "fs";
  public static final String PREFIX_GS = "gs";
  public static final String PREFIX_SS = "ss";

  static {
    // 64bit registers
    registers.put(REG_RAX, factory.variable(OperandSize.QWORD, REG_RAX));
    registers.put(REG_RBX, factory.variable(OperandSize.QWORD, REG_RBX));
    registers.put(REG_RCX, factory.variable(OperandSize.QWORD, REG_RCX));
    registers.put(REG_RDX, factory.variable(OperandSize.QWORD, REG_RDX));
    registers.put(REG_RDI, factory.variable(OperandSize.QWORD, REG_RDI));
    registers.put(REG_RSI, factory.variable(OperandSize.QWORD, REG_RSI));
    registers.put(REG_RBP, factory.variable(OperandSize.QWORD, REG_RBP));
    registers.put(REG_RSP, factory.variable(OperandSize.QWORD, REG_RSP));
    registers.put(REG_R8, factory.variable(OperandSize.QWORD, REG_R8));
    registers.put(REG_R9, factory.variable(OperandSize.QWORD, REG_R9));
    registers.put(REG_R10, factory.variable(OperandSize.QWORD, REG_R10));
    registers.put(REG_R11, factory.variable(OperandSize.QWORD, REG_R11));
    registers.put(REG_R12, factory.variable(OperandSize.QWORD, REG_R12));
    registers.put(REG_R13, factory.variable(OperandSize.QWORD, REG_R13));
    registers.put(REG_R14, factory.variable(OperandSize.QWORD, REG_R15));
    registers.put(REG_R15, factory.variable(OperandSize.QWORD, REG_R15));

    // Segment registers
    registers.put(PREFIX_CS, factory.variable(OperandSize.QWORD, PREFIX_CS));
    registers.put(PREFIX_DS, factory.variable(OperandSize.QWORD, PREFIX_DS));
    registers.put(PREFIX_ES, factory.variable(OperandSize.QWORD, PREFIX_ES));
    registers.put(PREFIX_FS, factory.variable(OperandSize.QWORD, PREFIX_FS));
    registers.put(PREFIX_GS, factory.variable(OperandSize.QWORD, PREFIX_GS));
    registers.put(PREFIX_SS, factory.variable(OperandSize.QWORD, PREFIX_SS));

    // Program counter
    registers.put(REG_RIP, factory.variable(OperandSize.QWORD, REG_RIP));

    // 32bit registers
    registers.put("eax", factory.variable(OperandSize.DWORD, REG_RAX));
    registers.put("ebx", factory.variable(OperandSize.DWORD, REG_RBX));
    registers.put("ecx", factory.variable(OperandSize.DWORD, REG_RCX));
    registers.put("edx", factory.variable(OperandSize.DWORD, REG_RDX));
    registers.put("edi", factory.variable(OperandSize.DWORD, REG_RDI));
    registers.put("esi", factory.variable(OperandSize.DWORD, REG_RSI));
    registers.put("ebp", factory.variable(OperandSize.DWORD, REG_RBP));
    registers.put("esp", factory.variable(OperandSize.DWORD, REG_RSP));
    registers.put("r8d", factory.variable(OperandSize.DWORD, REG_R8));
    registers.put("r9d", factory.variable(OperandSize.DWORD, REG_R9));
    registers.put("r10d", factory.variable(OperandSize.DWORD, REG_R10));
    registers.put("r11d", factory.variable(OperandSize.DWORD, REG_R11));
    registers.put("r12d", factory.variable(OperandSize.DWORD, REG_R12));
    registers.put("r13d", factory.variable(OperandSize.DWORD, REG_R13));
    registers.put("r14d", factory.variable(OperandSize.DWORD, REG_R14));
    registers.put("r15d", factory.variable(OperandSize.DWORD, REG_R15));

    // 16bit registers
    registers.put("ax", factory.variable(OperandSize.WORD, REG_RAX));
    registers.put("bx", factory.variable(OperandSize.WORD, REG_RBX));
    registers.put("cx", factory.variable(OperandSize.WORD, REG_RCX));
    registers.put("dx", factory.variable(OperandSize.WORD, REG_RDX));
    registers.put("di", factory.variable(OperandSize.WORD, REG_RDI));
    registers.put("si", factory.variable(OperandSize.WORD, REG_RSI));
    registers.put("bp", factory.variable(OperandSize.WORD, REG_RBP));
    registers.put("sp", factory.variable(OperandSize.WORD, REG_RSP));
    registers.put("r10w", factory.variable(OperandSize.WORD, REG_R10));
    registers.put("r11w", factory.variable(OperandSize.WORD, REG_R11));
    registers.put("r12w", factory.variable(OperandSize.WORD, REG_R12));
    registers.put("r13w", factory.variable(OperandSize.WORD, REG_R13));
    registers.put("r14w", factory.variable(OperandSize.WORD, REG_R14));
    registers.put("r15w", factory.variable(OperandSize.WORD, REG_R15));

    // 8bit registers
    registers.put("al", factory.variable(OperandSize.BYTE, REG_RAX));
    registers.put("bl", factory.variable(OperandSize.BYTE, REG_RBX));
    registers.put("cl", factory.variable(OperandSize.BYTE, REG_RCX));
    registers.put("dl", factory.variable(OperandSize.BYTE, REG_RDX));
    registers.put("dil", factory.variable(OperandSize.BYTE, REG_RDI));
    registers.put("sil", factory.variable(OperandSize.BYTE, REG_RSI));
    registers.put("bpl", factory.variable(OperandSize.BYTE, REG_RBP));
    registers.put("spl", factory.variable(OperandSize.BYTE, REG_RSP));
    registers.put("r10b", factory.variable(OperandSize.BYTE, REG_R10));
    registers.put("r11b", factory.variable(OperandSize.BYTE, REG_R11));
    registers.put("r12b", factory.variable(OperandSize.BYTE, REG_R12));
    registers.put("r13b", factory.variable(OperandSize.BYTE, REG_R13));
    registers.put("r14b", factory.variable(OperandSize.BYTE, REG_R14));
    registers.put("r15b", factory.variable(OperandSize.BYTE, REG_R15));

    // 8bit registers with offset
    registers.put("ah", factory.variable(OperandSize.BYTE, REG_RAX).withOffset(8, OperandSize.BYTE));
    registers.put("bh", factory.variable(OperandSize.BYTE, REG_RBX).withOffset(8, OperandSize.BYTE));
    registers.put("ch", factory.variable(OperandSize.BYTE, REG_RCX).withOffset(8, OperandSize.BYTE));
    registers.put("dh", factory.variable(OperandSize.BYTE, REG_RDX).withOffset(8, OperandSize.BYTE));
  }

  private X64RegisterTranslator () {
  }

  @Override public LowLevelRReilOpnd translateRegister (String name) {
    if (registers.containsKey(name))
      return registers.get(name);
    throw new IllegalArgumentException("Invalid register '" + name + "'");
  }

  @Override public LowLevelRReilOpnd temporaryRegister (final TranslationCtx env,
      final OperandSize size) {
    return factory.variable(size, env.getNextVariableString());
  }

  @Override public LowLevelRReilOpnd temporaryRegister (final TranslationCtx env, final Number size) {
    return factory.variable(size, env.getNextVariableString());
  }

  @Override public int defaultArchitectureSize () {
    return 64;
  }
}
