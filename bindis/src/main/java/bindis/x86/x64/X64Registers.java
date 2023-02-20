package bindis.x86.x64;

import bindis.x86.common.X86RegOpnd;

public class X64Registers {
  public static final X86RegOpnd RAX = new X86RegOpnd("rax", 64);
  public static final X86RegOpnd RCX = new X86RegOpnd("rcx", 64);
  public static final X86RegOpnd RDX = new X86RegOpnd("rdx", 64);
  public static final X86RegOpnd RBX = new X86RegOpnd("rbx", 64);
  public static final X86RegOpnd RSP = new X86RegOpnd("rsp", 64);
  public static final X86RegOpnd RBP = new X86RegOpnd("rbp", 64);
  public static final X86RegOpnd RSI = new X86RegOpnd("rsi", 64);
  public static final X86RegOpnd RDI = new X86RegOpnd("rdi", 64);
  public static final X86RegOpnd R8 = new X86RegOpnd("r8", 64);
  public static final X86RegOpnd R9 = new X86RegOpnd("r9", 64);
  public static final X86RegOpnd R10 = new X86RegOpnd("r10", 64);
  public static final X86RegOpnd R11 = new X86RegOpnd("r11", 64);
  public static final X86RegOpnd R12 = new X86RegOpnd("r12", 64);
  public static final X86RegOpnd R13 = new X86RegOpnd("r13", 64);
  public static final X86RegOpnd R14 = new X86RegOpnd("r14", 64);
  public static final X86RegOpnd R15 = new X86RegOpnd("r15", 64);
  public static final X86RegOpnd EAX = new X86RegOpnd("eax", 32);
  public static final X86RegOpnd ECX = new X86RegOpnd("ecx", 32);
  public static final X86RegOpnd EDX = new X86RegOpnd("edx", 32);
  public static final X86RegOpnd EBX = new X86RegOpnd("ebx", 32);
  public static final X86RegOpnd ESP = new X86RegOpnd("esp", 32);
  public static final X86RegOpnd EBP = new X86RegOpnd("ebp", 32);
  public static final X86RegOpnd ESI = new X86RegOpnd("esi", 32);
  public static final X86RegOpnd EDI = new X86RegOpnd("edi", 32);
  public static final X86RegOpnd R8D = new X86RegOpnd("r8d", 32);
  public static final X86RegOpnd R9D = new X86RegOpnd("r9d", 32);
  public static final X86RegOpnd R10D = new X86RegOpnd("r10d", 32);
  public static final X86RegOpnd R11D = new X86RegOpnd("r11d", 32);
  public static final X86RegOpnd R12D = new X86RegOpnd("r12d", 32);
  public static final X86RegOpnd R13D = new X86RegOpnd("r13d", 32);
  public static final X86RegOpnd R14D = new X86RegOpnd("r14d", 32);
  public static final X86RegOpnd R15D = new X86RegOpnd("r15d", 32);
  public static final X86RegOpnd AX = new X86RegOpnd("ax", 16);
  public static final X86RegOpnd CX = new X86RegOpnd("cx", 16);
  public static final X86RegOpnd DX = new X86RegOpnd("dx", 16);
  public static final X86RegOpnd BX = new X86RegOpnd("bx", 16);
  public static final X86RegOpnd SP = new X86RegOpnd("sp", 16);
  public static final X86RegOpnd BP = new X86RegOpnd("bp", 16);
  public static final X86RegOpnd SI = new X86RegOpnd("si", 16);
  public static final X86RegOpnd DI = new X86RegOpnd("di", 16);
  public static final X86RegOpnd R8W = new X86RegOpnd("r8w", 16);
  public static final X86RegOpnd R9W = new X86RegOpnd("r9w", 16);
  public static final X86RegOpnd R10W = new X86RegOpnd("r10w", 16);
  public static final X86RegOpnd R11W = new X86RegOpnd("r11w", 16);
  public static final X86RegOpnd R12W = new X86RegOpnd("r12w", 16);
  public static final X86RegOpnd R13W = new X86RegOpnd("r13w", 16);
  public static final X86RegOpnd R14W = new X86RegOpnd("r14w", 16);
  public static final X86RegOpnd R15W = new X86RegOpnd("r15w", 16);
  public static final X86RegOpnd AL = new X86RegOpnd("al", 8);
  public static final X86RegOpnd CL = new X86RegOpnd("cl", 8);
  public static final X86RegOpnd DL = new X86RegOpnd("dl", 8);
  public static final X86RegOpnd BL = new X86RegOpnd("bl", 8);
  public static final X86RegOpnd AH = new X86RegOpnd("ah", 8);
  public static final X86RegOpnd CH = new X86RegOpnd("ch", 8);
  public static final X86RegOpnd DH = new X86RegOpnd("dh", 8);
  public static final X86RegOpnd BH = new X86RegOpnd("bh", 8);
  public static final X86RegOpnd R8L = new X86RegOpnd("r8b", 8);
  public static final X86RegOpnd R9L = new X86RegOpnd("r9b", 8);
  public static final X86RegOpnd R10L = new X86RegOpnd("r10b", 8);
  public static final X86RegOpnd R11L = new X86RegOpnd("r11b", 8);
  public static final X86RegOpnd R12L = new X86RegOpnd("r12b", 8);
  public static final X86RegOpnd R13L = new X86RegOpnd("r13b", 8);
  public static final X86RegOpnd R14L = new X86RegOpnd("r14b", 8);
  public static final X86RegOpnd R15L = new X86RegOpnd("r15b", 8);
  private static X86RegOpnd registers8[] =
      new X86RegOpnd[]{
    AL, CL, DL, BL, AH, CH, DH, BH, R8L, R9L, R10L, R11L, R12L, R13L, R14L, R15L
  };
  private static X86RegOpnd registers16[] =
      new X86RegOpnd[]{
    AX, CX, DX, BX, SP, BP, SI, DI, R8W, R9W, R10W, R11W, R12W, R13W, R14W, R15W
  };
  private static X86RegOpnd registers32[] =
      new X86RegOpnd[]{
    EAX, ECX, EDX, EBX, ESP, EBP, ESI, EDI, R8D, R9D, R10D, R11D, R12D, R13D, R14D, R15D
  };
  private static X86RegOpnd registers64[] =
      new X86RegOpnd[]{
    RAX, RCX, RDX, RBX, RSP, RBP, RSI, RDI, R8, R9, R10, R11, R12, R13, R14, R15
  };

  public static X86RegOpnd getRegister8 (int n) {
    return registers8[n];
  }

  public static X86RegOpnd getRegister16 (int n) {
    return registers16[n];
  }

  public static X86RegOpnd getRegister32 (int n) {
    return registers32[n];
  }

  public static X86RegOpnd getRegister64 (int n) {
    return registers64[n];
  }
}