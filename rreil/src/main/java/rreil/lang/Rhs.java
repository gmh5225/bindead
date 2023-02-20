package rreil.lang;

import static rreil.lang.RReil.$OffsetSeparator;
import static rreil.lang.RReil.$SizeSeparator;
import javalx.numeric.BigInt;
import javalx.numeric.FiniteRange;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import rreil.lang.util.RhsVisitor;

public abstract class Rhs implements AssemblerParseable, Reconstructable {

  public abstract <R, T> R accept (RhsVisitor<R, T> visitor, T data);

  public MemVar getRegionOrNull () {
    return null;
  }

  public Range getOffsetOrTop () {
    return Range.TOP;
  }


  /**
   * Binary Operations.
   */
  public static final class Bin extends Rhs {
    private final BinOp op;
    private final Rval left;
    private final Rval right;

    public Bin (Rval left, BinOp op, Rval right) {
      this.op = op;
      this.left = left;
      this.right = right;
    }

    public BinOp getOp () {
      return op;
    }

    public Rval getLeft () {
      return left;
    }

    public Rval getRight () {
      return right;
    }

    @Override public <R, T> R accept (RhsVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = 1;
      result = prime * result + (left == null ? 0 : left.hashCode());
      result = prime * result + (op == null ? 0 : op.hashCode());
      result = prime * result + (right == null ? 0 : right.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof Bin))
        return false;
      Bin other = (Bin) obj;
      if (left == null) {
        if (other.left != null)
          return false;
      } else if (!left.equals(other.left))
        return false;
      if (op != other.op)
        return false;
      if (right == null) {
        if (other.right != null)
          return false;
      } else if (!right.equals(other.right))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new Rhs.Bin(" + left.reconstructCode() + ", " + op.reconstructCode() + ", " + right.reconstructCode()
        + ")";
    }

    @Override public String toAssemblerString () {
      // NOTE: this needs postprocessing in RREIL.Assign where the lhs is known
      return mnemonic();
    }

    public String mnemonic () {
      return op.asPrefix();
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(mnemonic());
      builder.append(" ");
      builder.append(left);
      builder.append(RReil.$OperandSeparator);
      builder.append(right);
      return builder.toString();
    }

  }


  /**
   * Simple Expressions
   */
  public static abstract class SimpleExpression extends Rhs {
  }

  /**
   * Linear Expressions
   */
  public static abstract class Lin extends SimpleExpression {
    public abstract String mnemonic ();

    @Override public String toAssemblerString () {
      // NOTE: this needs postprocessing in RREIL.Assign where the lhs is known
      return mnemonic();
    }

    public abstract int getSize ();
  }

  /**
   * A binary combination of linear expressions. The combination operator is either addition or subtraction.
   */
  public static final class LinBin extends Lin {
    private final LinBinOp op;
    private final Lin left;
    private final Lin right;

    public LinBin (Lin left, LinBinOp op, Lin right) {
      this.op = op;
      this.left = left;
      this.right = right;
    }

    public LinBinOp getOp () {
      return op;
    }

    public Lin getLeft () {
      return left;
    }

    public Lin getRight () {
      return right;
    }

    @Override public int getSize () {
      int size = left.getSize();
      assert size == right.getSize() : "Operands of binary linear expression must have the same size";
      return size;
    }

    @Override public <R, T> R accept (RhsVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof LinBin))
        return false;
      LinBin other = (LinBin) obj;
      if (left == null) {
        if (other.left != null)
          return false;
      } else if (!left.equals(other.left))
        return false;
      if (op != other.op)
        return false;
      if (right == null) {
        if (other.right != null)
          return false;
      } else if (!right.equals(other.right))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new Rhs.LinBin(" + left.reconstructCode() + ", " + op.reconstructCode() + ", " + right.reconstructCode()
        + ")";
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(mnemonic());
      builder.append("(");
      builder.append(left);
      builder.append(RReil.$OperandSeparator);
      builder.append(right);
      builder.append(")");
      return builder.toString();
    }

    @Override public String mnemonic () {
      return op.asPrefix();
    }
  }

  public static final class LinScale extends Lin {
    private final Lin opnd;
    private final BigInt scale;

    public LinScale (Lin opnd, BigInt scale) {
      this.opnd = opnd;
      this.scale = scale;
    }

    public Lin getOpnd () {
      return opnd;
    }

    public BigInt getConst () {
      return scale;
    }

    @Override public int getSize () {
      return opnd.getSize();
    }

    @Override public MemVar getRegionOrNull () {
      return opnd.getRegionOrNull();
    }

    @Override public Range getOffsetOrTop () {
      return opnd.getOffsetOrTop();
    }

    @Override public <R, T> R accept (RhsVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof LinScale))
        return false;
      LinScale other = (LinScale) obj;
      if (opnd == null) {
        if (other.opnd != null)
          return false;
      } else if (!opnd.equals(other.opnd))
        return false;
      return scale.equals(other.scale);
    }

    @Override public String reconstructCode () {
      return "new Rhs.LinScale(" + opnd.reconstructCode() + ", BigInt.of(" + scale.longValue() + "))";
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(mnemonic());
      builder.append("(");
      builder.append(scale);
      builder.append(RReil.$OperandSeparator);
      builder.append(opnd);
      builder.append(")");
      return builder.toString();
    }

    @Override public String mnemonic () {
      return "scale";
    }
  }

  public static final class LinRval extends Lin {
    private final Rval val;

    public LinRval (Rval val) {
      this.val = val;
    }

    public Rval getRval () {
      return val;
    }

    @Override public int getSize () {
      return val.getSize();
    }

    @Override public MemVar getRegionOrNull () {
      return val.getRegionOrNull();
    }

    @Override public Range getOffsetOrTop () {
      return val.getOffsetOrTop();
    }

    @Override public <R, T> R accept (RhsVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof LinRval))
        return false;
      LinRval other = (LinRval) obj;
      if (val == null) {
        if (other.val != null)
          return false;
      } else if (!val.equals(other.val))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new Rhs.LinRval(" + val.reconstructCode() + ")";
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(val);
      return builder.toString();
    }

    @Override public String mnemonic () {
      return "LinRval";
    }
  }

  /**
   * Comparison/Relational operations
   */
  public static final class Cmp extends SimpleExpression {
    private final ComparisonOp op;
    private final Lin left;
    private final Lin right;

    public Cmp (Lin left, ComparisonOp op, Lin right) {
      this.op = op;
      this.left = left;
      this.right = right;
    }

    public Lin getLeft () {
      return left;
    }

    public ComparisonOp getOp () {
      return op;
    }

    public Cmp not() {
      switch (op) {
      case Cmpeq:
        return new Cmp(left, ComparisonOp.Cmpneq, right);
      case Cmpneq:
        return new Cmp(left, ComparisonOp.Cmpeq, right);
      case Cmples:
        return new Cmp(right, ComparisonOp.Cmplts, left);
      case Cmpleu:
        return new Cmp(right, ComparisonOp.Cmpltu, left);
      case Cmplts:
        return new Cmp(right, ComparisonOp.Cmples, left);
      case Cmpltu:
        return new Cmp(right, ComparisonOp.Cmpleu, left);
      default:
        throw new IllegalStateException();
      }
    }

    public Lin getRight () {
      return right;
    }

    @Override public <R, T> R accept (RhsVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = 1;
      result = prime * result + (left == null ? 0 : left.hashCode());
      result = prime * result + (op == null ? 0 : op.hashCode());
      result = prime * result + (right == null ? 0 : right.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof Cmp))
        return false;
      Cmp other = (Cmp) obj;
      if (left == null) {
        if (other.left != null)
          return false;
      } else if (!left.equals(other.left))
        return false;
      if (op != other.op)
        return false;
      if (right == null) {
        if (other.right != null)
          return false;
      } else if (!right.equals(other.right))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new Rhs.Cmp(" + left.reconstructCode() + ", " + op.reconstructCode() + ", " + right.reconstructCode()
        + ")";
    }

    @Override public String toAssemblerString () {
      // NOTE: this needs postprocessing in RREIL.Assign where the lhs is known
      return mnemonic();
    }

    public String mnemonic () {
      return op.asPrefix();
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(mnemonic());
      builder.append(" ");
      builder.append(left);
      builder.append(RReil.$OperandSeparator);
      builder.append(right);
      return builder.toString();
    }

  }

  /**
   * Assignments with sign-extension.
   */
  public static final class SignExtend extends Rhs {
    private final Rval rhs;

    public SignExtend (Rval rhs) {
      this.rhs = rhs;
    }

    public Rval getRhs () {
      return rhs;
    }

    @Override public <R, T> R accept (RhsVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = 1;
      result = prime * result + (rhs == null ? 0 : rhs.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof SignExtend))
        return false;
      SignExtend other = (SignExtend) obj;
      if (rhs == null) {
        if (other.rhs != null)
          return false;
      } else if (!rhs.equals(other.rhs))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new SignExtend(" + rhs.reconstructCode() + ")";
    }

    @Override public String toAssemblerString () {
      // NOTE: this needs postprocessing in RREIL.Assign where the lhs is known
      return mnemonic();
    }

    @SuppressWarnings("static-method") public String mnemonic () {
      return "sign-extend";
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(mnemonic());
      builder.append(" ");
      builder.append(rhs);
      return builder.toString();
    }

  }

  /**
   * Assignments with zero-extension.
   */
  public static final class Convert extends Rhs {
    private final Rval rhs;

    public Convert (Rval rhs) {
      this.rhs = rhs;
    }

    public Rval getRhs () {
      return rhs;
    }

    @Override public <R, T> R accept (RhsVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = 1;
      result = prime * result + (rhs == null ? 0 : rhs.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof Convert))
        return false;
      Convert other = (Convert) obj;
      if (rhs == null) {
        if (other.rhs != null)
          return false;
      } else if (!rhs.equals(other.rhs))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new Rhs.Convert(" + rhs.reconstructCode() + ")";
    }

    @Override public String toAssemblerString () {
      // NOTE: this needs postprocessing in RREIL.Assign where the lhs is known
      return mnemonic();
    }

    @SuppressWarnings("static-method") public String mnemonic () {
      return "convert";
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(mnemonic());
      builder.append(" ");
      builder.append(rhs);
      return builder.toString();
    }
  }

  public static abstract class Rval extends Rhs {
    private final int size;

    public Rval (int size) {
      this.size = size;
    }

    public int getSize () {
      return size;
    }

    /**
     * Same as {@link #toAssemblerString()} but if the class is a literal
     * it prints the value in hexadecimal.
     */
    public abstract String toHexAssemblerString ();

    @Override public int hashCode () {
      final int prime = 31;
      int result = 1;
      result = prime * result + size;
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof Rval))
        return false;
      Rval other = (Rval) obj;
      if (size != other.size)
        return false;
      return true;
    }
  }

  public static final class Rvar extends Rval {
    private final MemVar region;
    public final int offset;

    public Rvar (int size, int offset, MemVar region) {
      super(size);
      this.region = region;
      this.offset = offset;
    }

    public MemVar getRegionId () {
      return region;
    }

    public Lhs asLhs () {
      return new Lhs(getSize(), offset, region);
    }

    public int getOffset () {
      return offset;
    }

    /**
     * Return the bit positions of the memory region that this variable is representing.
     */
    public FiniteRange bitsRange () {
      return Field.finiteRangeKey(offset, getSize());
    }

    @Override public MemVar getRegionOrNull () {
      return region;
    }

    @Override public Range getOffsetOrTop () {
      return Range.from(offset);
    }

    @Override public <R, T> R accept (RhsVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }


    @Override public int hashCode () {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + offset;
      result = prime * result + (region == null ? 0 : region.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (!(obj instanceof Rvar))
        return false;
      Rvar other = (Rvar) obj;
      if (offset != other.offset)
        return false;
      if (region == null) {
        if (other.region != null)
          return false;
      } else if (!region.equals(other.region))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new Rhs.Rvar(" + getSize() + ", " + offset + ", " + region.reconstructCode() + ")";
    }

    @Override public String toAssemblerString () {
      StringBuilder builder = new StringBuilder();
      builder.append(region);
      if (offset != 0) {
        builder.append("/");
        builder.append(offset);
      }
      return builder.toString();
    }

    @Override public String toHexAssemblerString () {
      throw new IllegalStateException("Variables cannot be printed in as values in hexadecimal.");
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(region.toString());
      builder.append($SizeSeparator).append(getSize());
      if (offset != 0)
        builder.append($OffsetSeparator).append(offset);
      return builder.toString();
    }
  }

  public static final class Rlit extends Rval {
    public static final Rlit true_ = new Rlit(1, BigInt.of(1));
    public static final Rlit false_ = new Rlit(1, BigInt.of(0));

    private final BigInt value;

    public Rlit (int size, BigInt value) {
      super(size);
      this.value = value;
    }

    public BigInt getValue () {
      return value;
    }

    @Override public <R, T> R accept (RhsVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    public StringBuilder asHexPrefix (StringBuilder builder) {
      return builder.append(value.toHexString()).append($SizeSeparator).append(getSize());
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (value == null ? 0 : value.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (!(obj instanceof Rlit))
        return false;
      Rlit other = (Rlit) obj;
      if (value == null) {
        if (other.value != null)
          return false;
      } else if (!value.equals(other.value))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new Rhs.Rlit(" + getSize() + ", BigInt.of(" + value.longValue() + "))";
    }

    @Override public String toAssemblerString () {
      StringBuilder builder = new StringBuilder();
      builder.append(value);
      return builder.toString();
    }

    @Override public String toHexAssemblerString () {
      Address asAddress = new Address(0, RReilAddr.valueOf(value));
      return asAddress.toHexAssemblerString();
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(value);
      builder.append($SizeSeparator);
      builder.append(getSize());
      return builder.toString();
    }

    public String toHexString () {
      StringBuilder builder = new StringBuilder();
      builder.append("0x");
      builder.append(value.toHexString());
      builder.append($SizeSeparator);
      builder.append(getSize());
      return builder.toString();
    }

  }

  /**
   * An interval range value.
   */
  public static final class RangeRhs extends SimpleExpression {
    private final Interval range;
    private final int size;

    public RangeRhs (int size, Interval range) {
      this.range = range;
      this.size = size;
    }

    public Interval getRange () {
      return range;
    }

    public int getSize () {
      return size;
    }

    @Override public <R, T> R accept (RhsVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(range);
      builder.append($SizeSeparator);
      builder.append(size);
      return builder.toString();
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = 1;
      result = prime * result + (range == null ? 0 : range.hashCode());
      result = prime * result + size;
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof RangeRhs))
        return false;
      RangeRhs other = (RangeRhs) obj;
      if (range == null) {
        if (other.range != null)
          return false;
      } else if (!range.equals(other.range))
        return false;
      if (size != other.size)
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new Rhs.RangeRhs(" + size + ",  Interval.unsignedTop(" + size + "))";
    }

    @Override public String toAssemblerString () {
      StringBuilder builder = new StringBuilder();
      builder.append(range);
      return builder.toString();
    }
  }

  /**
   * RReil-addresses as operands supporting intra-RREIL jumps.
   */
  public static final class Address extends Rval implements AssemblerParseable, Reconstructable {
    private final RReilAddr address;

    public Address (int size, RReilAddr address) {
      super(size);
      this.address = address;
    }

    public RReilAddr getAddress () {
      return address;
    }

    @Override public <R, T> R accept (RhsVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (address == null ? 0 : address.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (!(obj instanceof Address))
        return false;
      Address other = (Address) obj;
      if (address == null) {
        if (other.address != null)
          return false;
      } else if (!address.equals(other.address))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new Rhs.Address(" + getSize() + ", " + address.reconstructCode() + ")";
    }

    @Override public String toAssemblerString () {
      return address.toAssemblerString();
    }

    @Override public String toHexAssemblerString () {
      return toAssemblerString();
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(address);
      builder.append($SizeSeparator);
      builder.append(getSize());
      return builder.toString();
    }

  }

}
