package rreil.disassembler;

import org.junit.Test;
import rreil.disassembly.RReilCtx;
import rreil.platform.api.RReilPlatforms;
import rreil.platform.RReilPlatform;
import rreil.shallowsyntax.LowLevelRReil;
import binspot.asm.NativeInstruction;

import static rreil.TstHelpers.pack;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 *
 * @author mb0
 */
public class X64RReilTest {
  private static final RReilPlatform x64 = RReilPlatforms.X86_64;

  @Test public void test001 () {
    // mov     eax, [ebx]
    byte[] opcode = pack(0x67, 0x8b, 0x03);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(obj.operand(0).toString(), is("eax"));
    assertThat(obj.operand(1).toString(), is("32[ebx]"));
  }

  @Test public void test002 () {
    // mov     eax, [ebx+edi]
    byte[] opcode = pack(0x67, 0x8b, 0x04, 0x3b);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(obj.operand(0).toString(), is("eax"));
    assertThat(obj.operand(1).toString(), is("32[ebx+edi]"));
  }

  @Test public void test003 () {
    // mov     rax, [rax]
    byte[] opcode = pack(0x48, 0x8b, 0x00);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(obj.operand(0).toString(), is("rax"));
    assertThat(obj.operand(1).toString(), is("64[rax]"));
  }

  @Test public void test004 () {
    // mov     r13, [r14+r15*8-1]
    byte[] opcode = pack(0x4f, 0x8b, 0x6c, 0xfe, 0xff);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(obj.operand(0).toString(), is("r13"));
    assertThat(obj.operand(1).toString(), is("64[r14+r15*8+ffffffffffffffff]"));
  }

  @Test public void test005 () {
    // mov     r13, [r14]
    byte[] opcode = pack(0x4d, 0x8b, 0x2e);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(obj.operand(0).toString(), is("r13"));
    assertThat(obj.operand(1).toString(), is("64[r14]"));
  }

  @Test public void test006 () {
    // mov     r13, [r14+r15*8]
    byte[] opcode = pack(0x4f, 0x8b, 0x2c, 0xfe);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(obj.operand(0).toString(), is("r13"));
    assertThat(obj.operand(1).toString(), is("64[r14+r15*8]"));
  }

  @Test public void test007 () {
    // mov     r13, qword_0[r15*8]
    byte[] opcode = pack(0x4e, 0x8b, 0x2c, 0xfd, 0x00, 0x00, 0x00, 0x00);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(obj.operand(0).toString(), is("r13"));
    assertThat(obj.operand(1).toString(), is("64[r15*8]"));
  }

  @Test public void test008 () {
    // add     eax, eax
    byte[] opcode = pack(0x01, 0xc0);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
    assertThat(obj.mnemonic(), is("add"));
    assertThat(obj.operand(0).toString(), is("eax"));
    assertThat(obj.operand(1).toString(), is("eax"));
  }

  @Test public void test009 () {
    // mov     rax, [eax]
    byte[] opcode = pack(0x67, 0x48, 0x8b, 0x00);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(obj.operand(0).toString(), is("rax"));
    assertThat(obj.operand(1).toString(), is("64[eax]"));
  }

  @Test public void test010 () {
    // mov    DWORD PTR [rsp+0x8],0x5
    byte[] opcode = pack(0xc7, 0x44, 0x24, 0x08, 0x05, 0x00, 0x00, 0x00);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(obj.operand(0).toString(), is("32[rsp+8]"));
    assertThat(obj.operand(1).toString(), is("5"));
  }

  @Test public void test011 () {
    // movsxd rdx, ecx
    byte[] opcode = pack(0x48, 0x63, 0xD1);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
    assertThat(obj.mnemonic(), is("movsxd"));
    assertThat(obj.operand(0).toString(), is("rdx"));
    assertThat(obj.operand(1).toString(), is("ecx"));
  }

  @Test public void test012 () {
    // 0x4004c2: mov eax,DWORD PTR [rip+0x2003e8]
    byte[] opcode = pack(0x8b, 0x05, 0xe8, 0x03, 0x20, 0x00);
    NativeInstruction obj = decodeNative(opcode, 0x4004c2L);
    decodeRReil(opcode, 0x4004c2L);
    assertThat(obj.mnemonic(), is("mov"));
    assertThat(obj.operand(0).toString(), is("eax"));
    assertThat(obj.operand(1).toString(), is("32[6008b0]"));
  }

  @Test public void test013 () {
    // lea eax, 32[rbx+rax]
    byte[] opcode = pack(0x8d, 0x04, 0x03);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
    assertThat(obj.mnemonic(), is("lea"));
    assertThat(obj.operand(0).toString(), is("eax"));
    assertThat(obj.operand(1).toString(), is("32[rbx+rax]"));
  }

  @Test public void test014 () {
    // cdqe
    byte[] opcode = pack(0x48, 0x98);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
    assertThat(obj.mnemonic(), is("cdqe"));
  }

  private NativeInstruction decodeNative (byte[] code) {
    return decodeNative(code, 0);
  }

  private NativeInstruction decodeNative (byte[] opcode, long address) {
    NativeInstruction obj = x64.getDisassemblerPlatform().getNativeDisassembler().decodeOne(opcode, 0, address);
    System.out.println(obj);
    return obj;
  }

  private void decodeRReil (byte[] code) {
    decodeRReil(code, 0);
  }

  private void decodeRReil (byte[] code, long address) {
    RReilCtx ctx = x64.getCtxDisassembler().decodeOne(code, 0, address);
    for (LowLevelRReil insn : ctx.getRReil()) {
      System.out.println("  " + insn);
    }
  }
}
