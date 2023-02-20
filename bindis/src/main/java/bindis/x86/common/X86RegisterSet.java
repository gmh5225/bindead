package bindis.x86.common;

/**
 * Definition of access to x86 registers.
 *
 * @author mb
 */
public interface X86RegisterSet {
  public X86Operand getGPR8 (X86Prefixes prefix, int n);

  public X86Operand getGPR16 (X86Prefixes prefix, int n);

  public X86Operand getGPR32 (X86Prefixes prefix, int n);

  public X86Operand getGPR64 (X86Prefixes prefixes, int n);

  public X86Operand getGPR (X86Prefixes prefixes, int n);

  public X86Operand getGPRAsAddressPart (X86Prefixes prefixes, int n);

  public X86Operand getXMMRegister (X86Prefixes prefix, int n);

  public X86Operand getMMXRegister (X86Prefixes prefix, int n);

  public X86Operand getSegmentRegister (X86Prefixes prefix, int n);

  public int getArchitectureSize ();

  public int effectiveMemoryAccessSize (X86Prefixes prefix, int operandType);

  public int effectiveAddressSize (X86Prefixes prefix, int operandType);

  public X86Operand getFloatingPointRegister (X86Prefixes prefixes, int n);

  public X86Operand getControlRegister (X86Prefixes prefixes, int regOrOpcode);

  public X86Operand getDebugRegister (X86Prefixes prefixes, int regOrOpcode);

  public boolean is64BitArchitecture ();
}
