package bindis.x86.common;

import static bindis.x86.common.X86Consts.AH;
import static bindis.x86.common.X86Consts.AL;
import static bindis.x86.common.X86Consts.BH;
import static bindis.x86.common.X86Consts.BL;
import static bindis.x86.common.X86Consts.CH;
import static bindis.x86.common.X86Consts.CL;
import static bindis.x86.common.X86Consts.CS;
import static bindis.x86.common.X86Consts.DH;
import static bindis.x86.common.X86Consts.DL;
import static bindis.x86.common.X86Consts.DS;
import static bindis.x86.common.X86Consts.DX;
import static bindis.x86.common.X86Consts.EAX;
import static bindis.x86.common.X86Consts.EBP;
import static bindis.x86.common.X86Consts.EBX;
import static bindis.x86.common.X86Consts.ECX;
import static bindis.x86.common.X86Consts.EDI;
import static bindis.x86.common.X86Consts.EDX;
import static bindis.x86.common.X86Consts.ES;
import static bindis.x86.common.X86Consts.ESI;
import static bindis.x86.common.X86Consts.ESP;
import static bindis.x86.common.X86Consts.FS;
import static bindis.x86.common.X86Consts.GS;
import static bindis.x86.common.X86Consts.RAX;
import static bindis.x86.common.X86Consts.RBP;
import static bindis.x86.common.X86Consts.RBX;
import static bindis.x86.common.X86Consts.RCX;
import static bindis.x86.common.X86Consts.RDI;
import static bindis.x86.common.X86Consts.RDX;
import static bindis.x86.common.X86Consts.RSI;
import static bindis.x86.common.X86Consts.RSP;
import static bindis.x86.common.X86Consts.SS;
import static bindis.x86.common.X86OperandDecoders.a_mode;
import static bindis.x86.common.X86OperandDecoders.b_mode;
import static bindis.x86.common.X86OperandDecoders.d_mode;
import static bindis.x86.common.X86OperandDecoders.dq_mode;
import static bindis.x86.common.X86OperandDecoders.p_mode;
import static bindis.x86.common.X86OperandDecoders.pd_mode;
import static bindis.x86.common.X86OperandDecoders.pi_mode;
import static bindis.x86.common.X86OperandDecoders.ps_mode;
import static bindis.x86.common.X86OperandDecoders.q_mode;
import static bindis.x86.common.X86OperandDecoders.sd_mode;
import static bindis.x86.common.X86OperandDecoders.ss_mode;
import static bindis.x86.common.X86OperandDecoders.v_mode;
import static bindis.x86.common.X86OperandDecoders.w_mode;

import bindis.DecodeException;
import bindis.DecodeStream;
import bindis.NativeInstruction;
import bindis.x86.common.X86OperandDecoders.AddrMode;

/**
 * X86 decode tables tailored to the 64bit decode mode.<br>
 * Copied and improved from the OpenJDK 6 Disassembler as part of the HotSpot Debugger (HSDB):
 * openjdk/hotspot/agent/src/share/classes/sun/jvm/hotspot/asm/x86/X86Disassembler.java
 * Note that the OpenJDK 7 version is the same and the code has been removed as of OpenJDK 8.
 *
 * @author mb0
 */
public class X86DecodeTable {
  protected final X86InstructionFactory factory;
  protected final X86OperandDecoders Mode;
  public final X86InstructionDecoder[] oneByteOpcodeTable;
  public final X86InstructionDecoder[] twoByteOpcodeTable;
  public final X86InstructionDecoder[][] floatOpcodeGroupTable;
  public final X86InstructionDecoder[][] floatOpcodeTableOne;
  public final X86InstructionDecoder[][] floatOpcodeTableTwo;
  public final X86InstructionDecoder[][] twoByteOpcodeGroupTable;
  public final X86InstructionDecoder[] twoByteOpcodePrefix66Table;
  public final X86InstructionDecoder[] twoByteOpcodePrefixF2Table;
  public final X86InstructionDecoder[] twoByteOpcodePrefixF3Table;
  public final X86InstructionDecoder[] threeByteOpcode38Table;
  public final X86InstructionDecoder[] threeByteOpcode3ATable;
  public final X86InstructionDecoder[] threeByteOpcode38Prefix66Table;
  public final X86InstructionDecoder[] threeByteOpcode3APrefix66Table;
  public final X86InstructionDecoder[] threeByteOpcode38PrefixF2Table;
  public final X86InstructionDecoder[] threeByteOpcode38PrefixF3Table;
  public final X86InstructionDecoder[][] opcodeGroupTable0F01;
  private final X86InstructionDecoder[][] opcodeGroupTable0FAE;

  public X86DecodeTable (X86OperandDecoders decoders, X86InstructionFactory factory) {
    this.Mode = decoders;
    this.opcodeGroupTable0F01 = create0F01OpcodeGroupTable();
    this.opcodeGroupTable0FAE = create0FAEOpcodeGroupTable();
    this.threeByteOpcode38Table = createThreeByteOpcodeTable();
    this.threeByteOpcode38Prefix66Table = createThreeByteOpcodePrefix66Table();
    this.threeByteOpcode38PrefixF2Table = createThreeByteOpcodePrefixF2Table();
    this.threeByteOpcode38PrefixF3Table = createThreeByteOpcodePrefixF3Table();
    this.threeByteOpcode3ATable = createThreeByteOpcode3ATable();
    this.threeByteOpcode3APrefix66Table = createThreeByteOpcode3APrefix66Table();
    this.oneByteOpcodeTable = createOneByteOpcodeTable();
    this.twoByteOpcodeGroupTable = createTwoByteOpcodeGroupTable();
    this.twoByteOpcodePrefix66Table = createTwoByteOpcodePrefix66Table();
    this.twoByteOpcodePrefixF2Table = createTwoByteOpcodePrefixF2Table();
    this.twoByteOpcodePrefixF3Table = createTwoByteOpcodePrefixF3Table();
    this.twoByteOpcodeTable = createTwoByteOpcodeTable();
    this.floatOpcodeGroupTable = createFloatOpcodeGroupTable();
    this.floatOpcodeTableOne = createFloatOpcodeTableOne();
    this.floatOpcodeTableTwo = createFloatOpcodeTableTwo();
    this.factory = factory;
  }

  public X86InstructionDecoder decode (String name) {
    return new X86InstructionDecoder(name) {
      @Override protected NativeInstruction decodeInstruction (final X86DecodeCtx ctx) {
        return factory.make(name, ctx);
      }
    };
  }

  public X86InstructionDecoder decode (String name, AddrMode addrMode1, int operandType1) {
    return new X86InstructionDecoder(name, addrMode1, operandType1) {
      @Override public NativeInstruction decodeInstruction (final X86DecodeCtx ctx) {
        X86Operand op1 = decodeOpnd1(ctx);
        return factory.make(name, op1, ctx);
      }
    };
  }

  public X86InstructionDecoder decode (String name, AddrMode addrMode1, int operandType1, AddrMode addrMode2, int operandType2) {
    return new X86InstructionDecoder(name, addrMode1, operandType1, addrMode2, operandType2) {
      @Override protected NativeInstruction decodeInstruction (final X86DecodeCtx ctx) {
        X86Operand op1 = decodeOpnd1(ctx);
        X86Operand op2 = decodeOpnd2(ctx);
        return factory.make(name, op1, op2, ctx);
      }
    };
  }

  public X86InstructionDecoder decode (
      String name, AddrMode addrMode1, int operandType1, AddrMode addrMode2, int operandType2, AddrMode addrMode3, int operandType3) {
    return new X86InstructionDecoder(name, addrMode1, operandType1, addrMode2, operandType2, addrMode3, operandType3) {
      @Override protected NativeInstruction decodeInstruction (final X86DecodeCtx ctx) {
        X86Operand op1 = decodeOpnd1(ctx);
        X86Operand op2 = decodeOpnd2(ctx);
        X86Operand op3 = decodeOpnd3(ctx);
        return factory.make(name, op1, op2, op3, ctx);
      }
    };
  }

  public X86InstructionDecoder decodeGroup (final int number) {
    return new X86NestedInstructionDecoder(null) {
      @Override public X86InstructionDecoder lookupDecoder (final X86DecodeCtx ctx) {
        final X86ModRM ModRM = ctx.getModRM();
        return twoByteOpcodeGroupTable[number][ModRM.getRegOrOpcode()];
      }
    };
  }

  public X86InstructionDecoder decodeFloat () {
    return new X86NestedInstructionDecoder(null) {
      @Override protected X86InstructionDecoder lookupDecoder (final X86DecodeCtx ctx) {
        DecodeStream in = ctx.getDecodeStream();
        int floatOpcode = in.previous8();
        final X86ModRM ModRM = ctx.getModRM();
        return lookupDecoder(ModRM, floatOpcode);
      }

      private X86InstructionDecoder lookupDecoder (final X86ModRM ModRM, int floatOpcode) {
        int opcode = ModRM.getRegOrOpcode();
        if (ModRM.getModRM() < 0xbf)
          return floatOpcodeTableOne[floatOpcode - 0xd8][opcode];
        else
          return floatOpcodeTableTwo[floatOpcode - 0xd8][opcode];
      }
    };
  }

  public X86InstructionDecoder decodeFloatGroup (final int number) {
    return new X86NestedInstructionDecoder(null) {
      @Override protected X86InstructionDecoder lookupDecoder (X86DecodeCtx ctx) {
        final X86ModRM ModRM = ctx.getModRM();
        return floatOpcodeGroupTable[number][ModRM.getRm()];
      }
    };
  }

  public X86InstructionDecoder decode (final X86InstructionDecoder[] decoders) {
    return new X86NestedInstructionDecoder(null) {
      @Override protected X86InstructionDecoder lookupDecoder (X86DecodeCtx ctx) {
        final int opcode = ctx.getDecodeStream().read8();
        if (opcode >= 0 && opcode < decoders.length)
          return decoders[opcode];
        throw DecodeException.unknownOpcode(ctx);
      }
    };
  }

  public X86InstructionDecoder decodeOrGroup (final X86InstructionDecoder[][] group, final X86InstructionDecoder[] groupTable) {
    return new X86NestedInstructionDecoder(null) {
      @Override public X86InstructionDecoder lookupDecoder (final X86DecodeCtx ctx) {
        X86ModRM ModRM = ctx.getModRM();
        X86InstructionDecoder decoder = null;
        int Mod67 = (ModRM.getModRM() >> 6) & 3;
        if (Mod67 == 3) {
          decoder = group[ModRM.getRegOrOpcode()][ModRM.getRm()];
          //ctx.getDecodeStream().read8();
        }
        if (decoder == null)
          decoder = groupTable[ModRM.getRegOrOpcode()];
        return decoder;
      }
    };
  }

  /**
   * Please refer to IA-32 Intel Architecture Software Developer's Manual Volume 2B
   * APPENDIX A - Table A-2. One-byte Opcode Map
   */
  private X86InstructionDecoder[] createOneByteOpcodeTable () {
    return new X86InstructionDecoder[]{
          /* 00 */
          decode("add", Mode.ADDR_E, b_mode, Mode.ADDR_G, b_mode),
          decode("add", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("add", Mode.ADDR_G, b_mode, Mode.ADDR_E, b_mode),
          decode("add", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("add", Mode.ADDR_REG, AL, Mode.ADDR_I, b_mode),
          decode("add", Mode.ADDR_REG, EAX, Mode.ADDR_I, v_mode),
          decode("push", Mode.ADDR_REG, ES),
          decode("pop", Mode.ADDR_REG, ES),
          /* 08 */
          decode("or", Mode.ADDR_E, b_mode, Mode.ADDR_G, b_mode),
          decode("or", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("or", Mode.ADDR_G, b_mode, Mode.ADDR_E, b_mode),
          decode("or", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("or", Mode.ADDR_REG, AL, Mode.ADDR_I, b_mode),
          decode("or", Mode.ADDR_REG, EAX, Mode.ADDR_I, v_mode),
          decode("push", Mode.ADDR_REG, CS),
          null, /* 0x0f extended opcode escape */
          /* 10 */
          decode("adc", Mode.ADDR_E, b_mode, Mode.ADDR_G, b_mode),
          decode("adc", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("adc", Mode.ADDR_G, b_mode, Mode.ADDR_E, b_mode),
          decode("adc", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("adc", Mode.ADDR_REG, AL, Mode.ADDR_I, b_mode),
          decode("adc", Mode.ADDR_REG, EAX, Mode.ADDR_I, v_mode),
          decode("push", Mode.ADDR_REG, SS),
          decode("pop", Mode.ADDR_REG, SS),
          /* 18 */
          decode("sbb", Mode.ADDR_E, b_mode, Mode.ADDR_G, b_mode),
          decode("sbb", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("sbb", Mode.ADDR_G, b_mode, Mode.ADDR_E, b_mode),
          decode("sbb", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("sbb", Mode.ADDR_REG, AL, Mode.ADDR_I, b_mode),
          decode("sbb", Mode.ADDR_REG, EAX, Mode.ADDR_I, v_mode),
          decode("push", Mode.ADDR_REG, DS),
          decode("pop", Mode.ADDR_REG, DS),
          /* 20 */
          decode("and", Mode.ADDR_E, b_mode, Mode.ADDR_G, b_mode),
          decode("and", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("and", Mode.ADDR_G, b_mode, Mode.ADDR_E, b_mode),
          decode("and", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("and", Mode.ADDR_REG, AL, Mode.ADDR_I, b_mode),
          decode("and", Mode.ADDR_REG, EAX, Mode.ADDR_I, v_mode),
          null, /* SEG es prefix */
          decode("daa"),
          /* 28 */
          decode("sub", Mode.ADDR_E, b_mode, Mode.ADDR_G, b_mode),
          decode("sub", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("sub", Mode.ADDR_G, b_mode, Mode.ADDR_E, b_mode),
          decode("sub", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("sub", Mode.ADDR_REG, AL, Mode.ADDR_I, b_mode),
          decode("sub", Mode.ADDR_REG, EAX, Mode.ADDR_I, v_mode),
          null, /* SEG CS prefix */
          decode("das"),
          /* 30 */
          decode("xor", Mode.ADDR_E, b_mode, Mode.ADDR_G, b_mode),
          decode("xor", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("xor", Mode.ADDR_G, b_mode, Mode.ADDR_E, b_mode),
          decode("xor", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("xor", Mode.ADDR_REG, AL, Mode.ADDR_I, b_mode),
          decode("xor", Mode.ADDR_REG, EAX, Mode.ADDR_I, v_mode),
          null, /* SEG SS prefix */
          decode("aaa"),
          /* 38 */
          decode("cmp", Mode.ADDR_E, b_mode, Mode.ADDR_G, b_mode),
          decode("cmp", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("cmp", Mode.ADDR_G, b_mode, Mode.ADDR_E, b_mode),
          decode("cmp", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("cmp", Mode.ADDR_REG, AL, Mode.ADDR_I, b_mode),
          decode("cmp", Mode.ADDR_REG, EAX, Mode.ADDR_I, v_mode),
          null, /* SEG DS prefix */
          decode("aas"),
          /* 40 */
          decode("inc", Mode.ADDR_REG, RAX),
          decode("inc", Mode.ADDR_REG, RCX),
          decode("inc", Mode.ADDR_REG, RDX),
          decode("inc", Mode.ADDR_REG, RBX),
          decode("inc", Mode.ADDR_REG, RSP),
          decode("inc", Mode.ADDR_REG, RBP),
          decode("inc", Mode.ADDR_REG, RSI),
          decode("inc", Mode.ADDR_REG, RDI),
          /* 48 */
          decode("dec", Mode.ADDR_REG, RAX),
          decode("dec", Mode.ADDR_REG, RCX),
          decode("dec", Mode.ADDR_REG, RDX),
          decode("dec", Mode.ADDR_REG, RBX),
          decode("dec", Mode.ADDR_REG, RSP),
          decode("dec", Mode.ADDR_REG, RBP),
          decode("dec", Mode.ADDR_REG, RSI),
          decode("dec", Mode.ADDR_REG, RDI),
          /* 50 */
          decode("push", Mode.ADDR_REG, RAX),
          decode("push", Mode.ADDR_REG, RCX),
          decode("push", Mode.ADDR_REG, RDX),
          decode("push", Mode.ADDR_REG, RBX),
          decode("push", Mode.ADDR_REG, RSP),
          decode("push", Mode.ADDR_REG, RBP),
          decode("push", Mode.ADDR_REG, RSI),
          decode("push", Mode.ADDR_REG, RDI),
          /* 58 */
          decode("pop", Mode.ADDR_REG, RAX),
          decode("pop", Mode.ADDR_REG, RCX),
          decode("pop", Mode.ADDR_REG, RDX),
          decode("pop", Mode.ADDR_REG, RBX),
          decode("pop", Mode.ADDR_REG, RSP),
          decode("pop", Mode.ADDR_REG, RBP),
          decode("pop", Mode.ADDR_REG, RSI),
          decode("pop", Mode.ADDR_REG, RDI),
          /* 60 */
          decode("pushad|pusha"),
          decode("popad|popa"),
          decode("bound", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("movsxd", Mode.ADDR_G, v_mode, Mode.ADDR_R, d_mode), // x86-64 only
          null, /* seg fs */
          null, /* seg gs */
          null, /* op size prefix */
          null, /* adr size prefix */
          /* 68 */
          decode("push", Mode.ADDR_I, v_mode), /* 386 book wrong */
          decode("imul", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode, Mode.ADDR_I, v_mode),
          decode("push", Mode.ADDR_I, b_mode), /* push of byte really pushes 4 bytes */
          decode("imul", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
          decode("insb", Mode.ADDR_Y, b_mode, Mode.ADDR_REG, DX),
          decode("insS", Mode.ADDR_Y, a_mode, Mode.ADDR_REG, DX),
          decode("outsb", Mode.ADDR_REG, DX, Mode.ADDR_X, b_mode),
          decode("outsS", Mode.ADDR_REG, DX, Mode.ADDR_X, a_mode),
          /* 70 */
          decode("jo", Mode.ADDR_J, b_mode),
          decode("jno", Mode.ADDR_J, b_mode),
          decode("jb", Mode.ADDR_J, b_mode),
          decode("jnb", Mode.ADDR_J, b_mode),
          decode("jz", Mode.ADDR_J, b_mode),
          decode("jnz", Mode.ADDR_J, b_mode),
          decode("jbe", Mode.ADDR_J, b_mode),
          decode("ja", Mode.ADDR_J, b_mode),
          /* 78 */
          decode("js", Mode.ADDR_J, b_mode),
          decode("jns", Mode.ADDR_J, b_mode),
          decode("jp", Mode.ADDR_J, b_mode),
          decode("jnp", Mode.ADDR_J, b_mode),
          decode("jl", Mode.ADDR_J, b_mode),
          decode("jnl", Mode.ADDR_J, b_mode),
          decode("jle", Mode.ADDR_J, b_mode),
          decode("jnle", Mode.ADDR_J, b_mode),
          /* 80 */
          decodeGroup(0),
          decodeGroup(1),
          null,
          decodeGroup(2),
          decode("test", Mode.ADDR_E, b_mode, Mode.ADDR_G, b_mode),
          decode("test", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("xchg", Mode.ADDR_E, b_mode, Mode.ADDR_G, b_mode),
          decode("xchg", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          /* 88 */
          decode("mov", Mode.ADDR_E, b_mode, Mode.ADDR_G, b_mode),
          decode("mov", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("mov", Mode.ADDR_G, b_mode, Mode.ADDR_E, b_mode),
          decode("mov", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("mov", Mode.ADDR_E, w_mode, Mode.ADDR_S, w_mode),
          decode("lea", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("mov", Mode.ADDR_S, w_mode, Mode.ADDR_E, w_mode),
          decode("pop", Mode.ADDR_E, v_mode),
          /* 90 */
          decode("nop"),
          decode("xchg", Mode.ADDR_REG, ECX, Mode.ADDR_REG, EAX),
          decode("xchg", Mode.ADDR_REG, EDX, Mode.ADDR_REG, EAX),
          decode("xchg", Mode.ADDR_REG, EBX, Mode.ADDR_REG, EAX),
          decode("xchg", Mode.ADDR_REG, ESP, Mode.ADDR_REG, EAX),
          decode("xchg", Mode.ADDR_REG, EBP, Mode.ADDR_REG, EAX),
          decode("xchg", Mode.ADDR_REG, ESI, Mode.ADDR_REG, EAX),
          decode("xchg", Mode.ADDR_REG, EDI, Mode.ADDR_REG, EAX),
          /* 98 */
          decode("cwde|cbw|cdqe"),
          decode("cdq|cwd|cqo"),
          decode("call", Mode.ADDR_A, p_mode),
          decode("fwait"),
          decode("pushfd|pushf"),
          decode("popfd|popf"),
          decode("sahf"),
          decode("lahf"),
          /* a0 */
          decode("mov", Mode.ADDR_REG, AL, Mode.ADDR_O, b_mode),
          decode("mov", Mode.ADDR_REG, EAX, Mode.ADDR_O, v_mode),
          decode("mov", Mode.ADDR_O, b_mode, Mode.ADDR_REG, AL),
          decode("mov", Mode.ADDR_O, v_mode, Mode.ADDR_REG, EAX),
          decode("movsb", Mode.ADDR_Y, b_mode, Mode.ADDR_X, b_mode),
          decode("movsS", Mode.ADDR_Y, a_mode, Mode.ADDR_X, a_mode),
          decode("Rcmpsb", Mode.ADDR_X, b_mode, Mode.ADDR_Y, b_mode),
          decode("RcmpsS", Mode.ADDR_X, a_mode, Mode.ADDR_Y, a_mode),
          /* a8 */
          decode("test", Mode.ADDR_REG, AL, Mode.ADDR_I, b_mode),
          decode("test", Mode.ADDR_REG, EAX, Mode.ADDR_I, v_mode),
          decode("stosb", Mode.ADDR_Y, b_mode, Mode.ADDR_REG, AL),
          decode("stosS", Mode.ADDR_Y, a_mode, Mode.ADDR_REG, EAX),
          decode("lodsb", Mode.ADDR_REG, AL, Mode.ADDR_X, b_mode),
          decode("lodsS", Mode.ADDR_REG, EAX, Mode.ADDR_X, a_mode),
          decode("scasb", Mode.ADDR_REG, AL, Mode.ADDR_Y, b_mode),
          decode("scasS", Mode.ADDR_REG, EAX, Mode.ADDR_Y, a_mode),
          /* b0 */
          decode("mov", Mode.ADDR_REG, AL, Mode.ADDR_I, b_mode),
          decode("mov", Mode.ADDR_REG, CL, Mode.ADDR_I, b_mode),
          decode("mov", Mode.ADDR_REG, DL, Mode.ADDR_I, b_mode),
          decode("mov", Mode.ADDR_REG, BL, Mode.ADDR_I, b_mode),
          decode("mov", Mode.ADDR_REG, AH, Mode.ADDR_I, b_mode),
          decode("mov", Mode.ADDR_REG, CH, Mode.ADDR_I, b_mode),
          decode("mov", Mode.ADDR_REG, DH, Mode.ADDR_I, b_mode),
          decode("mov", Mode.ADDR_REG, BH, Mode.ADDR_I, b_mode),
          /* b8 */
          decode("mov", Mode.ADDR_REG, EAX, Mode.ADDR_I64, v_mode),
          decode("mov", Mode.ADDR_REG, ECX, Mode.ADDR_I64, v_mode),
          decode("mov", Mode.ADDR_REG, EDX, Mode.ADDR_I64, v_mode),
          decode("mov", Mode.ADDR_REG, EBX, Mode.ADDR_I64, v_mode),
          decode("mov", Mode.ADDR_REG, ESP, Mode.ADDR_I64, v_mode),
          decode("mov", Mode.ADDR_REG, EBP, Mode.ADDR_I64, v_mode),
          decode("mov", Mode.ADDR_REG, ESI, Mode.ADDR_I64, v_mode),
          decode("mov", Mode.ADDR_REG, EDI, Mode.ADDR_I64, v_mode),
          /* c0 */
          decodeGroup(3),
          decodeGroup(4),
          decode("ret", Mode.ADDR_I, w_mode),
          decode("ret"),
          decode("les", Mode.ADDR_G, v_mode, Mode.ADDR_E, p_mode),
          decode("lds", Mode.ADDR_G, v_mode, Mode.ADDR_E, p_mode),
          decode("mov", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),
          decode("mov", Mode.ADDR_E, v_mode, Mode.ADDR_I, v_mode),
          /* c8 */
          decode("enter", Mode.ADDR_I, w_mode, Mode.ADDR_I, b_mode),
          decode("leave"),
          decode("ret", Mode.ADDR_I, w_mode),
          decode("ret"),
          decode("int3"),
          decode("int", Mode.ADDR_I, b_mode),
          decode("into"),
          decode("iretd|iret"),
          /* d0 */
          decodeGroup(5),
          decodeGroup(6),
          decodeGroup(7),
          decodeGroup(8),
          decode("aam", Mode.ADDR_I, b_mode),
          decode("aad", Mode.ADDR_I, b_mode),
          decode("salc"),
          decode("xlat"),
          /* d8 */
          decodeFloat(),
          decodeFloat(),
          decodeFloat(),
          decodeFloat(),
          decodeFloat(),
          decodeFloat(),
          decodeFloat(),
          decodeFloat(),
          /* e0 */
          decode("loopne", Mode.ADDR_J, b_mode),
          decode("loope", Mode.ADDR_J, b_mode),
          decode("loop", Mode.ADDR_J, b_mode),
          decode("jC64cxz", Mode.ADDR_J, b_mode),
          decode("in", Mode.ADDR_REG, AL, Mode.ADDR_I, b_mode),
          decode("in", Mode.ADDR_REG, EAX, Mode.ADDR_I, b_mode),
          decode("out", Mode.ADDR_I, b_mode, Mode.ADDR_REG, AL),
          decode("out", Mode.ADDR_I, b_mode, Mode.ADDR_REG, EAX),
          /* e8 */
          decode("call", Mode.ADDR_J, v_mode),
          decode("jmp", Mode.ADDR_J, v_mode),
          decode("jmp", Mode.ADDR_A, p_mode),
          decode("jmp", Mode.ADDR_J, b_mode),
          decode("in", Mode.ADDR_REG, AL, Mode.ADDR_REG, DX),
          decode("in", Mode.ADDR_REG, EAX, Mode.ADDR_REG, DX),
          decode("out", Mode.ADDR_REG, DX, Mode.ADDR_REG, AL),
          decode("out", Mode.ADDR_REG, DX, Mode.ADDR_REG, EAX),
          /* f0 */
          decode("lock"), /* lock prefix */
          decode("int1"),
          decode("repne"), /* repne */
          decode("rep"), /* repz */
          decode("hlt"),
          decode("cmc"),
          decodeGroup(9),
          decodeGroup(10),
          /* f8 */
          decode("clc"),
          decode("stc"),
          decode("cli"),
          decode("sti"),
          decode("cld"),
          decode("std"),
          decodeGroup(11),
          decodeGroup(12)
        };
  }

  /**
   * Please refer to IA-32 Intel Architecture Software Developer's Manual Volume 2
   * APPENDIX A - Table A-3. Two-byte Opcode Map
   */
  private X86InstructionDecoder[] createTwoByteOpcodeTable () {
    return new X86InstructionDecoder[]{
          /* 00 */
          decodeGroup(13),
          decodeOrGroup(opcodeGroupTable0F01, twoByteOpcodeGroupTable[14]), //decodeGroup(14),
          decode("lar", Mode.ADDR_G, v_mode, Mode.ADDR_E, w_mode),
          decode("lsl", Mode.ADDR_G, v_mode, Mode.ADDR_E, w_mode),
          null,
          decode("syscall"),
          decode("clts"),
          decode("sysret"),
          /* 08 */
          decode("invd"),
          decode("wbinvd"),
          null,
          decode("ud2"),
          null,
          decode("prefetch_exclusive", Mode.ADDR_M, b_mode),
          decode("femms"),
          null,
          /* 10 */ //SSE
          decode("movups", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          decode("movups", Mode.ADDR_W, ps_mode, Mode.ADDR_V, ps_mode),
          decode("movlps", Mode.ADDR_W, q_mode, Mode.ADDR_V, q_mode),
          decode("movlps", Mode.ADDR_V, q_mode, Mode.ADDR_W, q_mode),
          decode("unpcklps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, q_mode),
          decode("unpckhps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, q_mode),
          decode("movhps", Mode.ADDR_V, q_mode, Mode.ADDR_W, q_mode),
          decode("movhps", Mode.ADDR_W, q_mode, Mode.ADDR_V, q_mode),
          /* 18 */
          decodeGroup(21),
          null,
          null,
          null,
          null,
          null,
          null,
          decode("nop", Mode.ADDR_E, v_mode),
          /* 20 */
          /* these are all backward in appendix A of the intel book */
          decode("mov", Mode.ADDR_R, d_mode, Mode.ADDR_C, d_mode),
          decode("mov", Mode.ADDR_R, d_mode, Mode.ADDR_D, d_mode),
          decode("mov", Mode.ADDR_C, d_mode, Mode.ADDR_R, d_mode),
          decode("mov", Mode.ADDR_D, d_mode, Mode.ADDR_R, d_mode),
          decode("mov", Mode.ADDR_R, d_mode, Mode.ADDR_T, d_mode),
          null,
          decode("mov", Mode.ADDR_T, d_mode, Mode.ADDR_R, d_mode),
          null,
          /* 28 */
          decode("movaps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          decode("movaps", Mode.ADDR_W, ps_mode, Mode.ADDR_V, ps_mode),
          decode("cvtpi2ps", Mode.ADDR_V, ps_mode, Mode.ADDR_Q, q_mode),
          decode("movntps", Mode.ADDR_W, ps_mode, Mode.ADDR_V, ps_mode),
          decode("cvttps2pi", Mode.ADDR_Q, q_mode, Mode.ADDR_W, ps_mode),
          decode("cvtps2pi", Mode.ADDR_Q, q_mode, Mode.ADDR_W, ps_mode),
          decode("ucomiss", Mode.ADDR_V, ss_mode, Mode.ADDR_W, ss_mode),
          decode("comiss", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          /* 30 */
          decode("wrmsr"),
          decode("rdtsc"),
          decode("rdmsr"),
          decode("rdpmc"),
          decode("sysenter"),
          decode("sysexit"),
          null,
          decode("getsec"),
          /* 38 */
          decode(threeByteOpcode38Table),
          null,
          decode(threeByteOpcode3ATable),
          null,
          decode("movnti", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          null,
          null,
          null,
          /* 40 */
          decode("cmovo", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("cmovno", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("cmovb", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("cmovnb", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("cmovz", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("cmovnz", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("cmovbe", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("cmovnbe", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          /* 48 */
          decode("cmovs", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("cmovns", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("cmovp", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("cmovnp", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("cmovl", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("cmovnl", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("cmovle", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("cmovnle", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          /* 50 */
          decode("movmskps", Mode.ADDR_E, d_mode, Mode.ADDR_V, ps_mode),
          decode("sqrtps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          decode("rsqrtps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          decode("rcpps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          decode("andps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          decode("andnps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          decode("orps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          decode("xorps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          /* 58 */
          decode("addps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          decode("mulps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          decode("cvtps2pd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, ps_mode),
          decode("cvtdq2ps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, dq_mode),
          decode("subps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          decode("minps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          decode("divps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          decode("maxps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          /* 60 */
          decode("punpcklbw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, d_mode),
          decode("punpcklwd", Mode.ADDR_P, q_mode, Mode.ADDR_Q, d_mode),
          decode("punpckldq", Mode.ADDR_P, q_mode, Mode.ADDR_Q, d_mode),
          decode("packsswb", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pcmpgtb", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pcmpgtw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pcmpgtd", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("packuswb", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          /* 68 */
          decode("punpckhbw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, d_mode),
          decode("punpckhwd", Mode.ADDR_P, q_mode, Mode.ADDR_Q, d_mode),
          decode("punpckhdq", Mode.ADDR_P, q_mode, Mode.ADDR_Q, d_mode),
          decode("packssdw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, d_mode),
          null,
          null,
          decode("movd", Mode.ADDR_P, d_mode, Mode.ADDR_E, d_mode),
          decode("movq", Mode.ADDR_P, q_mode, Mode.ADDR_E, q_mode),
          /* 70 */
          decode("pshufw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode, Mode.ADDR_I, b_mode),
          decodeGroup(17),
          decodeGroup(18),
          decodeGroup(19),
          decode("pcmpeqb", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pcmpeqw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pcmpeqd", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("emms"),
          /* 78 */
          decode("vmread", Mode.ADDR_M, d_mode, Mode.ADDR_G, d_mode),
          decode("vmwrite", Mode.ADDR_G, d_mode, Mode.ADDR_M, d_mode),
          null,
          null,
          null,
          null,
          decode("movd", Mode.ADDR_E, d_mode, Mode.ADDR_P, d_mode),
          decode("movq", Mode.ADDR_Q, q_mode, Mode.ADDR_P, q_mode),
          /* 80 */
          decode("jo", Mode.ADDR_J, v_mode),
          decode("jno", Mode.ADDR_J, v_mode),
          decode("jb", Mode.ADDR_J, v_mode),
          decode("jnb", Mode.ADDR_J, v_mode),
          decode("jz", Mode.ADDR_J, v_mode),
          decode("jnz", Mode.ADDR_J, v_mode),
          decode("jbe", Mode.ADDR_J, v_mode),
          decode("ja", Mode.ADDR_J, v_mode),
          /* 88 */
          decode("js", Mode.ADDR_J, v_mode),
          decode("jns", Mode.ADDR_J, v_mode),
          decode("jp", Mode.ADDR_J, v_mode),
          decode("jnp", Mode.ADDR_J, v_mode),
          decode("jl", Mode.ADDR_J, v_mode),
          decode("jnl", Mode.ADDR_J, v_mode),
          decode("jle", Mode.ADDR_J, v_mode),
          decode("jnle", Mode.ADDR_J, v_mode),
          /* 90 */
          decode("seto", Mode.ADDR_E, b_mode),
          decode("setno", Mode.ADDR_E, b_mode),
          decode("setb", Mode.ADDR_E, b_mode),
          decode("setnb", Mode.ADDR_E, b_mode),
          decode("setz", Mode.ADDR_E, b_mode),
          decode("setnz", Mode.ADDR_E, b_mode),
          decode("setbe", Mode.ADDR_E, b_mode),
          decode("setnbe", Mode.ADDR_E, b_mode),
          /* 98 */
          decode("sets", Mode.ADDR_E, b_mode),
          decode("setns", Mode.ADDR_E, b_mode),
          decode("setp", Mode.ADDR_E, b_mode),
          decode("setnp", Mode.ADDR_E, b_mode),
          decode("setl", Mode.ADDR_E, b_mode),
          decode("setnl", Mode.ADDR_E, b_mode),
          decode("setle", Mode.ADDR_E, b_mode),
          decode("setnle", Mode.ADDR_E, b_mode),
          /* a0 */
          decode("push", Mode.ADDR_REG, FS),
          decode("pop", Mode.ADDR_REG, FS),
          decode("cpuid"),
          decode("bt", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("shld", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode, Mode.ADDR_I, b_mode),
          decode("shld", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode, Mode.ADDR_REG, CL),
          null,
          null,
          /* a8 */
          decode("push", Mode.ADDR_REG, GS),
          decode("pop", Mode.ADDR_REG, GS),
          decode("rsm"),
          decode("bts", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("shrd", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode, Mode.ADDR_I, b_mode),
          decode("shrd", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode, Mode.ADDR_REG, CL),
          decodeOrGroup(opcodeGroupTable0FAE, twoByteOpcodeGroupTable[20]),
          decode("imul", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          /* b0 */
          decode("cmpxchg", Mode.ADDR_E, b_mode, Mode.ADDR_G, b_mode),
          decode("cmpxchg", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("lss", Mode.ADDR_G, v_mode, Mode.ADDR_M, p_mode),
          decode("btr", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("lfs", Mode.ADDR_G, v_mode, Mode.ADDR_M, p_mode),
          decode("lgs", Mode.ADDR_G, v_mode, Mode.ADDR_M, p_mode),
          decode("movzx", Mode.ADDR_G, v_mode, Mode.ADDR_E, b_mode),
          decode("movzx", Mode.ADDR_G, v_mode, Mode.ADDR_E, w_mode),
          /* b8 */
          null,
          null,
          decodeGroup(15),
          decode("btc", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("bsf", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("bsr", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode),
          decode("movsx", Mode.ADDR_G, v_mode, Mode.ADDR_E, b_mode),
          decode("movsx", Mode.ADDR_G, v_mode, Mode.ADDR_E, w_mode),
          /* c0 */
          decode("xadd", Mode.ADDR_E, b_mode, Mode.ADDR_G, b_mode),
          decode("xadd", Mode.ADDR_E, v_mode, Mode.ADDR_G, v_mode),
          decode("cmpps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode, Mode.ADDR_I, b_mode),
          decode("movnti", Mode.ADDR_E, d_mode, Mode.ADDR_G, d_mode),
          decode("pinsrw", Mode.ADDR_P, q_mode, Mode.ADDR_E, d_mode, Mode.ADDR_I, b_mode),
          decode("pextrw", Mode.ADDR_G, d_mode, Mode.ADDR_P, q_mode, Mode.ADDR_I, b_mode),
          decode("shufps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode, Mode.ADDR_I, b_mode),
          decodeGroup(16),
          /* c8 */
          decode("bswap", Mode.ADDR_REG, EAX),
          decode("bswap", Mode.ADDR_REG, ECX),
          decode("bswap", Mode.ADDR_REG, EDX),
          decode("bswap", Mode.ADDR_REG, EBX),
          decode("bswap", Mode.ADDR_REG, ESP),
          decode("bswap", Mode.ADDR_REG, EBP),
          decode("bswap", Mode.ADDR_REG, ESI),
          decode("bswap", Mode.ADDR_REG, EDI),
          /* d0 */
          null,
          decode("psrlw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("psrld", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("psrlq", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("paddq", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pmullw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          null,
          decode("pmovmskb", Mode.ADDR_G, d_mode, Mode.ADDR_P, q_mode),
          /* d8 */
          decode("psubusb", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("psubusw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pminub", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pand", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("paddusb", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("paddusw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pmaxub", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pandn", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          /* e0 */
          decode("pavgb", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("psraw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("psrad", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pavgw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pmulhuw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pmulhw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          null,
          decode("movntq", Mode.ADDR_W, q_mode, Mode.ADDR_V, q_mode),
          /* e8 */
          decode("psubsb", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("psubsw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pminsw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("por", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("paddsb", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("paddsw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pmaxsw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pxor", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          /* f0 */
          null,
          decode("psllw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pslld", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("psllq", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pmuludq", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("pmaddwd", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("psadbw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("maskmoveq", Mode.ADDR_P, pi_mode, Mode.ADDR_Q, pi_mode),
          /* f8 */
          decode("psubb", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("psubw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("psubd", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("psubq", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("paddb", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("paddw", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          decode("paddd", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode),
          null
        };
  }

  /**
   * Please refer to IA-32 Intel Architecture Software Developer's Manual Volume 2
   */
  private X86InstructionDecoder[] createTwoByteOpcodePrefix66Table () {
    return new X86InstructionDecoder[]{
          /* 00 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 08 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 10 */
          decode("movupd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          decode("movupd", Mode.ADDR_W, pd_mode, Mode.ADDR_V, pd_mode),
          decode("movlpd", Mode.ADDR_V, q_mode, Mode.ADDR_W, q_mode),
          decode("movlpd", Mode.ADDR_W, q_mode, Mode.ADDR_V, q_mode),
          decode("unpcklpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, q_mode),
          decode("unpckhpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, q_mode),
          decode("movhpd", Mode.ADDR_V, q_mode, Mode.ADDR_W, q_mode),
          decode("movhpd", Mode.ADDR_W, q_mode, Mode.ADDR_V, q_mode),
          /* 18 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          decode("nop", Mode.ADDR_E, v_mode),
          /* 20 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 28 */
          decode("movapd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          decode("movapd", Mode.ADDR_W, pd_mode, Mode.ADDR_V, pd_mode),
          decode("cvtpi2pd", Mode.ADDR_V, pd_mode, Mode.ADDR_Q, dq_mode),
          decode("movntpd", Mode.ADDR_W, pd_mode, Mode.ADDR_V, pd_mode),
          decode("cvttpd2pi", Mode.ADDR_Q, dq_mode, Mode.ADDR_W, pd_mode),
          decode("cvtpd2pi", Mode.ADDR_Q, dq_mode, Mode.ADDR_W, pd_mode),
          decode("ucomisd", Mode.ADDR_V, sd_mode, Mode.ADDR_W, sd_mode),
          decode("comisd", Mode.ADDR_V, sd_mode, Mode.ADDR_W, sd_mode),
          /* 30 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 38 */
          decode(threeByteOpcode38Prefix66Table),
          null,
          decode(threeByteOpcode3APrefix66Table),
          null,
          null,
          null,
          null,
          null,
          /* 40 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 48 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 50 */
          decode("movmskpd", Mode.ADDR_E, d_mode, Mode.ADDR_V, pd_mode),
          decode("sqrtpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          null,
          null,
          decode("andpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          decode("andnpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          decode("orpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          decode("xorpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          /* 58 */
          decode("addpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          decode("mulpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          decode("cvtpd2ps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, pd_mode),
          decode("cvtps2dq", Mode.ADDR_V, dq_mode, Mode.ADDR_W, ps_mode),
          decode("subpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          decode("minpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          decode("divpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          decode("maxpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          /* 60 */
          decode("punpcklbw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("punpcklwd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("punpckldq", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("packsswb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pcmpgtb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pcmpgtw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pcmpgtd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("packuswb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          /* 68 */
          decode("punpckhbw", Mode.ADDR_P, dq_mode, Mode.ADDR_Q, dq_mode),
          decode("punpckhwd", Mode.ADDR_P, dq_mode, Mode.ADDR_Q, dq_mode),
          decode("punpckhdq", Mode.ADDR_P, dq_mode, Mode.ADDR_Q, dq_mode),
          decode("packssdw", Mode.ADDR_P, dq_mode, Mode.ADDR_Q, dq_mode),
          decode("punpcklqdq", Mode.ADDR_P, dq_mode, Mode.ADDR_Q, dq_mode),
          decode("punpckhqdq", Mode.ADDR_P, dq_mode, Mode.ADDR_Q, dq_mode),
          decode("movd", Mode.ADDR_V, dq_mode, Mode.ADDR_E, d_mode),
          decode("movdqa", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          /* 70 */
          decode("pshufd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode),
          decodeGroup(22),
          decodeGroup(23),
          decodeGroup(24),
          decode("pcmpeqb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pcmpeqw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pcmpeqd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          null,
          /* 78 */
          decode("extrq", Mode.ADDR_V, dq_mode, Mode.ADDR_I, b_mode, Mode.ADDR_I, b_mode),
          decode("extrq", Mode.ADDR_V, dq_mode, Mode.ADDR_U, q_mode),
          null,
          null,
          decode("haddpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          decode("hsubpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          decode("movd", Mode.ADDR_E, d_mode, Mode.ADDR_V, dq_mode),
          decode("movdqa", Mode.ADDR_W, dq_mode, Mode.ADDR_V, dq_mode),
          /* 80 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 88 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 90 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 98 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* a0 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* a8 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* b0 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* b8 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* c0 */
          null,
          null,
          decode("cmppd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode, Mode.ADDR_I, b_mode),
          null,
          decode("pinsrw", Mode.ADDR_V, dq_mode, Mode.ADDR_E, d_mode, Mode.ADDR_I, b_mode),
          decode("pextrw", Mode.ADDR_G, d_mode, Mode.ADDR_V, dq_mode, Mode.ADDR_I, b_mode),
          decode("shufpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode, Mode.ADDR_I, b_mode),
          null,
          /* c8 */
          decode("bswap", Mode.ADDR_REG, EAX),
          decode("bswap", Mode.ADDR_REG, ECX),
          decode("bswap", Mode.ADDR_REG, EDX),
          decode("bswap", Mode.ADDR_REG, EBX),
          decode("bswap", Mode.ADDR_REG, ESP),
          decode("bswap", Mode.ADDR_REG, EBP),
          decode("bswap", Mode.ADDR_REG, ESI),
          decode("bswap", Mode.ADDR_REG, EDI),
          /* d0 */
          decode("addsubpd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, pd_mode),
          decode("psrlw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("psrld", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("psrlq", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("paddq", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pmullw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("movq", Mode.ADDR_W, q_mode, Mode.ADDR_V, q_mode),
          decode("pmovmskb", Mode.ADDR_G, d_mode, Mode.ADDR_V, dq_mode),
          /* d8 */
          decode("psubusb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("psubusw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pminub", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pand", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("paddusb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("paddusw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pmaxub", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pandn", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          /* e0 */
          decode("pavgb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("psraw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("psrad", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pavgw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pmulhuw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pmulhw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("cvttpd2dq", Mode.ADDR_V, dq_mode, Mode.ADDR_W, pd_mode),
          decode("movntdq", Mode.ADDR_W, dq_mode, Mode.ADDR_V, dq_mode),
          /* e8 */
          decode("psubsb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("psubsw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pminsw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("por", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("paddsb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("paddsw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pmaxsw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pxor", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          /* f0 */
          null,
          decode("psllw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pslld", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("psllq", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pmuludq", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("pmaddwd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("psadbw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("maskmovdqu", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          /* f8 */
          decode("psubb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("psubw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("psubd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("psubq", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("paddb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("paddw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          decode("paddd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          null
        };
  }

  /**
   * Please refer to IA-32 Intel Architecture Software Developer's Manual Volume 2
   */
  private X86InstructionDecoder[] createTwoByteOpcodePrefixF2Table () {
    return new X86InstructionDecoder[]{
          /* 00 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 08 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 10 */
          decode("movsd", Mode.ADDR_V, sd_mode, Mode.ADDR_W, sd_mode),
          decode("movsd", Mode.ADDR_W, sd_mode, Mode.ADDR_V, sd_mode),
          decode("movddup", Mode.ADDR_V, dq_mode, Mode.ADDR_W, ps_mode),
          null,
          null,
          null,
          null,
          null,
          /* 18 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 20 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 28 */
          null,
          null,
          decode("cvtsi2sd", Mode.ADDR_V, sd_mode, Mode.ADDR_E, d_mode),
          decode("movntsd", Mode.ADDR_M, q_mode, Mode.ADDR_V, sd_mode),
          decode("cvttsd2si", Mode.ADDR_G, d_mode, Mode.ADDR_W, sd_mode),
          decode("cvtsd2si", Mode.ADDR_G, d_mode, Mode.ADDR_W, sd_mode),
          null,
          null,
          /* 30 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 38 */
          decode(threeByteOpcode38PrefixF2Table),
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 40 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 48 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 50 */
          null,
          decode("sqrtsd", Mode.ADDR_V, sd_mode, Mode.ADDR_W, sd_mode),
          null,
          null,
          null,
          null,
          null,
          null,
          /* 58 */
          decode("addsd", Mode.ADDR_V, sd_mode, Mode.ADDR_W, sd_mode),
          decode("mulsd", Mode.ADDR_V, sd_mode, Mode.ADDR_W, sd_mode),
          decode("cvtsd2ss", Mode.ADDR_V, sd_mode, Mode.ADDR_W, sd_mode),
          null,
          decode("subsd", Mode.ADDR_V, sd_mode, Mode.ADDR_W, sd_mode),
          decode("minsd", Mode.ADDR_V, sd_mode, Mode.ADDR_W, sd_mode),
          decode("divsd", Mode.ADDR_V, sd_mode, Mode.ADDR_W, sd_mode),
          decode("maxsd", Mode.ADDR_V, sd_mode, Mode.ADDR_W, sd_mode),
          /* 60 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 68 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 70 */
          decode("pshuflw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode),
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 78 */
          null,
          null,
          null,
          null,
          decode("haddps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          decode("hsubps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          null,
          null,
          /* 80 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 88 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 90 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 98 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* a0 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* a8 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* b0 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* b8 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* c0 */
          null,
          null,
          decode("cmpsd_xmm", Mode.ADDR_V, sd_mode, Mode.ADDR_W, sd_mode, Mode.ADDR_I, b_mode),
          null,
          null,
          null,
          null,
          null,
          /* c8 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* d0 */
          decode("addsubps", Mode.ADDR_V, ps_mode, Mode.ADDR_W, ps_mode),
          null,
          null,
          null,
          null,
          null,
          decode("movdq2q", Mode.ADDR_P, q_mode, Mode.ADDR_W, q_mode),
          null,
          /* d8 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* e0 */
          null,
          null,
          null,
          null,
          null,
          null,
          decode("cvtpd2dq", Mode.ADDR_V, dq_mode, Mode.ADDR_W, pd_mode),
          null,
          /* e8 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* f0 */
          decode("lddqu", Mode.ADDR_V, q_mode, Mode.ADDR_M, v_mode),
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* f8 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null
        };
  }

  /**
   * Please refer to IA-32 Intel Architecture Software Developer's Manual Volume 2
   */
  private X86InstructionDecoder[] createTwoByteOpcodePrefixF3Table () {
    return new X86InstructionDecoder[]{
          /* 00 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 08 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 10 */
          decode("movss", Mode.ADDR_V, ss_mode, Mode.ADDR_W, ss_mode),
          decode("movss", Mode.ADDR_W, ss_mode, Mode.ADDR_V, ss_mode),
          decode("movsldup", Mode.ADDR_V, dq_mode, Mode.ADDR_W, ps_mode),
          null,
          null,
          null,
          decode("movshdup", Mode.ADDR_V, dq_mode, Mode.ADDR_W, ps_mode),
          null,
          /* 18 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 20 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 28 */
          null,
          null,
          decode("cvtsi2ss", Mode.ADDR_V, ss_mode, Mode.ADDR_E, d_mode),
          decode("movntss", Mode.ADDR_M, d_mode, Mode.ADDR_V, ss_mode),
          decode("cvttss2si", Mode.ADDR_G, d_mode, Mode.ADDR_W, ss_mode),
          decode("cvtss2si", Mode.ADDR_G, d_mode, Mode.ADDR_W, ss_mode),
          null,
          null,
          /* 30 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 38 */
          decode(threeByteOpcode38PrefixF3Table),
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 40 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 48 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 50 */
          null,
          decode("sqrtss", Mode.ADDR_V, ss_mode, Mode.ADDR_W, ss_mode),
          decode("rsqrtss", Mode.ADDR_V, ss_mode, Mode.ADDR_W, ss_mode),
          decode("rcpss", Mode.ADDR_V, ss_mode, Mode.ADDR_W, ss_mode),
          null,
          null,
          null,
          null,
          /* 58 */
          decode("addss", Mode.ADDR_V, ss_mode, Mode.ADDR_W, ss_mode),
          decode("mulss", Mode.ADDR_V, ss_mode, Mode.ADDR_W, ss_mode),
          decode("cvtss2sd", Mode.ADDR_V, ss_mode, Mode.ADDR_W, ss_mode),
          decode("cvttps2dq", Mode.ADDR_V, dq_mode, Mode.ADDR_W, ps_mode),
          decode("subss", Mode.ADDR_V, ss_mode, Mode.ADDR_W, ss_mode),
          decode("minss", Mode.ADDR_V, ss_mode, Mode.ADDR_W, ss_mode),
          decode("divss", Mode.ADDR_V, ss_mode, Mode.ADDR_W, ss_mode),
          decode("maxss", Mode.ADDR_V, ss_mode, Mode.ADDR_W, ss_mode),
          /* 60 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 68 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          decode("movdqu", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode),
          /* 70 */
          decode("pshufhw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode),
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 78 */
          null,
          null,
          null,
          null,
          null,
          null,
          decode("movq", Mode.ADDR_V, q_mode, Mode.ADDR_W, q_mode),
          decode("movdqu", Mode.ADDR_W, dq_mode, Mode.ADDR_V, dq_mode),
          /* 80 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 88 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 90 */
          decode("pause"),
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* 98 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* a0 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* a8 */
          null,
          null,
          null,
          decode("RstosS", Mode.ADDR_Y, a_mode, Mode.ADDR_REG, EAX),
          null,
          null,
          null,
          null,
          /* b0 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* b8 */
          decode("popcnt", Mode.ADDR_R, v_mode, Mode.ADDR_E, v_mode),
          null,
          null,
          null,
          null,
          decode("lzcnt", Mode.ADDR_R, v_mode, Mode.ADDR_E, v_mode),
          null,
          null,
          /* c0 */
          null,
          null,
          decode("cmpss", Mode.ADDR_V, ss_mode, Mode.ADDR_W, ss_mode, Mode.ADDR_I, b_mode),
          decode("ret"),
          null,
          null,
          null,
          null,
          /* c8 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* d0 */
          null,
          null,
          null,
          null,
          null,
          null,
          decode("movq2dq", Mode.ADDR_V, dq_mode, Mode.ADDR_Q, q_mode),
          null,
          /* d8 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* e0 */
          null,
          null,
          null,
          null,
          null,
          null,
          decode("cvtdq2pd", Mode.ADDR_V, pd_mode, Mode.ADDR_W, dq_mode),
          null,
          /* e8 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* f0 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          /* f8 */
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null
        };
  }

  /**
   * Please refer to IA-32 Intel Architecture Software Developer's Manual Volume 2
   * APPENDIX A - Escape opcodes
   */
  private X86InstructionDecoder[][] createFloatOpcodeGroupTable () {
    return new X86InstructionDecoder[][]{
          /* d9_2 */
          {
            decode("fnop"),
            null,
            null,
            null,
            null,
            null,
            null,
            null
          },
          /*  d9_4 */
          {
            decode("fchs"),
            decode("fabs"),
            null,
            null,
            decode("ftst"),
            decode("fxam"),
            null,
            null
          },
          /* d9_5 */
          {
            decode("fld1"),
            decode("fldl2t"),
            decode("fldl2e"),
            decode("fldpi"),
            decode("fldlg2"),
            decode("fldln2"),
            decode("fldz"),
            null
          },
          /* d9_6 */
          {
            decode("f2xm1"),
            decode("fyl2x"),
            decode("fptan"),
            decode("fpatan"),
            decode("fxtract"),
            decode("fprem1"),
            decode("fdecstp"),
            decode("fincstp")
          },
          /* d9_7 */
          {
            decode("fprem"),
            decode("fyl2xp1"),
            decode("fsqrt"),
            decode("fsincos"),
            decode("frndint"),
            decode("fscale"),
            decode("fsin"),
            decode("fcos")
          },
          /* da_5 */
          {
            null,
            decode("fucompp"),
            null,
            null,
            null,
            null,
            null,
            null
          },
          /* db_4 */
          {
            decode("feni(287 only)"),
            decode("fdisi(287 only)"),
            decode("fNclex"),
            decode("fNinit"),
            decode("fNsetpm(287 only)"),
            null,
            null,
            null
          },
          /* de_3 */
          {
            null,
            decode("fcompp"),
            null,
            null,
            null,
            null,
            null,
            null
          },
          /* df_4 */
          {
            decode("fNstsw"),
            null,
            null,
            null,
            null,
            null,
            null,
            null
          }
        };
  }

  /**
   * Please refer to IA-32 Intel Architecture Software Developer's Manual Volume 2
   * APPENDIX A - Table A-4. Opcode Extensions for One and Two-byte Opcodes by Group Number.
   */
  private X86InstructionDecoder[][] createTwoByteOpcodeGroupTable () {
    return new X86InstructionDecoder[][]{
          { //0
            decode("add", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),
            decode("or", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),
            decode("adc", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),
            decode("sbb", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),
            decode("and", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),
            decode("sub", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),
            decode("xor", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),
            decode("cmp", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode)
          },
          { //1
            decode("add", Mode.ADDR_E, v_mode, Mode.ADDR_I, v_mode),
            decode("or", Mode.ADDR_E, v_mode, Mode.ADDR_I, v_mode),
            decode("adc", Mode.ADDR_E, v_mode, Mode.ADDR_I, v_mode),
            decode("sbb", Mode.ADDR_E, v_mode, Mode.ADDR_I, v_mode),
            decode("and", Mode.ADDR_E, v_mode, Mode.ADDR_I, v_mode),
            decode("sub", Mode.ADDR_E, v_mode, Mode.ADDR_I, v_mode),
            decode("xor", Mode.ADDR_E, v_mode, Mode.ADDR_I, v_mode),
            decode("cmp", Mode.ADDR_E, v_mode, Mode.ADDR_I, v_mode)
          },
          { //2
            decode("add", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode), /*note: sIb here*/
            decode("or", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
            decode("adc", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
            decode("sbb", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
            decode("and", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
            decode("sub", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
            decode("xor", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
            decode("cmp", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode)
          },
          { //3
            decode("rol", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),
            decode("ror", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),
            decode("rcl", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),
            decode("rcr", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),
            decode("shl", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),
            decode("shr", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),
            null,
            decode("sar", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),},
          { //4
            decode("rol", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
            decode("ror", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
            decode("rcl", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
            decode("rcr", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
            decode("shl", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
            decode("shr", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
            null,
            decode("sar", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode)
          },
          { //5
            decode("rol", Mode.ADDR_E, b_mode, Mode.ADDR_IMM, 1),
            decode("ror", Mode.ADDR_E, b_mode, Mode.ADDR_IMM, 1),
            decode("rcl", Mode.ADDR_E, b_mode, Mode.ADDR_IMM, 1),
            decode("rcr", Mode.ADDR_E, b_mode, Mode.ADDR_IMM, 1),
            decode("shl", Mode.ADDR_E, b_mode, Mode.ADDR_IMM, 1),
            decode("shr", Mode.ADDR_E, b_mode, Mode.ADDR_IMM, 1),
            null,
            decode("sar", Mode.ADDR_E, b_mode, Mode.ADDR_IMM, 1)
          },
          { //6
            decode("rol", Mode.ADDR_E, v_mode, Mode.ADDR_IMM, 1),
            decode("ror", Mode.ADDR_E, v_mode, Mode.ADDR_IMM, 1),
            decode("rcl", Mode.ADDR_E, v_mode, Mode.ADDR_IMM, 1),
            decode("rcr", Mode.ADDR_E, v_mode, Mode.ADDR_IMM, 1),
            decode("shl", Mode.ADDR_E, v_mode, Mode.ADDR_IMM, 1),
            decode("shr", Mode.ADDR_E, v_mode, Mode.ADDR_IMM, 1),
            null,
            decode("sar", Mode.ADDR_E, v_mode, Mode.ADDR_IMM, 1)
          },
          { //7
            decode("rol", Mode.ADDR_E, b_mode, Mode.ADDR_REG, CL),
            decode("ror", Mode.ADDR_E, b_mode, Mode.ADDR_REG, CL),
            decode("rcl", Mode.ADDR_E, b_mode, Mode.ADDR_REG, CL),
            decode("rcr", Mode.ADDR_E, b_mode, Mode.ADDR_REG, CL),
            decode("shl", Mode.ADDR_E, b_mode, Mode.ADDR_REG, CL),
            decode("shr", Mode.ADDR_E, b_mode, Mode.ADDR_REG, CL),
            null,
            decode("sar", Mode.ADDR_E, b_mode, Mode.ADDR_REG, CL)
          },
          { //8
            decode("rol", Mode.ADDR_E, v_mode, Mode.ADDR_REG, CL),
            decode("ror", Mode.ADDR_E, v_mode, Mode.ADDR_REG, CL),
            decode("rcl", Mode.ADDR_E, v_mode, Mode.ADDR_REG, CL),
            decode("rcr", Mode.ADDR_E, v_mode, Mode.ADDR_REG, CL),
            decode("shl", Mode.ADDR_E, v_mode, Mode.ADDR_REG, CL),
            decode("shr", Mode.ADDR_E, v_mode, Mode.ADDR_REG, CL),
            null,
            decode("sar", Mode.ADDR_E, v_mode, Mode.ADDR_REG, CL)
          },
          { //9
            decode("test", Mode.ADDR_E, b_mode, Mode.ADDR_I, b_mode),
            null, /*decode("(bad)", Mode.ADDR_E, b_mode)*/
            decode("not", Mode.ADDR_E, b_mode),
            decode("neg", Mode.ADDR_E, b_mode),
            decode("mul", Mode.ADDR_REG, AL, Mode.ADDR_E, b_mode),
            decode("imul", Mode.ADDR_REG, AL, Mode.ADDR_E, b_mode),
            decode("div", Mode.ADDR_REG, AL, Mode.ADDR_E, b_mode),
            decode("idiv", Mode.ADDR_REG, AL, Mode.ADDR_E, b_mode)
          },
          { //10
            decode("test", Mode.ADDR_E, v_mode, Mode.ADDR_I, v_mode),
            null,
            decode("not", Mode.ADDR_E, v_mode),
            decode("neg", Mode.ADDR_E, v_mode),
            decode("mul", Mode.ADDR_REG, EAX, Mode.ADDR_E, v_mode),
            decode("imul", Mode.ADDR_REG, EAX, Mode.ADDR_E, v_mode),
            decode("div", Mode.ADDR_REG, EAX, Mode.ADDR_E, v_mode),
            decode("idiv", Mode.ADDR_REG, EAX, Mode.ADDR_E, v_mode)
          },
          { //11
            decode("inc", Mode.ADDR_E, b_mode),
            decode("dec", Mode.ADDR_E, b_mode),
            null,
            null,
            null,
            null,
            null,
            null
          },
          { //12
            decode("inc", Mode.ADDR_E, v_mode),
            decode("dec", Mode.ADDR_E, v_mode),
            decode("call", Mode.ADDR_E, q_mode),
            decode("call", Mode.ADDR_E, p_mode),
            decode("jmp", Mode.ADDR_E, q_mode),
            decode("jmp", Mode.ADDR_E, p_mode),
            decode("push", Mode.ADDR_E, v_mode),
            null
          },
          { //13
            decode("sldt", Mode.ADDR_E, w_mode),
            decode("str", Mode.ADDR_E, w_mode),
            decode("lldt", Mode.ADDR_E, w_mode),
            decode("ltr", Mode.ADDR_E, w_mode),
            decode("verr", Mode.ADDR_E, w_mode),
            decode("verw", Mode.ADDR_E, w_mode),
            null,
            null
          },
          { //14
            decode("sgdt", Mode.ADDR_E, w_mode),
            decode("sidt", Mode.ADDR_E, w_mode),
            decode("lgdt", Mode.ADDR_E, w_mode),
            decode("lidt", Mode.ADDR_E, w_mode),
            decode("smsw", Mode.ADDR_E, w_mode),
            null,
            decode("lmsw", Mode.ADDR_E, w_mode),
            decode("invlpg", Mode.ADDR_E, w_mode)
          },
          { //15
            null,
            null,
            null,
            null,
            decode("btS", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
            decode("btsS", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
            decode("btrS", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode),
            decode("btcS", Mode.ADDR_E, v_mode, Mode.ADDR_I, b_mode)
          },
          /*16*/
          {
            null,
            decode("cmpxch8b", Mode.ADDR_W, q_mode),
            null,
            null,
            null,
            null,
            null,
            null
          },
          /*17*/
          {
            null,
            null,
            decode("psrlw", Mode.ADDR_P, q_mode, Mode.ADDR_I, b_mode),
            null,
            decode("psraw", Mode.ADDR_P, q_mode, Mode.ADDR_I, b_mode),
            null,
            decode("psllw", Mode.ADDR_P, q_mode, Mode.ADDR_I, b_mode),
            null
          },
          /*18*/
          {
            null,
            null,
            decode("psrld", Mode.ADDR_P, q_mode, Mode.ADDR_I, b_mode),
            null,
            decode("psrad", Mode.ADDR_P, q_mode, Mode.ADDR_I, b_mode),
            null,
            decode("pslld", Mode.ADDR_P, q_mode, Mode.ADDR_I, b_mode),
            null
          },
          /*19*/
          {
            null,
            null,
            decode("psrlq", Mode.ADDR_P, q_mode, Mode.ADDR_I, b_mode),
            null,
            null,
            null,
            decode("psllq", Mode.ADDR_P, q_mode, Mode.ADDR_I, b_mode),
            null
          },
          /*20 - Grp15*/
          {
            decode("fxsave", Mode.ADDR_M, v_mode),
            decode("fxrstor", Mode.ADDR_M, v_mode),
            decode("ldmxcsr"),
            decode("stmxcsr"),
            decode("xsave", Mode.ADDR_M, v_mode),
            decode("xrstor", Mode.ADDR_M, v_mode),
            null,
            decode("clflush")
          },
          /*21 - Grp16*/
          {
            decode("prefetchnta", Mode.ADDR_M, v_mode),
            decode("prefetcht0"),
            decode("prefetcht1"),
            decode("prefetcht2"),
            null,
            null,
            null,
            null
          },
          /*22 - Grp12:66*/
          {
            null,
            null,
            decode("psrlw", Mode.ADDR_P, dq_mode, Mode.ADDR_I, b_mode),
            null,
            decode("psraw", Mode.ADDR_P, dq_mode, Mode.ADDR_I, b_mode),
            null,
            decode("psllw", Mode.ADDR_P, dq_mode, Mode.ADDR_I, b_mode),
            null
          },
          /*23 - Grp13:66*/
          {
            null,
            null,
            decode("psrld", Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode),
            null,
            decode("psrad", Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode),
            null,
            decode("pslld", Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode),
            null
          },
          /*24 - - Grp14:66*/
          {
            null,
            null,
            decode("psrlq", Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode),
            decode("psrldq", Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode),
            null,
            null,
            decode("psllq", Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode),
            decode("psllq", Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode)
          },};
  }

  // Table A-6. Opcode Extensions for One- and Two-byte Opcodes by Group Number
  private X86InstructionDecoder[][] create0F01OpcodeGroupTable () {
    return new X86InstructionDecoder[][]{
          { //000
            null,
            decode("vmcall"),
            decode("vmlaunch"),
            decode("vmresume"),
            decode("vmxoff"),
            null,
            null,
            null
          },
          { //001
            decode("monitor"),
            decode("mwait"),
            null,
            null,
            null,
            null,
            null,
            null
          },
          { //010
            decode("xgetbv"),
            decode("xsetbv"),
            null,
            null,
            null,
            null,
            null,
            null
          },
          { //011
            decode("vmrun"), //XXX: args
            decode("vmmcall"), //XXX: args
            decode("vmload"), //XXX: args
            decode("vmsave"),
            decode("stgi"),
            decode("clgi"),
            decode("skinit"),
            decode("invlpga")
          },
          { //100
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
          },
          { //101
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
          },
          { //110
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
          },
          { //111
            decode("swapgs"),
            decode("rdtscp"),
            null,
            null,
            null,
            null,
            null,
            null
          }
        /*
        { //111
        decode("vmrun"), //XXX args
        decode("vmmcall"),
        decode("vmload"), //XXX args
        decode("vmsave"),
        decode("stgi"),
        decode("clgi"),
        decode("skinit"), //XXX args
        decode("invlpga") //XXX args
        }*/
        };
  }

  // Table A-6. Opcode Extensions for One- and Two-byte Opcodes by Group Number
  private X86InstructionDecoder[][] create0FAEOpcodeGroupTable () {
    return new X86InstructionDecoder[][]{
          { //000
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
          },
          { //001
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
          },
          { //010
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
          },
          { //011
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
          },
          { //100
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
          },
          { //101
            decode("lfence"),
            decode("lfence"),
            decode("lfence"),
            decode("lfence"),
            decode("lfence"),
            decode("lfence"),
            decode("lfence"),
            decode("lfence")
          },
          { //110
            decode("mfence"),
            decode("mfence"),
            decode("mfence"),
            decode("mfence"),
            decode("mfence"),
            decode("mfence"),
            decode("mfence"),
            decode("mfence")
          },
          { //111
            decode("sfence"),
            decode("sfence"),
            decode("sfence"),
            decode("sfence"),
            decode("sfence"),
            decode("sfence"),
            decode("sfence"),
            decode("sfence")
          }
        };
  }

  /**
   * Please refer to IA-32 Intel Architecture Software Developer's Manual Volume 2
   * APPENDIX A - Escape opcodes
   *
   * When ModR/M byte is within 00h to BFh*
   */
  private X86InstructionDecoder[][] createFloatOpcodeTableOne () {
    return new X86InstructionDecoder[][]{
          /* d8 */
          {
            decode("fadd", Mode.ADDR_E, v_mode),
            decode("fmul", Mode.ADDR_E, v_mode),
            decode("fcom", Mode.ADDR_E, v_mode),
            decode("fcomp", Mode.ADDR_E, v_mode),
            decode("fsub", Mode.ADDR_E, v_mode),
            decode("fsubr", Mode.ADDR_E, v_mode),
            decode("fdiv", Mode.ADDR_E, v_mode),
            decode("fdivr", Mode.ADDR_E, v_mode)
          },
          /*  d9 */
          {
            decode("fld", Mode.ADDR_E, v_mode),
            null,
            decode("fst", Mode.ADDR_E, v_mode),
            decode("fstp", Mode.ADDR_E, v_mode),
            decode("fldenv", Mode.ADDR_E, v_mode),
            decode("fldcw", Mode.ADDR_E, v_mode),
            decode("fNstenv", Mode.ADDR_E, v_mode),
            decode("fNstcw", Mode.ADDR_E, v_mode)
          },
          /* da */
          {
            decode("fiadd", Mode.ADDR_E, v_mode),
            decode("fimul", Mode.ADDR_E, v_mode),
            decode("ficom", Mode.ADDR_E, v_mode),
            decode("ficomp", Mode.ADDR_E, v_mode),
            decode("fisub", Mode.ADDR_E, v_mode),
            decode("fisubr", Mode.ADDR_E, v_mode),
            decode("fidiv", Mode.ADDR_E, v_mode),
            decode("fidivr", Mode.ADDR_E, v_mode)
          },
          /* db */
          {
            decode("fild", Mode.ADDR_E, v_mode),
            null,
            decode("fist", Mode.ADDR_E, v_mode),
            decode("fistp", Mode.ADDR_E, v_mode),
            null,
            decode("fld", Mode.ADDR_E, v_mode),
            null,
            decode("fstpt", Mode.ADDR_E, v_mode)
          },
          /* dc */
          {
            decode("fadd", Mode.ADDR_E, v_mode),
            decode("fmul", Mode.ADDR_E, v_mode),
            decode("fcom", Mode.ADDR_E, v_mode),
            decode("fcomp", Mode.ADDR_E, v_mode),
            decode("fsub", Mode.ADDR_E, v_mode),
            decode("fsubr", Mode.ADDR_E, v_mode),
            decode("fdiv", Mode.ADDR_E, v_mode),
            decode("fdivr", Mode.ADDR_E, v_mode)
          },
          /* dd */
          {
            decode("fld", Mode.ADDR_E, v_mode),
            null,
            decode("fst", Mode.ADDR_E, v_mode),
            decode("fstp", Mode.ADDR_E, v_mode),
            decode("frstor", Mode.ADDR_E, v_mode),
            null,
            decode("fNsave", Mode.ADDR_E, v_mode),
            decode("fNstsw", Mode.ADDR_E, v_mode)
          },
          /* de */
          {
            decode("fiadd", Mode.ADDR_E, v_mode),
            decode("fimul", Mode.ADDR_E, v_mode),
            decode("ficom", Mode.ADDR_E, v_mode),
            decode("ficomp", Mode.ADDR_E, v_mode),
            decode("fisub", Mode.ADDR_E, v_mode),
            decode("fisubr", Mode.ADDR_E, v_mode),
            decode("fidiv", Mode.ADDR_E, v_mode),
            decode("fidivr", Mode.ADDR_E, v_mode)
          },
          /* df */
          {
            decode("fild", Mode.ADDR_E, v_mode),
            null,
            decode("fist", Mode.ADDR_E, v_mode),
            decode("fistp", Mode.ADDR_E, v_mode),
            decode("fbld", Mode.ADDR_E, v_mode),
            decode("fild", Mode.ADDR_E, v_mode),
            decode("fbstp", Mode.ADDR_E, v_mode),
            decode("fistp", Mode.ADDR_E, v_mode)
          }
        };
  }

  /**
   * Please refer to IA-32 Intel Architecture Software Developer's Manual Volume 2
   * APPENDIX A - Escape opcodes
   *
   * When ModR/M byte is outside 00h to BFh*
   */
  private X86InstructionDecoder[][] createFloatOpcodeTableTwo () {
    return new X86InstructionDecoder[][]{
          /* d8 */
          /*parameter for Mode.ADDR_FPREG, 0 means ST(0), 1 means ST at rm value. */
          {
            decode("fadd", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            decode("fmul", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            decode("fcom", Mode.ADDR_FREG, 1),
            decode("fcomp", Mode.ADDR_FREG, 1),
            decode("fsub", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            decode("fsubr", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            decode("fdiv", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            decode("fdivr", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1)
          },
          /* d9 */
          {
            decode("fld", Mode.ADDR_FREG, 1),
            decode("fxch", Mode.ADDR_FREG, 1),
            decodeFloatGroup(0),
            null,
            decodeFloatGroup(1),
            decodeFloatGroup(2),
            decodeFloatGroup(3),
            decodeFloatGroup(4)
          },
          /* da */
          {
            decode("fcmovb", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            decode("fcmove", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            decode("fcmovbe", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            decode("fcmovu", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            null,
            decodeFloatGroup(5),
            null,
            null
          },
          /* db */
          {
            decode("fcmovnb", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            decode("fcmovne", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            decode("fcmovnbe", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            decode("fcmovnu", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            decodeFloatGroup(6),
            decode("fucomi", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            decode("fcomi", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            null
          },
          /* dc */
          {
            decode("fadd", Mode.ADDR_FREG, 1, Mode.ADDR_FREG, 0),
            decode("fmul", Mode.ADDR_FREG, 1, Mode.ADDR_FREG, 0),
            null,
            null,
            decode("fsub", Mode.ADDR_FREG, 1, Mode.ADDR_FREG, 0),
            decode("fsubr", Mode.ADDR_FREG, 1, Mode.ADDR_FREG, 0),
            decode("fdiv", Mode.ADDR_FREG, 1, Mode.ADDR_FREG, 0),
            decode("fdivr", Mode.ADDR_FREG, 1, Mode.ADDR_FREG, 0)
          },
          /* dd */
          {
            decode("ffree", Mode.ADDR_FREG, 1),
            null,
            decode("fst", Mode.ADDR_FREG, 1),
            decode("fstp", Mode.ADDR_FREG, 1),
            decode("fucom", Mode.ADDR_FREG, 1),
            decode("fucomp", Mode.ADDR_FREG, 1),
            null,
            null
          },
          /* de */
          {
            decode("faddp", Mode.ADDR_FREG, 1, Mode.ADDR_FREG, 0),
            decode("fmulp", Mode.ADDR_FREG, 1, Mode.ADDR_FREG, 0),
            null,
            decodeFloatGroup(7),
            decode("fsubrp", Mode.ADDR_FREG, 1, Mode.ADDR_FREG, 0),
            decode("fsubp", Mode.ADDR_FREG, 1, Mode.ADDR_FREG, 0),
            decode("fdivrp", Mode.ADDR_FREG, 1, Mode.ADDR_FREG, 0),
            decode("fdivp", Mode.ADDR_FREG, 1, Mode.ADDR_FREG, 0)
          },
          /* df */
          {
            decode("ffreep", Mode.ADDR_FREG, 1),
            null,
            decode("fst", Mode.ADDR_FREG, 1),
            decode("fstp", Mode.ADDR_FREG, 1),
            decodeFloatGroup(7),
            decode("fucomip", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            decode("fcomip", Mode.ADDR_FREG, 0, Mode.ADDR_FREG, 1),
            null
          }
        };
  }

  private X86InstructionDecoder[] createThreeByteOpcodeTable () {
    X86InstructionDecoder[] decoders = new X86InstructionDecoder[0xff];
    decoders[0x00] = decode("pshufb", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode);
    decoders[0x0f] = decode("palignr", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode, Mode.ADDR_I, b_mode);
    decoders[0xf0] = decode("movbe", Mode.ADDR_G, v_mode, Mode.ADDR_M, v_mode);
    decoders[0xf1] = decode("movbe", Mode.ADDR_M, v_mode, Mode.ADDR_G, v_mode);
    return decoders;
  }

  private X86InstructionDecoder[] createThreeByteOpcodePrefix66Table () {
    X86InstructionDecoder[] decoders = new X86InstructionDecoder[0xff];
    decoders[0x00] = decode("pshufb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x01] = decode("phaddw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x02] = decode("phaddd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x03] = decode("phaddsw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x04] = decode("pmaddubsw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x05] = decode("phsubw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x06] = decode("phsubd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x07] = decode("phsubsw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x08] = decode("psignb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x09] = decode("psignw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x0a] = decode("psignd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x0b] = decode("pmulhrsw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x10] = decode("pblendvb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x14] = decode("blendvps", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x15] = decode("blendvpd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x17] = decode("ptest", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x1c] = decode("pabsb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x1d] = decode("pabsw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x1e] = decode("pabsd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x20] = decode("pmovsxbw", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x21] = decode("pmovsxbd", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x22] = decode("pmovsxbq", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x23] = decode("pmovsxwd", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x24] = decode("pmovsxwq", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x25] = decode("pmovsxdq", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x28] = decode("pmuldq", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x29] = decode("pcmpeqq", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x2A] = decode("movntdqa", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x2B] = decode("packusdw", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x30] = decode("pmovzxbw", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x31] = decode("pmovzxbd", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x32] = decode("pmovzxbq", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x33] = decode("pmovzxwd", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x34] = decode("pmovzxwq", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x35] = decode("pmovzxdq", Mode.ADDR_V, dq_mode, Mode.ADDR_U, dq_mode);
    decoders[0x37] = decode("pcmpgtq", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x38] = decode("pminsb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x39] = decode("pminsd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x3A] = decode("pminuw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x3B] = decode("pminud", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x3C] = decode("pmaxsb", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x3D] = decode("pmaxsd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x3E] = decode("pmaxuw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x3F] = decode("pmaxud", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x40] = decode("pmulld", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0x41] = decode("phminposuw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0xdb] = decode("aesimc", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0xdc] = decode("aesenc", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0xdd] = decode("aesenclast", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0xde] = decode("aesdec", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    decoders[0xdf] = decode("aesdeclast", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode);
    return decoders;
  }

  private X86InstructionDecoder[] createThreeByteOpcodePrefixF2Table () {
    X86InstructionDecoder[] decoders = new X86InstructionDecoder[0xff];
    decoders[0xf0] = decode("crc32", Mode.ADDR_G, v_mode, Mode.ADDR_E, b_mode);
    decoders[0xf1] = decode("crc32", Mode.ADDR_G, v_mode, Mode.ADDR_E, v_mode);
    return decoders;
  }

  private static X86InstructionDecoder[] createThreeByteOpcodePrefixF3Table () {
    X86InstructionDecoder[] decoders = new X86InstructionDecoder[0xff];
    return decoders;
  }

  private X86InstructionDecoder[] createThreeByteOpcode3ATable () {
    X86InstructionDecoder[] decoders = new X86InstructionDecoder[0xff];
    decoders[0x0f] = decode("palignr", Mode.ADDR_P, q_mode, Mode.ADDR_Q, q_mode, Mode.ADDR_I, b_mode);
    return decoders;
  }

  private X86InstructionDecoder[] createThreeByteOpcode3APrefix66Table () {
    X86InstructionDecoder[] decoders = new X86InstructionDecoder[0xff];
    decoders[0x08] = decode("roundps", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x09] = decode("roundpd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x0a] = decode("roundss", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x0b] = decode("roundsd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x0c] = decode("blendps", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x0d] = decode("blendpd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x0e] = decode("pblendw", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x0f] = decode("palignr", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x14] = decode("pextrb", Mode.ADDR_R, d_mode, Mode.ADDR_V, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x16] = decode("pextrd", Mode.ADDR_R, d_mode, Mode.ADDR_V, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x17] = decode("extractps", Mode.ADDR_R, d_mode, Mode.ADDR_V, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x20] = decode("pinsrb", Mode.ADDR_V, dq_mode, Mode.ADDR_R, d_mode, Mode.ADDR_I, b_mode);
    decoders[0x21] = decode("insertps", Mode.ADDR_V, dq_mode, Mode.ADDR_R, d_mode, Mode.ADDR_I, b_mode);
    decoders[0x40] = decode("dpps", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x41] = decode("dppd", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x42] = decode("mpsadbw", Mode.ADDR_V, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x44] = decode("pclmulqdq", Mode.ADDR_V, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x60] = decode("pcmpestrm", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x61] = decode("pcmpestri", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x62] = decode("pcmpistrm", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0x63] = decode("pcmpistri", Mode.ADDR_V, dq_mode, Mode.ADDR_W, dq_mode, Mode.ADDR_I, b_mode);
    decoders[0xdf] = decode("aeskeygenassist", Mode.ADDR_V, dq_mode, Mode.ADDR_I, b_mode);
    return decoders;
  }
}
