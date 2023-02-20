package bindis.avr8;

import static bindis.TstHelpers.pack;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import bindis.DecodeStream;
import bindis.INativeInstruction;
import bindis.NativeDisassembler;
import bindis.NativeInstruction;
import bindis.OperandKind;
import bindis.avr8.AVRDis;
import bindis.avr8.AVRImmOpnd;
import bindis.avr8.AVRInsn;
import bindis.avr8.AVRMemOpnd;
import bindis.avr8.AVRRegOpnd;
import bindis.avr8.AVRRegisterAlterationType;

/**
 */
public class AVRDisTest {
  private static final NativeDisassembler avr = AVRDis.$;

  @Test public void testAdc () {
    testInstruction(pack(0x5b, 0x1e), "adc", OperandKind.REG, 5, OperandKind.REG, 27);
  }

  @Test public void testAdd () {
    testInstruction(pack(0x73, 0x0e), "add", OperandKind.REG, 7, OperandKind.REG, 19);
  }

  @Test public void testAdiw () {
    testInstruction(pack(0xb3, 0x96), "adiw", OperandKind.REG, 30, OperandKind.IMM, 35);
  }

  @Test public void testSub () {
    testInstruction(pack(0xc3, 0x19), "sub", OperandKind.REG, 28, OperandKind.REG, 3);
  }

  @Test public void testSubi () {
    testInstruction(pack(0x3a, 0x52), "subi", OperandKind.REG, 19, OperandKind.IMM, 42);
  }

  @Test public void testSbc () {
    testInstruction(pack(0x39, 0x0b), "sbc", OperandKind.REG, 19, OperandKind.REG, 25);
  }

  @Test public void testSbci () {
    testInstruction(pack(0x91, 0x42), "sbci", OperandKind.REG, 25, OperandKind.IMM, 33);
  }

  @Test public void testSbiw () {
    testInstruction(pack(0xd7, 0x97), "sbiw", OperandKind.REG, 26, OperandKind.IMM, 55);
  }

  @Test public void testAnd () {
    testInstruction(pack(0x3b, 0x22), "and", OperandKind.REG, 3, OperandKind.REG, 27);
  }

  @Test public void testAndi () {
    testInstruction(pack(0x2a, 0x72), "andi", OperandKind.REG, 18, OperandKind.IMM, 42);
  }

  @Test public void testOr () {
    testInstruction(pack(0x77, 0x29), "or", OperandKind.REG, 23, OperandKind.REG, 7);
  }

  @Test public void testOri () {
    testInstruction(pack(0x73, 0x66), "ori", OperandKind.REG, 23, OperandKind.IMM, 99);
  }

  @Test public void testEor () {
    testInstruction(pack(0x1f, 0x26), "eor", OperandKind.REG, 1, OperandKind.REG, 31);
  }

  @Test public void testCom () {
    testInstruction(pack(0x60, 0x95), "com", OperandKind.REG, 22);
  }

  @Test public void testNeg () {
    testInstruction(pack(0x41, 0x94), "neg", OperandKind.REG, 4);
  }

  @Test public void testInc () {
    testInstruction(pack(0xf3, 0x94), "inc", OperandKind.REG, 15);
  }

  @Test public void testDec () {
    testInstruction(pack(0x8a, 0x94), "dec", OperandKind.REG, 8);
  }

  @Test public void testMul () {
    testInstruction(pack(0x63, 0x9d), "mul", OperandKind.REG, 22, OperandKind.REG, 3);
  }

  @Test public void testMuls () {
    testInstruction(pack(0x2f, 0x02), "muls", OperandKind.REG, 18, OperandKind.REG, 31);
  }

  @Test public void testMulsu () {
    testInstruction(pack(0x32, 0x03), "mulsu", OperandKind.REG, 19, OperandKind.REG, 18);
  }

  @Test public void testFmul () {
    testInstruction(pack(0x0e, 0x03), "fmul", OperandKind.REG, 16, OperandKind.REG, 22);
  }

  @Test public void testFmuls () {
    testInstruction(pack(0xa1, 0x03), "fmuls", OperandKind.REG, 18, OperandKind.REG, 17);
  }

  @Test public void testFmulsu () {
    testInstruction(pack(0xf9, 0x03), "fmulsu", OperandKind.REG, 23, OperandKind.REG, 17);
  }

  @Test public void testDes () {
    testInstruction(pack(0xeb, 0x94), "des", OperandKind.IMM, 14);
  }

  @Test public void testRjmp () {
//    testInstruction(pack(0xff, 0xcf), "rjmp", OpndKind.IMM, -1);
    testInstruction(pack(0xff, 0xcf), "rjmp", OperandKind.IMM, -2);
  }

  @Test public void testIjmp () {
    testInstruction(pack(0x09, 0x94), "ijmp");
  }

  @Test public void testEijmp () {
    testInstruction(pack(0x19, 0x94), "eijmp");
  }

  @Test public void testJmp () {
    testInstruction(pack(0x0c, 0x94, 0xfe, 0xaf), "jmp", OperandKind.IMM, 0xaffe);
  }

  @Test public void testRcall () {
//    testInstruction(pack(0xe5, 0xdf), "rcall", OpndKind.IMM, -27);
    testInstruction(pack(0xe5, 0xdf), "rcall", OperandKind.IMM, -54);
  }

  @Test public void testIcall () {
    testInstruction(pack(0x09, 0x95), "icall");
  }

  @Test public void testEicall () {
    testInstruction(pack(0x19, 0x95), "eicall");
  }

  @Test public void testCall () {
    testInstruction(pack(0xef, 0x94, 0xfe, 0xaf), "call", OperandKind.IMM, 0x1daffe);
  }

  @Test public void testRet () {
    testInstruction(pack(0x08, 0x95), "ret");
  }

  @Test public void testReti () {
    testInstruction(pack(0x18, 0x95), "reti");
  }

  @Test public void testCpse () {
    testInstruction(pack(0xa6, 0x12), "cpse", OperandKind.REG, 10, OperandKind.REG, 22);
  }

  @Test public void testCp () {
    testInstruction(pack(0x99, 0x16), "cp", OperandKind.REG, 9, OperandKind.REG, 25);
  }

  @Test public void testCpc () {
    testInstruction(pack(0x45, 0x05), "cpc", OperandKind.REG, 20, OperandKind.REG, 5);
  }

  @Test public void testCpi () {
    testInstruction(pack(0x4a, 0x32), "cpi", OperandKind.REG, 20, OperandKind.IMM, 42);
  }

  @Test public void testSbrc () {
    testInstruction(pack(0x07, 0xfc), "sbrc", OperandKind.REG, 0, OperandKind.IMM, 7);
  }

  @Test public void testSbrs () {
    testInstruction(pack(0xd2, 0xff), "sbrs", OperandKind.REG, 29, OperandKind.IMM, 2);
  }

  @Test public void testSbic () {
    testInstruction(pack(0xe1, 0x99), "sbic", OperandKind.IMM, 28, OperandKind.IMM, 1);
  }

  @Test public void testSbis () {
    testInstruction(pack(0x80, 0x9b), "sbis", OperandKind.IMM, 16, OperandKind.IMM, 0);
  }

  @Test public void testBrbc () {
    testInstruction(pack(0xf2, 0xf7), "brbc", OperandKind.IMM, 2, OperandKind.IMM, -4);
  }

  @Test public void testBrbs () {
    testInstruction(pack(0x04, 0xf2), "brbs", OperandKind.IMM, 4, OperandKind.IMM, -128);
  }

  @Test public void testMov () {
    testInstruction(pack(0x56, 0x2e), "mov", OperandKind.REG, 5, OperandKind.REG, 22);
  }

  @Test public void testMovw () {
    testInstruction(pack(0x80, 0x01), "movw", OperandKind.REG, 16, OperandKind.REG, 0);
  }

  @Test public void testLdi () {
    testInstruction(pack(0x0c, 0xe7), "ldi", OperandKind.REG, 16, OperandKind.IMM, 124);
  }

  @Test public void testLds () {
    testInstruction(pack(0x20, 0x90, 0xad, 0xde), "lds", OperandKind.REG, 2, OperandKind.MEM, null, AVRRegisterAlterationType.None, 0xdead);
  }

  @Test public void testLdXi () {
    testInstruction(pack(0x3c, 0x90), "ld", OperandKind.REG, 3, OperandKind.MEM, AVRRegOpnd.AVRReg.X.ordinal(), AVRRegisterAlterationType.None, null);
  }

  @Test public void testLdXii () {
    testInstruction(pack(0x2d, 0x91), "ld", OperandKind.REG, 18, OperandKind.MEM, AVRRegOpnd.AVRReg.X.ordinal(), AVRRegisterAlterationType.PostIncrement, null);
  }

  @Test public void testLdXiii () {
    testInstruction(pack(0xfe, 0x91), "ld", OperandKind.REG, 31, OperandKind.MEM, AVRRegOpnd.AVRReg.X.ordinal(), AVRRegisterAlterationType.PreDecrement, null);
  }

  @Test public void testLdYiiv () {
    testInstruction(pack(0x6a, 0xa5), "ld", OperandKind.REG, 22, OperandKind.MEM, AVRRegOpnd.AVRReg.Y.ordinal(), AVRRegisterAlterationType.None, 42);
  }

  @Test public void testLdYii () {
    testInstruction(pack(0x59, 0x90), "ld", OperandKind.REG, 5, OperandKind.MEM, AVRRegOpnd.AVRReg.Y.ordinal(), AVRRegisterAlterationType.PostIncrement, null);
  }

  @Test public void testLdYiii () {
    testInstruction(pack(0xba, 0x91), "ld", OperandKind.REG, 27, OperandKind.MEM, AVRRegOpnd.AVRReg.Y.ordinal(), AVRRegisterAlterationType.PreDecrement, null);
  }

  @Test public void testLdZiiv () {
    testInstruction(pack(0x94, 0xa5), "ld", OperandKind.REG, 25, OperandKind.MEM, AVRRegOpnd.AVRReg.Z.ordinal(), AVRRegisterAlterationType.None, 44);
  }

  @Test public void testLdZii () {
    testInstruction(pack(0x71, 0x90), "ld", OperandKind.REG, 7, OperandKind.MEM, AVRRegOpnd.AVRReg.Z.ordinal(), AVRRegisterAlterationType.PostIncrement, null);
  }

  @Test public void testLdZiii () {
    testInstruction(pack(0x52, 0x91), "ld", OperandKind.REG, 21, OperandKind.MEM, AVRRegOpnd.AVRReg.Z.ordinal(), AVRRegisterAlterationType.PreDecrement, null);
  }

  @Test public void testStXi () {
    testInstruction(pack(0x4c, 0x92), "st", OperandKind.MEM, AVRRegOpnd.AVRReg.X.ordinal(), AVRRegisterAlterationType.None, null, OperandKind.REG, 4);
  }

  @Test public void testStXii () {
    testInstruction(pack(0x7d, 0x93), "st", OperandKind.MEM, AVRRegOpnd.AVRReg.X.ordinal(), AVRRegisterAlterationType.PostIncrement, null, OperandKind.REG, 23);
  }

  @Test public void testStXiii () {
    testInstruction(pack(0xce, 0x92), "st", OperandKind.MEM, AVRRegOpnd.AVRReg.X.ordinal(), AVRRegisterAlterationType.PreDecrement, null, OperandKind.REG, 12);
  }

  @Test public void testStYi () {
    testInstruction(pack(0xa8, 0x82), "st", OperandKind.MEM, AVRRegOpnd.AVRReg.Y.ordinal(), AVRRegisterAlterationType.None, 0, OperandKind.REG, 10);
  }

  @Test public void testStYii () {
    testInstruction(pack(0x49, 0x92), "st", OperandKind.MEM, AVRRegOpnd.AVRReg.Y.ordinal(), AVRRegisterAlterationType.PostIncrement, null, OperandKind.REG, 4);
  }

  @Test public void testStYiii () {
    testInstruction(pack(0xaa, 0x93), "st", OperandKind.MEM, AVRRegOpnd.AVRReg.Y.ordinal(), AVRRegisterAlterationType.PreDecrement, null, OperandKind.REG, 26);
  }

  @Test public void testStYiv () {
    testInstruction(pack(0xfb, 0x8f), "st", OperandKind.MEM, AVRRegOpnd.AVRReg.Y.ordinal(), AVRRegisterAlterationType.None, 27, OperandKind.REG, 31);
  }

  @Test public void testStZi () {
    testInstruction(pack(0x90, 0x82), "st", OperandKind.MEM, AVRRegOpnd.AVRReg.Z.ordinal(), AVRRegisterAlterationType.None, 0, OperandKind.REG, 9);
  }

  @Test public void testStZii () {
    testInstruction(pack(0x21, 0x92), "st", OperandKind.MEM, AVRRegOpnd.AVRReg.Z.ordinal(), AVRRegisterAlterationType.PostIncrement, null, OperandKind.REG, 2);
  }

  @Test public void testStZiii () {
    testInstruction(pack(0x52, 0x93), "st", OperandKind.MEM, AVRRegOpnd.AVRReg.Z.ordinal(), AVRRegisterAlterationType.PreDecrement, null, OperandKind.REG, 21);
  }

  @Test public void testStZiv () {
    testInstruction(pack(0xb1, 0x82), "st", OperandKind.MEM, AVRRegOpnd.AVRReg.Z.ordinal(), AVRRegisterAlterationType.None, 1, OperandKind.REG, 11);
  }

  @Test public void testLpmi () {
    testInstruction(pack(0xc8, 0x95), "lpm", OperandKind.MEM, AVRRegOpnd.AVRReg.Z.ordinal(), AVRRegisterAlterationType.None, null);
  }

  @Test public void testLpmii () {
    testInstruction(pack(0x64, 0x90), "lpm", OperandKind.REG, 6, OperandKind.MEM, AVRRegOpnd.AVRReg.Z.ordinal(), AVRRegisterAlterationType.None, null);
  }

  @Test public void testLpmiii () {
    testInstruction(pack(0x65, 0x91), "lpm", OperandKind.REG, 22, OperandKind.MEM, AVRRegOpnd.AVRReg.Z.ordinal(), AVRRegisterAlterationType.PostIncrement, null);
  }

  @Test public void testElpmi () {
    testInstruction(pack(0xd8, 0x95), "elpm", OperandKind.MEM, AVRRegOpnd.AVRReg.Z.ordinal(), AVRRegisterAlterationType.None, null);
  }

  @Test public void testElpmii () {
    testInstruction(pack(0x86, 0x91), "elpm", OperandKind.REG, 24, OperandKind.MEM, AVRRegOpnd.AVRReg.Z.ordinal(), AVRRegisterAlterationType.None, null);
  }

  @Test public void testElpmiii () {
    testInstruction(pack(0xd7, 0x90), "elpm", OperandKind.REG, 13, OperandKind.MEM, AVRRegOpnd.AVRReg.Z.ordinal(), AVRRegisterAlterationType.PostIncrement, null);
  }

  @Test public void testSpmi () {
    testInstruction(pack(0xe8, 0x95), "spm", OperandKind.MEM, AVRRegOpnd.AVRReg.Z.ordinal(), AVRRegisterAlterationType.None, null);
  }

  @Test public void testSpmii () {
    testInstruction(pack(0xf8, 0x95), "spm", OperandKind.MEM, AVRRegOpnd.AVRReg.Z.ordinal(), AVRRegisterAlterationType.PostIncrement, null);
  }

  @Test public void testIn () {
    testInstruction(pack(0x65, 0xb3), "in", OperandKind.REG, 22, OperandKind.MEM, null, AVRRegisterAlterationType.None, 0x15);
  }

  @Test public void testOut () {
    testInstruction(pack(0x08, 0xbb), "out", OperandKind.MEM, null, AVRRegisterAlterationType.None, 0x18, OperandKind.REG, 16);
  }

  @Test public void testPush () {
    testInstruction(pack(0xcf, 0x92), "push", OperandKind.REG, 12);
  }

  @Test public void testPop () {
    testInstruction(pack(0xbf, 0x91), "pop", OperandKind.REG, 27);
  }

  @Test public void testXch () {
    testInstruction(pack(0xb4, 0x92), "xch", OperandKind.REG, 11);
  }

  @Test public void testLas () {
    testInstruction(pack(0x35, 0x93), "las", OperandKind.REG, 19);
  }

  @Test public void testLac () {
    testInstruction(pack(0xb6, 0x93), "lac", OperandKind.REG, 27);
  }

  @Test public void testLat () {
    testInstruction(pack(0x57, 0x92), "lat", OperandKind.REG, 5);
  }

  @Test public void testLsr () {
    testInstruction(pack(0xa6, 0x94), "lsr", OperandKind.REG, 10);
  }

  @Test public void testRor () {
    testInstruction(pack(0xd7, 0x94), "ror", OperandKind.REG, 13);
  }

  @Test public void testAsr () {
    testInstruction(pack(0x75, 0x94), "asr", OperandKind.REG, 7);
  }

  @Test public void testSwap () {
    testInstruction(pack(0x42, 0x94), "swap", OperandKind.REG, 4);
  }

  @Test public void testBset () {
    testInstruction(pack(0x38, 0x94), "bset", OperandKind.IMM, 3);
  }

  @Test public void testBclr () {
    testInstruction(pack(0xe8, 0x94), "bclr", OperandKind.IMM, 6);
  }

  @Test public void testSbi () {
    testInstruction(pack(0x8d, 0x9a), "sbi", OperandKind.MEM, null, AVRRegisterAlterationType.None, 0x11, OperandKind.IMM, 5);
  }

  @Test public void testCbi () {
    testInstruction(pack(0xdb, 0x98), "cbi", OperandKind.MEM, null, AVRRegisterAlterationType.None, 0x1b, OperandKind.IMM, 3);
  }

  @Test public void testBreak () {
    testInstruction(pack(0x98, 0x95), "break");
  }

  @Test public void testSleep () {
    testInstruction(pack(0x88, 0x95), "sleep");
  }

  @Test public void testNop () {
    testInstruction(pack(0x00, 0x00), "nop");
  }

  @Test public void testWdr () {
    testInstruction(pack(0xa8, 0x95), "wdr");
  }

  private static void testInstruction (byte code[], String mnemonic, Object... operands) {
    AVRInsn insn = decode(code);
    assertThat("Mnemonic", insn.mnemonic(), is(mnemonic));

    int j = 0;
    for (int i = 0; i < operands.length;) {
      if (j >= insn.numberOfOperands()) {
        j++;
        break;
      }

      OperandKind kind = (OperandKind) operands[i++];

      switch (kind) {
        case IMM: {
          if (i >= operands.length)
            throw new RuntimeException("There are not enough parameters for this operand type.");

          Integer value = (Integer) operands[i++];

          AVRImmOpnd op = (AVRImmOpnd) insn.operand(j++);

          assertThat("Immediate value", op.getValue(), is(value));
          break;
        }
        case REG: {
          if (i >= operands.length)
            throw new RuntimeException("There are not enough parameters for this operand type.");

          Integer register = (Integer) operands[i++];

          AVRRegOpnd op = (AVRRegOpnd) insn.operand(j++);

          assertThat("Register number", op.getRegNo(), is(register));
          break;
        }
        case MEM: {
          if (i + 2 >= operands.length)
            throw new RuntimeException("There are not enough parameters for this operand type.");

          Integer register = (Integer) operands[i++];
          AVRRegisterAlterationType altType = (AVRRegisterAlterationType) operands[i++];
          Integer displacement = (Integer) operands[i++];

          AVRMemOpnd op = (AVRMemOpnd) insn.operand(j++);

          if (register != null)
            assertThat("Memory register", op.getMemReg().getRegNo(), is(register));
          else
            assertNull("Memory register (null)", op.getMemReg());
          assertThat("Memory register alteration type", op.getAltType(), is(altType));
          if (displacement != null)
            assertThat("Displace (not null)", op.getDisplacement().getValue(), is(displacement));
          else
            assertNull("Displacement (null)", op.getDisplacement());

          break;
        }
        default: {
          throw new RuntimeException("Operand type is unknown.");
        }
      }
    }

    assertThat("Number of operands", insn.numberOfOperands(), is(j));
  }

  private static AVRInsn decode (final byte[] code) {
    INativeInstruction insn = avr.decode(new DecodeStream(code, 0), 0l);
//    System.out.println(insn);
    return (AVRInsn) insn;
  }
}
