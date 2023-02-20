package rreil.disassembler.translators.x86.x32;

import java.util.HashMap;

import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class X32RegisterTranslator implements RegisterTranslator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static final X32RegisterTranslator $ = new X32RegisterTranslator();
  public static final HashMap<String, LowLevelRReilOpnd> registers = new HashMap<String, LowLevelRReilOpnd>();
  public static final String REG_EAX = "eax";
  public static final String REG_EBX = "ebx";
  public static final String REG_ECX = "ecx";
  public static final String REG_EDX = "edx";
  public static final String REG_EDI = "edi";
  public static final String REG_ESI = "esi";
  public static final String REG_EBP = "ebp";
  public static final String REG_ESP = "esp";
  public static final String REG_EIP = "eip";
  public static final String PREFIX_CS = "cs";
  public static final String PREFIX_DS = "ds";
  public static final String PREFIX_ES = "es";
  public static final String PREFIX_FS = "fs";
  public static final String PREFIX_GS = "gs";
  public static final String PREFIX_SS = "ss";

  static {
    // 32bit registers
    registers.put(REG_EAX, factory.variable(OperandSize.DWORD, REG_EAX));
    registers.put(REG_EBX, factory.variable(OperandSize.DWORD, REG_EBX));
    registers.put(REG_ECX, factory.variable(OperandSize.DWORD, REG_ECX));
    registers.put(REG_EDX, factory.variable(OperandSize.DWORD, REG_EDX));
    registers.put(REG_EDI, factory.variable(OperandSize.DWORD, REG_EDI));
    registers.put(REG_ESI, factory.variable(OperandSize.DWORD, REG_ESI));
    registers.put(REG_EBP, factory.variable(OperandSize.DWORD, REG_EBP));
    registers.put(REG_ESP, factory.variable(OperandSize.DWORD, REG_ESP));

    // Segment registers
    registers.put(PREFIX_CS, factory.variable(OperandSize.DWORD, PREFIX_CS));
    registers.put(PREFIX_DS, factory.variable(OperandSize.DWORD, PREFIX_DS));
    registers.put(PREFIX_ES, factory.variable(OperandSize.DWORD, PREFIX_ES));
    registers.put(PREFIX_FS, factory.variable(OperandSize.DWORD, PREFIX_FS));
    registers.put(PREFIX_GS, factory.variable(OperandSize.DWORD, PREFIX_GS));
    registers.put(PREFIX_SS, factory.variable(OperandSize.DWORD, PREFIX_SS));

    // Program counter
    registers.put(REG_EIP, factory.variable(OperandSize.DWORD, REG_EIP));

    // 16bit registers
    registers.put("ax", factory.variable(OperandSize.WORD, REG_EAX));
    registers.put("bx", factory.variable(OperandSize.WORD, REG_EBX));
    registers.put("cx", factory.variable(OperandSize.WORD, REG_ECX));
    registers.put("dx", factory.variable(OperandSize.WORD, REG_EDX));
    registers.put("di", factory.variable(OperandSize.WORD, REG_EDI));
    registers.put("si", factory.variable(OperandSize.WORD, REG_ESI));
    registers.put("bp", factory.variable(OperandSize.WORD, REG_EBP));
    registers.put("sp", factory.variable(OperandSize.WORD, REG_ESP));

    // 8bit registers
    registers.put("al", factory.variable(OperandSize.BYTE, REG_EAX));
    registers.put("bl", factory.variable(OperandSize.BYTE, REG_EBX));
    registers.put("cl", factory.variable(OperandSize.BYTE, REG_ECX));
    registers.put("dl", factory.variable(OperandSize.BYTE, REG_EDX));
    registers.put("dil", factory.variable(OperandSize.BYTE, REG_EDI));
    registers.put("sil", factory.variable(OperandSize.BYTE, REG_ESI));
    registers.put("bpl", factory.variable(OperandSize.BYTE, REG_EBP));
    registers.put("spl", factory.variable(OperandSize.BYTE, REG_ESP));

    // 8bit registers with offset
    registers.put("ah", factory.variable(OperandSize.BYTE, REG_EAX).withOffset(8, OperandSize.BYTE));
    registers.put("bh", factory.variable(OperandSize.BYTE, REG_EBX).withOffset(8, OperandSize.BYTE));
    registers.put("ch", factory.variable(OperandSize.BYTE, REG_ECX).withOffset(8, OperandSize.BYTE));
    registers.put("dh", factory.variable(OperandSize.BYTE, REG_EDX).withOffset(8, OperandSize.BYTE));
  }

  private X32RegisterTranslator () {
  }

  @Override public LowLevelRReilOpnd translateRegister (String name) {
    if (registers.containsKey(name))
      return registers.get(name);
    throw new IllegalArgumentException("Invalid register '" + name + "'");
  }

  @Override public LowLevelRReilOpnd temporaryRegister (final TranslationCtx env, final OperandSize size) {
    return factory.variable(size, env.getNextVariableString());
  }

  @Override public LowLevelRReilOpnd temporaryRegister (final TranslationCtx env, final Number size) {
    return factory.variable(size, env.getNextVariableString());
  }

  @Override public int defaultArchitectureSize () {
    return 32;
  }
}
