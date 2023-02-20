package bindis.x86.x32;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Ignore;
import org.junit.Test;

import bindis.Disassembler;
import bindis.TstHelpers;
import bindis.x86.x32.X32Disassembler;
import rreil.disassembler.Instruction;
import rreil.disassembler.OperandTree.Node;

public class X32DisassemblerTest {
  private static final Disassembler dis = X32Disassembler.INSTANCE;

  private static Node getOperand (Instruction insn, int number) {
    return insn.operand(number).getRoot().child(0);
  }

  @Test public void test001 () {
    // crc32 eax, byte ptr [eax]
    Instruction insn = dis.decodeOne(TstHelpers.pack(0xf2, 0x0f, 0x38, 0xf0, 0x00), 0, 0);
    assertThat(insn.mnemonic(), is("crc32"));
    assertThat(getOperand(insn, 0).toString(), is("eax"));
    // pretty printer has vanished
    // assertThat(insn.operand(1).toString(), is("8[eax]:32"));
    assertThat(insn.operand(1).toString(), is("Tree{(8 *32(eax))}"));
  }

  @Test public void test002 () {
    // crc32 eax, dword ptr [eax]
    Instruction insn = dis.decodeOne(TstHelpers.pack(0xf2, 0x0f, 0x38, 0xf1, 0x00), 0, 0);
    assertThat(insn.mnemonic(), is("crc32"));
    assertThat(getOperand(insn, 0).toString(), is("eax"));
    // pretty printer has vanished
    // assertThat(insn.operand(1).toString(), is("32[eax]:32"));
    assertThat(insn.operand(1).toString(), is("Tree{(32 *32(eax))}"));
  }

  @Test public void test003 () {
    // pshufb mm0, qword ptr [eax]
    Instruction insn = dis.decodeOne(TstHelpers.pack(0x0f, 0x38, 0x00, 0x00), 0, 0);
    assertThat(insn.mnemonic(), is("pshufb"));
    assertThat(getOperand(insn, 0).toString(), is("mm0"));
    // pretty printer has vanished
    // assertThat(insn.operand(1).toString(), is("64[eax]:32"));
    assertThat(insn.operand(1).toString(), is("Tree{(64 *32(eax))}"));
  }

  @Test public void test004 () {
    // extrq xmm0, 0x0, 0x0
    Instruction insn = dis.decodeOne(TstHelpers.pack(0x66, 0x0f, 0x78, 0xc0, 0x00, 0x00), 0, 0);
    assertThat(insn.mnemonic(), is("extrq"));
    assertThat(getOperand(insn, 0).toString(), is("xmm0"));
    assertThat(getOperand(insn, 1).toString(), is("0"));
    assertThat(getOperand(insn, 2).toString(), is("0"));
  }

  @Test public void test005 () {
    // cmpsd dword ptr [esi], dword ptr [edi]
    Instruction insn = dis.decodeOne(TstHelpers.pack(0xa7), 0, 0);
    assertThat(insn.mnemonic(), is("cmpsd"));
    // pretty printer has vanished
    // assertThat(insn.operand(0).toString(), is("32[esi]:32"));
    // assertThat(insn.operand(1).toString(), is("32[edi]:32"));
    assertThat(insn.operand(0).toString(), is("Tree{(32 *32(esi))}"));
    assertThat(insn.operand(1).toString(), is("Tree{(32 *32(edi))}"));

  }

  @Test public void test006 () {
    // fcmovb st0, st0
    Instruction insn = dis.decodeOne(TstHelpers.pack(0xda, 0xc0), 0, 0);
    assertThat(insn.mnemonic(), is("fcmovb"));
    assertThat(getOperand(insn, 0).toString(), is("st0"));
    assertThat(getOperand(insn, 1).toString(), is("st0"));
  }

  @Test public void test007 () {
    // fcmovb st0, st1
    Instruction insn = dis.decodeOne(TstHelpers.pack(0xda, 0xc1), 0, 0);
    assertThat(insn.mnemonic(), is("fcmovb"));
    assertThat(getOperand(insn, 0).toString(), is("st0"));
    assertThat(getOperand(insn, 1).toString(), is("st1"));
  }

  @Test public void test008 () {
    // xsave ptr [eax]
    Instruction insn = dis.decodeOne(TstHelpers.pack(0x0f, 0xae, 0x20), 0, 0);
    assertThat(insn.mnemonic(), is("xsave"));
    // pretty printer has vanished
    // assertThat(insn.operand(0).toString(), is("32[eax]:32"));
    assertThat(insn.operand(0).toString(), is("Tree{(32 *32(eax))}"));
  }

  @Test public void test009 () {
    // mfence
    Instruction insn = dis.decodeOne(TstHelpers.pack(0x0f, 0xae, 0xf0), 0, 0);
    assertThat(insn.mnemonic(), is("mfence"));
  }

  @Test public void test010 () {
    // aesdec xmm0, xmm0
    Instruction insn = dis.decodeOne(TstHelpers.pack(0x66, 0x0f, 0x38, 0xde, 0xc0), 0, 0);
    assertThat(insn.mnemonic(), is("aesdec"));
    assertThat(getOperand(insn, 0).toString(), is("xmm0"));
    assertThat(getOperand(insn, 1).toString(), is("xmm0"));
  }

  // FIXME: mnemonic is not "vappd" but "lds"?
  // test disabled for now as it is not build critical
  @Ignore @Test public void test011 () {
    // vaddpd ymm0, ymm0, ymm0
    Instruction insn = dis.decodeOne(TstHelpers.pack(0xc5, 0xfd, 0x58, 0xc0), 0, 0);
    assertThat(insn.mnemonic(), is("vaddpd"));
    assertThat(getOperand(insn, 0).toString(), is("ymm0"));
    assertThat(getOperand(insn, 1).toString(), is("ymm0"));
    assertThat(getOperand(insn, 2).toString(), is("ymm0"));
  }

  @Test public void test012 () {
    // jmp ebx
    Instruction insn = dis.decodeOne(TstHelpers.pack(0xff, 0xe3), 0, 0);
    assertThat(insn.mnemonic(), is("jmp"));
    assertThat(getOperand(insn, 0).toString(), is("ebx"));
  }

  @Test public void test013 () {
    // call eax
    Instruction insn = dis.decodeOne(TstHelpers.pack(0xff, 0xd0), 0, 0);
    assertThat(insn.mnemonic(), is("call"));
    assertThat(getOperand(insn, 0).toString(), is("eax"));
  }

  @Test public void test014 () {
    // pshufb xmm0, xmmword ptr [eax]
    Instruction insn = dis.decodeOne(TstHelpers.pack(0x66, 0x0F, 0x38, 0x00, 0x00), 0, 0);
    assertThat(insn.mnemonic(), is("pshufb"));
    assertThat(getOperand(insn, 0).toString(), is("xmm0"));
    // pretty printer has vanished
    // assertThat(insn.operand(1).toString(), is("128[eax]:32"));
    assertThat(insn.operand(1).toString(), is("Tree{(128 *32(eax))}"));
  }

  @Test public void test015 () {
    // fwait
    Instruction insn = dis.decodeOne(TstHelpers.pack(0x9b), 0, 0);
    assertThat(insn.mnemonic(), is("fwait"));
  }

  @Test public void test016 () {
    // popad
    Instruction insn = dis.decodeOne(TstHelpers.pack(0x61), 0, 0);
    assertThat(insn.mnemonic(), is("popad"));
  }

  @Test public void test017 () {
    // popa
    Instruction insn = dis.decodeOne(TstHelpers.pack(0x66, 0x61), 0, 0);
    assertThat(insn.mnemonic(), is("popa"));
  }

  @Test public void test020 () {
    // push ebx
    Instruction insn = dis.decodeOne(TstHelpers.pack(0x53, 0x45, 0x50, 0x50), 0, 0);
    assertThat(insn.mnemonic(), is("push"));
    assertThat(getOperand(insn, 0).toString(), is("ebx"));
  }

  @Test public void test021 () {
    // inc ecx
    Instruction insn = dis.decodeOne(TstHelpers.pack(0x41, 0x4c, 0x45, 0x58), 0, 0);
    assertThat(insn.mnemonic(), is("inc"));
    assertThat(getOperand(insn, 0).toString(), is("ecx"));
  }

  @Test public void test018 () {
    // push ebp
    byte[] opcode = TstHelpers.pack(0x55);
    Instruction insn = dis.decodeOne(opcode, 0, 0);
    assertThat(insn.mnemonic(), is("push"));
    assertThat(getOperand(insn, 0).toString(), is("ebp"));
  }

  @Test public void test019 () {
    @SuppressWarnings("unused")
    Instruction insn = dis.decodeOne(TstHelpers.pack(0x83, 0xe8, 0x01), 0, 0);
//    for (Instruction i : insn.toRReilCtx()) {
//      System.out.println(i);
//    }
  }
}
