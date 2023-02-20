package rreil.assembler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;

import org.junit.Test;

import rreil.assembler.parser.ASTIdent;
import rreil.assembler.parser.ASTInsn;
import rreil.assembler.parser.ASTIntegerOrNull;
import rreil.assembler.parser.ASTLabel;
import rreil.assembler.parser.ASTModule;
import rreil.assembler.parser.ASTPtr;
import rreil.assembler.parser.ASTSize;
import rreil.assembler.parser.ParseException;
import rreil.assembler.parser.RReilParser;
import rreil.assembler.parser.VarSize;

public class ParseTreeTest {
  @Test
  public void parsePointerDeclarationWithHexAddress () throws ParseException {
    String input = ".@sp.d@0xdeadbeaf";
    Reader in = new BufferedReader(new StringReader(input));
    RReilParser p = new RReilParser(in);

    ASTPtr ptr = p.PointerDeclaration();
    ptr.dump("");

    ASTIdent identifierNode = (ASTIdent) ptr.jjtGetChild(0);
    String identifier = (String) identifierNode.jjtGetValue();

    ASTSize sizeNode = (ASTSize) ptr.jjtGetChild(1);
    Integer size = ((VarSize)sizeNode.jjtGetValue()).asInteger();

    ASTIntegerOrNull addressNode = (ASTIntegerOrNull) ptr.jjtGetChild(2);
    BigInteger address = (BigInteger) addressNode.jjtGetValue();

    assertEquals("sp", identifier);
    assertEquals(Integer.valueOf(32), size);
    assertEquals(new BigInteger("deadbeaf", 16), address);
  }

  @Test
  public void parsePointerDeclarationWithArbitraryAddress () throws ParseException {
    String input = ".@sp.d@?";
    Reader in = new BufferedReader(new StringReader(input));
    RReilParser p = new RReilParser(in);

    ASTPtr ptr = p.PointerDeclaration();
    // output the module structure if needed for debugging
//  ptr.dump("");

    ASTIdent identifierNode = (ASTIdent) ptr.jjtGetChild(0);
    String identifier = (String) identifierNode.jjtGetValue();

    ASTSize sizeNode = (ASTSize) ptr.jjtGetChild(1);
    Integer size = ((VarSize)sizeNode.jjtGetValue()).asInteger();

    ASTIntegerOrNull addressNode = (ASTIntegerOrNull) ptr.jjtGetChild(2);

    assertEquals("sp", identifier);
    assertEquals(Integer.valueOf(32), size);
    assertTrue(addressNode.jjtGetValue() == null);
  }

  @Test
  public void parseModule () throws ParseException {
    String input = ".@sp.d@?\n.@bp.d@0x100\nmov.d r1,100\nmov.d r2,100\nadd.d r3,r1,r2";
    Reader in = new BufferedReader(new StringReader(input));
    RReilParser p = new RReilParser(in);

    ASTModule module = p.Module();
    // output the module structure if needed for debugging
//  module.dump("");
  }

  @Test
  public void parseLabelDeclaration () throws ParseException {
    String input = "foobar:";
    Reader in = new BufferedReader(new StringReader(input));
    RReilParser p = new RReilParser(in);

    ASTLabel label = p.Label();
    // output the module structure if needed for debugging
//  label.dump("");

    assertEquals("foobar", label.jjtGetValue());
  }

  @Test
  public void parseVariable () throws ParseException {
    String input = "r1";
    Reader in = new BufferedReader(new StringReader(input));
    RReilParser p = new RReilParser(in);

    p.Variable();
  }

  @Test
  public void parseVariableWithOffset () throws ParseException {
    String input = "r1/0";
    Reader in = new BufferedReader(new StringReader(input));
    RReilParser p = new RReilParser(in);
    p.Variable();
  }

  @Test
  public void parseAddInstructionVariant1 () throws ParseException {
    ASTInsn insn = parseInstruction("add.d r1,r2,r3");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("Add", mnemonic);
  }

  @Test
  public void parseAddInstructionVariant2 () throws ParseException {
    ASTInsn insn = parseInstruction("add.d r1/0,r2/8,r3/16");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("Add", mnemonic);
  }

  /** == Parse example instances for each instruction == */
  @Test
  public void parseAddInstructionVariant () throws ParseException {
    ASTInsn insn = parseInstruction("add.d r1,r2,r3");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("Add", mnemonic);
  }

  @Test
  public void parseSubInstructionVariant () throws ParseException {
    ASTInsn insn = parseInstruction("sub.d r1,r2,r3");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("Sub", mnemonic);
  }

  @Test
  public void parseMovInstructionVariant () throws ParseException {
    ASTInsn insn = parseInstruction("mov.d r1,r2");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("Mov", mnemonic);
  }

  @Test
  public void parseRangeVariant1 () throws ParseException {
    ASTInsn insn = parseInstruction("mov.d r1,[1, 2]");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("Mov", mnemonic);
  }

  @Test
  public void parseRangeVariant2 () throws ParseException {
    ASTInsn insn = parseInstruction("mov.d r1,[1, +oo]");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("Mov", mnemonic);
  }

  @Test
  public void parseRangeVariant3 () throws ParseException {
    ASTInsn insn = parseInstruction("mov.d r1,[-oo, 2]");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("Mov", mnemonic);
  }

  @Test
  public void parseMovsxInstructionVariant () throws ParseException {
    ASTInsn insn = parseInstruction("sign-extend.d.w r1,r2");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("SignExtend", mnemonic);
  }

  @Test
  public void parseMovzxInstructionVariant () throws ParseException {
    ASTInsn insn = parseInstruction("convert.d.w r1,r2");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("Convert", mnemonic);
  }

  @Test
  public void parseLdmInstructionVariant () throws ParseException {
    ASTInsn insn = parseInstruction("load.b.d r1,r2");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("Ldm", mnemonic);
  }

  @Test
  public void parseStmInstructionVariant () throws ParseException {
    ASTInsn insn = parseInstruction("store.d.b r1,r2");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("Stm", mnemonic);
  }

  @Test
  public void parseBrcInstructionVariant () throws ParseException {
    ASTInsn insn = parseInstruction("brc.d ZF,loopHeader:");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("Brc", mnemonic);
  }

  @Test
  public void parseBrInstructionVariant () throws ParseException {
    ASTInsn insn = parseInstruction("br.d loopHeader:");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("Br", mnemonic);
  }

  @Test
  public void parseCallInstructionVariant () throws ParseException {
    ASTInsn insn = parseInstruction("call.d malloc:");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("Call", mnemonic);
  }

  @Test
  public void parseReturnInstructionVariant () throws ParseException {
    ASTInsn insn = parseInstruction("return.d t0");
    String mnemonic = insn.jjtGetChild(0).toString();

    assertEquals("Return", mnemonic);
  }

  private static ASTInsn parseInstruction (String input) throws ParseException {
    Reader in = new BufferedReader(new StringReader(input));
    RReilParser p = new RReilParser(in);

    ASTInsn insn = p.Instruction();
    // output the module structure if needed for debugging
//  insn.dump("");
    return insn;
  }
}
