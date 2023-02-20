package bindis.x86.common;

import static bindis.x86.common.X86Consts.AH;
import static bindis.x86.common.X86Consts.AL;
import static bindis.x86.common.X86Consts.AX;
import static bindis.x86.common.X86Consts.BH;
import static bindis.x86.common.X86Consts.BL;
import static bindis.x86.common.X86Consts.BP;
import static bindis.x86.common.X86Consts.BX;
import static bindis.x86.common.X86Consts.CH;
import static bindis.x86.common.X86Consts.CL;
import static bindis.x86.common.X86Consts.CS;
import static bindis.x86.common.X86Consts.CX;
import static bindis.x86.common.X86Consts.DH;
import static bindis.x86.common.X86Consts.DI;
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
import static bindis.x86.common.X86Consts.SI;
import static bindis.x86.common.X86Consts.SP;
import static bindis.x86.common.X86Consts.SS;

import bindis.DecodeException;
import bindis.DecodeStream;
import bindis.x86.common.X86Prefixes.Prefix;

/**
 * Decoders for X86 operands.
 *
 * @author mb
 */
public class X86OperandDecoders {
  /* == Operand types == */
  /**
   * Two one-word operands in memory or two double-word operands in memory, depending on the operand-size attribute
   * (used only by the BOUND instruction).
   */
  public static final int a_mode = 1;
  /** Byte sized operand, regardless of operand-size attributes. */
  public static final int b_mode = 2;
  /** Byte or word, depending on operand-size attribute. */
  public static final int c_mode = 3;
  /** Doubleword, regardless of operand-size attribute. */
  public static final int d_mode = 4;
  /** Double-quadword, regardless of operand-size attribute. */
  public static final int dq_mode = 5;
  /** 32-bit, 48-bit or 80-bit pointer, depending on operand-size attribute. */
  public static final int p_mode = 6;
  /** 128-packed double-precision floating-point data. */
  public static final int pd_mode = 7;
  /** Quadword MMX technology register (for example: mm0). */
  public static final int pi_mode = 8;
  /** 128-bit packed single-precision floating-point data. */
  public static final int ps_mode = 9;
  /** Quadword, regardless of operand-size attribute. */
  public static final int q_mode = 10;
  /** 6-byte or 10-byte pseudo-descriptor. */
  public static final int s_mode = 11;
  /** ??? */
  public static final int sd_mode = 12;
  /** Doubleword integer register (for example: eax). */
  public static final int si_mode = 13;
  /** Scalar element of a 128-bit packed single-precision floating data. */
  public static final int ss_mode = 14;
  /** Word, doubleword or quadword (in 64-bit mode), depending on operand-size attribute. */
  public static final int v_mode = 15;
  /** Word, regardless of operand-size attribute. */
  public static final int w_mode = 16;
  /** Word for 16-bit operand-size or doubleword for 32 or 64-bit operand-size. */
  public static final int z_mode = 17;
  public static final int INVALID_OPERANDTYPE = -1;

  /* == Addressing modes == */
  /**
   * @see AddrModeA
   */
  public final AddrMode ADDR_A = new AddrModeA();
  /**
   * @see AddrModeC
   */
  public final AddrMode ADDR_C = new AddrModeC();
  /**
   * @see AddrModeD
   */
  public final AddrMode ADDR_D = new AddrModeD();
  /**
   * @see AddrModeE
   */
  public final AddrMode ADDR_E = new AddrModeE();
  /**
   * @see AddrModeF
   */
  public final AddrMode ADDR_F = new AddrModeF();
  /**
   * @see AddrModeFreg
   */
  public final AddrMode ADDR_FREG = new AddrModeFreg();
  /**
   * @see AddrModeG
   */
  public final AddrMode ADDR_G = new AddrModeG();
  /**
   * @see AddrModeI
   */
  public final AddrMode ADDR_I = new AddrModeI();
  /**
   * @see AddrModeI
   */
  public final AddrMode ADDR_I64 = new AddrModeI64();
  /**
   * @see AddrModeJ
   */
  public final AddrMode ADDR_J = new AddrModeJ();
  /**
   * @see AddrModeM
   */
  public final AddrMode ADDR_M = new AddrModeM();
  /**
   * @see AddrModeN
   */
  public final AddrMode ADDR_N = new AddrModeN();
  /**
   * @see AddrModeO
   */
  public final AddrMode ADDR_O = new AddrModeO();
  /**
   * @see AddrModeP
   */
  public final AddrMode ADDR_P = new AddrModeP();
  /**
   * @see AddrModeQ
   */
  public final AddrMode ADDR_Q = new AddrModeQ();
  /**
   * @see AddrModeR
   */
  public final AddrMode ADDR_R = new AddrModeR();
  /**
   * @see AddrModeReg
   */
  public final AddrMode ADDR_REG = new AddrModeReg();
  /**
   * @see AddrModeS
   */
  public final AddrMode ADDR_S = new AddrModeS();
  /**
   * @see AddrModeT
   */
  public final AddrMode ADDR_T = new AddrModeT();
  /**
   * @see AddrModeU
   */
  public final AddrMode ADDR_U = new AddrModeU();
  /**
   * @see AddrModeV
   */
  public final AddrMode ADDR_V = new AddrModeV();
  /**
   * @see AddrModeW
   */
  public final AddrMode ADDR_W = new AddrModeW();
  /**
   * @see AddrModeX
   */
  public final AddrMode ADDR_X = new AddrModeX();
  /**
   * @see AddrModeY
   */
  public final AddrMode ADDR_Y = new AddrModeY();
  public final AddrMode ADDR_IMM = new AddrModeImm();
  private final X86RegisterSet registerSet;

  public X86OperandDecoders (X86RegisterSet registerSet) {
    this.registerSet = registerSet;
  }

  /**
   * Direct address: the instruction has no ModR/M byte; the address of the operand is encoded in the instruction.
   * No base register, index register or scaling factor can be applied.
   */
  public class AddrModeA extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      DecodeStream in = ctx.getDecodeStream();
      X86Prefixes prefixes = ctx.getPrefixes();
      int memSz = registerSet.effectiveMemoryAccessSize(prefixes, opndType);
      X86ImmOpnd segment = null;
      X86ImmOpnd offset = null;
      X86Operand op;
      boolean addrSize = !ctx.getPrefixes().contains(Prefix.ADDRSZ);
      switch (opndType) {
      case p_mode:
        if (addrSize) {
          offset = imm32(in);
          segment = imm16(in);
        } else {
          offset = imm16(in);
          segment = imm16(in);
        }
        op = new X86MemOpnd(memSz, addrSize ? (byte) 32 : (byte) 16, segment, offset);
        break;
      case v_mode:
        if (addrSize)
          offset = imm32(in);
        else
          offset = imm16(in);
        op = X86MemOpnd.mk(offset.size(), offset);
        break;
      default:
        throw DecodeException.inconsistentDecoder(ctx);
      }
      return op;
    }
  }

  public class AddrModeImm extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      X86Prefixes prefixes = ctx.getPrefixes();
      X86Operand opnd;
      opnd = new X86ImmOpnd((byte) 8, opndType);
      return opnd;
    }
  }

  /**
   * The reg field of the ModR/M byte selects a control register.
   */
  public class AddrModeC extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      X86Prefixes prefixes = ctx.getPrefixes();
      int regOrOpcode = ctx.getModRM().getRegOrOpcode();
      regOrOpcode = prefixes.contains(Prefix.REXR) ? regOrOpcode + 8 : regOrOpcode;
      return registerSet.getControlRegister(prefixes, regOrOpcode);
    }
  }

  /**
   * The reg field of the ModR/M byte selects a debug register.
   */
  public class AddrModeD extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      X86Prefixes prefixes = ctx.getPrefixes();
      int regOrOpcode = ctx.getModRM().getRegOrOpcode();
      regOrOpcode = prefixes.contains(Prefix.REXR) ? regOrOpcode + 8 : regOrOpcode;
      return registerSet.getDebugRegister(prefixes, regOrOpcode);
    }
  }

  /**
   * A ModR/M byte follows the opcode and specifies the operand. The operand is either a general-purpose register or a
   * memory address. If it is a memory address, the address is computed from segment register and any of the following
   * values: a base register, an index register, a scaling factor, a displacement.
   */
  public class AddrModeE extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      X86Prefixes prefixes = ctx.getPrefixes();
      X86Operand opnd = null;
      int rm = ctx.getModRM().getRm();
      int mod = ctx.getModRM().getMod();
      rm = prefixes.contains(Prefix.REXB) ? rm + 8 : rm;
      if (mod == 3)
        switch (opndType) {
        case b_mode:
          opnd = registerSet.getGPR8(prefixes, rm);
          break;
        case w_mode:
          opnd = registerSet.getGPR16(prefixes, rm);
          break;
        case v_mode:
          opnd = registerSet.getGPR(prefixes, rm);
          break;
        case p_mode:
          X86RegOpnd reg = (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, rm);
          opnd = X86MemOpnd.mk(registerSet.effectiveMemoryAccessSize(ctx.getPrefixes(), opndType), reg);
          break;
        case d_mode:
          opnd = registerSet.getGPR32(prefixes, rm);
          break;
        case q_mode:
          opnd = registerSet.getGPR64(prefixes, rm);
          break;
        default:
          throw DecodeException.inconsistentDecoder(ctx);
        }
      else
        opnd = decodeModRM(ctx, registerSet, opndType);
      return opnd;
    }
  }

  /**
   * EFLAGS/RFLAGS Register.
   */
  public class AddrModeF extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      throw new UnsupportedOperationException("Not supported yet.");
    }
  }

  /**
   * Floating point Register.
   */
  public class AddrModeFreg extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      X86Prefixes prefixes = ctx.getPrefixes();
      int rm = ctx.getModRM().getRm();
      X86Operand opnd;
      switch (opndType) {
      case 0:
        opnd = registerSet.getFloatingPointRegister(prefixes, 0);
        break;
      case 1:
        opnd = registerSet.getFloatingPointRegister(prefixes, rm);
        break;
      default:
        throw DecodeException.inconsistentDecoder(ctx);
      }
      return opnd;
    }
  }

  /**
   * The reg field of the ModR/M bytes selects a general register.
   */
  public class AddrModeG extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      X86Prefixes prefixes = ctx.getPrefixes();
      int regOrOpcode = ctx.getModRM().getRegOrOpcode();
      regOrOpcode = prefixes.contains(Prefix.REXR) ? regOrOpcode + 8 : regOrOpcode;
      X86Operand opnd;
      switch (opndType) {
      case b_mode:
        opnd = registerSet.getGPR8(prefixes, regOrOpcode);
        break;
      case w_mode:
        opnd = registerSet.getGPR16(prefixes, regOrOpcode);
        break;
      case d_mode:
        opnd = registerSet.getGPR32(prefixes, regOrOpcode);
        break;
      case v_mode:
        opnd = registerSet.getGPR(prefixes, regOrOpcode);
        break;
      default:
        throw DecodeException.inconsistentDecoder(ctx);
      }
      return opnd;
    }
  }

  /**
   * Immediate data: the operand value is encoded in subsequent bytes of the instruction.
   */
  public class AddrModeI extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      DecodeStream in = ctx.getDecodeStream();
      X86Operand opnd;
      switch (opndType) {
      case b_mode:
        opnd = signedImm8(8, in);
        break;
      case w_mode:
        opnd = signedImm16(16, in);
        break;
      case v_mode:
        if (ctx.getPrefixes().contains(X86Prefixes.Prefix.OPNDSZ))
          opnd = signedImm16(16, in);
        else
          opnd = signedImm32(32, in);
        break;
      default:
        throw DecodeException.inconsistentDecoder(ctx);
      }
      return opnd;
    }
  }

  /**
   * Immediate data: the operand value is encoded in subsequent bytes of the instruction.
   */
  public class AddrModeI64 extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      DecodeStream in = ctx.getDecodeStream();
      X86Operand opnd;
      switch (opndType) {
      case b_mode:
        opnd = signedImm8(8, in);
        break;
      case w_mode:
        opnd = signedImm16(16, in);
        break;
      case v_mode:
        if (ctx.getPrefixes().contains(X86Prefixes.Prefix.OPNDSZ))
          opnd = signedImm16(16, in);
        else if (ctx.getPrefixes().contains(X86Prefixes.Prefix.REXW))
          opnd = signedImm64(64, in);
        else
          opnd = signedImm32(32, in);
        break;
      default:
        throw DecodeException.inconsistentDecoder(ctx);
      }
      return opnd;
    }
  }

  /**
   * The instruction contains a relative offset to be added to the instruction pointer register.
   */
  public class AddrModeJ extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      DecodeStream in = ctx.getDecodeStream();
      boolean operandSize = ctx.getPrefixes().contains(Prefix.OPNDSZ);
      long disp = 0;
      long startPc = ctx.getStartPc();
      // The effective address is Instruction pointer + relative offset
      switch (opndType) {
      case b_mode:
        disp = in.raw8();
        break;
      case v_mode:
        if (operandSize)
          disp = in.raw16();
        else
          disp = in.raw32();
        break;
      default:
        throw DecodeException.inconsistentDecoder(ctx);
      }
      disp += startPc + in.consumed();
      return new X86ImmOpnd(registerSet.getArchitectureSize(), disp);
    }
  }

  /**
   * The ModR/M byte may refer only to memory (LES, LDS, ...).
   */
  public class AddrModeM extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      return decodeModRM(ctx, registerSet, opndType);
    }
  }

  /**
   * The R/M field of the ModR/M byte selects a packed-quadword, MMX-technology register.
   */
  public class AddrModeN extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      X86Prefixes prefixes = ctx.getPrefixes();
      int rm = ctx.getModRM().getRm();
      rm = prefixes.contains(Prefix.REXR) ? rm + 8 : rm;
      return registerSet.getMMXRegister(prefixes, rm);
    }
  }

  /**
   * The instruction has no ModR/M byte. The offset of the operand is coded as a word or double word (depending
   * on address size attribute) in the instruction. No base register, index register or scaling factor can be applied.
   * (for example, MOV (A0-A3)).
   */
  public class AddrModeO extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      DecodeStream in = ctx.getDecodeStream();
      boolean addressSize = ctx.getPrefixes().contains(Prefix.ADDRSZ);
      X86ImmOpnd offsetOpnd = addressSize ? imm16(in) : imm32(in);
      return X86MemOpnd.mk(offsetOpnd.size(), offsetOpnd);
    }
  }

  /**
   * The reg field of the ModR/M byte selects a packed quadword MMX technology register.
   */
  public class AddrModeP extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      X86Prefixes prefixes = ctx.getPrefixes();
      int regOrOpcode = ctx.getModRM().getRegOrOpcode();
      regOrOpcode = prefixes.contains(Prefix.REXR) ? regOrOpcode + 8 : regOrOpcode;
      return registerSet.getMMXRegister(prefixes, regOrOpcode);
    }
  }

  /**
   * A ModR/M byte follows the opcode and specifies the operand. The operand is either an MMX technology register or a
   * memory address. If it is a memory address, the address is computed for a segment register and any of the following
   * values: a base register, an index register, a scaling factor, and a displacement.
   */
  public class AddrModeQ extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      X86Prefixes prefixes = ctx.getPrefixes();
      int rm = ctx.getModRM().getRm();
      int mod = ctx.getModRM().getMod();
      if (mod == 3)
        return registerSet.getMMXRegister(prefixes, rm);
      return decodeModRM(ctx, registerSet, opndType);
    }
  }

  /**
   * The R/M field of the ModR/M byte may refer only to a general register.
   */
  public class AddrModeR extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      X86Prefixes prefixes = ctx.getPrefixes();
      boolean opndSize = prefixes.contains(Prefix.OPNDSZ);
      boolean rexw = prefixes.contains(Prefix.REXW);
      int rm = ctx.getModRM().getRm();
      rm = prefixes.contains(Prefix.REXR) ? rm + 8 : rm;
      X86Operand op;
      switch (opndType) {
      case b_mode:
        op = registerSet.getGPR8(prefixes, rm);
        break;
      case w_mode:
        op = registerSet.getGPR16(prefixes, rm);
        break;
      case d_mode:
        op = registerSet.getGPR32(prefixes, rm);
        break;
      case v_mode:
        if (rexw)
          op = registerSet.getGPR(prefixes, rm);
        else if (opndSize)
          op = registerSet.getGPR16(prefixes, rm);
        else
          op = registerSet.getGPR32(prefixes, rm);
        break;
      default:
        throw DecodeException.inconsistentDecoder(ctx);
      }
      return op;
    }
  }

  /**
   * The reg field of the ModR/M byte selects a segment register.
   */
  public class AddrModeS extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      return registerSet.getSegmentRegister(ctx.getPrefixes(), ctx.getModRM().getRegOrOpcode());
    }
  }

  /**
   * The reg field of the ModR/M byte selects a test register.
   */
  public class AddrModeT extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * The R/M field of the ModR/M byte selects a 128-bit XMM register.
   */
  public class AddrModeU extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      return registerSet.getXMMRegister(ctx.getPrefixes(), ctx.getModRM().getRm());
    }
  }

  /**
   * The reg field of the ModR/M byte selects a 128-bit XMM register.
   */
  public class AddrModeV extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      X86Prefixes prefixes = ctx.getPrefixes();
      int regOrOpcode = ctx.getModRM().getRegOrOpcode();
      regOrOpcode = prefixes.contains(Prefix.REXR) ? regOrOpcode + 8 : regOrOpcode;
      return registerSet.getXMMRegister(prefixes, regOrOpcode);
    }
  }

  /**
   * A ModR/M byte follows the opcode and specifies the operand. The operand is either a 128-bit XMM register or a
   * memory address. If it is a memory address, the address is computed from a segment register and any of the following
   * values: a base register, an index register, a scaling factor, and a displacement.
   */
  public class AddrModeW extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      X86Prefixes prefixes = ctx.getPrefixes();
      int rm = ctx.getModRM().getRm();
      int mod = ctx.getModRM().getMod();
      if (mod == 3)
        return registerSet.getXMMRegister(prefixes, rm);
      return decodeModRM(ctx, registerSet, opndType);
    }
  }

  /**
   * Memory addressed by the DS:rSI register pair (MOVS, CMPS, ...). The default address size in 64bit mode is 64bit.
   */
  public class AddrModeX extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      // TODO: handle segment prefixes.
      X86Prefixes prefixes = ctx.getPrefixes();
      int size = registerSet.effectiveMemoryAccessSize(prefixes, opndType);
      return X86MemOpnd.mk(size, (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, 6));
    }
  }

  /**
   * Memory addressed by the ES:rDI register pair (MOVS, CMPS, ...). The default address size in 64bit mode is 64bit.
   */
  public class AddrModeY extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      // TODO: handle segment prefixes.
      X86Prefixes prefixes = ctx.getPrefixes();
      int size = registerSet.effectiveMemoryAccessSize(prefixes, opndType);
      return X86MemOpnd.mk(size, (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, 7));
    }
  }

  /**
   * Directly decode the register passed as {@code opndType} parameter to {@code decode()}.
   */
  public class AddrModeReg extends AddrMode {
    @Override public X86Operand decode (X86DecodeCtx ctx, int opndType) {
      X86Prefixes prefixes = ctx.getPrefixes();
      X86Operand opnd;
      int opndOffset = prefixes.contains(Prefix.REXB) ? opndType + 8 : opndType;
      switch (opndType) {
      case EAX:
      case ECX:
      case EDX:
      case EBX:
      case ESP:
      case EBP:
      case ESI:
      case EDI:
        if (prefixes.contains(Prefix.REXW)) {
          opnd = registerSet.getGPR64(prefixes, opndOffset - EAX);
        } else
          opnd = registerSet.getGPR32(prefixes, opndOffset - EAX);
        break;
      case RAX:
      case RCX:
      case RDX:
      case RBX:
      case RSP:
      case RBP:
      case RSI:
      case RDI:
        opnd = registerSet.getGPR64(prefixes, opndOffset - RAX);
        break;
      case AX:
      case CX:
      case DX:
      case BX:
      case SP:
      case BP:
      case SI:
      case DI:
        opnd = registerSet.getGPR16(prefixes, opndOffset - AX);
        break;
      case AL:
      case CL:
      case DL:
      case BL:
      case AH:
      case CH:
      case DH:
      case BH:
        opnd = registerSet.getGPR8(prefixes, opndOffset - AL);
        break;
      case ES:  // ES, CS, SS, DS, FS, GS
      case CS:
      case SS:
      case DS:
      case FS:
      case GS:
        opnd = registerSet.getSegmentRegister(prefixes, opndOffset - ES);
        break;
      default:
        throw DecodeException.inconsistentDecoder(ctx);
      }
      return opnd;
    }
  }

  public static abstract class AddrMode {
    public abstract X86Operand decode (X86DecodeCtx ctx, int opndType);

    protected X86ImmOpnd imm8 (final DecodeStream in) {
      return new X86ImmOpnd((byte) 8, in.read8());
    }

    protected X86ImmOpnd imm16 (final DecodeStream in) {
      return new X86ImmOpnd((byte) 16, in.read16());
    }

    protected X86ImmOpnd imm32 (final DecodeStream in) {
      return new X86ImmOpnd((byte) 32, in.read32());
    }

    protected X86ImmOpnd signedImm8 (final int size, final DecodeStream in) {
      return new X86ImmOpnd((byte) size, in.raw8());
    }

    protected X86ImmOpnd signedImm16 (final int size, final DecodeStream in) {
      return new X86ImmOpnd((byte) size, in.raw16());
    }

    protected X86ImmOpnd signedImm32 (final int size, final DecodeStream in) {
      return new X86ImmOpnd((byte) size, in.raw32());
    }

    protected X86ImmOpnd signedImm64 (final int size, final DecodeStream in) {
      return new X86ImmOpnd((byte) size, in.raw64());
    }

    public X86Operand decodeModRM (X86DecodeCtx ctx, X86RegisterSet registerSet, int opndType) {
      X86Prefixes prefixes = ctx.getPrefixes();
      X86Operand opnd = null;
      DecodeStream in = ctx.getDecodeStream();
      X86ModRM modRM = ctx.getModRM();
      int rm = modRM.getRm();
      int mod = modRM.getMod();
      int memSize = registerSet.effectiveMemoryAccessSize(ctx.getPrefixes(), opndType);
      int addrSize = registerSet.effectiveAddressSize(ctx.getPrefixes(), opndType);
      // mod != 3
      // SIB follows for (rm==4), SIB gives scale, index and base in this case
      // disp32 is present for (mod==0 && rm==5) || (mod==2)
      // disp8 is present for (mod==1)
      // for (rm!=4) base is register at rm.
      int scale = 0;
      int index = 0;
      int base = 0;
      long disp = 0;
      if (rm == 4) { // Sib requested
        X86Sib sib = X86Sib.decode(in);
        scale = sib.getScale();
        index = sib.getIndex();
        base = sib.getBase();
        index = (prefixes.contains(Prefix.REXX) ? index + 8 : index);
        base = (prefixes.contains(Prefix.REXB) ? base + 8 : base);
      } else
        // no Sib
        rm = (prefixes.contains(Prefix.REXB) ? rm + 8 : rm);
      switch (mod) {
      case 3:
        throw DecodeException.inconsistentDecoder(ctx);
      case 0: {
        switch (rm) {
        case 4: {
          if (base == 5) {
            disp = in.read32();
            if (index != 4) {
              X86RegOpnd indexOpnd = (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, index);
              X86ImmOpnd dispOpnd = new X86ImmOpnd(addrSize, disp);
              opnd = X86MemOpnd.mk(memSize, indexOpnd, (byte) scale, dispOpnd);
            } else {
              X86ImmOpnd dispOpnd = new X86ImmOpnd(addrSize, disp);
              opnd = X86MemOpnd.mk(memSize, dispOpnd);
            }
          } else if (index != 4) {
            X86RegOpnd baseOpnd = (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, base);
            X86RegOpnd indexOpnd = (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, index);
            opnd = X86MemOpnd.mk(memSize, baseOpnd, indexOpnd, (byte) scale);
          } else {
            X86RegOpnd baseOpnd = (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, rm);
            opnd = X86MemOpnd.mk(memSize, baseOpnd);
          }
          break;
        }
        case 5: {
          disp = in.read32();
          // In 64bit mode only, this displacement is "rip" relative.
          if (registerSet.is64BitArchitecture())
            disp += ctx.getStartPc() + in.consumed();
          X86ImmOpnd dispOpnd = new X86ImmOpnd(addrSize, disp);
          opnd = X86MemOpnd.mk(memSize, dispOpnd);
          break;
        }
        default: {
          base = rm;
          X86RegOpnd baseOpnd = (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, base);
          opnd = X86MemOpnd.mk(memSize, baseOpnd);
          break;
        }
        }
        break;
      }
      case 1: {
        disp = (byte) in.read8();
        if (rm != 4) {
          base = rm;
          X86RegOpnd baseOpnd = (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, base);
          X86ImmOpnd dispOpnd = new X86ImmOpnd(addrSize, disp);
          opnd = X86MemOpnd.mk(memSize, baseOpnd, dispOpnd);
        } else if (index != 4) {
          X86RegOpnd baseOpnd = (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, base);
          X86RegOpnd indexOpnd = (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, index);
          X86ImmOpnd dispOpnd = new X86ImmOpnd(addrSize, disp);
          opnd = X86MemOpnd.mk(memSize, baseOpnd, indexOpnd, (byte) scale, dispOpnd);
        } else {
          X86RegOpnd baseOpnd = (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, base);
          X86ImmOpnd dispOpnd = new X86ImmOpnd(addrSize, disp);
          opnd = X86MemOpnd.mk(memSize, baseOpnd, dispOpnd);
        }
        break;
      }
      case 2: {
        disp = in.read32();
        if (rm != 4) {
          base = rm;
          X86RegOpnd baseOpnd = (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, base);
          X86ImmOpnd dispOpnd = new X86ImmOpnd(addrSize, disp);
          opnd = X86MemOpnd.mk(memSize, baseOpnd, dispOpnd);
        } else if (index != 4) {
          X86RegOpnd baseOpnd = (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, base);
          X86RegOpnd indexOpnd = (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, index);
          X86ImmOpnd dispOpnd = new X86ImmOpnd(addrSize, disp);
          opnd = X86MemOpnd.mk(memSize, baseOpnd, indexOpnd, (byte) scale, dispOpnd);
        } else {
          X86RegOpnd baseOpnd = (X86RegOpnd) registerSet.getGPRAsAddressPart(prefixes, base);
          X86ImmOpnd dispOpnd = new X86ImmOpnd(addrSize, disp);
          opnd = X86MemOpnd.mk(memSize, baseOpnd, dispOpnd);
        }
        break;
      }
      }
      return opnd;
    }
  }
}
