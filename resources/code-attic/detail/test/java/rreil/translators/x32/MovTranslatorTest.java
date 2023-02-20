package rreil.translators.x32;

import static org.junit.Assert.assertEquals;
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
  private static final RReilPlatform x32 = RReilPlatforms.X86_32;

  @Test public void test001 () {
    // mov eax, ebx
    final byte[] code = TstHelpers.pack(0x89, 0xd8);
    final InterpCtx ctx = x32.emptyInterpCtx();

    ctx.set("eax", BigInt.ZERO);
    ctx.set("ebx", BigInt.ONE);

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    x32.getMachine().evalOne(machCtx);

    assertEquals(BigInt.ONE, ctx.get("eax"));
  }

  @Test public void test002 () {
    // mov ah, al
    final byte[] code = TstHelpers.pack(0x88, 0xc4);
    final InterpCtx ctx = x32.emptyInterpCtx();

    ctx.set("eax", BigInt.valueOf("FFFFFF0F", 16));

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    x32.getMachine().evalOne(machCtx);

    assertEquals(BigInt.valueOf("FFFF0F0F", 16), ctx.get("eax"));
  }

  @Test public void test003 () {
    // mov eax, dword ptr [ebx+edi*4-0x1]
    final byte[] code = TstHelpers.pack(0x8b, 0x44, 0xbb, 0xff);
    final InterpCtx ctx = x32.emptyInterpCtx();
    final BigInt token = BigInt.valueOf("DEADBEAF", 16);

    ctx.set("ebx", BigInt.valueOf(MemoryModel.DEFAULT_BASE_ADDRESS));
    ctx.set("edi", BigInt.ONE);
    ctx.store(32, MemoryModel.DEFAULT_BASE_ADDRESS + 3, token);

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    x32.getMachine().evalOne(machCtx);

    assertEquals(BigInt.valueOf(MemoryModel.DEFAULT_BASE_ADDRESS), ctx.get("ebx"));
    assertEquals(BigInt.ONE, ctx.get("edi"));
    assertEquals(token, ctx.load(32, MemoryModel.DEFAULT_BASE_ADDRESS + 3));
    assertEquals(token, ctx.get("eax"));
  }

  @Test public void test004 () {
    // mov eax, dword ptr [ebx+edi*4+0xff]
    final byte[] code = TstHelpers.pack(0x8b, 0x84, 0xbb, 0xff, 0x00, 0x00, 0x00);
    final InterpCtx ctx = x32.emptyInterpCtx();
    final BigInt token = BigInt.valueOf("DEADBEAF", 16);

    ctx.set("ebx", BigInt.valueOf(MemoryModel.DEFAULT_BASE_ADDRESS - 0xff));
    ctx.set("edi", BigInt.ZERO);
    ctx.store(32, MemoryModel.DEFAULT_BASE_ADDRESS, token);

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    x32.getMachine().evalOne(machCtx);

    assertEquals(BigInt.valueOf(MemoryModel.DEFAULT_BASE_ADDRESS - 0xff), ctx.get("ebx"));
    assertEquals(BigInt.ZERO, ctx.get("edi"));
    assertEquals(token, ctx.load(32, MemoryModel.DEFAULT_BASE_ADDRESS));
    assertEquals(token, ctx.get("eax"));
  }

  @Test public void test005 () {
    // mov al, byte ptr [ebx+edi*4-0x1]
    final byte[] code = TstHelpers.pack(0x8a, 0x44, 0xbb, 0xff);
    final InterpCtx ctx = x32.emptyInterpCtx();
    final BigInt token = BigInt.valueOf("DE", 16);

    ctx.set("eax", BigInt.MINUSONE);
    ctx.set("ebx", BigInt.valueOf(MemoryModel.DEFAULT_BASE_ADDRESS));
    ctx.set("edi", BigInt.ONE);
    ctx.store(8, MemoryModel.DEFAULT_BASE_ADDRESS + 3, token);

    final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    x32.getMachine().evalOne(machCtx);

    assertEquals(BigInt.valueOf(MemoryModel.DEFAULT_BASE_ADDRESS), ctx.get("ebx"));
    assertEquals(BigInt.ONE, ctx.get("edi"));
    assertEquals(token, ctx.load(8, MemoryModel.DEFAULT_BASE_ADDRESS + 3));
    assertEquals(token, ctx.get("al"));
  }
}
