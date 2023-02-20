package bindis.x86.common;

import static bindis.x86.common.X86OperandDecoders.INVALID_OPERANDTYPE;

import bindis.NativeInstruction;
import bindis.x86.common.X86OperandDecoders.AddrMode;

// basic instruction decoder class
public abstract class X86InstructionDecoder {
  protected String name;
  protected final AddrMode addrMode1;
  protected final int operandType1;
  protected final AddrMode addrMode2;
  protected final int operandType2;
  protected final AddrMode addrMode3;
  protected final int operandType3;

  public X86InstructionDecoder (String name) {
    this(name, null, INVALID_OPERANDTYPE, null, INVALID_OPERANDTYPE, null, INVALID_OPERANDTYPE);
  }

  public X86InstructionDecoder (String name, AddrMode addrMode1, int operandType1) {
    this(name, addrMode1, operandType1, null, INVALID_OPERANDTYPE, null, INVALID_OPERANDTYPE);
  }

  public X86InstructionDecoder (String name, AddrMode addrMode1, int operandType1, AddrMode addrMode2, int operandType2) {
    this(name, addrMode1, operandType1, addrMode2, operandType2, null, INVALID_OPERANDTYPE);
  }

  public X86InstructionDecoder (String name, AddrMode addrMode1, int operandType1, AddrMode addrMode2, int operandType2, AddrMode addrMode3, int operandType3) {
    this.name = name;
    this.operandType1 = operandType1;
    this.operandType2 = operandType2;
    this.operandType3 = operandType3;
    this.addrMode1 = addrMode1;
    this.addrMode2 = addrMode2;
    this.addrMode3 = addrMode3;
  }

  public NativeInstruction decode (final X86DecodeCtx ctx) {
    final X86InstructionDecoder decoder = lookupDecoder(ctx);
    if (decoder != null)
      return decoder.decodeInstruction(ctx);
    return null;
  }

  protected abstract NativeInstruction decodeInstruction (final X86DecodeCtx ctx);

  protected X86InstructionDecoder lookupDecoder (final X86DecodeCtx ctx) {
    return this;
  }

  // "operand1"
  protected final X86Operand decodeOpnd1 (final X86DecodeCtx ctx) {
    if ((addrMode1 != null) && (operandType1 != INVALID_OPERANDTYPE))
      return addrMode1.decode(ctx, operandType1);
    return null;
  }

  // "operand2"
  protected final X86Operand decodeOpnd2 (final X86DecodeCtx ctx) {
    if ((addrMode2 != null) && (operandType2 != INVALID_OPERANDTYPE))
      return addrMode2.decode(ctx, operandType2);
    return null;
  }

  // "operand3"
  protected final X86Operand decodeOpnd3 (final X86DecodeCtx ctx) {
    if ((addrMode3 != null) && (operandType3 != INVALID_OPERANDTYPE))
      return addrMode3.decode(ctx, operandType3);
    return null;
  }
}
