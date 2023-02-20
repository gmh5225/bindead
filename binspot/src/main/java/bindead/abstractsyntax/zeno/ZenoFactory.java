package bindead.abstractsyntax.zeno;

import javalx.numeric.BigInt;
import javalx.numeric.Interval;
import bindead.abstractsyntax.zeno.Zeno.Assign;
import bindead.abstractsyntax.zeno.Zeno.Bin;
import bindead.abstractsyntax.zeno.Zeno.Lhs;
import bindead.abstractsyntax.zeno.Zeno.RangeRhs;
import bindead.abstractsyntax.zeno.Zeno.Rhs;
import bindead.abstractsyntax.zeno.Zeno.Rlin;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.abstractsyntax.zeno.Zeno.ZenoBinOp;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.data.Linear;
import bindead.data.NumVar;

public class ZenoFactory {
  private static final ZenoFactory INSTANCE = new ZenoFactory();

  private ZenoFactory () {
  }

  public static ZenoFactory getInstance () {
    return INSTANCE;
  }

  public Assign assign (Lhs lhs, Rhs rhs) {
    return new Zeno.Assign(lhs, rhs);
  }

  public Bin binary (Rlin left, ZenoBinOp op, Rlin rhs) {
    return new Zeno.Bin(left, op, rhs);
  }

  public Test comparison (Zeno.Rlin expr, ZenoTestOp op) {
    return new Zeno.Test(expr.getLinearTerm(), op);
  }

  public Test comparison (Linear expr, ZenoTestOp op) {
    return comparison(linear(expr), op);
  }

  public Lhs variable (NumVar id) {
    return new Zeno.Lhs(id);
  }

  public Rlin literal (BigInt value) {
    return linear(Linear.linear(value));
  }

  public Rlin linear (NumVar id) {
    return new Zeno.Rlin(Linear.linear(Linear.term(id)));
  }

  public Rlin linear (Linear lin) {
    return new Zeno.Rlin(lin);
  }

  public Rlin linear (Linear lin, BigInt divisor) {
    return new Zeno.Rlin(lin, divisor);
  }

  public RangeRhs range (Interval range) {
    return new Zeno.RangeRhs(range);
  }

  public RangeRhs range (javalx.numeric.Range range) {
    return new Zeno.RangeRhs(range);
  }
}
