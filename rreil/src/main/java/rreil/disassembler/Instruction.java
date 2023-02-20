package rreil.disassembler;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import rreil.disassembler.translators.common.TranslationException;
import rreil.lang.RReilAddr;
import rreil.lang.lowlevel.LowLevelRReil;

/**
 * Instruction representation as expression trees.
 *
 * Similar to IDA-Pro and BinNavi instruction-trees an instruction consists of an address, mnemonic, opcode and a list
 * of {@link OperandTree}s.
 */
public abstract class Instruction {
  private final RReilAddr address;
  private final String mnemonic;
  private final List<OperandTree> opnds;
  private final byte[] opcode;

  protected Instruction (long address, byte[] opcode, String mnemonic, List<OperandTree> opnds) {
    this(RReilAddr.valueOf(address), opcode, mnemonic, opnds);
  }

  protected Instruction (RReilAddr address, byte[] opcode, String mnemonic, OperandTree... opnds) {
    this(address, opcode, mnemonic, Arrays.asList(opnds));
  }

  protected Instruction (RReilAddr address, byte[] opcode, String mnemonic, List<OperandTree> opnds) {
    this.opcode = opcode;
    this.mnemonic = mnemonic;
    this.opnds = Collections.unmodifiableList(opnds);
    this.address = address;
  }

  /**
   * Return this instruction's mnemonic.
   *
   * @return
   */
  public String mnemonic () {
    return mnemonic;
  }

  /**
   * Return this instruction's operands.
   *
   * @return
   */
  public List<OperandTree> operands () {
    return opnds;
  }

  /**
   * Return the {@code i}th operand of this instruction.
   *
   * @param i The operand index.
   * @return
   */
  public OperandTree operand (int i) {
    return opnds.get(i);
  }

  /**
   * {@inheritDoc}
   */
  @Override public String toString () {
    StringBuilder pretty = new StringBuilder("{");
    pretty.append(address).append(": ");
    asString(pretty);
    return pretty.append('}').toString();
  }

  /**
   * Render this instruction's opcode to the given string-builder.
   *
   * @param buf The string-builder.
   * @return The updated string-builder.
   */
  public StringBuilder opcode (StringBuilder buf) {
    return OpcodeFormatter.format(opcode, buf);
  }

  /**
   * Render this instruction's mnemonic and operands to {@code buf}.
   *
   * @param buf The string-builder to pretty print into.
   * @return The updated string-builder.
   */
  public StringBuilder asString (StringBuilder buf) {
    buf.append(mnemonic).append(' ');
    for (OperandTree t : opnds) {
      t.getRoot().asString(buf).append(' ');
    }
    return buf;
  }

  /**
   * Return this instructions opcode length (in bytes).
   */
  public int length () {
    return opcode.length;
  }

  /**
   * Return a copy of this instruction's opcodes.
   */
  public byte[] opcode () {
    final byte[] data = new byte[opcode.length];
    System.arraycopy(opcode, 0, data, 0, opcode.length);
    return data;
  }

  /**
   * Return {@code this} instructions base-address.
   */
  public long baseAddress () {
    return address.base();
  }

  /**
   * Return {@code this} instructions address.
   */
  public RReilAddr address () {
    return address;
  }

  /**
   * Translate this instruction to a (list of) RREIL instruction(s). This abstract methods needs to be provided by
   * the underlying platform.
   *
   * @return The semantic equivalent of {@code this} instruction in RREIL.
   * @throws TranslationException
   */
  public abstract List<LowLevelRReil> toRReilInstructions () throws TranslationException;

  public static interface InstructionFactory {
    public Instruction build (long address, byte[] opcode, String mnemonic, List<OperandTree> opnds);
  }

  /**
   * Builder class for {@link rreil.disassembler.generic.tree.Instruction}s.
   */
  public static class InstructionBuilder {
    private final InstructionFactory instructionFactory;
    private final List<OperandTree> operands = new LinkedList<OperandTree>();
    private byte[] opcode = new byte[]{};
    private String mnemonic;
    private long address;

    /**
     * Build a fresh instruction builder with {@code address} as instruction address.
     *
     * @param address
     */
    public InstructionBuilder (InstructionFactory factory) {
      this.instructionFactory = factory;
    }

    /**
     * Set the address of this instruction.
     *
     * @param address
     * @return The updated builder state.
     */
    public InstructionBuilder address (long address) {
      this.address = address;
      return this;
    }

    /**
     * Set the mnemonic of this instruction.
     *
     * @param mnemonic
     * @return The updated builder state.
     */
    public InstructionBuilder mnemonic (String mnemonic) {
      this.mnemonic = mnemonic;
      return this;
    }

    /**
     * Set {@code opcode} as this instruction's opcode.
     *
     * @param opcode
     * @return The updated builder state.
     */
    public InstructionBuilder opcode (byte[] opcode) {
      this.opcode = opcode;
      return this;
    }

    /**
     * Build an empty opcode.
     *
     * @return The updated builder state.
     */
    public InstructionBuilder opcode () {
      this.opcode = new byte[]{};
      return this;
    }

    /**
     * Add {@code opnd} to this operand list.
     *
     * @param opnd
     * @return The updated builder state.
     */
    public InstructionBuilder link (OperandTree opnd) {
      this.operands.add(opnd);
      return this;
    }

    /**
     * Append all operands in {@code opnds} to this operand list.
     *
     * @param opnds
     * @return The updated builder state.
     */
    public InstructionBuilder link (List<OperandTree> opnds) {
      this.operands.addAll(opnds);
      return this;
    }

    /**
     * Build an instruction from this builder state.
     *
     * @return The build instruction.
     */
    public Instruction build () {
      assert (mnemonic != null) : "Null mnemonic";
      assert (opcode != null) : "Invalid instruction length";
      return instructionFactory.build(address, opcode, mnemonic, operands);
    }

    /**
     * {@inheritDoc}
     */
    @Override public String toString () {
      return "InstructionBuilder{" +
          "address=" + address +
          ", opcode=" + opcode +
          ", mnemonic=" + mnemonic +
          ", operands=" + operands + '}';
    }
  }
}
