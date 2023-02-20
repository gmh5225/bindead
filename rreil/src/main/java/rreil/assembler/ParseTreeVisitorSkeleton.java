package rreil.assembler;

import rreil.assembler.parser.ASTAdd;
import rreil.assembler.parser.ASTAnd;
import rreil.assembler.parser.ASTAssert;
import rreil.assembler.parser.ASTAssertionComparison;
import rreil.assembler.parser.ASTAssertionComparisonOperator;
import rreil.assembler.parser.ASTAssertionReachable;
import rreil.assembler.parser.ASTAssertionUnreachable;
import rreil.assembler.parser.ASTAssertionWarnings;
import rreil.assembler.parser.ASTBlock;
import rreil.assembler.parser.ASTBlockEnd;
import rreil.assembler.parser.ASTBlockStart;
import rreil.assembler.parser.ASTBr;
import rreil.assembler.parser.ASTBrc;
import rreil.assembler.parser.ASTBrci;
import rreil.assembler.parser.ASTCall;
import rreil.assembler.parser.ASTCallNat;
import rreil.assembler.parser.ASTCallPrim;
import rreil.assembler.parser.ASTCmpeq;
import rreil.assembler.parser.ASTCmples;
import rreil.assembler.parser.ASTCmpleu;
import rreil.assembler.parser.ASTCmplts;
import rreil.assembler.parser.ASTCmpltu;
import rreil.assembler.parser.ASTCmpneq;
import rreil.assembler.parser.ASTConvert;
import rreil.assembler.parser.ASTDiv;
import rreil.assembler.parser.ASTDivs;
import rreil.assembler.parser.ASTFunArgs;
import rreil.assembler.parser.ASTHalt;
import rreil.assembler.parser.ASTIdent;
import rreil.assembler.parser.ASTIfElse;
import rreil.assembler.parser.ASTInsn;
import rreil.assembler.parser.ASTInteger;
import rreil.assembler.parser.ASTIntegerOrNull;
import rreil.assembler.parser.ASTInterval;
import rreil.assembler.parser.ASTIntervalBound;
import rreil.assembler.parser.ASTIntervalSet;
import rreil.assembler.parser.ASTJumpTargetAddress;
import rreil.assembler.parser.ASTLabel;
import rreil.assembler.parser.ASTLabelDeref;
import rreil.assembler.parser.ASTLdm;
import rreil.assembler.parser.ASTMod;
import rreil.assembler.parser.ASTModule;
import rreil.assembler.parser.ASTMov;
import rreil.assembler.parser.ASTMul;
import rreil.assembler.parser.ASTNatDef;
import rreil.assembler.parser.ASTNatLabel;
import rreil.assembler.parser.ASTNatSizeParam;
import rreil.assembler.parser.ASTNatSizeParams;
import rreil.assembler.parser.ASTNop;
import rreil.assembler.parser.ASTOpt;
import rreil.assembler.parser.ASTOptID;
import rreil.assembler.parser.ASTOptVal;
import rreil.assembler.parser.ASTOr;
import rreil.assembler.parser.ASTPrimLabel;
import rreil.assembler.parser.ASTPtr;
import rreil.assembler.parser.ASTRReilAddress;
import rreil.assembler.parser.ASTReturn;
import rreil.assembler.parser.ASTRvalue;
import rreil.assembler.parser.ASTShl;
import rreil.assembler.parser.ASTShr;
import rreil.assembler.parser.ASTShrs;
import rreil.assembler.parser.ASTSignExtend;
import rreil.assembler.parser.ASTSize;
import rreil.assembler.parser.ASTStm;
import rreil.assembler.parser.ASTStmt;
import rreil.assembler.parser.ASTSub;
import rreil.assembler.parser.ASTTest;
import rreil.assembler.parser.ASTVariable;
import rreil.assembler.parser.ASTXor;
import rreil.assembler.parser.ParseException;
import rreil.assembler.parser.RReilParserVisitor;
import rreil.assembler.parser.SimpleNode;

public class ParseTreeVisitorSkeleton implements RReilParserVisitor {
  @Override public Object visit (SimpleNode node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTModule node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTPtr node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTOpt node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTOptID node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTOptVal node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTStmt node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTInsn node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTAdd node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTSub node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTMul node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTDiv node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTDivs node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTMod node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTShl node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTShr node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTShrs node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTXor node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTOr node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTAnd node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTCmpneq node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTCmpeq node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTCmples node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTCmpleu node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTCmplts node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTCmpltu node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTMov node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTSignExtend node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTConvert node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTLdm node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTStm node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTBrci node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTBrc node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTBr node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTCall node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTReturn node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTHalt node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTRvalue node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTNop node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTLabelDeref node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTVariable node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTSize node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTIntegerOrNull node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTInteger node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTIdent node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTLabel node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTInterval node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTIntervalBound node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTIntervalSet node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTAssert node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTAssertionReachable node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTAssertionUnreachable node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTAssertionWarnings node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTAssertionComparison node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTAssertionComparisonOperator node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTCallNat node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTNatDef node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTNatSizeParams node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTNatSizeParam node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTNatLabel node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTFunArgs node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTCallPrim node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTPrimLabel node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTIfElse node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTTest node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTBlockStart node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTBlockEnd node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTBlock node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTRReilAddress node, Object data) throws ParseException {
    return null;
  }

  @Override public Object visit (ASTJumpTargetAddress node, Object data) throws ParseException {
    return null;
  }

}
