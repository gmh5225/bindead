package rreil.translators.x64;

import static org.junit.Assert.assertEquals;
import javalx.data.BigInt;

import org.junit.Test;

import rreil.platform.api.RReilPlatforms;
import rreil.platform.RReilPlatform;
import rreil.interpreter.InterpCtx;
import rreil.interpreter.MachineCtx;
import rreil.translators.x86.common.X86Helpers;
import rreil.TstHelpers;

/**
 *
 * @author mb0
 */
public class AddTranslatorTest {
  private static final RReilPlatform x64 = RReilPlatforms.X86_64;

  @Test public void test001 () {
    // add eax, 1
    final byte[] code = TstHelpers.pack(0x83, 0xc0, 0x01);
    final InterpCtx ctx = x64.emptyInterpCtx();

    ctx.set("rax", BigInt.MINUSONE);

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    x64.getMachine().evalOne(machCtx);

    // Set carry but not overflow
    assertEquals(BigInt.valueOf(0), ctx.get("rax"));
    assertEquals(BigInt.ONE, ctx.getBool("CF"));
    assertEquals(BigInt.ZERO, ctx.getBool("OF"));
    assertEquals(BigInt.ZERO, ctx.getBool("SF"));
    assertEquals(BigInt.ONE, ctx.getBool("ZF"));
    assertEquals(BigInt.ONE, ctx.getBool(X86Helpers.BELOW_OR_EQUAL_FLAG));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.LESS_FLAG));
    assertEquals(BigInt.ONE, ctx.getBool(X86Helpers.LESS_OR_EQUAL_FLAG));
  }

  @Test public void test002 () {
    // add eax, 0x80000000
    final byte[] code = TstHelpers.pack(0x05, 0x00, 0x00, 0x00, 0x80);
    final InterpCtx ctx = x64.emptyInterpCtx();

    ctx.set("rax", BigInt.valueOf(0x80000000l));

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    x64.getMachine().evalOne(machCtx);

    // Set carry and overflow
    assertEquals(BigInt.valueOf(0), ctx.get("rax"));
    assertEquals(BigInt.ONE, ctx.getBool("CF"));
    assertEquals(BigInt.ONE, ctx.getBool("OF"));
    assertEquals(BigInt.ZERO, ctx.getBool("SF"));
    assertEquals(BigInt.ONE, ctx.getBool("ZF"));
    assertEquals(BigInt.ONE, ctx.getBool(X86Helpers.BELOW_OR_EQUAL_FLAG));
    assertEquals(BigInt.ONE, ctx.getBool(X86Helpers.LESS_FLAG));
    assertEquals(BigInt.ONE, ctx.getBool(X86Helpers.LESS_OR_EQUAL_FLAG));
  }

  @Test public void test003 () {
    // add eax, 1
    final byte[] code = TstHelpers.pack(0x83, 0xc0, 0x01);
    final InterpCtx ctx = x64.emptyInterpCtx();

    ctx.set("rax", BigInt.valueOf(0x7FFFFFFFl));

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    x64.getMachine().evalOne(machCtx);

    // Set overflow but not carry
    assertEquals(BigInt.valueOf(0x80000000l), ctx.get("rax"));
    assertEquals(BigInt.ZERO, ctx.getBool("CF"));
    assertEquals(BigInt.ONE, ctx.getBool("OF"));
    assertEquals(BigInt.ONE, ctx.getBool("SF"));
    assertEquals(BigInt.ZERO, ctx.getBool("ZF"));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.BELOW_OR_EQUAL_FLAG));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.LESS_FLAG));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.LESS_OR_EQUAL_FLAG));
  }

  @Test public void test004 () {
    // add eax, ebx
    final byte[] code = TstHelpers.pack(0x01, 0xd8);
    final InterpCtx ctx = x64.emptyInterpCtx();

    ctx.set("rax", BigInt.valueOf(0x2000));
    ctx.set("rbx", BigInt.valueOf(0x3000));

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    x64.getMachine().evalOne(machCtx);

    assertEquals(BigInt.valueOf(0x5000), ctx.get("rax"));
    assertEquals(BigInt.valueOf(0x3000), ctx.get("rbx"));
    assertEquals(BigInt.ZERO, ctx.getBool("CF"));
    assertEquals(BigInt.ZERO, ctx.getBool("OF"));
    assertEquals(BigInt.ZERO, ctx.getBool("SF"));
    assertEquals(BigInt.ZERO, ctx.getBool("ZF"));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.BELOW_OR_EQUAL_FLAG));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.LESS_FLAG));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.LESS_OR_EQUAL_FLAG));
  }

  @Test public void test005 () {
    // add eax, eax
    final byte[] code = TstHelpers.pack(0x01, 0xc0);
    final InterpCtx ctx = x64.emptyInterpCtx();

    ctx.set("rax", BigInt.valueOf(0x2000));

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    x64.getMachine().evalOne(machCtx);

    assertEquals(BigInt.valueOf(0x4000), ctx.get("rax"));
    assertEquals(BigInt.ZERO, ctx.getBool("CF"));
    assertEquals(BigInt.ZERO, ctx.getBool("OF"));
    assertEquals(BigInt.ZERO, ctx.getBool("SF"));
    assertEquals(BigInt.ZERO, ctx.getBool("ZF"));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.BELOW_OR_EQUAL_FLAG));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.LESS_FLAG));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.LESS_OR_EQUAL_FLAG));
  }
}
