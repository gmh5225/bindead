package rreil.translators.avr8;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import javalx.data.BigInt;
import rreil.platform.api.RReilPlatforms;
import rreil.platform.RReilPlatform;
import rreil.interpreter.InterpCtx;
import rreil.interpreter.MachineCtx;
import rreil.TstHelpers;

/**
 *
 * @author mb0
 */
public class AddTranslatorTest {
  private static final RReilPlatform avr8 = RReilPlatforms.AVR_8_ATMEGA32L;

  @Test public void test001 () {
    // add r0, r18 (r18 = 1)
    byte[] code = TstHelpers.pack(0x02, 0x0e);
    InterpCtx ctx = avr8.emptyInterpCtx();

    ctx.set("r0", BigInt.MINUSONE);
    ctx.set("r18", BigInt.ONE);

    MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    avr8.getMachine().evalOne(machCtx);

    // Set carry but not overflow
    Assert.assertThat(ctx.get("r0"), CoreMatchers.is(BigInt.valueOf(0x00)));
    Assert.assertThat(ctx.getBool("C"), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool("V"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("N"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("Z"), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool(AVR8Helpers.BE), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool(AVR8Helpers.L), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.LE), CoreMatchers.is(BigInt.ONE));
  }

  @Test public void test001x () {
    // add r16, r17
    byte[] code = TstHelpers.pack(0x01, 0x0f);
    InterpCtx ctx = avr8.emptyInterpCtx();

    ctx.set("r16", BigInt.valueOf(30));
    ctx.set("r17", BigInt.valueOf(22));

    MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    avr8.getMachine().evalOne(machCtx);

    // Set carry but not overflow
    Assert.assertThat(ctx.get("r16"), CoreMatchers.is(BigInt.valueOf(52)));
    Assert.assertThat(ctx.get("C"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.get("V"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.get("N"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.get("Z"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.get("H"), CoreMatchers.is(BigInt.ONE));
  }

  @Test public void test002 () {
    // add r0, r18 (r18 = 0x80)
    byte[] code = TstHelpers.pack(0x02, 0x0e);
    InterpCtx ctx = avr8.emptyInterpCtx();

    ctx.set("r0", BigInt.valueOf(0x80));
    ctx.set("r18", BigInt.valueOf(0x80));

    MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    avr8.getMachine().evalOne(machCtx);

    // Set carry and overflow
    Assert.assertThat(ctx.get("r0"), CoreMatchers.is(BigInt.valueOf(0x00)));
    Assert.assertThat(ctx.getBool("C"), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool("V"), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool("N"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("Z"), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool(AVR8Helpers.BE), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool(AVR8Helpers.L), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool(AVR8Helpers.LE), CoreMatchers.is(BigInt.ONE));
  }

  @Test public void test003 () {
    // add r0, r18 (r18 = 1)
    byte[] code = TstHelpers.pack(0x02, 0x0e);
    InterpCtx ctx = avr8.emptyInterpCtx();

    ctx.set("r0", BigInt.valueOf(0x7f));
    ctx.set("r18", BigInt.ONE);

    MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    avr8.getMachine().evalOne(machCtx);

    // Set overflow but not carry
    Assert.assertThat(ctx.get("r0"), CoreMatchers.is(BigInt.valueOf(0x80)));
    Assert.assertThat(ctx.getBool("C"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("V"), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool("N"), CoreMatchers.is(BigInt.ONE));
    Assert.assertThat(ctx.getBool("Z"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.BE), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.L), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.LE), CoreMatchers.is(BigInt.ZERO));
  }

  @Test public void test004 () {
    // add r0, r18
    byte[] code = TstHelpers.pack(0x02, 0x0e);
    InterpCtx ctx = avr8.emptyInterpCtx();

    ctx.set("r0", BigInt.valueOf(0x20));
    ctx.set("r18", BigInt.valueOf(0x30));

    MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    avr8.getMachine().evalOne(machCtx);

    Assert.assertThat(ctx.get("r0"), CoreMatchers.is(BigInt.valueOf(0x50)));
    Assert.assertThat(ctx.get("r18"), CoreMatchers.is(BigInt.valueOf(0x30)));
    Assert.assertThat(ctx.getBool("C"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("V"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("N"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("Z"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.BE), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.L), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.LE), CoreMatchers.is(BigInt.ZERO));
  }

  @Test public void test005 () {
    // add r0, r0
    byte[] code = TstHelpers.pack(0x00, 0x0c);
    InterpCtx ctx = avr8.emptyInterpCtx();

    ctx.set("r0", BigInt.valueOf(0x20));

    MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    avr8.getMachine().evalOne(machCtx);

    Assert.assertThat(ctx.get("r0"), CoreMatchers.is(BigInt.valueOf(0x40)));
    Assert.assertThat(ctx.getBool("C"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("V"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("N"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool("Z"), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.BE), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.L), CoreMatchers.is(BigInt.ZERO));
    Assert.assertThat(ctx.getBool(AVR8Helpers.LE), CoreMatchers.is(BigInt.ZERO));
  }
}
