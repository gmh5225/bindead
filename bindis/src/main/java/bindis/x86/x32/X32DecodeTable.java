package bindis.x86.x32;

import static bindis.x86.common.X86Consts.EAX;
import static bindis.x86.common.X86Consts.EBP;
import static bindis.x86.common.X86Consts.EBX;
import static bindis.x86.common.X86Consts.ECX;
import static bindis.x86.common.X86Consts.EDI;
import static bindis.x86.common.X86Consts.EDX;
import static bindis.x86.common.X86Consts.ESI;
import static bindis.x86.common.X86Consts.ESP;
import static bindis.x86.common.X86OperandDecoders.b_mode;
import static bindis.x86.common.X86OperandDecoders.v_mode;
import static bindis.x86.common.X86OperandDecoders.w_mode;

import bindis.x86.common.X86DecodeTable;
import bindis.x86.common.X86OperandDecoders;

/**
 * X86 32bit decode table.
 *
 * @author mb0
 */
public class X32DecodeTable extends X86DecodeTable {
  public X32DecodeTable () {
    super(new X86OperandDecoders(X32RegisterSet.$), new X32InstructionFactory());
    twoByteOpcodeGroupTable[12][2] = decode("call", Mode.ADDR_E, v_mode);
    twoByteOpcodeGroupTable[12][4] = decode("jmp", Mode.ADDR_E, v_mode);
    oneByteOpcodeTable[0xe3] = decode("jC32cxz", Mode.ADDR_J, b_mode);
    oneByteOpcodeTable[0x63] = decode("arpl", Mode.ADDR_E, w_mode, Mode.ADDR_G, w_mode);
    /* 40 */
    oneByteOpcodeTable[0x40] = decode("inc", Mode.ADDR_REG, EAX);
    oneByteOpcodeTable[0x41] = decode("inc", Mode.ADDR_REG, ECX);
    oneByteOpcodeTable[0x42] = decode("inc", Mode.ADDR_REG, EDX);
    oneByteOpcodeTable[0x43] = decode("inc", Mode.ADDR_REG, EBX);
    oneByteOpcodeTable[0x44] = decode("inc", Mode.ADDR_REG, ESP);
    oneByteOpcodeTable[0x45] = decode("inc", Mode.ADDR_REG, EBP);
    oneByteOpcodeTable[0x46] = decode("inc", Mode.ADDR_REG, ESI);
    oneByteOpcodeTable[0x47] = decode("inc", Mode.ADDR_REG, EDI);
    /* 48 */
    oneByteOpcodeTable[0x48] = decode("dec", Mode.ADDR_REG, EAX);
    oneByteOpcodeTable[0x49] = decode("dec", Mode.ADDR_REG, ECX);
    oneByteOpcodeTable[0x4A] = decode("dec", Mode.ADDR_REG, EDX);
    oneByteOpcodeTable[0x4B] = decode("dec", Mode.ADDR_REG, EBX);
    oneByteOpcodeTable[0x4C] = decode("dec", Mode.ADDR_REG, ESP);
    oneByteOpcodeTable[0x4D] = decode("dec", Mode.ADDR_REG, EBP);
    oneByteOpcodeTable[0x4E] = decode("dec", Mode.ADDR_REG, ESI);
    oneByteOpcodeTable[0x4F] = decode("dec", Mode.ADDR_REG, EDI);
    /* 50 */
    oneByteOpcodeTable[0x50] = decode("push", Mode.ADDR_REG, EAX);
    oneByteOpcodeTable[0x51] = decode("push", Mode.ADDR_REG, ECX);
    oneByteOpcodeTable[0x52] = decode("push", Mode.ADDR_REG, EDX);
    oneByteOpcodeTable[0x53] = decode("push", Mode.ADDR_REG, EBX);
    oneByteOpcodeTable[0x54] = decode("push", Mode.ADDR_REG, ESP);
    oneByteOpcodeTable[0x55] = decode("push", Mode.ADDR_REG, EBP);
    oneByteOpcodeTable[0x56] = decode("push", Mode.ADDR_REG, ESI);
    oneByteOpcodeTable[0x57] = decode("push", Mode.ADDR_REG, EDI);
    /* 58 */
    oneByteOpcodeTable[0x58] = decode("pop", Mode.ADDR_REG, EAX);
    oneByteOpcodeTable[0x59] = decode("pop", Mode.ADDR_REG, ECX);
    oneByteOpcodeTable[0x5A] = decode("pop", Mode.ADDR_REG, EDX);
    oneByteOpcodeTable[0x5B] = decode("pop", Mode.ADDR_REG, EBX);
    oneByteOpcodeTable[0x5C] = decode("pop", Mode.ADDR_REG, ESP);
    oneByteOpcodeTable[0x5D] = decode("pop", Mode.ADDR_REG, EBP);
    oneByteOpcodeTable[0x5E] = decode("pop", Mode.ADDR_REG, ESI);
    oneByteOpcodeTable[0x5F] = decode("pop", Mode.ADDR_REG, EDI);
  }
}
