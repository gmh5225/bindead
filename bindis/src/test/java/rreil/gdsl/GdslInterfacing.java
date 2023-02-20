package rreil.gdsl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import gdsl.Gdsl;
import gdsl.decoder.Decoder;
import gdsl.decoder.NativeInstruction;
import gdsl.rreil.DefaultRReilBuilder;
import gdsl.rreil.IRReilBuilder;
import gdsl.rreil.IRReilCollection;
import gdsl.rreil.statement.IStatement;
import gdsl.translator.OptimizationOptions;
import gdsl.translator.TranslatedBlock;
import gdsl.translator.Translator;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedMap;

import javalx.numeric.BigInt;
import javalx.numeric.Interval;

import org.junit.Before;
import org.junit.Test;

import bindis.gdsl.GdslX86_32Disassembler;
import bindis.gdsl.GdslX86_64Disassembler;
import rreil.gdsl.builder.Builder;
import rreil.lang.BinOp;
import rreil.lang.ComparisonOp;
import rreil.lang.FlopOp;
import rreil.lang.Lhs;
import rreil.lang.LinBinOp;
import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.RReil.Branch.BranchTypeHint;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;

public class GdslInterfacing {
  @Before public void precondition () {
    // ignore Tests if the preconditions (native library installed) are not satisfied
    assumeTrue(haveGDSLNativeLibraries());
  }

  /**
   * See if the native lib for GDSL is present on this machine.
   */
  public static boolean haveGDSLNativeLibraries () {
    try {
      System.loadLibrary("jgdsl");
    } catch (UnsatisfiedLinkError e) {
      return false;
    }
    return true;
  }

  IRReilCollection<IStatement> translateSingle (Gdsl gdsl, IRReilBuilder builder) {
    Decoder decoder = new Decoder(gdsl);
    NativeInstruction insn = decoder.decodeOne();

    Translator translator = new Translator(gdsl, builder);
    return translator.translate(insn);
  }

  IRReilCollection<IStatement> translateBlock (Gdsl gdsl, IRReilBuilder builder) {
    Translator translator = new Translator(gdsl, builder);
    TranslatedBlock tb =
      translator.translateOptimizeBlock(Long.MAX_VALUE,
          OptimizationOptions.PRESERVE_CONTEXT.and(OptimizationOptions.LIVENESS).and(OptimizationOptions.FSUBST));
    return tb.getRreil();
  }

  IRReilCollection<IStatement> translateDispatch (boolean block, Gdsl gdsl, IRReilBuilder builder) {
    if (block)
      return translateBlock(gdsl, builder);
    else
      return translateSingle(gdsl, builder);
  }

  private void comparePrint (boolean block, byte[] bytes) {
//    X86Binder x86 = new X86Binder();
//    System.out.println("Using frontend " + x86.getFrontend() + "...");
    Gdsl gdsl = new Gdsl(GdslX86_32Disassembler.$Disassembler.getFrontend());

    System.out.println("Decoding and translating...");

    ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
    buffer.put(bytes);

    gdsl.setCode(buffer, 0, 0);
    IRReilCollection<IStatement> c = translateDispatch(block, gdsl, new DefaultRReilBuilder());

    System.out.println("GDSL RReil:");
    for (int i = 0; i < c.size(); i++) {
      System.out.println(c.get(i));
    }

    gdsl.setCode(buffer, 0, 0);
    c = translateDispatch(block, gdsl, new BindeadGdslRReilBuilder());
    @SuppressWarnings("unchecked")
    StatementCollection statements = ((Builder<StatementCollection>) c).build().getResult();

    System.out.println("Bindead RReil:");

    SortedMap<RReilAddr, RReil> instructions = statements.getInstructions();
    for (RReil r : instructions.values())
      System.out.println(r.getRReilAddress() + " " + r);
    for (RReil r : instructions.values())
      System.out.println("comparisonValue.add(" + r.reconstructCode() + ");");
  }

  private SortedMap<RReilAddr, RReil> testBindeadGdslX86Build (boolean block, byte[] bytes) {
    Gdsl gdsl = new Gdsl(GdslX86_32Disassembler.$Disassembler.getFrontend());

    ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
    buffer.put(bytes);

    gdsl.setCode(buffer, 0, 0);
    IRReilCollection<IStatement> c = translateDispatch(block, gdsl, new BindeadGdslRReilBuilder());
    @SuppressWarnings("unchecked")
    StatementCollection statements = ((Builder<StatementCollection>) c).build().getResult();

    return statements.getInstructions();
  }

  private void testBindeadGdslX86Compare (SortedMap<RReilAddr, RReil> instructions, ArrayList<RReil> comparisonValue) {
    assertEquals("Same number of instructions", instructions.size(), comparisonValue.size());

    for (Iterator<RReil> it_gdsl = instructions.values().iterator(), it_comp = comparisonValue.iterator(); it_gdsl
        .hasNext();) {
      RReil gdsl = it_gdsl.next();
      RReil comp = it_comp.next();
      assertTrue("gdsl:{" + gdsl.getRReilAddress().offset() + "$" + gdsl + " == " + comp.getRReilAddress().offset()
        + "$" + comp + "}:comp", gdsl.equals(comp));
    }
  }

  @Test public void testBindeadGdslRReilX86BsrInstruction () {
    byte[] bytes = new byte[] {(byte) 0x0f, (byte) 0xbd, (byte) 0xf0};

    comparePrint(false, bytes);

    MemVar.reset();
    SortedMap<RReilAddr, RReil> instructions = testBindeadGdslX86Build(false, bytes);

    ArrayList<RReil> comparisonValue = new ArrayList<RReil>();

    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 0), new Lhs(64, 0, MemVar.getVarOrFresh("ip")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(3))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 1), new Lhs(1, 0, MemVar.getVarOrFresh("t1")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("a"))), ComparisonOp.Cmpneq, new Rhs.LinRval(
        new Rhs.Rlit(32, BigInt.of(0))))));
    comparisonValue.add(new RReil.BranchToNative(new RReilAddr(0, 2), new Rhs.LinRval(new Rhs.Rvar(1, 0, MemVar
        .getVarOrFresh("t1"))), new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 4)))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 3),
      new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 16))), BranchTypeHint.Jump));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 4), new Lhs(32, 0, MemVar.getVarOrFresh("t0")),
      new Rhs.LinRval(new Rhs.Rlit(32, BigInt.of(31)))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 5), new Lhs(32, 0, MemVar.getVarOrFresh("t1")),
      new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("a")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 6), new Lhs(1, 0, MemVar.getVarOrFresh("t3")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rvar(1, 31, MemVar.getVarOrFresh("t1"))), ComparisonOp.Cmpneq, new Rhs.LinRval(
        new Rhs.Rlit(1, BigInt.of(1))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 7), new Lhs(1, 0, MemVar.getVarOrFresh("t2")),
      new Rhs.LinRval(new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("t3")))));
    comparisonValue.add(new RReil.BranchToNative(new RReilAddr(0, 8), new Rhs.LinRval(new Rhs.Rvar(1, 0, MemVar
        .getVarOrFresh("t2"))), new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 10)))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 9),
      new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 15))), BranchTypeHint.Jump));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 10), new Lhs(32, 0, MemVar.getVarOrFresh("t0")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("t0"))), LinBinOp.Sub, new Rhs.LinRval(
        new Rhs.Rlit(32, BigInt.of(1))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 11), new Lhs(32, 0, MemVar.getVarOrFresh("t1")), new Rhs.Bin(
      new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("t1")), BinOp.Shl, new Rhs.Rlit(32, BigInt.of(1)))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 12), new Lhs(1, 0, MemVar.getVarOrFresh("t3")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rvar(1, 31, MemVar.getVarOrFresh("t1"))), ComparisonOp.Cmpneq, new Rhs.LinRval(
        new Rhs.Rlit(1, BigInt.of(1))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 13), new Lhs(1, 0, MemVar.getVarOrFresh("t2")),
      new Rhs.LinRval(new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("t3")))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 14),
      new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 8))), BranchTypeHint.Jump));
    comparisonValue.add(new RReil.Nop(new RReilAddr(0, 15)));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 16), new Lhs(1, 6, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("a"))), ComparisonOp.Cmpeq, new Rhs.LinRval(
        new Rhs.Rlit(32, BigInt.of(0))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 17), new Lhs(1, 0, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 18), new Lhs(1, 11, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 19), new Lhs(1, 7, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 20), new Lhs(1, 4, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 21), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 22), new Lhs(1, 0, MemVar.getVarOrFresh("leu")), new Rhs.Bin(
      new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("flags")), BinOp.Or, new Rhs.Rvar(1, 6, MemVar.getVarOrFresh("flags")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 23), new Lhs(1, 0, MemVar.getVarOrFresh("lts")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rvar(1, 7, MemVar.getVarOrFresh("flags"))), ComparisonOp.Cmpneq, new Rhs.LinRval(
        new Rhs.Rvar(1, 11, MemVar.getVarOrFresh("flags"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 24), new Lhs(1, 0, MemVar.getVarOrFresh("les")), new Rhs.Bin(
      new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("lts")), BinOp.Or, new Rhs.Rvar(1, 6, MemVar.getVarOrFresh("flags")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 25), new Lhs(32, 0, MemVar.getVarOrFresh("si")),
      new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("t0")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 26), new Lhs(32, 32, MemVar.getVarOrFresh("si")),
      new Rhs.LinRval(new Rhs.Rlit(32, BigInt.of(0)))));


    testBindeadGdslX86Compare(instructions, comparisonValue);
  }

  @Test public void testBindeadGdslRReilX86AesdecInstruction () {
    byte[] bytes = new byte[] {(byte) 0x66, (byte) 0x0f, (byte) 0x38, (byte) 0xde, (byte) 0xc1};

    comparePrint(false, bytes);

    MemVar.reset();
    SortedMap<RReilAddr, RReil> instructions = testBindeadGdslX86Build(false, bytes);

    ArrayList<RReil> comparisonValue = new ArrayList<RReil>();

    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 0), new Lhs(64, 0, MemVar.getVarOrFresh("ip")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(5))))));
    comparisonValue.add(new RReil.PrimOp(new RReilAddr(0, 1), "AESDEC", Arrays.asList(new Lhs[] {new Lhs(128, 0, MemVar
        .getVarOrFresh("t0"))}), Arrays.asList(new Rhs.Rval[] {new Rhs.Rvar(128, 0, MemVar.getVarOrFresh("xmm0")),
      new Rhs.Rvar(128, 0, MemVar.getVarOrFresh("xmm1"))})));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 2), new Lhs(128, 0, MemVar.getVarOrFresh("xmm0")),
      new Rhs.LinRval(new Rhs.Rvar(128, 0, MemVar.getVarOrFresh("t0")))));


    testBindeadGdslX86Compare(instructions, comparisonValue);
  }

  @Test public void testBindeadGdslRReilX86FaddInstruction () {
    byte[] bytes = new byte[] {(byte) 0xdc, (byte) 0xc1};

    comparePrint(false, bytes);

    MemVar.reset();
    SortedMap<RReilAddr, RReil> instructions = testBindeadGdslX86Build(false, bytes);

    ArrayList<RReil> comparisonValue = new ArrayList<RReil>();

    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 0), new Lhs(64, 0, MemVar.getVarOrFresh("ip")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(2))))));
    comparisonValue.add(new RReil.Flop(new RReilAddr(0, 1), FlopOp.Fadd,
      new Rhs.Rvar(80, 0, MemVar.getVarOrFresh("t0")), Arrays.asList(new Rhs.Rvar[] {
        new Rhs.Rvar(80, 0, MemVar.getVarOrFresh("st1")), new Rhs.Rvar(80, 0, MemVar.getVarOrFresh("st0"))}),
      new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("floating_flags"))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 2), new Lhs(80, 0, MemVar.getVarOrFresh("st1")),
      new Rhs.LinRval(new Rhs.Rvar(80, 0, MemVar.getVarOrFresh("t0")))));


    testBindeadGdslX86Compare(instructions, comparisonValue);
  }
  
  @Test public void testBranch () {
    byte[] bytes = new byte[] {(byte) 0xe9, (byte) 0xa6, (byte)01, (byte)00, (byte)00};

    comparePrint(false, bytes);

    MemVar.reset();
    SortedMap<RReilAddr, RReil> instructions = testBindeadGdslX86Build(false, bytes);

    ArrayList<RReil> comparisonValue = new ArrayList<RReil>();
    
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 0), new Lhs(64, 0, MemVar.getVarOrFresh("ip")), new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(new Rhs.Rlit(64, BigInt.of(5))))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 1), new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(new Rhs.Rlit(64, BigInt.of(422)))), BranchTypeHint.Jump));
    comparisonValue.add(new RReil.Nop(new RReilAddr(0, 2)));

    testBindeadGdslX86Compare(instructions, comparisonValue);
  }
  
  @Test public void testCondBranch () {
    byte[] bytes = new byte[] {(byte) 0x75, (byte) 0xd0};

    comparePrint(false, bytes);

    MemVar.reset();
    SortedMap<RReilAddr, RReil> instructions = testBindeadGdslX86Build(false, bytes);

    ArrayList<RReil> comparisonValue = new ArrayList<RReil>();
    
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 0), new Lhs(64, 0, MemVar.getVarOrFresh("ip")), new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(new Rhs.Rlit(64, BigInt.of(2))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 1), new Lhs(1, 0, MemVar.getVarOrFresh("t0")), new Rhs.Bin(new Rhs.Rvar(1, 6, MemVar.getVarOrFresh("flags")), BinOp.Xor, new Rhs.Rlit(1, BigInt.of(1)))));
    comparisonValue.add(new RReil.BranchToNative(new RReilAddr(0, 2), new Rhs.LinRval(new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("t0"))), new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rlit(64, BigInt.of(-48))), LinBinOp.Add, new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 3), new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), BranchTypeHint.Jump));
    comparisonValue.add(new RReil.Nop(new RReilAddr(0, 4)));

    testBindeadGdslX86Compare(instructions, comparisonValue);
  }

  @Test public void testCompareConditionalBranchBlockOpt () {
    byte[] bytes = new byte[] {(byte) 0x3b, (byte) 0xfe, (byte) 0x78, (byte) 0x05, (byte) 0xc3};

    comparePrint(true, bytes);

    MemVar.reset();
    SortedMap<RReilAddr, RReil> instructions = testBindeadGdslX86Build(true, bytes);

    ArrayList<RReil> comparisonValue = new ArrayList<RReil>();

    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 0), new Lhs(64, 0, MemVar.getVarOrFresh("ip")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(2))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 1), new Lhs(1, 7, MemVar.getVarOrFresh("flags")), new Rhs.Cmp(
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("di"))), LinBinOp.Sub, new Rhs.LinRval(
        new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("si")))), ComparisonOp.Cmplts, new Rhs.LinRval(new Rhs.Rlit(32, BigInt
          .of(0))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 2), new Lhs(1, 6, MemVar.getVarOrFresh("flags")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("di"))), ComparisonOp.Cmpeq, new Rhs.LinRval(
        new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("si"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 3), new Lhs(1, 0, MemVar.getVarOrFresh("flags")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("di"))), ComparisonOp.Cmpltu, new Rhs.LinRval(
        new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("si"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 4), new Lhs(1, 0, MemVar.getVarOrFresh("leu")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("di"))), ComparisonOp.Cmpleu, new Rhs.LinRval(
        new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("si"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 5), new Lhs(1, 0, MemVar.getVarOrFresh("lts")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("di"))), ComparisonOp.Cmplts, new Rhs.LinRval(
        new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("si"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 6), new Lhs(1, 11, MemVar.getVarOrFresh("flags")),
      new Rhs.Bin(new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("lts")), BinOp.Xor, new Rhs.Rvar(1, 7, MemVar
          .getVarOrFresh("flags")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 7), new Lhs(1, 0, MemVar.getVarOrFresh("les")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("di"))), ComparisonOp.Cmples, new Rhs.LinRval(
        new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("si"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 8), new Lhs(1, 4, MemVar.getVarOrFresh("flags")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rvar(4, 0, MemVar.getVarOrFresh("di"))), ComparisonOp.Cmpltu, new Rhs.LinRval(
        new Rhs.Rvar(4, 0, MemVar.getVarOrFresh("si"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 9), new Lhs(8, 0, MemVar.getVarOrFresh("t4")), new Rhs.LinBin(
      new Rhs.LinRval(new Rhs.Rvar(8, 0, MemVar.getVarOrFresh("di"))), LinBinOp.Sub, new Rhs.LinRval(new Rhs.Rvar(8, 0,
        MemVar.getVarOrFresh("si"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 10), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(1, 7, MemVar.getVarOrFresh("t4"))), ComparisonOp.Cmpeq, new Rhs.LinRval(
        new Rhs.Rvar(1, 6, MemVar.getVarOrFresh("t4"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 11), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(1, 2, MemVar.getVarOrFresh("flags"))), ComparisonOp.Cmpeq,
        new Rhs.LinRval(new Rhs.Rvar(1, 5, MemVar.getVarOrFresh("t4"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 12), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(1, 2, MemVar.getVarOrFresh("flags"))), ComparisonOp.Cmpeq,
        new Rhs.LinRval(new Rhs.Rvar(1, 4, MemVar.getVarOrFresh("t4"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 13), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(1, 2, MemVar.getVarOrFresh("flags"))), ComparisonOp.Cmpeq,
        new Rhs.LinRval(new Rhs.Rvar(1, 3, MemVar.getVarOrFresh("t4"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 14), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(1, 2, MemVar.getVarOrFresh("flags"))), ComparisonOp.Cmpeq,
        new Rhs.LinRval(new Rhs.Rvar(1, 2, MemVar.getVarOrFresh("t4"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 15), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(1, 2, MemVar.getVarOrFresh("flags"))), ComparisonOp.Cmpeq,
        new Rhs.LinRval(new Rhs.Rvar(1, 1, MemVar.getVarOrFresh("t4"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 16), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(1, 2, MemVar.getVarOrFresh("flags"))), ComparisonOp.Cmpeq,
        new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("di"))), LinBinOp.Sub, new Rhs.LinRval(
          new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("si")))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 17), new Lhs(64, 0, MemVar.getVarOrFresh("ip")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(2))))));
    comparisonValue.add(new RReil.BranchToNative(new RReilAddr(0, 18), new Rhs.Cmp(new Rhs.LinBin(new Rhs.LinRval(
      new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("di"))), LinBinOp.Sub, new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar
        .getVarOrFresh("si")))), ComparisonOp.Cmplts, new Rhs.LinRval(new Rhs.Rlit(32, BigInt.of(0)))), new Rhs.LinBin(
      new Rhs.LinRval(new Rhs.Rlit(64, BigInt.of(5))), LinBinOp.Add, new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar
          .getVarOrFresh("ip"))))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 19), new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar
        .getVarOrFresh("ip"))), BranchTypeHint.Jump));

    testBindeadGdslX86Compare(instructions, comparisonValue);
  }

  @Test public void testStoreBlockOpt () {
    byte[] bytes = new byte[] {(byte) 0xc6, (byte) 0x44, (byte) 0x1f, (byte) 0x03, (byte) 0x3f, (byte) 0xc3};

    comparePrint(true, bytes);

    MemVar.reset();
    SortedMap<RReilAddr, RReil> instructions = testBindeadGdslX86Build(true, bytes);

    ArrayList<RReil> comparisonValue = new ArrayList<RReil>();

    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 0), new Lhs(64, 0, MemVar.getVarOrFresh("ip")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(5))))));
    comparisonValue.add(new RReil.Store(new RReilAddr(0, 1), new Rhs.LinBin(new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(
      64, 0, MemVar.getVarOrFresh("di"))), LinBinOp.Add, new Rhs.LinScale(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar
        .getVarOrFresh("b"))), BigInt.of(1))), LinBinOp.Add, new Rhs.LinRval(new Rhs.Rlit(64, BigInt.of(3)))),
      new Rhs.LinRval(new Rhs.Rlit(8, BigInt.of(63)))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 2), new Lhs(64, 0, MemVar.getVarOrFresh("ip")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(1))))));
    comparisonValue.add(new RReil.Load(new RReilAddr(0, 3), new Lhs(64, 0, MemVar.getVarOrFresh("t0")), new Rhs.LinRval(
      new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("sp")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 4), new Lhs(64, 0, MemVar.getVarOrFresh("sp")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("sp"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(8))))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 5), new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar
        .getVarOrFresh("t0"))), BranchTypeHint.Return));

    testBindeadGdslX86Compare(instructions, comparisonValue);
  }

  @Test public void testLoadBlockOpt () {
    byte[] bytes = new byte[] {(byte) 0x0f, (byte) 0xb6, (byte) 0x34, (byte) 0x07, (byte) 0xc3};

    comparePrint(true, bytes);

    MemVar.reset();
    SortedMap<RReilAddr, RReil> instructions = testBindeadGdslX86Build(true, bytes);

    ArrayList<RReil> comparisonValue = new ArrayList<RReil>();

    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 0), new Lhs(64, 0, MemVar.getVarOrFresh("ip")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(4))))));
    comparisonValue.add(new RReil.Load(new RReilAddr(0, 1), new Lhs(8, 0, MemVar.getVarOrFresh("t0")), new Rhs.LinBin(
      new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("di"))), LinBinOp.Add, new Rhs.LinScale(new Rhs.LinRval(
        new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("a"))), BigInt.of(1)))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 2), new Lhs(32, 0, MemVar.getVarOrFresh("t1")),
      new Rhs.Convert(new Rhs.Rvar(8, 0, MemVar.getVarOrFresh("t0")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 3), new Lhs(32, 0, MemVar.getVarOrFresh("si")),
      new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("t1")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 4), new Lhs(32, 32, MemVar.getVarOrFresh("si")),
      new Rhs.LinRval(new Rhs.Rlit(32, BigInt.of(0)))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 5), new Lhs(64, 0, MemVar.getVarOrFresh("ip")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(1))))));
    comparisonValue.add(new RReil.Load(new RReilAddr(0, 6), new Lhs(64, 0, MemVar.getVarOrFresh("t0")), new Rhs.LinRval(
      new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("sp")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 7), new Lhs(64, 0, MemVar.getVarOrFresh("sp")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("sp"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(8))))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 8), new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar
        .getVarOrFresh("t0"))), BranchTypeHint.Return));

    testBindeadGdslX86Compare(instructions, comparisonValue);
  }

  @Test public void testShrBlockOpt () {
    byte[] bytes = new byte[] {(byte) 0xd3, (byte) 0xe8, (byte) 0xc3};

    comparePrint(true, bytes);

    MemVar.reset();
    SortedMap<RReilAddr, RReil> instructions = testBindeadGdslX86Build(true, bytes);

    ArrayList<RReil> comparisonValue = new ArrayList<RReil>();

    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 0), new Lhs(64, 0, MemVar.getVarOrFresh("ip")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(2))))));
    comparisonValue.add(new RReil.BranchToNative(new RReilAddr(0, 1), new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(32, 0,
      MemVar.getVarOrFresh("c"))), ComparisonOp.Cmpneq, new Rhs.LinRval(new Rhs.Rlit(32, BigInt.of(0)))),
      new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 3)))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 2), new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 4))),
      BranchTypeHint.Jump));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 3), new Lhs(1, 4, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 4), new Lhs(5, 0, MemVar.getVarOrFresh("t0")),
      new Rhs.LinRval(new Rhs.Rvar(5, 0, MemVar.getVarOrFresh("c")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 5), new Lhs(27, 5, MemVar.getVarOrFresh("t0")),
      new Rhs.LinRval(new Rhs.Rlit(27, BigInt.of(0)))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 6), new Lhs(1, 0, MemVar.getVarOrFresh("t2")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("t0"))), ComparisonOp.Cmpleu, new Rhs.LinRval(
        new Rhs.Rlit(32, BigInt.of(0))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 7), new Lhs(1, 0, MemVar.getVarOrFresh("t2")), new Rhs.Bin(
      new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("t2")), BinOp.Xor, new Rhs.Rlit(1, BigInt.of(1)))));
    comparisonValue.add(new RReil.BranchToNative(new RReilAddr(0, 8), new Rhs.LinRval(new Rhs.Rvar(1, 0, MemVar
        .getVarOrFresh("t2"))), new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 14)))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 9), new Lhs(32, 0, MemVar.getVarOrFresh("t1")),
      new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("a")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 10), new Lhs(1, 7, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 11), new Lhs(1, 6, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 12), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 13),
      new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 33))), BranchTypeHint.Jump));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 14), new Lhs(32, 0, MemVar.getVarOrFresh("tt0")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("t0"))), LinBinOp.Sub, new Rhs.LinRval(
        new Rhs.Rlit(32, BigInt.of(1))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 15), new Lhs(32, 0, MemVar.getVarOrFresh("t1")), new Rhs.Bin(
      new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("a")), BinOp.Shr, new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("tt0")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 16), new Lhs(1, 0, MemVar.getVarOrFresh("t3")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rlit(8, BigInt.of(32))), ComparisonOp.Cmpltu, new Rhs.LinRval(new Rhs.Rvar(8, 0, MemVar
          .getVarOrFresh("c"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 17), new Lhs(1, 0, MemVar.getVarOrFresh("t3")), new Rhs.Bin(
      new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("t3")), BinOp.Xor, new Rhs.Rlit(1, BigInt.of(1)))));
    comparisonValue.add(new RReil.BranchToNative(new RReilAddr(0, 18), new Rhs.LinRval(new Rhs.Rvar(1, 0, MemVar
        .getVarOrFresh("t3"))), new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 21)))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 19), new Lhs(1, 0, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 20),
      new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 22))), BranchTypeHint.Jump));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 21), new Lhs(1, 0, MemVar.getVarOrFresh("flags")),
      new Rhs.LinRval(new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("t1")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 22), new Lhs(32, 0, MemVar.getVarOrFresh("t1")), new Rhs.Bin(
      new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("t1")), BinOp.Shr, new Rhs.Rlit(32, BigInt.of(1)))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 23), new Lhs(1, 7, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("t1"))), ComparisonOp.Cmplts,
        new Rhs.LinRval(new Rhs.Rlit(32, BigInt.of(0))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 24), new Lhs(8, 0, MemVar.getVarOrFresh("t3")),
      new Rhs.LinRval(new Rhs.Rvar(8, 0, MemVar.getVarOrFresh("t1")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 25), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(1, 7, MemVar.getVarOrFresh("t3"))), ComparisonOp.Cmpeq, new Rhs.LinRval(
        new Rhs.Rvar(1, 6, MemVar.getVarOrFresh("t3"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 26), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(1, 2, MemVar.getVarOrFresh("flags"))), ComparisonOp.Cmpeq,
        new Rhs.LinRval(new Rhs.Rvar(1, 5, MemVar.getVarOrFresh("t3"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 27), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(1, 2, MemVar.getVarOrFresh("flags"))), ComparisonOp.Cmpeq,
        new Rhs.LinRval(new Rhs.Rvar(1, 4, MemVar.getVarOrFresh("t3"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 28), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(1, 2, MemVar.getVarOrFresh("flags"))), ComparisonOp.Cmpeq,
        new Rhs.LinRval(new Rhs.Rvar(1, 3, MemVar.getVarOrFresh("t3"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 29), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(1, 2, MemVar.getVarOrFresh("flags"))), ComparisonOp.Cmpeq,
        new Rhs.LinRval(new Rhs.Rvar(1, 2, MemVar.getVarOrFresh("t3"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 30), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(1, 2, MemVar.getVarOrFresh("flags"))), ComparisonOp.Cmpeq,
        new Rhs.LinRval(new Rhs.Rvar(1, 1, MemVar.getVarOrFresh("t3"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 31), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(1, 2, MemVar.getVarOrFresh("flags"))), ComparisonOp.Cmpeq,
        new Rhs.LinRval(new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("t1"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 32), new Lhs(1, 6, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("t1"))), ComparisonOp.Cmpeq, new Rhs.LinRval(
        new Rhs.Rlit(32, BigInt.of(0))))));
    comparisonValue.add(new RReil.BranchToNative(new RReilAddr(0, 33), new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(32, 0,
      MemVar.getVarOrFresh("t0"))), ComparisonOp.Cmpeq, new Rhs.LinRval(new Rhs.Rlit(32, BigInt.of(1)))),
      new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 39)))));
    comparisonValue.add(new RReil.BranchToNative(new RReilAddr(0, 34), new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(32, 0,
      MemVar.getVarOrFresh("t0"))), ComparisonOp.Cmpneq, new Rhs.LinRval(new Rhs.Rlit(32, BigInt.of(0)))),
      new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 36)))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 35),
      new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 37))), BranchTypeHint.Jump));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 36), new Lhs(1, 11, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Nop(new RReilAddr(0, 37)));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 38),
      new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 41))), BranchTypeHint.Jump));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 39), new Lhs(32, 0, MemVar.getVarOrFresh("t2")),
      new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("a")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 40), new Lhs(1, 11, MemVar.getVarOrFresh("flags")),
      new Rhs.LinRval(new Rhs.Rvar(1, 31, MemVar.getVarOrFresh("t2")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 41), new Lhs(1, 0, MemVar.getVarOrFresh("leu")), new Rhs.Bin(
      new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("flags")), BinOp.Or, new Rhs.Rvar(1, 6, MemVar.getVarOrFresh("flags")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 42), new Lhs(1, 0, MemVar.getVarOrFresh("lts")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rvar(1, 7, MemVar.getVarOrFresh("flags"))), ComparisonOp.Cmpneq, new Rhs.LinRval(
        new Rhs.Rvar(1, 11, MemVar.getVarOrFresh("flags"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 43), new Lhs(1, 0, MemVar.getVarOrFresh("les")), new Rhs.Bin(
      new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("lts")), BinOp.Or, new Rhs.Rvar(1, 6, MemVar.getVarOrFresh("flags")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 44), new Lhs(32, 0, MemVar.getVarOrFresh("a")),
      new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("t1")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 45), new Lhs(32, 32, MemVar.getVarOrFresh("a")),
      new Rhs.LinRval(new Rhs.Rlit(32, BigInt.of(0)))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 46), new Lhs(64, 0, MemVar.getVarOrFresh("ip")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(1))))));
    comparisonValue.add(new RReil.Load(new RReilAddr(0, 47), new Lhs(64, 0, MemVar.getVarOrFresh("t0")),
      new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("sp")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 48), new Lhs(64, 0, MemVar.getVarOrFresh("sp")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("sp"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(8))))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 49), new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar
        .getVarOrFresh("t0"))), BranchTypeHint.Return));

    testBindeadGdslX86Compare(instructions, comparisonValue);
  }

  @Test public void testBsfBlockOpt () {
    byte[] bytes = new byte[] {(byte) 0x0f, (byte) 0xbc, (byte) 0xc0, (byte) 0xc3};

    comparePrint(true, bytes);

    MemVar.reset();
    SortedMap<RReilAddr, RReil> instructions = testBindeadGdslX86Build(true, bytes);

    ArrayList<RReil> comparisonValue = new ArrayList<RReil>();

    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 0), new Lhs(64, 0, MemVar.getVarOrFresh("ip")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(3))))));
    comparisonValue.add(new RReil.BranchToNative(new RReilAddr(0, 1), new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(32, 0,
      MemVar.getVarOrFresh("a"))), ComparisonOp.Cmpneq, new Rhs.LinRval(new Rhs.Rlit(32, BigInt.of(0)))),
      new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 3)))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 2),
      new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 13))), BranchTypeHint.Jump));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 3), new Lhs(32, 0, MemVar.getVarOrFresh("t0")),
      new Rhs.LinRval(new Rhs.Rlit(32, BigInt.of(0)))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 4), new Lhs(32, 0, MemVar.getVarOrFresh("t1")),
      new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("a")))));
    comparisonValue.add(new RReil.BranchToNative(new RReilAddr(0, 5), new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(1, 0,
      MemVar.getVarOrFresh("a"))), ComparisonOp.Cmpneq, new Rhs.LinRval(new Rhs.Rlit(1, BigInt.of(1)))),
      new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 7)))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 6),
      new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 12))), BranchTypeHint.Jump));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 7), new Lhs(32, 0, MemVar.getVarOrFresh("t0")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("t0"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(32, BigInt.of(1))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 8), new Lhs(32, 0, MemVar.getVarOrFresh("t1")), new Rhs.Bin(
      new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("t1")), BinOp.Shr, new Rhs.Rlit(32, BigInt.of(1)))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 9), new Lhs(1, 0, MemVar.getVarOrFresh("t3")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("t1"))), ComparisonOp.Cmpneq, new Rhs.LinRval(
        new Rhs.Rlit(1, BigInt.of(1))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 10), new Lhs(1, 0, MemVar.getVarOrFresh("t2")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("t1"))), ComparisonOp.Cmpneq, new Rhs.LinRval(
        new Rhs.Rlit(1, BigInt.of(1))))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 11),
      new Rhs.LinRval(new Rhs.Address(64, new RReilAddr(0, 5))), BranchTypeHint.Jump));
    comparisonValue.add(new RReil.Nop(new RReilAddr(0, 12)));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 13), new Lhs(1, 6, MemVar.getVarOrFresh("flags")),
      new Rhs.Cmp(new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("a"))), ComparisonOp.Cmpeq, new Rhs.LinRval(
        new Rhs.Rlit(32, BigInt.of(0))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 14), new Lhs(1, 0, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 15), new Lhs(1, 11, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 16), new Lhs(1, 7, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 17), new Lhs(1, 4, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 18), new Lhs(1, 2, MemVar.getVarOrFresh("flags")),
      new Rhs.RangeRhs(1, Interval.unsignedTop(1))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 19), new Lhs(1, 0, MemVar.getVarOrFresh("leu")), new Rhs.Bin(
      new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("flags")), BinOp.Or, new Rhs.Rvar(1, 6, MemVar.getVarOrFresh("flags")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 20), new Lhs(1, 0, MemVar.getVarOrFresh("lts")), new Rhs.Cmp(
      new Rhs.LinRval(new Rhs.Rvar(1, 7, MemVar.getVarOrFresh("flags"))), ComparisonOp.Cmpneq, new Rhs.LinRval(
        new Rhs.Rvar(1, 11, MemVar.getVarOrFresh("flags"))))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 21), new Lhs(1, 0, MemVar.getVarOrFresh("les")), new Rhs.Bin(
      new Rhs.Rvar(1, 0, MemVar.getVarOrFresh("lts")), BinOp.Or, new Rhs.Rvar(1, 6, MemVar.getVarOrFresh("flags")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 22), new Lhs(32, 0, MemVar.getVarOrFresh("a")),
      new Rhs.LinRval(new Rhs.Rvar(32, 0, MemVar.getVarOrFresh("t0")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 23), new Lhs(32, 32, MemVar.getVarOrFresh("a")),
      new Rhs.LinRval(new Rhs.Rlit(32, BigInt.of(0)))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 24), new Lhs(64, 0, MemVar.getVarOrFresh("ip")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("ip"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(1))))));
    comparisonValue.add(new RReil.Load(new RReilAddr(0, 25), new Lhs(64, 0, MemVar.getVarOrFresh("t0")),
      new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("sp")))));
    comparisonValue.add(new RReil.Assign(new RReilAddr(0, 26), new Lhs(64, 0, MemVar.getVarOrFresh("sp")),
      new Rhs.LinBin(new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar.getVarOrFresh("sp"))), LinBinOp.Add, new Rhs.LinRval(
        new Rhs.Rlit(64, BigInt.of(8))))));
    comparisonValue.add(new RReil.Branch(new RReilAddr(0, 27), new Rhs.LinRval(new Rhs.Rvar(64, 0, MemVar
        .getVarOrFresh("t0"))), BranchTypeHint.Return));

    testBindeadGdslX86Compare(instructions, comparisonValue);
  }
  
  @Test public void testStosdOpt () {
    byte[] bytes = new byte[] {(byte)0xb9, (byte)0x0a, 00, 00, (byte)00, (byte)0xf3, (byte) 0xab, (byte)0xc3};

    comparePrint(true, bytes);

    MemVar.reset();
    SortedMap<RReilAddr, RReil> instructions = testBindeadGdslX86Build(true, bytes);

    ArrayList<RReil> comparisonValue = new ArrayList<RReil>();

    testBindeadGdslX86Compare(instructions, comparisonValue);
  }
}
