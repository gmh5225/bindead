package rreil.lang;

import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import javalx.exceptions.UnimplementedException;
import rreil.RReilGrammarException;
import rreil.assembler.CompiledAssembler;
import rreil.lang.Rhs.Address;
import rreil.lang.Rhs.Bin;
import rreil.lang.Rhs.Cmp;
import rreil.lang.Rhs.Convert;
import rreil.lang.Rhs.Lin;
import rreil.lang.Rhs.LinRval;
import rreil.lang.Rhs.RangeRhs;
import rreil.lang.Rhs.Rlit;
import rreil.lang.Rhs.Rval;
import rreil.lang.Rhs.Rvar;
import rreil.lang.Rhs.SignExtend;
import rreil.lang.Rhs.SimpleExpression;
import rreil.lang.util.RReilVisitor;

public abstract class RReil implements AssemblerParseable, Reconstructable {
  protected static final char $AddressSeparator = '&';
  protected static final String $OperandSeparator = ", ";
  protected static final char $SizeSeparator = ':';
  protected static final char $OffsetSeparator = '/';
  private final RReilAddr address;

  private RReil (RReilAddr rreilAddress) {
    this.address = rreilAddress;
  }

  /**
   * Parse and return a RREIL instruction from a RREIL assembler string.
   */
  public static RReil from (String instruction) {
    CompiledAssembler compiled = CompiledAssembler.from(instruction);
    SortedMap<RReilAddr, RReil> instructions = compiled.getInstructions();
    assert instructions.size() == 1;
    return instructions.get(instructions.firstKey());
  }

  public RReilAddr getRReilAddress () {
    return address;
  }

  public abstract String mnemonic ();

  protected String formattedMnemonic () {
    return String.format("%-8s", mnemonic());
  }

  public abstract <R, T> R accept (RReilVisitor<R, T> visitor, T data);


  public boolean isBranch () {
    return false;
  }

  public boolean isAlwaysTakenBranch () {
    return false;
  }

  public boolean isDeadBranch () {
    return false;
  }

  public boolean isConditionalBranch () {
    return false;
  }

  public boolean isIndirectBranch () {
    return false;
  }

  @Override public int hashCode () {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((address == null) ? 0 : address.hashCode());
    return result;
  }

  @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof RReil))
      return false;
    RReil other = (RReil) obj;
    if (address == null) {
      if (other.address != null)
        return false;
    } else if (!address.equals(other.address))
      return false;
    return true;
  }

  public static String reconstructList (String type, List<? extends Reconstructable> list) {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (Reconstructable lhs : list) {
      if (!first)
        builder.append(", ");
      first = false;
      builder.append(lhs.reconstructCode());
    }
    return "Arrays.asList(new " + type + "[] {" + builder.toString() + "})";
  }

  /**
   * Assertions of various types.
   */
  public static abstract class Assertion extends RReil {
    public Assertion (RReilAddr rreilAddress) {
      super(rreilAddress);
    }

    @Override public <R, T> R accept (RReilVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public String toAssemblerString () {
      throw new UnimplementedException();
    }

    @Override public String mnemonic () {
      return "assert";
    }


    /**
     * Assertion for the comparison of variables and their values.
     */
    public static final class AssertionCompare extends Assertion {
      private final Rhs lhs;
      private final AssertionOp operator;
      private final Rhs rhs;
      private final int size;

      public AssertionCompare (RReilAddr rreilAddress, Rhs lhs, AssertionOp operator, Rhs rhs, int size) {
        super(rreilAddress);
        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;
        this.size = size;
      }

      public Rhs getLhs () {
        return lhs;
      }

      public AssertionOp getOperator () {
        return operator;
      }

      public Rhs getRhs () {
        return rhs;
      }

      public int getSize () {
        return size;
      }

      @Override public <R, T> R accept (RReilVisitor<R, T> visitor, T data) {
        return visitor.visit(this, data);
      }

      @Override public String toString () {
        StringBuilder builder = new StringBuilder();
        builder.append(formattedMnemonic());
        builder.append(" ");
        builder.append(lhs);
        builder.append(" ");
        builder.append(operator);
        builder.append(" ");
        builder.append(rhs);
        return builder.toString();
      }

      @Override public int hashCode () {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((lhs == null) ? 0 : lhs.hashCode());
        result = prime * result + ((operator == null) ? 0 : operator.hashCode());
        result = prime * result + ((rhs == null) ? 0 : rhs.hashCode());
        result = prime * result + size;
        return result;
      }

      @Override public boolean equals (Object obj) {
        if (this == obj)
          return true;
        if (!super.equals(obj))
          return false;
        if (!(obj instanceof AssertionCompare))
          return false;
        AssertionCompare other = (AssertionCompare) obj;
        if (lhs == null) {
          if (other.lhs != null)
            return false;
        } else if (!lhs.equals(other.lhs))
          return false;
        if (operator != other.operator)
          return false;
        if (rhs == null) {
          if (other.rhs != null)
            return false;
        } else if (!rhs.equals(other.rhs))
          return false;
        if (size != other.size)
          return false;
        return true;
      }

      @Override public String reconstructCode () {
        return "new RReil.AssertionValues(" + getRReilAddress().reconstructCode() + ", " + lhs.reconstructCode() + ", "
          + operator.reconstructCode() + ", " + rhs.reconstructCode() + ")";
      }
    }

    /**
     * Assertion that a location is definitely reachable by the control flow.
     */
    public static final class AssertionReachable extends Assertion {

      public AssertionReachable (RReilAddr rreilAddress) {
        super(rreilAddress);
      }

      @Override public boolean equals (Object obj) {
        if (!(obj instanceof AssertionReachable))
          return false;
        return super.equals(obj);
      }

      @Override public String toString () {
        StringBuilder builder = new StringBuilder();
        builder.append(formattedMnemonic());
        builder.append(" ");
        builder.append("<reachable>");
        return builder.toString();
      }

      @Override public String reconstructCode () {
        return "new AssertionReachable(" + getRReilAddress().reconstructCode() + ")";
      }
    }

    /**
     * Assertion that a location is <b>not</b> reachable by the control flow.
     */
    public static final class AssertionUnreachable extends Assertion {

      public AssertionUnreachable (RReilAddr rreilAddress) {
        super(rreilAddress);
      }

      @Override public boolean equals (Object obj) {
        if (!(obj instanceof AssertionUnreachable))
          return false;
        return super.equals(obj);
      }

      @Override public String toString () {
        StringBuilder builder = new StringBuilder();
        builder.append(formattedMnemonic());
        builder.append(" ");
        builder.append("<unreachable>");
        return builder.toString();
      }

      @Override public String reconstructCode () {
        return "new RReil.AssertionUnreachable(" + getRReilAddress().reconstructCode() + ")";
      }
    }

    /**
     * Assertion that a location is not reachable by the control flow.
     */
    public static final class AssertionWarnings extends Assertion {
      private final int numberOfExpectedWarnings;

      public AssertionWarnings (RReilAddr rreilAddress,
          int numberOfExpectedWarnings) {
        super(rreilAddress);
        this.numberOfExpectedWarnings = numberOfExpectedWarnings;
      }

      @Override public String toString () {
        StringBuilder builder = new StringBuilder();
        builder.append(formattedMnemonic());
        builder.append(" ");
        builder.append("#warnings");
        builder.append(" = ");
        builder.append(numberOfExpectedWarnings);
        return builder.toString();
      }

      public int getNumberOfExpectedWarnings () {
        return numberOfExpectedWarnings;
      }

      @Override public int hashCode () {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + numberOfExpectedWarnings;
        return result;
      }

      @Override public boolean equals (Object obj) {
        if (this == obj)
          return true;
        if (!super.equals(obj))
          return false;
        if (!(obj instanceof AssertionWarnings))
          return false;
        AssertionWarnings other = (AssertionWarnings) obj;
        if (numberOfExpectedWarnings != other.numberOfExpectedWarnings)
          return false;
        return true;
      }

      @Override public String reconstructCode () {
        return "new RReil.AssertionWarnings(" + getRReilAddress().reconstructCode() + ", " + numberOfExpectedWarnings
          + ")";
      }
    }
  }

  /**
   * Assignments.
   */
  public static final class Assign extends RReil {
    private final Lhs lhs;
    private final Rhs rhs;

    public Assign (RReilAddr rreilAddress, Rvar lhs, Rhs rhs) {
      super(rreilAddress);
      this.lhs = new Lhs(lhs.getSize(), lhs.getOffset(), lhs.getRegionId());
      this.rhs = rhs;
    }

    public Assign (RReilAddr rreilAddress, Lhs lhs, Rhs rhs) {
      super(rreilAddress);
      this.lhs = lhs;
      this.rhs = rhs;
    }

    public int size () {
      return lhs.getSize();
    }

    public Lhs getLhs () {
      return lhs;
    }

    public Rhs getRhs () {
      return rhs;
    }

    @Override public <R, T> R accept (RReilVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public String mnemonic () {
      if (rhs instanceof Rval || rhs instanceof RangeRhs)
        return "mov";
      else if (rhs instanceof Bin)
        return ((Bin) rhs).mnemonic();
      else if (rhs instanceof Lin)
        return "mov";
      else if (rhs instanceof Cmp)
        return ((Cmp) rhs).mnemonic();
      else if (rhs instanceof SignExtend)
        return ((SignExtend) rhs).mnemonic();
      else if (rhs instanceof Convert)
        return ((Convert) rhs).mnemonic();
      else
        throw new IllegalStateException();
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(formattedMnemonic());
      builder.append(" ");
      if (rhs instanceof Rval || rhs instanceof RangeRhs) {
        builder.append(lhs);
        builder.append(", ");
        builder.append(rhs);
      } else if (rhs instanceof Bin) {
        Bin binexpr = (Bin) rhs;
        Rval par1 = binexpr.getLeft();
        Rval par2 = binexpr.getRight();
        builder.append(lhs);
        builder.append(", ");
        builder.append(par1);
        builder.append(", ");
        builder.append(par2);
      } else if (rhs instanceof Lin) {
        builder.append(lhs);
        builder.append(", ");
        builder.append(rhs.toString());
      } else if (rhs instanceof Cmp) {
        Cmp cmpexpr = (Cmp) rhs;
        Lin par1 = cmpexpr.getLeft();
        Lin par2 = cmpexpr.getRight();
        builder.append(lhs);
        builder.append(", ");
        builder.append(par1);
        builder.append(", ");
        builder.append(par2);
      } else if (rhs instanceof SignExtend) {
        SignExtend signextendexpr = (SignExtend) rhs;
        builder.append(lhs);
        builder.append(", ");
        builder.append(signextendexpr.getRhs());
      } else if (rhs instanceof Convert) {
        Convert convertexpr = (Convert) rhs;
        builder.append(lhs);
        builder.append(", ");
        builder.append(convertexpr.getRhs());
      } else {
        throw new IllegalStateException();
      }
      return builder.toString();
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((lhs == null) ? 0 : lhs.hashCode());
      result = prime * result + ((rhs == null) ? 0 : rhs.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (!(obj instanceof Assign))
        return false;
      Assign other = (Assign) obj;
      if (lhs == null) {
        if (other.lhs != null)
          return false;
      } else if (!lhs.equals(other.lhs))
        return false;
      if (rhs == null) {
        if (other.rhs != null)
          return false;
      } else if (!rhs.equals(other.rhs))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new RReil.Assign(" + getRReilAddress().reconstructCode() + ", " + lhs.reconstructCode() + ", "
        + rhs.reconstructCode() + ")";
    }

    @Override public String toAssemblerString () {
      StringBuilder builder = new StringBuilder();
      builder.append(getRReilAddress().toAssemblerString());
      builder.append(" ");
      if (rhs instanceof Rval || rhs instanceof RangeRhs) {
        builder.append("mov");
        builder.append("." + lhs.getSize());
        builder.append(" ");
        builder.append(lhs.toAssemblerString());
        builder.append(", ");
        builder.append(rhs.toAssemblerString());
      } else if (rhs instanceof Bin) {
        Bin binexpr = (Bin) rhs;
        Rval par1 = binexpr.getLeft();
        Rval par2 = binexpr.getRight();
        String operation = binexpr.mnemonic();
        builder.append(operation);
        builder.append("." + lhs.getSize());
        builder.append(" ");
        builder.append(lhs.toAssemblerString());
        builder.append(", ");
        builder.append(par1.toAssemblerString());
        builder.append(", ");
        builder.append(par2.toAssemblerString());
      } else if (rhs instanceof Cmp) {
        Cmp cmpexpr = (Cmp) rhs;
        Lin par1 = cmpexpr.getLeft();
        Lin par2 = cmpexpr.getRight();
        String operation = cmpexpr.mnemonic();
        builder.append(operation);
        builder.append("." + par1.getSize());
        builder.append(" ");
        builder.append(lhs.toAssemblerString());
        builder.append(", ");
        builder.append(par1.toAssemblerString());
        builder.append(", ");
        builder.append(par2.toAssemblerString());
      } else if (rhs instanceof SignExtend) {
        SignExtend signextendexpr = (SignExtend) rhs;
        String operation = signextendexpr.mnemonic();
        builder.append(operation);
        builder.append("." + lhs.getSize());
        builder.append("." + signextendexpr.getRhs().getSize());
        builder.append(" ");
        builder.append(lhs.toAssemblerString());
        builder.append(", ");
        builder.append(signextendexpr.getRhs().toAssemblerString());
      } else if (rhs instanceof Convert) {
        Convert convertexpr = (Convert) rhs;
        String operation = convertexpr.mnemonic();
        builder.append(operation);
        builder.append("." + lhs.getSize());
        builder.append("." + convertexpr.getRhs().getSize());
        builder.append(" ");
        builder.append(lhs.toAssemblerString());
        builder.append(", ");
        builder.append(convertexpr.getRhs().toAssemblerString());
      } else {
        throw new IllegalStateException();
      }
      return builder.toString();
    }

  }

  /**
   * Load from memory.
   */
  public static final class Load extends RReil {
    private final Lin readAddress;
    private final Lhs lhs;

    public Load (RReilAddr rreilAddress, Rvar lhs, Lin rhs) {
      super(rreilAddress);
      this.lhs = new Lhs(lhs.getSize(), lhs.getOffset(), lhs.getRegionId());
      this.readAddress = rhs;
    }

    public Load (RReilAddr rreilAddress, Lhs lhs, Lin address) {
      super(rreilAddress);
      this.readAddress = address;
      this.lhs = lhs;
    }

    public int pointerSize () {
      return readAddress.getSize();
    }

    public Lin getReadAddress () {
      return readAddress;
    }

    public int lhsSize () {
      return lhs.getSize();
    }

    public Lhs getLhs () {
      return lhs;
    }

    @Override public <R, T> R accept (RReilVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public String mnemonic () {
      return "load";
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(formattedMnemonic());
      builder.append(" ");
      builder.append(lhs);
      builder.append(", ").append('[');
      builder.append(readAddress);
      builder.append(']');
      return builder.toString();
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((lhs == null) ? 0 : lhs.hashCode());
      result = prime * result + ((readAddress == null) ? 0 : readAddress.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (!(obj instanceof Load))
        return false;
      Load other = (Load) obj;
      if (lhs == null) {
        if (other.lhs != null)
          return false;
      } else if (!lhs.equals(other.lhs))
        return false;
      if (readAddress == null) {
        if (other.readAddress != null)
          return false;
      } else if (!readAddress.equals(other.readAddress))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new RReil.Load(" + getRReilAddress().reconstructCode() + ", " + lhs.reconstructCode() + ", "
        + readAddress.reconstructCode() + ")";
    }

    @Override public String toAssemblerString () {
      StringBuilder builder = new StringBuilder();
      builder.append(getRReilAddress().toAssemblerString());
      builder.append(" ");
      builder.append("load");
      builder.append("." + lhs.getSize());
      builder.append("." + readAddress.getSize());
      builder.append(" ");
      builder.append(lhs.toAssemblerString());
      builder.append(", ");
      builder.append(readAddress.toAssemblerString());
      return builder.toString();
    }

  }

  /**
   * Store in memory.
   */
  public static final class Store extends RReil {
    private final Lin writeAddress;
    private final Lin rhs;

    public Store (RReilAddr rreilAddress, Lin address, Lin rhs) {
      super(rreilAddress);
      this.writeAddress = address;
      this.rhs = rhs;
    }

    public int pointerSize () {
      return writeAddress.getSize();
    }

    public Lin getWriteAddress () {
      return writeAddress;
    }

    public int rhsSize () {
      return rhs.getSize();
    }

    public Lin getRhs () {
      return rhs;
    }

    @Override public <R, T> R accept (RReilVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public String mnemonic () {
      return "store";
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(formattedMnemonic());
      builder.append(" ");
      builder.append('[');
      builder.append(writeAddress);
      builder.append(']');
      builder.append(", ");
      builder.append(rhs);
      return builder.toString();
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((rhs == null) ? 0 : rhs.hashCode());
      result = prime * result + ((writeAddress == null) ? 0 : writeAddress.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (!(obj instanceof Store))
        return false;
      Store other = (Store) obj;
      if (rhs == null) {
        if (other.rhs != null)
          return false;
      } else if (!rhs.equals(other.rhs))
        return false;
      if (writeAddress == null) {
        if (other.writeAddress != null)
          return false;
      } else if (!writeAddress.equals(other.writeAddress))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new RReil.Store(" + getRReilAddress().reconstructCode() + ", " + writeAddress.reconstructCode() + ", "
        + rhs.reconstructCode() + ")";
    }

    @Override public String toAssemblerString () {
      StringBuilder builder = new StringBuilder();
      builder.append(getRReilAddress().toAssemblerString());
      builder.append(" ");
      builder.append("store");
      builder.append("." + writeAddress.getSize());
      builder.append("." + rhs.getSize());
      builder.append(" ");
      builder.append(writeAddress.toAssemblerString());
      builder.append(", ");
      builder.append(rhs.toAssemblerString());
      return builder.toString();
    }

  }

  /**
   * Conditional branches.
   */
  public static final class BranchToNative extends RReil {
    private final SimpleExpression cond;
    private final Lin target;

    public BranchToNative (RReilAddr rreilAddress, SimpleExpression cond, Lin target) {
      super(rreilAddress);
      this.cond = cond;
      this.target = target;
    }

    public SimpleExpression getCond () {
      return cond;
    }

    public Lin getTarget () {
      return target;
    }

    @Override public <R, T> R accept (RReilVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    /**
     * Returns <code>true</code> if this jump will always be taken, i.e. it does
     * not have a fall-through alternative branch.
     */
    @Override public boolean isAlwaysTakenBranch () {
      if (cond instanceof LinRval) {
        Rval condRval = ((LinRval) cond).getRval();
        if (condRval instanceof Rlit)
          return !((Rlit) condRval).getValue().isZero();
      }
      return false;
    }

    /**
     * Returns <code>true</code> if this jump will never be taken (statically).
     */
    @Override public boolean isDeadBranch () {
      if (cond instanceof LinRval) {
        Rval condRval = ((LinRval) cond).getRval();
        if (condRval instanceof Rlit)
          return ((Rlit) condRval).getValue().isZero();
      }
      return false;
    }

    @Override public boolean isBranch () {
      return true;
    }

    /**
     * Returns <code>true</code> if this instruction is a conditional jump, i.e.
     * it has two branches that will be taken depending on the evaluation of a
     * condition.
     */
    @Override public boolean isConditionalBranch () {
      return !(cond instanceof LinRval && ((LinRval) cond).getRval() instanceof Rlit);
    }

    /**
     * Returns <code>true</code> if this instruction is a computed jump.
     */
    @Override public boolean isIndirectBranch () {
      return !(target instanceof LinRval && ((LinRval) target).getRval() instanceof Rlit);
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((cond == null) ? 0 : cond.hashCode());
      result = prime * result + ((target == null) ? 0 : target.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (!(obj instanceof BranchToNative))
        return false;
      BranchToNative other = (BranchToNative) obj;
      if (cond == null) {
        if (other.cond != null)
          return false;
      } else if (!cond.equals(other.cond))
        return false;
      if (target == null) {
        if (other.target != null)
          return false;
      } else if (!target.equals(other.target))
        return false;
      return true;
    }

    @Override public String mnemonic () {
      if (isAlwaysTakenBranch()) {
        return "if TRUE";
      } else if (isDeadBranch()) {
        return "if FALSE";
      } else {
        return "if";
      }
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(formattedMnemonic());
      builder.append(" ");
      if (isConditionalBranch()) {
        builder.append(cond);
        builder.append(" ");
      }
      builder.append("goto native");
      builder.append(" ");
      if (target instanceof LinRval && ((LinRval) target).getRval() instanceof Rlit)
        ((Rlit) ((LinRval) target).getRval()).asHexPrefix(builder);
      else
        builder.append(target);
      return builder.toString();
    }

    @Override public String reconstructCode () {
      return "new RReil.BranchToNative(" + getRReilAddress().reconstructCode() + ", " + cond.reconstructCode() + ", "
        + target.reconstructCode() + ")";
    }

    @Override public String toAssemblerString () {
      StringBuilder builder = new StringBuilder();
      int size = getTarget().getSize();
      builder.append(getRReilAddress().toAssemblerString());
      builder.append(" ");
      builder.append("brc");
      builder.append("." + size);
      builder.append(" ");
      builder.append(cond.toAssemblerString());
      builder.append(", ");
      /**
       * Todo: New RReil grammar
       */
      throw new RReilGrammarException();
//      builder.append(target.toHexAssemblerString());
//      return builder.toString();
    }
  }

  /**
   * Conditional jumps/branches supporting intra-RREIL jumps.
   */
  public static final class BranchToRReil extends RReil {
    private final SimpleExpression cond;
    private final Address target;

    public BranchToRReil (RReilAddr rreilAddress, SimpleExpression cond, Address target) {
      super(rreilAddress);
      this.cond = cond;
      this.target = target;
    }

    public SimpleExpression getCond () {
      return cond;
    }

    public Address getTarget () {
      return target;
    }

    @Override public <R, T> R accept (RReilVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    /**
     * Returns <code>true</code> if this jump will always be taken, i.e. it does
     * not have a fall-through alternative branch.
     */
    @Override public boolean isAlwaysTakenBranch () {
      if (cond instanceof LinRval) {
        Rval condRval = ((LinRval) cond).getRval();
        if (condRval instanceof Rlit)
          return !((Rlit) condRval).getValue().isZero();
      }
      return false;
    }

    @Override public boolean isBranch () {
      return true;
    }

    /**
     * Returns <code>true</code> if this instruction is a conditional jump, i.e.
     * it has two branches that will be taken depending on the evaluation of a
     * condition.
     */
    @Override public boolean isConditionalBranch () {
      return !(cond instanceof LinRval && ((LinRval) cond).getRval() instanceof Rlit);
    }

    @Override public String mnemonic () {
      if (isAlwaysTakenBranch()) {
        return "if TRUE";
      } else if (isDeadBranch()) {
        return "if FALSE";
      } else {
        return "if";
      }
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(formattedMnemonic());
      builder.append(" ");
      if (isConditionalBranch()) {
        builder.append(cond);
        builder.append(" ");
      }
      builder.append("goto rreil");
      builder.append(" ");
      builder.append(target);
      return builder.toString();
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((cond == null) ? 0 : cond.hashCode());
      result = prime * result + ((target == null) ? 0 : target.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (!(obj instanceof BranchToRReil))
        return false;
      BranchToRReil other = (BranchToRReil) obj;
      if (cond == null) {
        if (other.cond != null)
          return false;
      } else if (!cond.equals(other.cond))
        return false;
      if (target == null) {
        if (other.target != null)
          return false;
      } else if (!target.equals(other.target))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new RReil.BranchToRReil(" + getRReilAddress().reconstructCode() + ", " + cond.reconstructCode() + ", "
        + target.reconstructCode() + ")";
    }

    @Override public String toAssemblerString () {
      StringBuilder builder = new StringBuilder();
      int size = getTarget().getSize();
      builder.append(getRReilAddress().toAssemblerString());
      builder.append(" ");
      builder.append("brci");
      builder.append("." + size);
      builder.append(" ");
      builder.append(cond.toAssemblerString());
      builder.append(", ");
      builder.append(target.toAssemblerString());
      return builder.toString();
    }

  }

  /**
   * Unconditional branches, including calls, jumps and returns.
   */
  public static final class Branch extends RReil {
    private final Lin target;
    private final BranchTypeHint hint;

    public enum BranchTypeHint {
      Call, Jump, Return
    };

    public Branch (RReilAddr rreilAddress, Lin target, BranchTypeHint hint) {
      super(rreilAddress);
      this.target = target;
      this.hint = hint;
    }

    public Lin getTarget () {
      return target;
    }

    public BranchTypeHint getBranchType () {
      return hint;
    }

    @Override public boolean isBranch () {
      return true;
    }

    @Override public boolean isIndirectBranch () {
      return !(target instanceof LinRval && ((LinRval) target).getRval() instanceof Rlit);
    }

    @Override public <R, T> R accept (RReilVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public String mnemonic () {
      return hint.toString().toLowerCase();
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(formattedMnemonic());
      builder.append(" ");
      if (target instanceof LinRval && ((LinRval) target).getRval() instanceof Rlit)
        ((Rlit) ((LinRval) target).getRval()).asHexPrefix(builder);
      else
        builder.append(target);
      return builder.toString();
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((hint == null) ? 0 : hint.hashCode());
      result = prime * result + ((target == null) ? 0 : target.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (!(obj instanceof Branch))
        return false;
      Branch other = (Branch) obj;
      if (hint != other.hint)
        return false;
      if (target == null) {
        if (other.target != null)
          return false;
      } else if (!target.equals(other.target))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new RReil.Branch(" + getRReilAddress().reconstructCode() + ", " + target.reconstructCode() + ", "
        + "BranchTypeHint." + hint + ")";
    }

    @Override public String toAssemblerString () {
      StringBuilder builder = new StringBuilder();
      int size = getTarget().getSize();
      builder.append(getRReilAddress().toAssemblerString());
      builder.append(" ");
      builder.append("br");
      builder.append("." + size);
      builder.append(" ");
      builder.append(target.toAssemblerString());
      builder.append(" // ");
      builder.append(hint); // add the hint as comment to know where the jump comes from
      return builder.toString();
    }

  }

  /**
   * Nop
   */
  public static final class Nop extends RReil {
    public Nop (RReilAddr address) {
      super(address);
    }

    @Override public <R, T> R accept (RReilVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public String mnemonic () {
      return "nop";
    }

    @Override public String toString () {
      return mnemonic();
    }

    @Override public boolean equals (Object obj) {
      if (!(obj instanceof Nop))
        return false;
      return super.equals(obj);
    }

    @Override public String reconstructCode () {
      return "new RReil.Nop(" + getRReilAddress().reconstructCode() + ")";
    }

    @Override public String toAssemblerString () {
      StringBuilder builder = new StringBuilder();
      builder.append(getRReilAddress().toAssemblerString());
      builder.append(" ");
      builder.append("nop");
      return builder.toString();
    }
  }

  /**
   * Primitive operations.
   */
  public static final class PrimOp extends RReil {
    private final String name;
    private final List<Lhs> outArgs;
    private final List<Rhs.Rval> inArgs;

    public PrimOp (String name, List<Lhs> outArgs, List<Rhs.Rval> inArgs) {
      this(null, name, outArgs, inArgs);
    }

    public PrimOp (RReilAddr address, String name, List<Lhs> outArgs, List<Rhs.Rval> inArgs) {
      super(address);
      this.name = name;
      this.outArgs = outArgs != null ? outArgs : Collections.<Lhs>emptyList();
      this.inArgs = inArgs != null ? inArgs : Collections.<Rhs.Rval>emptyList();
    }

    public String getName () {
      return name;
    }

    public List<Lhs> getOutArgs () {
      return outArgs;
    }

    public Lhs getOutArg (int number) {
      return outArgs.get(number);
    }

    public List<Rhs.Rval> getInArgs () {
      return inArgs;
    }

    public Rhs.Rval getInArg (int number) {
      return inArgs.get(number);
    }

    @Override public <R, T> R accept (RReilVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    /**
     * Check if this PrimOp is the primitive with the given name and parameters.
     *
     * @param operationName
     *          The name of the primitive operation
     * @param outArgsNumber
     *          The number of parameters that are written. For variable
     *          arguments use the constant {@link Integer#MAX_VALUE}.
     * @param inArgsNumber
     *          The number of parameters that are read. For variable arguments
     *          use the constant {@link Integer#MAX_VALUE}.
     * @return {@code true} if this PrimOp is the primitive with the given
     *         configuration
     */
    public boolean is (String operationName, int outArgsNumber, int inArgsNumber) {
      if (!name.equals(operationName))
        return false;
      if (outArgsNumber != Integer.MAX_VALUE && outArgs.size() != outArgsNumber)
        return false;
      if (inArgsNumber != Integer.MAX_VALUE && inArgs.size() != inArgsNumber)
        return false;
      return true;
    }

    @Override public String mnemonic () {
      return name;
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      if (!outArgs.isEmpty()) {
        if (outArgs.size() > 1)
          builder.append("<");
        String sep = "";
        for (Lhs lhs : outArgs) {
          builder.append(sep);
          builder.append(lhs);
          sep = ", ";
        }
        if (outArgs.size() > 1)
          builder.append(">");
        builder.append(" = ");
      }
      builder.append(name);
      builder.append("(");
      String sep = "";
      for (Rhs.Rval rhs : inArgs) {
        builder.append(sep);
        builder.append(rhs);
        sep = ", ";
      }
      builder.append(")");
      return builder.toString();
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((inArgs == null) ? 0 : inArgs.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((outArgs == null) ? 0 : outArgs.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (!(obj instanceof PrimOp))
        return false;
      PrimOp other = (PrimOp) obj;
      if (inArgs == null) {
        if (other.inArgs != null)
          return false;
      } else if (!inArgs.equals(other.inArgs))
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      if (outArgs == null) {
        if (other.outArgs != null)
          return false;
      } else if (!outArgs.equals(other.outArgs))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new RReil.PrimOp(" + getRReilAddress().reconstructCode() + ", \"" + name + "\", "
        + reconstructList("Lhs", outArgs)
        + ", " + reconstructList("Rhs.Rval", inArgs) + ")";
    }

    @Override public String toAssemblerString () {
      // TODO: this only works for some simple primitives I guess.
      return toString();
    }
  }

  /**
   * Unknown (untranslated) native instruction.
   */
  public static final class Native extends RReil {
    private final String name;
    private final Rhs.Rlit opnd;

    public Native (RReilAddr address, String name, Rhs.Rlit opnd) {
      super(address);
      this.name = name;
      this.opnd = opnd;
    }

    public String getName () {
      return name;
    }

    public Rhs.Rlit getOpnd () {
      return opnd;
    }

    @Override public <R, T> R accept (RReilVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public String mnemonic () {
      return "native " + name;
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(formattedMnemonic());
      builder.append(" ");
      builder.append("(");
      builder.append(opnd);
      builder.append(")");
      builder.append(":");
      builder.append(opnd.getSize());
      return builder.toString();
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((opnd == null) ? 0 : opnd.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (!(obj instanceof Native))
        return false;
      Native other = (Native) obj;
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      if (opnd == null) {
        if (other.opnd != null)
          return false;
      } else if (!opnd.equals(other.opnd))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new RReil.Native(" + getRReilAddress().reconstructCode() + ", " + name + ", " + opnd.reconstructCode()
        + ")";
    }

    @Override public String toAssemblerString () {
      throw new UnimplementedException();
    }

  }

  /**
   * Throw statement for processor exceptions
   */
  public static final class Throw extends RReil {
    private final String exception;

    public Throw (RReilAddr address, String exception) {
      super(address);
      this.exception = exception;
    }

    @Override public <R, T> R accept (RReilVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public String mnemonic () {
      return "throw";
    }

    public String getException () {
      return exception;
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(formattedMnemonic());
      builder.append(" ");
      builder.append(exception);
      return builder.toString();
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((exception == null) ? 0 : exception.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (!(obj instanceof Throw))
        return false;
      Throw other = (Throw) obj;
      if (exception == null) {
        if (other.exception != null)
          return false;
      } else if (!exception.equals(other.exception))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new RReil.Throw(" + getRReilAddress().reconstructCode() + ", " + exception + ")";
    }

    @Override public String toAssemblerString () {
      throw new UnimplementedException();
    }

  }

  public static final class Flop extends RReil {
    private final FlopOp flop;
    private final Rvar lhs;
    private final List<Rvar> rhs;
    private final Rvar flags;

    public Flop (RReilAddr address, FlopOp flop, Rvar lhs, List<Rvar> rhs, Rvar flags) {
      super(address);
      this.flop = flop;
      this.lhs = lhs;
      this.rhs = rhs;
      this.flags = flags;
    }

    public FlopOp getOp () {
      return flop;
    }

    public Rvar getLhs () {
      return lhs;
    }

    public List<Rvar> getRhs () {
      return rhs;
    }

    public Rvar getFlags () {
      return flags;
    }

    @Override public <R, T> R accept (RReilVisitor<R, T> visitor, T data) {
      return visitor.visit(this, data);
    }

    @Override public String mnemonic () {
      return flop.asPrefix();
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(formattedMnemonic());
      builder.append(" ");
      builder.append(lhs);
      builder.append(", ");
      builder.append("(");
      boolean first = true;
      for (Rvar var : rhs) {
        if (!first)
          builder.append(", ");
        first = false;
        builder.append(var);
      }
      builder.append("), [");
      builder.append(flags);
      builder.append("]");
      return builder.toString();
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((flags == null) ? 0 : flags.hashCode());
      result = prime * result + ((flop == null) ? 0 : flop.hashCode());
      result = prime * result + ((lhs == null) ? 0 : lhs.hashCode());
      result = prime * result + ((rhs == null) ? 0 : rhs.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (!(obj instanceof Flop))
        return false;
      Flop other = (Flop) obj;
      if (flags == null) {
        if (other.flags != null)
          return false;
      } else if (!flags.equals(other.flags))
        return false;
      if (flop != other.flop)
        return false;
      if (lhs == null) {
        if (other.lhs != null)
          return false;
      } else if (!lhs.equals(other.lhs))
        return false;
      if (rhs == null) {
        if (other.rhs != null)
          return false;
      } else if (!rhs.equals(other.rhs))
        return false;
      return true;
    }

    @Override public String reconstructCode () {
      return "new RReil.Flop(" + getRReilAddress().reconstructCode() + ", " + flop.reconstructCode() + ", "
        + lhs.reconstructCode() + ", " + reconstructList("Rhs.Rvar", rhs) + ", " + flags.reconstructCode() + ")";
    }

    @Override public String toAssemblerString () {
      throw new UnimplementedException();
    }

  }
}
