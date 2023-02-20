package rreil.cfa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rreil.abstractsyntax.RReil;

public final class Block extends ArrayList<RReil.Statement> {
  private static final long serialVersionUID = 1L;

  private Block (List<RReil.Statement> instructions) {
    addAll(instructions);
  }

  public static Block singleton (RReil.Statement insn) {
    List<RReil.Statement> instructions = new ArrayList<RReil.Statement>();
    instructions.add(insn);
    return new Block(instructions);
  }

  public static Block block (List<RReil.Statement> instructions) {
    return new Block(instructions);
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    Iterator<RReil.Statement> it = iterator();
    while (it.hasNext()) {
      RReil.Statement statement = it.next();
      builder.append(statement.getRReilAddress());
      builder.append(": ");
      builder.append(statement);
      if (it.hasNext())
        builder.append("\n");
    }
    return builder.toString();
  }
}
