package bindis.x86.x64;

import static bindis.TstHelpers.pack;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import bindis.Disassembler;
import bindis.x86.x64.X64Disassembler;
import rreil.disassembler.Instruction;
import rreil.disassembler.OperandTree.Node;

/**
 */
public class X64DisassemblerTest {
  private static final Disassembler x64 = X64Disassembler.INSTANCE;

  private static Node getOperand (Instruction insn, int number) {
    return insn.operand(number).getRoot().child(0);
  }

  @Test public void test001 () {
    // mov eax, [ebx]
    byte[] opcode = pack(0x67, 0x8b, 0x03);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(getOperand(obj, 0).toString(), is("eax"));
//    assertThat(obj.operand(1).toString(), is("32[ebx]:32"));
    assertThat(obj.operand(1).toString(), is("Tree{(32 *32(ebx))}"));
  }

  @Test public void test002 () {
    // mov eax, [ebx+edi]
    byte[] opcode = pack(0x67, 0x8b, 0x04, 0x3b);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(getOperand(obj, 0).toString(), is("eax"));
//    assertThat(obj.operand(1).toString(), is("32[ebx+edi]:32"));
    assertThat(obj.operand(1).toString(), is("Tree{(32 *32((+ ebx edi)))}"));
  }

  @Test public void test003 () {
    // mov rax, [rax]
    byte[] opcode = pack(0x48, 0x8b, 0x00);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(getOperand(obj, 0).toString(), is("rax"));
//    assertThat(obj.operand(1).toString(), is("64[rax]:64"));
    assertThat(obj.operand(1).toString(), is("Tree{(64 *64(rax))}"));
  }

  @Test public void test004 () {
    // mov r13, [r14+r15*8-1]
    byte[] opcode = pack(0x4f, 0x8b, 0x6c, 0xfe, 0xff);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(getOperand(obj, 0).toString(), is("r13"));
//    assertThat(obj.operand(1).toString(), is("64[r14+r15*8+ffffffffffffffff]:64"));
    assertThat(obj.operand(1).toString(), is("Tree{(64 *64((+ r14 (* r15 8) -1)))}"));
  }

  @Test public void test005 () {
    // mov r13, [r14]
    byte[] opcode = pack(0x4d, 0x8b, 0x2e);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(getOperand(obj, 0).toString(), is("r13"));
//    assertThat(obj.operand(1).toString(), is("64[r14]:64"));
    assertThat(obj.operand(1).toString(), is("Tree{(64 *64(r14))}"));
  }

  @Test public void test006 () {
    // mov r13, [r14+r15*8]
    byte[] opcode = pack(0x4f, 0x8b, 0x2c, 0xfe);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(getOperand(obj, 0).toString(), is("r13"));
//    assertThat(obj.operand(1).toString(), is("64[r14+r15*8]:64"));
    assertThat(obj.operand(1).toString(), is("Tree{(64 *64((+ r14 (* r15 8))))}"));
  }

  @Test public void test007 () {
    // mov r13, qword_0[r15*8]
    byte[] opcode = pack(0x4e, 0x8b, 0x2c, 0xfd, 0x00, 0x00, 0x00, 0x00);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(getOperand(obj, 0).toString(), is("r13"));
//    assertThat(obj.operand(1).toString(), is("64[r15*8]:64"));
    assertThat(obj.operand(1).toString(), is("Tree{(64 *64((+ (* r15 8) 0)))}"));
  }

  @Test public void test008 () {
    // mov rax, [eax]
    byte[] opcode = pack(0x67, 0x48, 0x8b, 0x00);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(getOperand(obj, 0).toString(), is("rax"));
//    assertThat(obj.operand(1).toString(), is("64[eax]:32"));
    assertThat(obj.operand(1).toString(), is("Tree{(64 *32(eax))}"));
  }

  @Test public void test009 () {
    // mov DWORD PTR [rsp+0x8],0x5
    byte[] opcode = pack(0xc7, 0x44, 0x24, 0x08, 0x05, 0x00, 0x00, 0x00);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("mov"));
//    assertThat(obj.operand(0).toString(), is("32[rsp+8]:64"));
    assertThat(obj.operand(0).toString(), is("Tree{(32 *64((+ rsp 8)))}"));
    assertThat(getOperand(obj, 1).toString(), is("5"));
  }

  @Test public void test010 () {
    // mov DWORD PTR [rsp],0x2
    byte[] opcode = pack(0xc7, 0x04, 0x24, 0x02, 0x00, 0x00, 0x00);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("mov"));
//    assertThat(obj.operand(0).toString(), is("32[rsp]:64"));
    assertThat(obj.operand(0).toString(), is("Tree{(32 *64(rsp))}"));
    assertThat(getOperand(obj, 1).toString(), is("2"));
  }

  @Test public void test011 () {
    // movsxd rdx, ecx
    byte[] opcode = pack(0x48, 0x63, 0xD1);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("movsxd"));
    assertThat(getOperand(obj, 0).toString(), is("rdx"));
    assertThat(getOperand(obj, 1).toString(), is("ecx"));
  }

  @Test public void test012 () {
    // 0x4004c2: mov eax,DWORD PTR [rip+0x2003e8]
    byte[] opcode = pack(0x8b, 0x05, 0xe8, 0x03, 0x20, 0x00);
    Instruction obj = decode(opcode, 0x4004c2L);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(getOperand(obj, 0).toString(), is("eax"));
//    assertThat(obj.operand(1).toString(), is("32[6008b0]:64"));
    assertThat(obj.operand(1).toString(), is("Tree{(32 *64(6293680))}"));
  }

  @Test public void test013 () {
    // lea eax, 32[rbx+rax]
    byte[] opcode = pack(0x8d, 0x04, 0x03);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("lea"));
    assertThat(getOperand(obj, 0).toString(), is("eax"));
//    assertThat(obj.operand(1).toString(), is("32[rbx+rax]:64"));
    assertThat(obj.operand(1).toString(), is("Tree{(32 *64((+ rbx rax)))}"));
  }

  @Test public void test014 () {
    // jmp rdx
    byte[] opcode = pack(0xff, 0xe2);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("jmp"));
    assertThat(getOperand(obj, 0).toString(), is("rdx"));
  }

  @Test public void test015 () {
    // call rax
    byte[] opcode = pack(0xff, 0xd0);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("call"));
    assertThat(getOperand(obj, 0).toString(), is("rax"));
  }

  @Test public void test016 () {
    // push rbp
    byte[] opcode = pack(0x55);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("push"));
    assertThat(getOperand(obj, 0).toString(), is("rbp"));
  }

  @Test public void test017 () {
    // mov r12, rcx
    byte[] opcode = pack(0x49, 0x89, 0xcc);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(getOperand(obj, 0).toString(), is("r12"));
    assertThat(getOperand(obj, 1).toString(), is("rcx"));
  }

  @Test public void test018 () {
    // push r13
    byte[] opcode = pack(0x41, 0x55);
    Instruction obj = decode(opcode);
    assertThat(obj.mnemonic(), is("push"));
    assertThat(getOperand(obj, 0).toString(), is("r13"));
  }

  @Test public void test019 () {
    // movabs rax,0xaaa....b
    byte[] opcode = pack(0x48, 0xb8, 0xab, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa);
    Instruction obj = decode(opcode);
    assertThat(obj.opcode().length, is(10));
    assertThat(obj.mnemonic(), is("mov"));
  }

  private static Instruction decode (byte[] opcode) {
    return decode(opcode, 0);
  }

  private static Instruction decode (byte[] opcode, long address) {
    Instruction obj = x64.decodeOne(opcode, 0, address);
    return obj;
  }
}
