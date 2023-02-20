package rreil.translators.avr8;

import javalx.data.BigInt;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import rreil.TstHelpers;
import rreil.interpreter.InterpCtx;
import rreil.interpreter.MachineCtx;
import rreil.platform.api.RReilPlatforms;
import rreil.platform.RReilPlatform;

/**
 *
 * @author mb0
 */
public class SubTranslatorTest {
  private static final RReilPlatform avr8 = RReilPlatforms.AVR_8_ATMEGA32L;

  @Test public void test001 () {
    // subi r16, -1
    final byte[] code = TstHelpers.pack(0x0f, 0x5f);
    final InterpCtx ctx = SubTranslatorTest.avr8.emptyInterpCtx();

    ctx.set("r16", BigInt.ONE);

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    SubTranslatorTest.avr8.getMachine().evalOne(machCtx);

    // Set carry but not overflow
    Assert.assertThat(ctx.get("r16"), CoreMatchers.is(BigInt.valueOf(2)));
    Assert.assertThat(ctx.getBool("C"), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool("V"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("N"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("Z"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("S"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.BE), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool(AVR8Helpers.L), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.LE), CoreMatchers.is(BigInt.ZERO));
  }

  @Test public void test002 () {
    // subi r16, 0x80
    final byte[] code = TstHelpers.pack(0x00, 0x58);
    final InterpCtx ctx = SubTranslatorTest.avr8.emptyInterpCtx();

    ctx.set("r16", BigInt.valueOf(0x7f));

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    SubTranslatorTest.avr8.getMachine().evalOne(machCtx);

    // Set carry and overflow
    Assert.assertThat(ctx.get("r16"), CoreMatchers.is(BigInt.valueOf(0xff)));
    Assert.assertThat(ctx.getBool("C"), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool("V"), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool("N"), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool("Z"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.BE), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool(AVR8Helpers.L), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.LE), CoreMatchers.is(BigInt.ZERO));
  }

  @Test public void test003 () {
    // subi r16, 1
    final byte[] code = TstHelpers.pack(0x01, 0x50);
    final InterpCtx ctx = SubTranslatorTest.avr8.emptyInterpCtx();

    ctx.set("r16", BigInt.valueOf(0x80));

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    SubTranslatorTest.avr8.getMachine().evalOne(machCtx);

    // Set overflow but not carry
    Assert.assertThat(ctx.get("r16"), CoreMatchers.is(BigInt.valueOf(0x7f)));
    Assert.assertThat(ctx.getBool("C"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("V"), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool("N"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("Z"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.BE), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.L), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool(AVR8Helpers.LE), CoreMatchers.is(BigInt.ONE));
  }

  @Test public void test004 () {
    // sub r0, r1
    final byte[] code = TstHelpers.pack(0x01, 0x18);
    final InterpCtx ctx = SubTranslatorTest.avr8.emptyInterpCtx();

    ctx.set("r0", BigInt.valueOf(0x20));
    ctx.set("r1", BigInt.valueOf(0x30));

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    SubTranslatorTest.avr8.getMachine().evalOne(machCtx);

    Assert.assertThat(ctx.get("r0"), CoreMatchers.is(BigInt.valueOf(0xf0)));
    Assert.assertThat(ctx.get("r1"), CoreMatchers.is(BigInt.valueOf(0x30)));
    Assert.assertThat(ctx.getBool("C"), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool("V"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("N"), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool("Z"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.BE), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool(AVR8Helpers.L), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool(AVR8Helpers.LE), CoreMatchers.is(BigInt.ONE));
  }

  @Test public void test005 () {
    // sub r0, r0
    final byte[] code = TstHelpers.pack(0x00, 0x18);
    final InterpCtx ctx = SubTranslatorTest.avr8.emptyInterpCtx();

    ctx.set("r0", BigInt.valueOf(0x20));

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    SubTranslatorTest.avr8.getMachine().evalOne(machCtx);

    Assert.assertThat(ctx.get("r0"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("C"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("V"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("N"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("Z"), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool(AVR8Helpers.BE), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool(AVR8Helpers.L), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.LE), CoreMatchers.is(BigInt.ONE));
  }
}
