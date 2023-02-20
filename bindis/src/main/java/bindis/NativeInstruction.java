package bindis;

import rreil.disassembler.OpcodeFormatter;

/**
 * Abstract base class modeling decoded native instructions.
 *
 * @author mb0
 */
public abstract class NativeInstruction implements INativeInstruction {
  private final long address;
  private final String mnemonic;
  private final byte[] opcode;
  private final Operand[] opnds;

  public NativeInstruction (long address, String mnemonic, byte[] opcode,
      Operand... opnds) {
    this.mnemonic = mnemonic;
    this.opcode = opcode;
    this.address = address;
    int cnt;
    // Filter null opnds.
    for (cnt = 0; cnt < opnds.length; cnt++) {
      Operand opnd = opnds[cnt];
      if (opnd == null)
        break;
    }
    this.opnds = new Operand[cnt];
    System.arraycopy(opnds, 0, this.opnds, 0, cnt);
  }

  @Override public String mnemonic () {
    return mnemonic;
  }

  @Override public byte[] opcode () {
    final byte[] data = new byte[opcode.length];
    System.arraycopy(opcode, 0, data, 0, opcode.length);
    return data;
  }

  @Override public long address () {
    return address;
  }

  @Override public Operand operand (int idx) {
    return opnds[idx];
  }

  @Override public int numberOfOperands () {
    return opnds.length;
  }

  @Override public Operand[] operands () {
    final Operand[] data = new Operand[opnds.length];
    System.arraycopy(opnds, 0, data, 0, opnds.length);
    return data;
  }

  @Override public int length () {
    return opcode.length;
  }

  @Override public String toString () {
    final StringBuilder pretty = new StringBuilder();
    pretty.append('{');
    address(pretty).append(": ");
    opcode(pretty).append(" -- ");
    asString(pretty);
    return pretty.append('}').toString();
  }

  @Override public StringBuilder asString (StringBuilder pretty) {
    pretty.append(mnemonic());
    pretty.append(" ");
    for (int i = 0; i < opnds.length; i++) {
      if (i != 0)
        pretty.append(", ");
      opnds[i].asString(pretty);
    }
    return pretty;
  }

  @Override public StringBuilder opcode (StringBuilder buf) {
    return OpcodeFormatter.format(opcode, buf);
  }

  @Override public StringBuilder address (StringBuilder buf) {
    buf.append(String.format("%08x", address));
    return buf;
  }
}
