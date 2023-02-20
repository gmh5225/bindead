package bindis.x86.x32;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import bindis.Callback;
import bindis.DecodeStream;
import bindis.Disassembler;
import bindis.x86.x32.X32Disassembler;
import rreil.disassembler.Instruction;
import binparse.Binary;
import binparse.Segment;
import binparse.elf.ElfBinary;

public class X32MnemonicComparisonTest {
  private static final String $OpcodeBinary = X32MnemonicComparisonTest.class.getResource("/opcodes32-reduced-corkami.o").getPath();
  private static final String $OpcodeXed2Output = X32MnemonicComparisonTest.class.getResource("/opcodes32-reduced-corkami-xed2.txt").getPath();

  @Test public void compareMnemonics () throws IOException {
    Map<Long, Columns> xed2Instructions = parseXed2Output();
    Map<Long, Instruction> nativeInstructions = decodeBinary();
    for (Columns xed2Insn : xed2Instructions.values()) {
      Instruction nativeInsn = nativeInstructions.get(xed2Insn.address());
      if (nativeInsn.mnemonic().equals("ja")) // skip the "ja" mnemonic as it is called "jnbe" in xed
        continue;
      if (!xed2Insn.mnemonic().equals(nativeInsn.mnemonic()))
        assertThat("Comparing native instruction: " + nativeInsn.toString(), nativeInsn.mnemonic(), is(xed2Insn.mnemonic()));
    }
    assertThat(xed2Instructions.size(), is(nativeInstructions.size()));
  }

  private static Map<Long, Columns> parseXed2Output () throws IOException {
    Map<Long, Columns> insns = new HashMap<Long, Columns>();
    BufferedReader in = new BufferedReader(new FileReader($OpcodeXed2Output));
    for (String line = in.readLine(); line != null; line = in.readLine()) {
      if (line.startsWith("ERROR"))
        continue;
      Columns row = new Columns(line);
      insns.put(row.address(), row);
    }
    in.close();
    return insns;
  }

  private static Map<Long, Instruction> decodeBinary () throws IOException {
    final Map<Long, Instruction> insns = new HashMap<Long, Instruction>();
    Binary binary = new ElfBinary($OpcodeBinary);
    Segment codeSection = binary.getSegment(".text").get();
    LinearSweepDisassembler.decodeLinearSweep(codeSection.getData(), 0, codeSection.getAddress(), new Callback<Instruction>() {
      @Override public boolean visit (Instruction insn) {
        insns.put(insn.baseAddress(), insn);
        return true;
      }
    });
    return insns;
  }

  public static class LinearSweepDisassembler {
    private static final Disassembler dis = X32Disassembler.INSTANCE;;

    private static void decodeLinearSweep (byte[] data, int i, long startPc, Callback<Instruction> callback) {
      DecodeStream in = new DecodeStream(data, i);
      long currentPc = startPc;
      while (in.available() > 0) {
        in.mark();
        Instruction insn = dis.decodeOne(in, currentPc);
        currentPc += in.consumed();
        if (!callback.visit(insn))
          return;
      }
    }
  }

  public static class Columns {
    private static int $AddressIndex = 1;
    private static int $MnemonicIndex = 5;
    private final String[] columns;

    public Columns (String line) {
      this.columns = line.replace("\t", " ").split(" +");
    }

    public Long address () {
      String address = columns[$AddressIndex];
      return Long.parseLong(address.substring(0, address.length() - 1), 16);
    }

    public String mnemonic () {
      String mnemonic = columns[$MnemonicIndex];
      if (mnemonic.startsWith("data"))
        mnemonic = columns[$MnemonicIndex + 1];
      if (mnemonic.equals("lock"))
        mnemonic = "lock " + columns[$MnemonicIndex + 1];
      return mnemonic;
    }
  }
}
