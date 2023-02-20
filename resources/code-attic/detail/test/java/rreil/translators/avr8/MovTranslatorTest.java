package rreil.translators.avr8;

import static org.junit.Assert.assertEquals;

import java.nio.ByteOrder;

import implementations.avr.AvrImplementation;
import javalx.data.BigInt;

import org.junit.Test;

import rreil.platform.api.RReilPlatforms;
import rreil.platform.RReilPlatform;
import rreil.interpreter.InterpCtx;
import rreil.interpreter.MachineCtx;
import rreil.interpreter.MemoryModel;
import rreil.TstHelpers;

/**
 *
 * @author mb0
 */
public class MovTranslatorTest {
  private static final RReilPlatform avr8 = RReilPlatforms.AVR_8_ATMEGA32L;

  @Test public void test001 () {
    // mov r0, r1
    final byte[] code = TstHelpers.pack(0x01, 0x2c);
    final InterpCtx ctx = avr8.emptyInterpCtx();

    ctx.set("r0", BigInt.ZERO);
    ctx.set("r1", BigInt.ONE);

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    avr8.getMachine().evalOne(machCtx);

    assertEquals(BigInt.ONE, ctx.get("r0"));
  }

  @Test public void testldi () {
    // ldi r16, 30
    final byte[] code = TstHelpers.pack(0x0e, 0xe1);
    final InterpCtx ctx = avr8.emptyInterpCtx();

    ctx.set("r16", BigInt.ZERO);

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    avr8.getMachine().evalOne(machCtx);

    assertEquals(BigInt.valueOf(30), ctx.get("r16"));
  }

  @Test public void test003 () {
    // ld r0, Y + 4
    final byte[] code = TstHelpers.pack(0x0c, 0x80);
    final InterpCtx ctx = avr8.emptyInterpCtx();
    final BigInt token = BigInt.valueOf("de", 16);

    int address = 1028;
    ctx.set("Y", BigInt.valueOf(address));
    ctx.store(8, address + 4, token);

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    avr8.getMachine().evalOne(machCtx);

//    assertEquals(BigInt.valueOf(), ctx.get("ebx"));
//    assertEquals(BigInt.ONE, ctx.get("edi"));
//    assertEquals(token, ctx.load(32, MemoryModel.DEFAULT_BASE_ADDRESS + 3));
    assertEquals(token, ctx.get("r0"));
  }

//
//  @Test public void test004 () {
//    // mov eax, dword ptr [ebx+edi*4+0xff]
//    final byte[] code = TstHelpers.pack(0x8b, 0x84, 0xbb, 0xff, 0x00, 0x00, 0x00);
//    final InterpCtx ctx = x32.emptyInterpCtx();
//    final BigInt token = BigInt.valueOf("DEADBEAF", 16);
//
//    ctx.set("ebx", BigInt.valueOf(MemoryModel.DEFAULT_BASE_ADDRESS - 0xff));
//    ctx.set("edi", BigInt.ZERO);
//    ctx.store(32, MemoryModel.DEFAULT_BASE_ADDRESS, token);
//
//    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
//    x32.getMachine().evalOne(machCtx);
//
//    assertEquals(BigInt.valueOf(MemoryModel.DEFAULT_BASE_ADDRESS - 0xff), ctx.get("ebx"));
//    assertEquals(BigInt.ZERO, ctx.get("edi"));
//    assertEquals(token, ctx.load(32, MemoryModel.DEFAULT_BASE_ADDRESS));
//    assertEquals(token, ctx.get("eax"));
//  }
//
//  @Test public void test005 () {
//    // mov al, byte ptr [ebx+edi*4-0x1]
//    final byte[] code = TstHelpers.pack(0x8a, 0x44, 0xbb, 0xff);
//    final InterpCtx ctx = x32.emptyInterpCtx();
//    final BigInt token = BigInt.valueOf("DE", 16);
//
//    ctx.set("eax", BigInt.MINUSONE);
//    ctx.set("ebx", BigInt.valueOf(MemoryModel.DEFAULT_BASE_ADDRESS));
//    ctx.set("edi", BigInt.ONE);
//    ctx.store(8, MemoryModel.DEFAULT_BASE_ADDRESS + 3, token);
//
//    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
//    x32.getMachine().evalOne(machCtx);
//
//    assertEquals(BigInt.valueOf(MemoryModel.DEFAULT_BASE_ADDRESS), ctx.get("ebx"));
//    assertEquals(BigInt.ONE, ctx.get("edi"));
//    assertEquals(token, ctx.load(8, MemoryModel.DEFAULT_BASE_ADDRESS + 3));
//    assertEquals(token, ctx.get("al"));
//  }
}
