package rreil.assembler;

import java.util.HashMap;
import java.util.Map;

import rreil.assembler.parser.ASTInsn;
import rreil.assembler.parser.ASTLabel;
import rreil.assembler.parser.ASTModule;
import rreil.assembler.parser.ASTNatDef;
import rreil.assembler.parser.ASTStmt;
import rreil.assembler.parser.ParseException;
import rreil.lang.RReilAddr;

public class LabelCollector extends ParseTreeVisitorSkeleton {
  private final Map<String, RReilAddr> labels = new HashMap<String, RReilAddr>();
  private RReilAddr currentInstructionOffset = RReilAddr.ZERO;

  @Override public Object visit (ASTModule node, Object _) throws ParseException {
    return node.childrenAccept(this, _);
  }

  @Override public Object visit (ASTStmt node, Object _) throws ParseException {
    return node.childrenAccept(this, _);
  }

  @Override public Object visit (ASTNatDef node, Object data) throws ParseException {
    return node.childrenAccept(this, data);
  }

  @Override public Object visit (ASTInsn insn, Object _) {
    insn.jjtSetValue(currentInstructionOffset);
    currentInstructionOffset = currentInstructionOffset.nextBase();
    return null;
  }

  @Override public Object visit (ASTLabel label, Object _) {
    String name = (String) label.jjtGetValue();
    registerLabel(name);
    return null;
  }

  private void registerLabel (String newLabel) {
    if (labels.containsKey(newLabel))
      throw new RuntimeException("Multiple label declarations for label: " + newLabel);
    labels.put(newLabel, currentInstructionOffset);
  }

  public Map<String, RReilAddr> getLabels () {
    return labels;
  }

  public RReilAddr getLabelAddress (String label) {
    return labels.get(label);
  }
}
