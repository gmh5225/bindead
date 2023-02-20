package rreil.disassembler;

import java.util.Formatter;

/**
 * Pretty print opcodes.
 *
 * @author mb0
 */
public class OpcodeFormatter {
  /**
   * Render {@code opcode} into the given string-builder.
   *
   * @param opcode The opcode.
   * @param buf The string-builder.
   * @return The updated string-builder.
   */
  public static StringBuilder format (byte[] opcode, StringBuilder buf) {
    Formatter fmt = new Formatter(buf);
    if (opcode == null || opcode.length == 0) {
      fmt.close();
      return buf;
    }
    for (int i = 0; i < opcode.length - 1; i++) {
      fmt.format("%02x ", opcode[i]);
    }
    fmt.format("%02x", opcode[opcode.length - 1]);
    fmt.close();
    return buf;
  }
}
