package rreil.disassembler;

import org.junit.Test;

import rreil.platform.api.RReilPlatforms;
import rreil.platform.RReilPlatform;
import rreil.shallowsyntax.LowLevelRReil;
import binspot.asm.NativeInstruction;
import rreil.disassembly.RReilCtx;

import static rreil.TstHelpers.pack;

/**
 * 
 * @author mb0
 */
public class AVR8RReilTest {
  private static final RReilPlatform platform = RReilPlatforms.AVR_8_ATMEGA32L;

   @Test public void testAdc () {
   byte[] opcode = pack(0x5b, 0x1e);
   NativeInstruction obj = decodeNative(opcode);
   decodeRReil(opcode);
   }
  //
  // @Test public void testAdd () {
  // byte[] opcode = pack(0x73, 0x0e);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // assertThat(obj.mnemonic(), is("add"));
  // assertThat(obj.opnd(0).toString(), is("r7"));
  // assertThat(obj.opnd(1).toString(), is("r19"));
  // }
  //
  // @Test public void testAdiw () {
  // byte[] opcode = pack(0xb3, 0x96);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testAnd () {
  // byte[] opcode = pack(0x3b, 0x22);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testAndi () {
  // byte[] opcode = pack(0x2a, 0x72);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testAsr () {
  // byte[] opcode = pack(0x75, 0x94);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testBclr () {
  // byte[] opcode = pack(0xa8, 0x94);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  // @Test public void testBclr2 () {
  // byte[] opcode = pack(0xf8, 0x94);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testBld () {
  // byte[] opcode = pack(0x11, 0xf8);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  //
  // @Test public void testBrbc () {
  // byte[] opcode = pack(0xf2, 0xf7);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }

  //
  // @Test public void testBrbs () {
  // byte[] opcode = pack(0x04, 0xf2);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testBset () {
  // byte[] opcode = pack(0x38, 0x94);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testBst () {
  // byte[] opcode = pack(0x12, 0xfa);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testCall () {
  // byte[] opcode = pack(0xef, 0x94, 0xfe, 0xaf);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  //
  // @Test public void testCbi () {
  // byte[] opcode = pack(0xdb, 0x98);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testCbr () {
  // }
  //
  // @Test public void testCom () {
  // byte[] opcode = pack(0x60, 0x95);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testCp () {
  // byte[] opcode = pack(0x99, 0x16);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testCpc () {
  // byte[] opcode = pack(0x45, 0x05);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testCpi () {
  // byte[] opcode = pack(0x4a, 0x32);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testCpse () {
  // byte[] opcode = pack(0xa6, 0x12);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testDec () {
  // byte[] opcode = pack(0x8a, 0x94);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testEor () {
  // byte[] opcode = pack(0x1f, 0x26);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testFmul () {
  // byte[] opcode = pack(0x0e, 0x03);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testFmuls () {
  // byte[] opcode = pack(0xa1, 0x03);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testFmulsu () {
  // byte[] opcode = pack(0xf9, 0x03);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testIcall () {
  // byte[] opcode = pack(0x09, 0x95);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testIjmp () {
  // byte[] opcode = pack(0x09, 0x94);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testIn () {
  // byte[] opcode = pack(0x65, 0xb3);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  // @Test public void testInc () {
  // byte[] opcode = pack(0xf3, 0x94);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testJmp () {
  // byte[] opcode = pack(0x0c, 0x94, 0xfe, 0xaf);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testLac () {
  // byte[] opcode = pack(0xb6, 0x93);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testLas () {
  // byte[] opcode = pack(0x35, 0x93);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testLat () {
  // byte[] opcode = pack(0x57, 0x92);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testLdXi () {
  // byte[] opcode = pack(0x3c, 0x90);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  @Test public void testLdXii () {
    byte[] opcode = pack(0x2d, 0x91);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
  }

  //
  @Test public void testLdXiii () {
    byte[] opcode = pack(0xfe, 0x91);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
  }

  @Test public void testLdYiiv () {
    byte[] opcode = pack(0x6a, 0xa5);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
  }

  @Test public void testLd__x () {
    byte[] opcode = pack(0x0c, 0x80);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
  }

  @Test public void testLdYii () {
    byte[] opcode = pack(0x59, 0x90);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
  }

  @Test public void testLdYiii () {
    byte[] opcode = pack(0xba, 0x91);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
  }

  @Test public void testLdZiiv () {
    byte[] opcode = pack(0x94, 0xa5);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
  }

  @Test public void testLdZii () {
    byte[] opcode = pack(0x71, 0x90);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
  }

  @Test public void testLdZiii () {
    byte[] opcode = pack(0x52, 0x91);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
  }

  @Test public void testLdi () {
    byte[] opcode = pack(0x0c, 0xe7);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
  }

  //
  // @Test public void testLds () {
  // byte[] opcode = pack(0x20, 0x90, 0xad, 0xde);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testLpmi () {
  // byte[] opcode = pack(0xc8, 0x95);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testLpmii () {
  // byte[] opcode = pack(0x64, 0x90);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testLpmiii () {
  // byte[] opcode = pack(0x65, 0x91);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testMov () {
  // byte[] opcode = pack(0x56, 0x2e);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testMovw () {
  // byte[] opcode = pack(0x80, 0x01);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testMul () {
  // byte[] opcode = pack(0x63, 0x9d);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testMuls () {
  // byte[] opcode = pack(0x2f, 0x02);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testMulsu () {
  // byte[] opcode = pack(0x32, 0x03);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testNeg () {
  // byte[] opcode = pack(0x41, 0x94);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testNop () {
  // byte[] opcode = pack(0x00, 0x00);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testOr () {
  // byte[] opcode = pack(0x77, 0x29);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testOri () {
  // byte[] opcode = pack(0x73, 0x66);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testOut () {
  // byte[] opcode = pack(0x08, 0xbb);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testPop () {
  // byte[] opcode = pack(0xbf, 0x91);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testPush () {
  // byte[] opcode = pack(0xcf, 0x92);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  //
  // @Test public void testRcall () {
  // byte[] opcode = pack(0xe5, 0xdf);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testRet () {
  // byte[] opcode = pack(0x08, 0x95);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  //
  // @Test public void testReti () {
  // byte[] opcode = pack(0x18, 0x95);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testReti () {
  // byte[] opcode = pack(0xff, 0xcf);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testRor () {
  // byte[] opcode = pack(0xd7, 0x94);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testSbc () {
  // byte[] opcode = pack(0x39, 0x0b);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testSbci () {
  // byte[] opcode = pack(0x91, 0x42);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testSbi () {
  // byte[] opcode = pack(0x8d, 0x9a);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testSbic () {
  // byte[] opcode = pack(0xe1, 0x99);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testSbis () {
  // byte[] opcode = pack(0x80, 0x9b);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testSbiw () {
  // byte[] opcode = pack(0xd7, 0x97);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testSbrc () {
  // byte[] opcode = pack(0x07, 0xfc);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testSbrs () {
  // byte[] opcode = pack(0xd2, 0xff);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testSleep () {
  // byte[] opcode = pack(0x88, 0x95);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testSpmi () {
  // byte[] opcode = pack(0xe8, 0x95);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testSpmii () {
  // byte[] opcode = pack(0xf8, 0x95);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  @Test public void testStXi () {
    byte[] opcode = pack(0x4c, 0x92);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
  }

  @Test public void testStXii () {
    byte[] opcode = pack(0x7d, 0x93);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
  }

  @Test public void testStXiii () {
    byte[] opcode = pack(0xce, 0x92);
    NativeInstruction obj = decodeNative(opcode);
    decodeRReil(opcode);
  }

  //
  // @Test public void testStYi () {
  // byte[] opcode = pack(0xa8, 0x82);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testStYii () {
  // byte[] opcode = pack(0x49, 0x92);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testStYiii () {
  // byte[] opcode = pack(0xaa, 0x93);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testStYiv () {
  // byte[] opcode = pack(0xfb, 0x8f);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testStZi () {
  // byte[] opcode = pack(0x90, 0x82);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testStZii () {
  // byte[] opcode = pack(0x21, 0x92);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testStZiii () {
  // byte[] opcode = pack(0x52, 0x93);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testStZiv () {
  // byte[] opcode = pack(0xb1, 0x82);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testSts () {
  // byte[] opcode = pack(0x70, 0x92, 0x0f, 0x00);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testSwap () {
  // byte[] opcode = pack(0x42, 0x94);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  //
  // @Test public void testXch () {
  // byte[] opcode = pack(0xb4, 0x92);
  // NativeInstruction obj = decodeNative(opcode);
  // decodeRReil(opcode);
  // }
  private NativeInstruction decodeNative (final byte[] code) {
    NativeInstruction insn = platform.getDisassemblerPlatform().getNativeDisassembler().decodeOne(code, 0, 0);
    System.out.println(insn);
    return insn;
  }

  private void decodeRReil (final byte[] code) {
    RReilCtx ctx = platform.getCtxDisassembler().decodeOne(code, 0, 0);
    for(LowLevelRReil insn : ctx.getRReil()) {
      System.out.println("  " + insn);
    }
  }
}
