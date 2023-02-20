package rreil.assembler;


import static rreil.assembler.TestsHelper.lines;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.SortedMap;

import org.junit.Test;

import rreil.assembler.parser.ASTModule;
import rreil.assembler.parser.ParseException;
import rreil.assembler.parser.RReilParser;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;

public class ParseTreeTranslatorTest {
  @Test public void translateAddInstruction () throws ParseException {
    String input = ".@sp.d@?\n.@bp.d@0x100\nadd.d r3,r1,r2";
    parseAndDump(input);
  }

  @Test public void translateAddInstructionWithRegisterOffset () throws ParseException {
    String input = ".@sp.d@?\n.@bp.d@0x100\nadd.d r3,r1,r2/32";
    parseAndDump(input);
  }

  public void parseAndDump (String input) throws ParseException {
    Reader in = new BufferedReader(new StringReader(input));
    RReilParser p = new RReilParser(in);
    ASTModule module = p.Module();
 // output the module structure if needed for debugging
//  module.dump("");
    ParseTreeTranslator translator = new ParseTreeTranslator();
    CompiledAssembler cu = translator.translate(module);
    for (RReil insn : cu.getInstructions().values()) {
// TODO: test only prints the instructions. Add assertions here.
//      System.out.println(insn);
    }
  }

  @Test public void parseRangeVariant1 () throws ParseException {
    parseAndDump("mov.d r1,[1, 2]");
  }

  @Test public void parseRangeVariant2 () throws ParseException {
    parseAndDump("mov.d r1,[1, +oo]");
  }

  @Test public void parseRangeVariant3 () throws ParseException {
    parseAndDump("mov.d r1,[-oo, 2]");
  }

  @Test public void translateFromFile () throws ParseException {
    InputStream in = ParseTreeTranslatorTest.class.getResourceAsStream("/rreil/assembler/example-instructions.rr");
    RReilParser p = new RReilParser(in);
    ASTModule module = p.Module();
 // output the module structure if needed for debugging
//  module.dump("");
    ParseTreeTranslator translator = new ParseTreeTranslator();
    CompiledAssembler cu = translator.translate(module);
    for (RReil insn : cu.getInstructions().values()) {
   // TODO: test only prints the instructions. Add assertions here.
//    System.out.println(insn);
    }
  }

  @Test public void halt01 () throws ParseException {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "store sp, 1",
        "mov bp, sp",
        "mov eax, [0, 16]",
        "mul t0, eax, 4",
        "add t1, bp, t0",
        "store t1, 0xf00",
        "exit:",
        "halt"
      );
    RReilParser p = new RReilParser(new StringReader(assembly));
    ASTModule module = p.Module();
    ParseTreeTranslator translator = new ParseTreeTranslator();
    translator.translate(module);
  }

  @Test public void halt02 () throws ParseException {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "store sp, 1",
        "mov bp, sp",
        "mov eax, [0, 16]",
        "mul t0, eax, 4",
        "add t1, bp, t0",
        "store t1, 0xf00",
        "exit:",
        "prim () = halt ()"
      );
    RReilParser p = new RReilParser(new ByteArrayInputStream(assembly.getBytes()));
    ASTModule module = p.Module();
    ParseTreeTranslator translator = new ParseTreeTranslator();
    translator.translate(module);
  }

  @Test public void halt03 () throws ParseException {
    String assembly = lines(
        "option DEFAULT_SIZE = d",
        "store sp, 1",
        "mov bp, sp",
        "mov eax, [0, 16]",
        "mul t0, eax, 4",
        "add t1, bp, t0",
        "store t1, 0xf00",
        "exit:",
        "prim () = halt"
      );
    RReilParser p = new RReilParser(new ByteArrayInputStream(assembly.getBytes()));
    ASTModule module = p.Module();
    ParseTreeTranslator translator = new ParseTreeTranslator();
    translator.translate(module);
  }

  @Test public void halt04 () throws ParseException {
    String assembly = lines(
        "option DEFAULT_SIZE = d",
        "store sp, 1",
        "mov bp, sp",
        "mov eax, [0, 16]",
        "mul t0, eax, 4",
        "add t1, bp, t0",
        "store t1, 0xf00",
        "exit:",
        "prim halt()"
      );
    RReilParser p = new RReilParser(new ByteArrayInputStream(assembly.getBytes()));
    ASTModule module = p.Module();
    ParseTreeTranslator translator = new ParseTreeTranslator();
    translator.translate(module);
  }

  @Test public void halt05 () throws ParseException {
    String assembly = lines(
        "option DEFAULT_SIZE = d",
        "store sp, 1",
        "mov bp, sp",
        "mov eax, [0, 16]",
        "mul t0, eax, 4",
        "add t1, bp, t0",
        "store t1, 0xf00",
        "exit:",
        "prim halt"
      );
    RReilParser p = new RReilParser(new ByteArrayInputStream(assembly.getBytes()));
    ASTModule module = p.Module();
    ParseTreeTranslator translator = new ParseTreeTranslator();
    translator.translate(module);
  }

  @Test public void instructionsWithAddress () throws ParseException {
      // instruction addresses need to be hex values
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "        store sp, 1",
        "0x1234: mov bp, sp",
        "        mov eax, [0, 16]",
        "        mul t0, eax, 4",
        "0x123:    add t1, bp, t0",
        "        store t1, 0xf00",
        "exit:",
        "halt"
      );
    RReilParser p = new RReilParser(new StringReader(assembly));
    ASTModule module = p.Module();
    ParseTreeTranslator translator = new ParseTreeTranslator();
    CompiledAssembler assembler = translator.translate(module);
    SortedMap<RReilAddr, RReil> instructions = assembler.getInstructions();
    assert instructions.containsKey(RReilAddr.valueOf(0));
    assert instructions.containsKey(RReilAddr.valueOf(0x1234));
    assert instructions.containsKey(RReilAddr.valueOf(0x1235));
    assert instructions.containsKey(RReilAddr.valueOf(0x123));
    assert instructions.containsKey(RReilAddr.valueOf(0x124));
  }
}
