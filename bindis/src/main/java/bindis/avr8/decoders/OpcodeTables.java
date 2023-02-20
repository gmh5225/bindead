package bindis.avr8.decoders;

import static bindis.avr8.decoders.OpndDecoder.A;
import static bindis.avr8.decoders.OpndDecoder.B;
import static bindis.avr8.decoders.OpndDecoder.Imm0x00f00000;
import static bindis.avr8.decoders.OpndDecoder.Imm0x0f0f0000;
import static bindis.avr8.decoders.OpndDecoder.ImmNegRel0x03f80000;
import static bindis.avr8.decoders.OpndDecoder.ImmNegRelAdd0x0fff0000;
import static bindis.avr8.decoders.OpndDecoder.K;
import static bindis.avr8.decoders.OpndDecoder.KLong;
import static bindis.avr8.decoders.OpndDecoder.MemA;
import static bindis.avr8.decoders.OpndDecoder.MemImm0x0000ffff;
import static bindis.avr8.decoders.OpndDecoder.MemImm0x060f0000;
import static bindis.avr8.decoders.OpndDecoder.MinusX;
import static bindis.avr8.decoders.OpndDecoder.MinusY;
import static bindis.avr8.decoders.OpndDecoder.MinusZ;
import static bindis.avr8.decoders.OpndDecoder.Rd5;
import static bindis.avr8.decoders.OpndDecoder.RdDv4;
import static bindis.avr8.decoders.OpndDecoder.RdDvUpper2;
import static bindis.avr8.decoders.OpndDecoder.RdUpper4;
import static bindis.avr8.decoders.OpndDecoder.RdUpperHalf3;
import static bindis.avr8.decoders.OpndDecoder.Rr5;
import static bindis.avr8.decoders.OpndDecoder.RrDv4;
import static bindis.avr8.decoders.OpndDecoder.RrUpper4;
import static bindis.avr8.decoders.OpndDecoder.RrUpperHalf3;
import static bindis.avr8.decoders.OpndDecoder.S;
import static bindis.avr8.decoders.OpndDecoder.X;
import static bindis.avr8.decoders.OpndDecoder.XPlus;
import static bindis.avr8.decoders.OpndDecoder.YDispImm0x2c070000;
import static bindis.avr8.decoders.OpndDecoder.YPlus;
import static bindis.avr8.decoders.OpndDecoder.Z;
import static bindis.avr8.decoders.OpndDecoder.ZDispImm0x2c070000;
import static bindis.avr8.decoders.OpndDecoder.ZPlus;

import java.util.HashMap;

/**
 *
 * @author mb0
 */
public final class OpcodeTables {
  public static final HashMap<Integer, InsnDecoder> noopTable = generateNoopTable();

  private OpcodeTables () {
  }

  public static final InsnDecoder[] prefixTable = {
    // 0
    Decode.decodeGroup(0),
    Decode.decodeGroup(1),
    Decode.decodeGroup(2),
    Decode.decode("cpi", RdUpper4, Imm0x0f0f0000),
    Decode.decode("sbci", RdUpper4, Imm0x0f0f0000),
    Decode.decode("subi", RdUpper4, Imm0x0f0f0000),
    Decode.decode("ori", RdUpper4, Imm0x0f0f0000),
    Decode.decode("andi", RdUpper4, Imm0x0f0f0000),
    // 8
    Decode.decodeOverlayGroup(9, Slice.S3191),
    Decode.decodeGroup(3),
    Decode.decodeOverlayGroup(9, Slice.S3191),
    Decode.decodeOverlayGroup(10, Slice.S111),
    Decode.decode("rjmp", ImmNegRelAdd0x0fff0000),
    Decode.decode("rcall", ImmNegRelAdd0x0fff0000),
    Decode.decode("ldi", RdUpper4, Imm0x0f0f0000),
    Decode.decodeGroup(4)
  };
  public static final InsnDecoder[][] groupTable = {
    // Group 0
    { // 0
      null,
      Decode.decode("movw", RdDv4, RrDv4),
      Decode.decode("muls", RdUpper4, RrUpper4),
      Decode.decodeOverlayGroup(0, Slice.F1),
      Decode.decode("cpc", Rd5, Rr5),
      Decode.decode("cpc", Rd5, Rr5),
      Decode.decode("cpc", Rd5, Rr5),
      Decode.decode("cpc", Rd5, Rr5),
      Decode.decode("sbc", Rd5, Rr5),
      Decode.decode("sbc", Rd5, Rr5),
      Decode.decode("sbc", Rd5, Rr5),
      Decode.decode("sbc", Rd5, Rr5),
      Decode.decode("add", Rd5, Rr5),
      Decode.decode("add", Rd5, Rr5),
      Decode.decode("add", Rd5, Rr5),
      Decode.decode("add", Rd5, Rr5)
    },
    // Group 1
    {
      Decode.decode("cpse", Rd5, Rr5),
      Decode.decode("cpse", Rd5, Rr5),
      Decode.decode("cpse", Rd5, Rr5),
      Decode.decode("cpse", Rd5, Rr5),
      Decode.decode("cp", Rd5, Rr5),
      Decode.decode("cp", Rd5, Rr5),
      Decode.decode("cp", Rd5, Rr5),
      Decode.decode("cp", Rd5, Rr5),
      Decode.decode("sub", Rd5, Rr5),
      Decode.decode("sub", Rd5, Rr5),
      Decode.decode("sub", Rd5, Rr5),
      Decode.decode("sub", Rd5, Rr5),
      Decode.decode("adc", Rd5, Rr5),
      Decode.decode("adc", Rd5, Rr5),
      Decode.decode("adc", Rd5, Rr5),
      Decode.decode("adc", Rd5, Rr5)
    },
    { // 0
      Decode.decode("and", Rd5, Rr5),
      Decode.decode("and", Rd5, Rr5),
      Decode.decode("and", Rd5, Rr5),
      Decode.decode("and", Rd5, Rr5),
      Decode.decode("eor", Rd5, Rr5),
      Decode.decode("eor", Rd5, Rr5),
      Decode.decode("eor", Rd5, Rr5),
      Decode.decode("eor", Rd5, Rr5),
      Decode.decode("or", Rd5, Rr5),
      Decode.decode("or", Rd5, Rr5),
      Decode.decode("or", Rd5, Rr5),
      Decode.decode("or", Rd5, Rr5),
      Decode.decode("mov", Rd5, Rr5),
      Decode.decode("mov", Rd5, Rr5),
      Decode.decode("mov", Rd5, Rr5),
      Decode.decode("mov", Rd5, Rr5)
    },
    {
      Decode.decodeOverlayGroup(4, Slice.S4),
      Decode.decodeOverlayGroup(4, Slice.S4),
      Decode.decodeOverlayGroup(3, Slice.S4),
      Decode.decodeOverlayGroup(3, Slice.S4),
      Decode.decodeOverlayGroup(1, Slice.S4),
      Decode.decodeOverlayGroup(1, Slice.S4),
      Decode.decode("adiw", RdDvUpper2, K),
      Decode.decode("sbiw", RdDvUpper2, K),
      Decode.decode("cbi", MemA, B),
      Decode.decode("sbic", A, B),
      Decode.decode("sbi", MemA, B),
      Decode.decode("sbis", A, B),
      Decode.decode("mul", Rd5, Rr5),
      Decode.decode("mul", Rd5, Rr5),
      Decode.decode("mul", Rd5, Rr5),
      Decode.decode("mul", Rd5, Rr5)
    },
    {
      Decode.decode("brbs", B, ImmNegRel0x03f80000),
      Decode.decode("brbs", B, ImmNegRel0x03f80000),
      Decode.decode("brbs", B, ImmNegRel0x03f80000),
      Decode.decode("brbs", B, ImmNegRel0x03f80000),
      Decode.decode("brbc", B, ImmNegRel0x03f80000),
      Decode.decode("brbc", B, ImmNegRel0x03f80000),
      Decode.decode("brbc", B, ImmNegRel0x03f80000),
      Decode.decode("brbc", B, ImmNegRel0x03f80000),
      Decode.decodeOverlayGroup(5, Slice.S31),
      null,
      Decode.decodeOverlayGroup(6, Slice.S31),
      Decode.decodeOverlayGroup(6, Slice.S31),
      Decode.decodeOverlayGroup(7, Slice.S31),
      Decode.decodeOverlayGroup(7, Slice.S31),
      Decode.decodeOverlayGroup(8, Slice.S31),
      Decode.decodeOverlayGroup(8, Slice.S31),
    },
  };
  public static final InsnDecoder[][] overlayGroupTable = {
    // Group 0
    { // 0
      Decode.decode("mulsu", RdUpperHalf3, RrUpperHalf3),
      Decode.decode("fmul", RdUpperHalf3, RrUpperHalf3),
      Decode.decode("fmuls", RdUpperHalf3, RrUpperHalf3),
      Decode.decode("fmulsu", RdUpperHalf3, RrUpperHalf3)
    },
    // Group 1
    { // 0
      Decode.decode("com", Rd5),
      Decode.decode("neg", Rd5),
      Decode.decode("swap", Rd5),
      Decode.decode("inc", Rd5),
      null,
      Decode.decode("asr", Rd5),
      Decode.decode("lsr", Rd5),
      Decode.decode("ror", Rd5),
      Decode.decodeOverlayGroup(2, Slice.Sr1),
      null,
      Decode.decode("dec", Rd5),
      Decode.decode("des", Imm0x00f00000),
      Decode.decode("jmp", KLong),
      Decode.decode("jmp", KLong),
      Decode.decode("call", KLong),
      Decode.decode("call", KLong)
    },
    { // 0
      Decode.decode("bset", S),
      Decode.decode("bclr", S),
      null,
      null
    },
    {
      Decode.decode("sts", MemImm0x0000ffff, Rd5),
      Decode.decode("st", ZPlus, Rd5),
      Decode.decode("st", MinusZ, Rd5),
      null,
      Decode.decode("xch", Rd5),
      Decode.decode("las", Rd5),
      Decode.decode("lac", Rd5),
      Decode.decode("lat", Rd5),
      null,
      Decode.decode("st", YPlus, Rd5),
      Decode.decode("st", MinusY, Rd5),
      null,
      Decode.decode("st", X, Rd5),
      Decode.decode("st", XPlus, Rd5),
      Decode.decode("st", MinusX, Rd5),
      Decode.decode("push", Rd5)
    },
    {
      Decode.decode("lds", Rd5, MemImm0x0000ffff),
      Decode.decode("ld", Rd5, ZPlus),
      Decode.decode("ld", Rd5, MinusZ),
      null,
      Decode.decode("lpm", Rd5, Z),
      Decode.decode("lpm", Rd5, ZPlus),
      Decode.decode("elpm", Rd5, Z),
      Decode.decode("elpm", Rd5, ZPlus),
      null,
      Decode.decode("ld", Rd5, YPlus),
      Decode.decode("ld", Rd5, MinusY),
      null,
      Decode.decode("ld", Rd5, X),
      Decode.decode("ld", Rd5, XPlus),
      Decode.decode("ld", Rd5, MinusX),
      Decode.decode("pop", Rd5)
    },
    {
      Decode.decode("bld", Rd5, B),
      null
    },
    {
      Decode.decode("bst", Rd5, B),
      null
    },
    {
      Decode.decode("sbrc", Rd5, B),
      null
    },
    {
      Decode.decode("sbrs", Rd5, B),
      null
    },
    {
      Decode.decode("ld", Rd5, ZDispImm0x2c070000),
      Decode.decode("ld", Rd5, YDispImm0x2c070000),
      Decode.decode("st", ZDispImm0x2c070000, Rd5),
      Decode.decode("st", YDispImm0x2c070000, Rd5)
    },
    {
      Decode.decode("in", Rd5, MemImm0x060f0000),
      Decode.decode("out", MemImm0x060f0000, Rd5)
    }
  };

  private static HashMap<Integer, InsnDecoder> generateNoopTable () {
    HashMap<Integer, InsnDecoder> noopTable = new HashMap<Integer, InsnDecoder>();

    noopTable.put(0x0000, Decode.decode("nop"));
    noopTable.put(0x9598, Decode.decode("break"));
    noopTable.put(0x9509, Decode.decode("icall"));
    noopTable.put(0x9519, Decode.decode("eicall"));
    noopTable.put(0x9409, Decode.decode("ijmp"));
    noopTable.put(0x9419, Decode.decode("eijmp"));
    noopTable.put(0x95c8, Decode.decode("lpm", Z));
    noopTable.put(0x9508, Decode.decode("ret"));
    noopTable.put(0x9518, Decode.decode("reti"));
    noopTable.put(0x9588, Decode.decode("sleep"));
    noopTable.put(0x95e8, Decode.decode("spm", Z));
    noopTable.put(0x95f8, Decode.decode("spm", ZPlus));
    noopTable.put(0x95a8, Decode.decode("wdr"));
    noopTable.put(0x95d8, Decode.decode("elpm", Z));

    return noopTable;
  }
}
