package bindis.x86.x32;

import static bindis.x86.common.X86OperandDecoders.INVALID_OPERANDTYPE;
import static bindis.x86.common.X86OperandDecoders.a_mode;
import static bindis.x86.common.X86OperandDecoders.b_mode;
import static bindis.x86.common.X86OperandDecoders.d_mode;
import static bindis.x86.common.X86OperandDecoders.dq_mode;
import static bindis.x86.common.X86OperandDecoders.p_mode;
import static bindis.x86.common.X86OperandDecoders.pd_mode;
import static bindis.x86.common.X86OperandDecoders.pi_mode;
import static bindis.x86.common.X86OperandDecoders.ps_mode;
import static bindis.x86.common.X86OperandDecoders.q_mode;
import static bindis.x86.common.X86OperandDecoders.s_mode;
import static bindis.x86.common.X86OperandDecoders.sd_mode;
import static bindis.x86.common.X86OperandDecoders.si_mode;
import static bindis.x86.common.X86OperandDecoders.ss_mode;
import static bindis.x86.common.X86OperandDecoders.v_mode;
import static bindis.x86.common.X86OperandDecoders.w_mode;

import bindis.x86.common.X86ControlRegisters;
import bindis.x86.common.X86DebugRegisters;
import bindis.x86.common.X86FloatRegisters;
import bindis.x86.common.X86MMXRegisters;
import bindis.x86.common.X86Operand;
import bindis.x86.common.X86Prefixes;
import bindis.x86.common.X86RegisterSet;
import bindis.x86.common.X86SegmentRegisters;
import bindis.x86.common.X86XMMRegisters;
import bindis.x86.common.X86Prefixes.Prefix;

/**
 * X86 32bit register set implementation.
 *
 * @author mb0
 */
public class X32RegisterSet implements X86RegisterSet {
  public static final X32RegisterSet $ = new X32RegisterSet();

  private X32RegisterSet () {
  }

  @Override public X86Operand getGPR8 (X86Prefixes prefix, int n) {
    return X32Registers.getRegister8(n);
  }

  @Override public X86Operand getGPR16 (X86Prefixes prefix, int n) {
    return X32Registers.getRegister16(n);
  }

  @Override public int getArchitectureSize () {
    return 32;
  }

  @Override public X86Operand getGPR32 (X86Prefixes prefix, int n) {
    return X32Registers.getRegister32(n);
  }

  @Override public X86Operand getGPR64 (X86Prefixes prefixes, int n) {
    throw new UnsupportedOperationException();
  }

  @Override public X86Operand getGPR (X86Prefixes prefix, int n) {
    // TODO: What about the operand-size prefix?
    if (prefix.contains(Prefix.OPNDSZ))
      return X32Registers.getRegister16(n);
    return X32Registers.getRegister32(n);
  }

  @Override public X86Operand getGPRAsAddressPart (X86Prefixes prefixes, int n) {
    boolean addrSz = prefixes.contains(X86Prefixes.Prefix.ADDRSZ);
    if (addrSz)
      return X32Registers.getRegister16(n);
    return X32Registers.getRegister32(n);
  }

  @Override public X86Operand getXMMRegister (X86Prefixes prefix, int n) {
    return X86XMMRegisters.getRegister(n);
  }

  @Override public X86Operand getMMXRegister (X86Prefixes prefix, int n) {
    return X86MMXRegisters.getRegister(n);
  }

  @Override public X86Operand getSegmentRegister (X86Prefixes prefix, int n) {
    return X86SegmentRegisters.getSegmentRegister(n);
  }

  @Override public X86Operand getFloatingPointRegister (X86Prefixes prefixes, int n) {
    return X86FloatRegisters.getRegister(n);
  }

  @Override public X86Operand getControlRegister (X86Prefixes prefixes, int n) {
    return X86ControlRegisters.getRegister(n);
  }

  @Override public X86Operand getDebugRegister (X86Prefixes prefixes, int n) {
    return X86DebugRegisters.getRegister(n);
  }

  @Override public boolean is64BitArchitecture () {
    return false;
  }

  @Override public int effectiveMemoryAccessSize (X86Prefixes prefix, int opndType) {
    boolean operandSize = prefix.contains(X86Prefixes.Prefix.OPNDSZ);
    boolean addressSize = prefix.contains(X86Prefixes.Prefix.ADDRSZ);
    switch (opndType) {
      case b_mode:
        return 8;
      case a_mode:
      case v_mode:
        return chooseSz(operandSize, 16, 32);
      case w_mode:
        return 16;
      case d_mode:
        return 32;
      case p_mode:
        return chooseSz(operandSize, 32, 48);
      case dq_mode:
        return 128;
      case ps_mode:
        return 128;
      case pd_mode:
        return 128;
      case q_mode:
      case sd_mode:
        return 64;
      case ss_mode:
        return 32;
      case pi_mode:
      case s_mode:
      case si_mode:
      case INVALID_OPERANDTYPE:
      default:
        throw new UnsupportedOperationException();
    }
  }

  @Override public int effectiveAddressSize (X86Prefixes prefix, int opndType) {
    int size = getArchitectureSize();
    switch (opndType) {
      case a_mode:
      case v_mode:
        size = prefix.contains(Prefix.ADDRSZ) ? 16 : 32;
        break;
    }
    return size;
  }

  private static int chooseSz (boolean operandSz, int ifTrue, int ifFalse) {
    return operandSz ? ifTrue : ifFalse;
  }
}
