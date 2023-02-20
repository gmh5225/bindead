package bindis.x86.x64;

import bindis.x86.common.X86DecodeTable;
import bindis.x86.common.X86OperandDecoders;

/**
 * X86 64bit decode table.
 *
 * @author mb0
 */
public class X64DecodeTable extends X86DecodeTable {
  public X64DecodeTable () {
    super(new X86OperandDecoders(X64RegisterSet.$), new X64InstructionFactory());
  }
}
