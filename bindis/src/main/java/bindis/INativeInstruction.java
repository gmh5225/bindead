package bindis;

import rreil.disassembler.Instruction;

public interface INativeInstruction {
  /**
   * Convert this instruction to a generic tree-representation.
   *
   * @return This instruction represented as expression-tree.
   */
  public Instruction toTreeInstruction();

  /**
   * TODO: see if removable
   * Get this instruction's architecture string.
   */
  public abstract String architecture();

  public String mnemonic();

  public byte[] opcode();

  public long address();

  public IOperand operand(int idx);

  public int numberOfOperands();

  public IOperand[] operands();

  /**
   * Total length in byte of operands + opcode. Usually variable length on CISC
   * architectures and fixed size on RISC machines.
   *
   * {@codce length() == opcode.length}
   *
   * @return Encoded instruction length.
   */
  public int length();

  /**
   * Render the mnemonic and all operands into the given {@code StringBuilder}
   * buffer.
   *
   * @param pretty
   *          The destination buffer.
   * @return The updated buffer.
   */
  public StringBuilder asString(StringBuilder pretty);

  /**
   * Render this {@code opcode} to the given string-builder.
   *
   * @param buf
   *          The string-builder.
   * @return
   */
  public StringBuilder opcode (StringBuilder buf);

  /**
   * Render this instructions address to the given string-builder.
   *
   * @param buf
   *          The string-builder.
   * @return
   */
  public StringBuilder address (StringBuilder buf);
}
