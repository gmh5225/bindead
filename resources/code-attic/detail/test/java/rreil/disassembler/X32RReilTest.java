package rreil.disassembler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import rreil.TstHelpers;
import rreil.disassembly.RReilCtx;
import rreil.platform.api.RReilPlatforms;
import rreil.platform.RReilPlatform;
import binspot.asm.NativeInstruction;

/**
 *
 * @author mb0
 */
public class X32RReilTest {
  private static final RReilPlatform x32 = RReilPlatforms.X86_32;

  @Test public void test000 () {
    // mov ebp, esp
    byte[] code = TstHelpers.pack(0x89, 0xe5);
    NativeInstruction insn = decodeNative(code);
    decodeRReil(code);
    assertThat(insn.mnemonic(), is("mov"));
    assertThat(insn.operand(0).toString(), is("ebp"));
    assertThat(insn.operand(1).toString(), is("esp"));
  }

  @Test public void test001 () {
    // mov edx,DWORD PTR [ebx-0x4]
    byte[] code = TstHelpers.pack(0x8b, 0x93, 0xfc, 0xff, 0xff, 0xff);
    NativeInstruction insn = decodeNative(code);
    decodeRReil(code);
    assertThat(insn.mnemonic(), is("mov"));
    assertThat(insn.operand(0).toString(), is("edx"));
    assertThat(insn.operand(1).toString(), is("32[ebx+fffffffffffffffc]:32"));
  }

  @Test public void test002 () {
    // push ebp
    byte[] code = TstHelpers.pack(0x55);
    NativeInstruction insn = decodeNative(code);
    decodeRReil(code);
    assertThat(insn.mnemonic(), is("push"));
    assertThat(insn.operand(0).toString(), is("ebp"));
  }

  @Test public void test003 () {
    // mov eax,DWORD PTR [ebx+edi*1]
    byte[] code = TstHelpers.pack(0x8b, 0x04, 0x3b);
    NativeInstruction insn = decodeNative(code);
    decodeRReil(code);
    assertThat(insn.mnemonic(), is("mov"));
    assertThat(insn.operand(0).toString(), is("eax"));
    assertThat(insn.operand(1).toString(), is("32[ebx+edi]:32"));
  }

  @Test public void test004 () {
    // mov eax,DWORD PTR [ebx+edi*4+0xff]
    byte[] code = TstHelpers.pack(0x8b, 0x84, 0xbb, 0xff, 0x00, 0x00, 0x00);
    NativeInstruction insn = decodeNative(code);
    decodeRReil(code);
    assertThat(insn.mnemonic(), is("mov"));
    assertThat(insn.operand(0).toString(), is("eax"));
    assertThat(insn.operand(1).toString(), is("32[ebx+edi*4+ff]:32"));
  }

  @Test public void test005 () {
    // mov al,BYTE PTR [ebx+edi*4-0x1]
    byte[] code = TstHelpers.pack(0x8a, 0x44, 0xbb, 0xff);
    NativeInstruction insn = decodeNative(code);
    decodeRReil(code);
    assertThat(insn.mnemonic(), is("mov"));
    assertThat(insn.operand(0).toString(), is("al"));
    assertThat(insn.operand(1).toString(), is("8[ebx+edi*4+ffffffffffffffff]:32"));
  }

  @Test public void test006 () {
    // repz cmps BYTE PTR ds:[esi],BYTE PTR es:[edi]
    byte[] code = TstHelpers.pack(0xf3, 0xa6);
    NativeInstruction insn = decodeNative(code);
    decodeRReil(code);
    assertThat(insn.mnemonic(), is("repz cmpsb"));
    assertThat(insn.operand(0).toString(), is("8[esi]:32"));
    assertThat(insn.operand(1).toString(), is("8[edi]:32"));
  }

  @Test public void test007 () {
    // repz cmps BYTE PTR ds:[si],BYTE PTR es:[di]
    final byte[] code = TstHelpers.pack(0x67, 0xf3, 0xa6);
    NativeInstruction insn = decodeNative(code);
    decodeRReil(code);
    assertThat(insn.mnemonic(), is("repz cmpsb"));
    assertThat(insn.operand(0).toString(), is("8[si]:16"));
    assertThat(insn.operand(1).toString(), is("8[di]:16"));
  }

  @Test public void test008 () {
    // cmps BYTE PTR ds:[esi],BYTE PTR es:[edi]
    final byte[] code = TstHelpers.pack(0xa6);
    NativeInstruction insn = decodeNative(code);
    decodeRReil(code);
    assertThat(insn.mnemonic(), is("cmpsb"));
    assertThat(insn.operand(0).toString(), is("8[esi]:32"));
    assertThat(insn.operand(1).toString(), is("8[edi]:32"));
  }

  @Test public void test009 () {
    // ret
    final byte[] code = TstHelpers.pack(0xc3);
    decodeNative(code);
    decodeRReil(code);
  }

  @Test public void test010 () {
    // call ....
    final byte[] code = TstHelpers.pack(0xe8, 0x7d, 0xff, 0xff, 0xff);
    decodeNative(code);
    decodeRReil(code);
  }

  @Test public void test011 () {
    // leave
    final byte[] code = TstHelpers.pack(0xc9);
    decodeNative(code);
    decodeRReil(code);
  }

  @Test public void test012 () {
    // lea eax,ds:0x0
    byte[] code = TstHelpers.pack(0x8d, 0x05, 0x00, 0x00, 0x00, 0x00);
    NativeInstruction insn = decodeNative(code);
    decodeRReil(code);
    assertThat(insn.mnemonic(), is("lea"));
    assertThat(insn.operand(0).toString(), is("eax"));
    assertThat(insn.operand(1).toString(), is("32[0]:32"));
  }

  @Test public void test013 () {
    // mov ds:0x0, eax
    byte[] code = TstHelpers.pack(0xa3, 0x00, 0x00, 0x00, 0x00);
    decodeNative(code);
    decodeRReil(code);
  }

  @Test public void test014 () {
    // test ecx, ecx
    byte[] opcode = TstHelpers.pack(0x85, 0xc9);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
    assertThat(obj.mnemonic(), is("test"));
    assertThat(obj.operand(0).toString(), is("ecx"));
    assertThat(obj.operand(1).toString(), is("ecx"));
  }

  @Test public void test015 () {
    // lea esi,[esi+eiz*1+0x0]
    byte[] opcode = TstHelpers.pack(0x8d, 0xb4, 0x26, 0x00, 0x00, 0x00, 0x00);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
    assertThat(obj.mnemonic(), is("lea"));
    assertThat(obj.operand(0).toString(), is("esi"));
    assertThat(obj.operand(1).toString(), is("32[esi]:32"));
  }

  @Test public void test016 () {
    // fnop
    byte[] opcode = TstHelpers.pack(0xd9, 0xd0);
    NativeInstruction insn = decodeNative(opcode);
    assertThat(insn.mnemonic(), is("fnop"));
    decodeRReil(opcode);
  }

  private static NativeInstruction decodeNative (byte[] code) {
    NativeInstruction insn = x32.getDisassemblerPlatform().getNativeDisassembler().decodeOne(code, 0, 0);
    return insn;
  }

  private static void decodeRReil (byte[] code) {
    RReilCtx ctx = x32.getCtxDisassembler().decodeOne(code, 0, 0);
  }
}
