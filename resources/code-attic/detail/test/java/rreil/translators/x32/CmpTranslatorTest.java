package rreil.translators.x32;

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
public class CmpTranslatorTest {
  private static final RReilPlatform x32 = RReilPlatforms.X86_32;

  @Test public void test001 () {
    // cmp eax,0xffffffff
    final byte[] code = TstHelpers.pack(0x83, 0xf8, 0xff);
    final InterpCtx ctx = x32.emptyInterpCtx();

    ctx.set("eax", BigInt.ONE);

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    x32.getMachine().evalOne(machCtx);

    // Set carry but not overflow
    assertEquals(BigInt.valueOf(1), ctx.get("eax"));
    assertEquals(BigInt.ONE, ctx.getBool("CF"));
    assertEquals(BigInt.ZERO, ctx.getBool("OF"));
    assertEquals(BigInt.ZERO, ctx.getBool("SF"));
    assertEquals(BigInt.ZERO, ctx.getBool("ZF"));
    assertEquals(BigInt.ONE, ctx.getBool(X86Helpers.BELOW_OR_EQUAL_FLAG));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.LESS_FLAG));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.LESS_OR_EQUAL_FLAG));
  }

  @Test public void test002 () {
    // cmp eax,0x80000000
    final byte[] code = TstHelpers.pack(0x3d, 0x00, 0x00, 0x00, 0x80);
    final InterpCtx ctx = x32.emptyInterpCtx();

    ctx.set("eax", BigInt.valueOf(0x7FFFFFFFl));

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    x32.getMachine().evalOne(machCtx);

    // Set carry and overflow
    assertEquals(BigInt.valueOf(0x7FFFFFFFl), ctx.get("eax"));
    assertEquals(BigInt.ONE, ctx.getBool("CF"));
    assertEquals(BigInt.ONE, ctx.getBool("OF"));
    assertEquals(BigInt.ONE, ctx.getBool("SF"));
    assertEquals(BigInt.ZERO, ctx.getBool("ZF"));
    assertEquals(BigInt.ONE, ctx.getBool(X86Helpers.BELOW_OR_EQUAL_FLAG));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.LESS_FLAG));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.LESS_OR_EQUAL_FLAG));
  }

  @Test public void test003 () {
    // cmp eax,1
    final byte[] code = TstHelpers.pack(0x83, 0xf8, 0x01);
    final InterpCtx ctx = x32.emptyInterpCtx();

    ctx.set("eax", BigInt.valueOf(0x80000000l));

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    x32.getMachine().evalOne(machCtx);

    // Set overflow but not carry
    assertEquals(BigInt.valueOf(0x80000000l), ctx.get("eax"));
    assertEquals(BigInt.ZERO, ctx.getBool("CF"));
    assertEquals(BigInt.ONE, ctx.getBool("OF"));
    assertEquals(BigInt.ZERO, ctx.getBool("SF"));
    assertEquals(BigInt.ZERO, ctx.getBool("ZF"));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.BELOW_OR_EQUAL_FLAG));
    assertEquals(BigInt.ONE, ctx.getBool(X86Helpers.LESS_FLAG));
    assertEquals(BigInt.ONE, ctx.getBool(X86Helpers.LESS_OR_EQUAL_FLAG));
  }

  @Test public void test004 () {
    // cmp eax,ebx
    final byte[] code = TstHelpers.pack(0x39, 0xd8);
    final InterpCtx ctx = x32.emptyInterpCtx();

    ctx.set("eax", BigInt.valueOf(0x2000));
    ctx.set("ebx", BigInt.valueOf(0x3000));

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    x32.getMachine().evalOne(machCtx);

    assertEquals(BigInt.valueOf(0x2000), ctx.get("eax"));
    assertEquals(BigInt.valueOf(0x3000), ctx.get("ebx"));
    assertEquals(BigInt.ONE, ctx.getBool("CF"));
    assertEquals(BigInt.ZERO, ctx.getBool("OF"));
    assertEquals(BigInt.ONE, ctx.getBool("SF"));
    assertEquals(BigInt.ZERO, ctx.getBool("ZF"));
    assertEquals(BigInt.ONE, ctx.getBool(X86Helpers.BELOW_OR_EQUAL_FLAG));
    assertEquals(BigInt.ONE, ctx.getBool(X86Helpers.LESS_FLAG));
    assertEquals(BigInt.ONE, ctx.getBool(X86Helpers.LESS_OR_EQUAL_FLAG));
  }

  @Test public void test005 () {
    // cmp eax,eax
    final byte[] code = TstHelpers.pack(0x39, 0xc0);
    final InterpCtx ctx = x32.emptyInterpCtx();

    ctx.set("eax", BigInt.valueOf(0x2000));

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    x32.getMachine().evalOne(machCtx);

    assertEquals(BigInt.valueOf(0x2000), ctx.get("eax"));
    assertEquals(BigInt.ZERO, ctx.getBool("CF"));
    assertEquals(BigInt.ZERO, ctx.getBool("OF"));
    assertEquals(BigInt.ZERO, ctx.getBool("SF"));
    assertEquals(BigInt.ONE, ctx.getBool("ZF"));
    assertEquals(BigInt.ONE, ctx.getBool(X86Helpers.BELOW_OR_EQUAL_FLAG));
    assertEquals(BigInt.ZERO, ctx.getBool(X86Helpers.LESS_FLAG));
    assertEquals(BigInt.ONE, ctx.getBool(X86Helpers.LESS_OR_EQUAL_FLAG));
  }
}
