package rreil.lang.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javalx.exceptions.UnimplementedException;
import javalx.fn.Fn;
import javalx.mutablecollections.CollectionHelpers;
import rreil.lang.Lhs;
import rreil.lang.RReil;
import rreil.lang.RReil.Assertion;
import rreil.lang.RReil.Assertion.AssertionCompare;
import rreil.lang.RReil.Assign;
import rreil.lang.RReil.Branch;
import rreil.lang.RReil.BranchToNative;
import rreil.lang.RReil.BranchToRReil;
import rreil.lang.RReil.Flop;
import rreil.lang.RReil.Load;
import rreil.lang.RReil.Native;
import rreil.lang.RReil.Nop;
import rreil.lang.RReil.PrimOp;
import rreil.lang.RReil.Store;
import rreil.lang.RReil.Throw;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Address;
import rreil.lang.Rhs.Bin;
import rreil.lang.Rhs.Cmp;
import rreil.lang.Rhs.Convert;
import rreil.lang.Rhs.LinBin;
import rreil.lang.Rhs.LinRval;
import rreil.lang.Rhs.LinScale;
import rreil.lang.Rhs.RangeRhs;
import rreil.lang.Rhs.Rlit;
import rreil.lang.Rhs.Rval;
import rreil.lang.Rhs.Rvar;
import rreil.lang.Rhs.SignExtend;
import rreil.lang.Rhs.SimpleExpression;

/**
 * Retrieves all the variables that are used in an RREIL instruction.
 *
 * @author Bogdan Mihaila
 */
public class RvarExtractor {
  private static LhsExtractor lhsExtract = new LhsExtractor();
  private static RhsExtractor rhsExtract = new RhsExtractor();
  
  public static List<Rvar> fromSimpleExpression (SimpleExpression s) {
    Collector collector = new Collector();
    s.accept(collector, null);
    return collector.variables;
  }

  private static List<Rvar> singleton (Rvar variable) {
    return Collections.singletonList(variable);
  }

  private static List<Rvar> none () {
    return Collections.emptyList();
  }

  public static List<Rvar> fromRval (Rval variable) {
    if (variable instanceof Rvar)
      return singleton((Rvar) variable);
    else
      return none();
  }

  public static List<Rvar> fromRhs (Rhs rhs) {
    Collector collector = new Collector();
    rhs.accept(collector, null);
    return new ArrayList<Rvar>(collector.variables);
  }

  /**
   * Extract all the variables used in the instruction (may contain duplicate variables if they occur more than once).
   *
   * @param insn The RREIL instruction to retrieve the variables from
   * @return A list of all the variables used in the above instruction
   */
  public static List<Rvar> getAll (RReil insn) {
    Collector collector = new Collector();
    insn.accept(collector, null);
    return collector.variables;
  }

  /**
   * Extract all the variables and remove duplicates from the list.
   *
   * @param insn The RREIL instruction to retrieve the variables from
   * @return A set of all the variables used in the above instruction
   */
  public static Set<Rvar> getUnique (RReil insn) {
    Collector collector = new Collector();
    insn.accept(collector, null);
    return unique(collector.variables);
  }

  /**
   * Transform the passed in variable list to a set, i.e. make sure every variable occurs only once.
   */
  public static Set<Rvar> unique (List<Rvar> variables) {
    Set<Rvar> uniqueVariables = new LinkedHashSet<Rvar>();
    for (Rvar rvar : variables) {
      uniqueVariables.add(rvar);
    }
    return uniqueVariables;
  }

  /**
   * Extract all the variables used in the left-hand-side of this instruction
   * (may contain duplicate variables if they occur more than once).
   *
   * @param insn The RREIL instruction to retrieve the variables from
   * @return A list of all the lhs variables used in the above instruction
   */
  public static List<Rvar> getLhs (RReil insn) {
    return insn.accept(lhsExtract, null);
  }

  /**
   * Extract all the variables used in the right-hand-side of this instruction
   * (may contain duplicate variables if they occur more than once).
   *
   * @param insn The RREIL instruction to retrieve the variables from
   * @return A list of all the rhs variables used in the above instruction
   */
  public static List<Rvar> getRhs (RReil insn) {
    return insn.accept(rhsExtract, null);
  }

  private static class LhsExtractor implements RReilVisitor<List<Rvar>, Void> {

    LhsExtractor () {
    }

    @Override public List<Rvar> visit (Assign stmt, Void data) {
      return singleton(stmt.getLhs().asRvar());
    }

    @Override public List<Rvar> visit (Load stmt, Void data) {
      return singleton(stmt.getLhs().asRvar());
    }

    @Override public List<Rvar> visit (Store stmt, Void data) {
      return none();
    }

    @Override public List<Rvar> visit (BranchToNative stmt, Void data) {
      return none();
    }

    @Override public List<Rvar> visit (BranchToRReil stmt, Void data) {
      return none();
    }

    @Override public List<Rvar> visit (Nop stmt, Void data) {
      return none();
    }

    @Override public List<Rvar> visit (Assertion stmt, Void data) {
      if (stmt instanceof AssertionCompare) {
        AssertionCompare assertion = (AssertionCompare) stmt;
        return fromRhs(assertion.getLhs());
      }
      return none();
    }

    @Override public List<Rvar> visit (Branch stmt, Void data) {
      return none();
    }

    @Override public List<Rvar> visit (PrimOp stmt, Void data) {
      return CollectionHelpers.map(stmt.getOutArgs(), new Fn<Lhs, Rvar>() {
        @Override public Rvar apply (Lhs a) {
          return a.asRvar();
        }
      });
    }

    @Override public List<Rvar> visit (Native stmt, Void data) {
      return none();
    }

    @Override public List<Rvar> visit (Throw stmt, Void data) {
      return none();
    }

    @Override public List<Rvar> visit (Flop stmt, Void data) {
      return singleton(stmt.getLhs());
    }

  }

  private static class RhsExtractor implements RReilVisitor<List<Rvar>, Void> {

    @Override public List<Rvar> visit (Assign stmt, Void data) {
      return fromRhs(stmt.getRhs());
    }

    @Override public List<Rvar> visit (Load stmt, Void data) {
      return fromSimpleExpression(stmt.getReadAddress());
    }

    @Override public List<Rvar> visit (Store stmt, Void data) {
      List<Rvar> result = new ArrayList<Rvar>();
      result.addAll(fromSimpleExpression(stmt.getWriteAddress()));
      result.addAll(fromRhs(stmt.getRhs()));
      return result;
    }

    @Override public List<Rvar> visit (BranchToNative stmt, Void data) {
      List<Rvar> result = new ArrayList<Rvar>();
      result.addAll(fromSimpleExpression(stmt.getCond()));
      result.addAll(fromSimpleExpression(stmt.getTarget()));
      return result;
    }

    @Override public List<Rvar> visit (BranchToRReil stmt, Void data) {
      return fromSimpleExpression(stmt.getCond());
    }

    @Override public List<Rvar> visit (Nop stmt, Void data) {
      return none();
    }

    @Override public List<Rvar> visit (Assertion stmt, Void data) {
      if (stmt instanceof AssertionCompare) {
        AssertionCompare assertion = (AssertionCompare) stmt;
        return fromRhs(assertion.getRhs());
      }
      return none();
    }

    @Override public List<Rvar> visit (Branch stmt, Void data) {
      return fromSimpleExpression(stmt.getTarget());
    }

    @Override public List<Rvar> visit (PrimOp stmt, Void data) {
      List<Rvar> result = new ArrayList<Rvar>();
      for (Rval variable : stmt.getInArgs()) {
        if (variable instanceof Rvar)
          result.add((Rvar) variable);
      }
      return result;
    }

    @Override public List<Rvar> visit (Native stmt, Void data) {
      return none();
    }

    @Override public List<Rvar> visit (Throw stmt, Void data) {
      return none();
    }

    @Override public List<Rvar> visit (Flop stmt, Void data) {
      return stmt.getRhs();
    }

  }

  private static class Collector implements RReilVisitor<Void, Void>, RhsVisitor<Void, Void> {
    public final List<Rvar> variables;

    public Collector () {
      variables = new LinkedList<Rvar>();
    }

    @Override public Void visit (Bin expr, Void _) {
      expr.getLeft().accept(this, _);
      expr.getRight().accept(this, _);
      return null;
    }
    
    @Override public Void visit (LinBin expr, Void _) {
      expr.getLeft().accept(this, _);
      expr.getRight().accept(this, _);
      return null;
    }

    @Override public Void visit (LinScale expr, Void _) {
      expr.getOpnd().accept(this, _);
      return null;
    }
    

    @Override public Void visit (LinRval expr, Void _) {
      expr.getRval().accept(this, _);
      return null;
    }

    @Override public Void visit (Cmp expr, Void _) {
      expr.getLeft().accept(this, _);
      expr.getRight().accept(this, _);
      return null;
    }

    @Override public Void visit (SignExtend expr, Void _) {
      expr.getRhs().accept(this, _);
      return null;
    }

    @Override public Void visit (Convert expr, Void _) {
      expr.getRhs().accept(this, _);
      return null;
    }

    @Override public Void visit (Rvar expr, Void _) {
      variables.add(expr);
      return null;
    }

    @Override public Void visit (Rlit expr, Void _) {
      return null;
    }

    @Override public Void visit (Address expr, Void data) {
      return null;
    }

    @Override public Void visit (RangeRhs expr, Void _) {
      return null;
    }

    @Override public Void visit (Assign stmt, Void _) {
      collect(stmt.getLhs());
      stmt.getRhs().accept(this, _);
      return null;
    }

    @Override public Void visit (Load stmt, Void _) {
      collect(stmt.getLhs());
      stmt.getReadAddress().accept(this, _);
      return null;
    }

    @Override public Void visit (Store stmt, Void _) {
      stmt.getWriteAddress().accept(this, _);
      stmt.getRhs().accept(this, _);
      return null;
    }

    @Override public Void visit (BranchToNative stmt, Void _) {
      stmt.getCond().accept(this, _);
      stmt.getTarget().accept(this, _);
      return null;
    }

    @Override public Void visit (BranchToRReil stmt, Void _) {
      return null;
    }

    @Override public Void visit (Branch stmt, Void _) {
      stmt.getTarget().accept(this, _);
      return null;
    }

    @Override public Void visit (Nop stmt, Void _) {
      return null;
    }

    @Override public Void visit (Assertion stmt, Void _) {
      if (stmt instanceof AssertionCompare) {
        AssertionCompare assertion = (AssertionCompare) stmt;
        assertion.getLhs().accept(this, _);
        assertion.getRhs().accept(this, _);
      }
      return null;
    }

    @Override public Void visit (PrimOp stmt, Void _) {
      for (Lhs lhs : stmt.getOutArgs()) {
        collect(lhs);
      }
      for (Rval rhs : stmt.getInArgs()) {
        if (!(rhs instanceof Rvar))
          continue;
        variables.add((Rvar) rhs);
      }
      return null;
    }

    @Override public Void visit (Native stmt, Void _) {
      return null;
    }

    private void collect (Lhs lhs) {
      variables.add(lhs.asRvar());
    }

    @Override public Void visit (Throw stmt, Void data) {
      throw new UnimplementedException();
    }

    @Override public Void visit (Flop stmt, Void data) {
      throw new UnimplementedException();
    }
  }
}
