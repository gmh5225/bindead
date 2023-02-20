package rreil.lang.lowlevel;

import javalx.numeric.Interval;
import rreil.disassembler.OperandPostorderIterator;
import rreil.disassembler.OperandTree;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.Rval;
import rreil.lang.util.LowLevelToRReilTranslator;

/**
 * Representation of RREIL operands as {@link OperandTree}s.
 */
public class LowLevelRReilOpnd extends OperandTree {
  public static final String $OffsetOperatorSymbol = "/";
  public static final String $SubaddressOperatorSymbol = ".";

  public LowLevelRReilOpnd (Node root) {
    super(root);
  }

  public int size () {
    final Node sizePrefix = getRoot();
    assert sizePrefix.getType() == OperandTree.Type.Size;
    return ((Number) sizePrefix.getData()).intValue();
  }

  public LowLevelRReilOpnd withSize (final int size) {
    if (size() == size)
      return this;
    NodeBuilder fresh = new NodeBuilder().type(OperandTree.Type.Size).data(size);
    for (Node n : getRoot().getChildren()) {
      fresh.link(n);
    }
    return new LowLevelRReilOpnd(fresh.build());
  }

  public LowLevelRReilOpnd withSize (final OperandSize size) {
    return withSize(size.getBits());
  }

  public LowLevelRReilOpnd withOffset (int offset, int size) {
    if (offset == 0)
      return withSize(size);
    Node sym = getSymbolOrNull();
    int oldOffset = getOffsetOrZero();
    if (sym == null)
      throw new UnsupportedOperationException("withOffset not applicable");
    NodeBuilder offsetOp = new NodeBuilder().type(OperandTree.Type.Op).data($OffsetOperatorSymbol);
    Node offsetImm = new NodeBuilder().type(OperandTree.Type.Immi).data(oldOffset + offset).build();
    offsetOp.link(sym);
    offsetOp.link(offsetImm);
    Node fresh = new NodeBuilder().type(OperandTree.Type.Size).data(size).link(offsetOp.build()).build();
    return new LowLevelRReilOpnd(fresh);
  }

  public LowLevelRReilOpnd withOffset (final int offset, final OperandSize size) {
    return withOffset(offset, size.getBits());
  }

  public Node child () {
    return getRoot().getChildren().get(0);
  }

  private Node getSymbolOrNull () {
    Node child = child();
    if (child.getType() == OperandTree.Type.Sym)
      return child;
    if (child.getType() == OperandTree.Type.Op && $OffsetOperatorSymbol.equals(child.getData())) {
      Node sym = child.getChildren().get(0);
      assert sym.getType() == OperandTree.Type.Sym;
      return sym;
    }
    return null;
  }

  public int getOffsetOrZero () {
    final Node child = child();
    if (child.getType() == OperandTree.Type.Op && $OffsetOperatorSymbol.equals(child.getData()))
      return ((Number) child.getChildren().get(1).getData()).intValue();
    return 0;
  }

  public RReilAddr getRReilAddrOrNull () {
    final Node child = child();
    if (child.getType() == OperandTree.Type.Op && $SubaddressOperatorSymbol.equals(child.getData())) {
      final Number base = (Number) child.getChildren().get(0).getData();
      final Number offset = (Number) child.getChildren().get(1).getData();
      return new RReilAddr(base.longValue(), offset.intValue());
    }
    return null;
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    OperandPostorderIterator it = new OperandPostorderIterator(getRoot());
    while(it.next()) {
      Node node = it.current();
      switch (node.getType()) {
          case Immf:
          case Immi:
            builder.append(((Number)node.getData()).toString());
            break;
          case Immr:
            builder.append(((Interval) node.getData()).toString());
            break;
          case Sym:
            builder.append((String) node.getData());
            break;
          case Size:
            builder.append(':').append(node.getData());
            break;
          case Op:
            builder.append((String) node.getData());
            break;
          case Mem:
            builder.append('*').append(((Number) node.getData()).toString());
            break;
      }
    }
    return builder.toString();
  }

  public Rval toRReil () {
    return LowLevelToRReilTranslator.translateRval(this);
  }
}
