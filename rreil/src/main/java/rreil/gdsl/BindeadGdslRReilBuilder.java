package rreil.gdsl;

import gdsl.rreil.IAddress;
import gdsl.rreil.IBranchHint;
import gdsl.rreil.IFlop;
import gdsl.rreil.ILimitedVariable;
import gdsl.rreil.IRReilBuilder;
import gdsl.rreil.IRReilCollection;
import gdsl.rreil.IVariable;
import gdsl.rreil.exception.IException;
import gdsl.rreil.expression.ICompare;
import gdsl.rreil.expression.IExpression;
import gdsl.rreil.id.IId;
import gdsl.rreil.linear.ILinearExpression;
import gdsl.rreil.sexpression.ISimpleExpression;
import gdsl.rreil.statement.IStatement;
import javalx.numeric.BigInt;
import rreil.gdsl.builder.AddressBuilder;
import rreil.gdsl.builder.BranchHintBuilder;
import rreil.gdsl.builder.CompareBuilder;
import rreil.gdsl.builder.ExceptionBuilder;
import rreil.gdsl.builder.FlopOpBuilder;
import rreil.gdsl.builder.IdBuilder;
import rreil.gdsl.builder.StatementCollectionBuilder;
import rreil.gdsl.builder.VariableBuilder;
import rreil.gdsl.builder.VariableListBuilder;
import rreil.gdsl.builder.expression.ExpressionBuilder;
import rreil.gdsl.builder.expression.ExpressionSexprBuilder;
import rreil.gdsl.builder.expression.ExtensionBuilder;
import rreil.gdsl.builder.linear.BinopBuilder;
import rreil.gdsl.builder.linear.LinearBuilder;
import rreil.gdsl.builder.linear.LinearVariableBuilder;
import rreil.gdsl.builder.linear.LiteralBuilder;
import rreil.gdsl.builder.linear.ScaleBuilder;
import rreil.gdsl.builder.sexpr.ArbitraryBuilder;
import rreil.gdsl.builder.sexpr.SexprBuilder;
import rreil.gdsl.builder.sexpr.SexprCompareBuilder;
import rreil.gdsl.builder.sexpr.SexprLinearBuilder;
import rreil.gdsl.builder.statement.AssignBuilder;
import rreil.gdsl.builder.statement.BranchBuilder;
import rreil.gdsl.builder.statement.ConditionalBranchBuilder;
import rreil.gdsl.builder.statement.FlopBuilder;
import rreil.gdsl.builder.statement.IteBuilder;
import rreil.gdsl.builder.statement.LoadBuilder;
import rreil.gdsl.builder.statement.PrimitiveBuilder;
import rreil.gdsl.builder.statement.StoreBuilder;
import rreil.gdsl.builder.statement.ThrowBuilder;
import rreil.gdsl.builder.statement.WhileBuilder;
import rreil.lang.BinOp;
import rreil.lang.ComparisonOp;
import rreil.lang.FlopOp;
import rreil.lang.LinBinOp;
import rreil.lang.RReil.Branch.BranchTypeHint;
import rreil.lang.RReilAddr;

/**
 * This class contains all callbacks needed to rebuild an Gdsl RReil AST in Java.
 *
 * @author Julian Kranz
 */
public class BindeadGdslRReilBuilder implements IRReilBuilder {
  private BuildingStateManager manager;

  /**
   * Build the BindeadGdslRReilBuilder
   *
   * @param address the address to place the translated RReil statements at
   */
  public BindeadGdslRReilBuilder (RReilAddr address) {
    manager = new BuildingStateManager(address);
  }

  /**
   * Build the BindeadGdslRReilBuilder and place RReil statements at address ZERO
   */
  public BindeadGdslRReilBuilder () {
    manager = new BuildingStateManager();
  }

  /*
   * sem_id
   */

  @Override public IId shared_floating_flags () {
    return new IdBuilder(manager, "floating_flags");
  }

  @Override public IId virt_t (long temp) {
    return new IdBuilder(manager, "t" + temp);
  }

  @Override public IId arch (String name) {
    return new IdBuilder(manager, name);
  }

  /*
   * sem_exception
   */

  @Override public IException exception_shared_division_by_zero () {
    return new ExceptionBuilder(manager, "division_by_zero");
  }

  @Override public IException exception_arch (String name) {
    return new ExceptionBuilder(manager, name);
  }

  /*
   * sem_address
   */

  @Override public IAddress sem_address (long size, ILinearExpression address) {
    return new AddressBuilder(manager, (int) size, (LinearBuilder) address);
  }

  /*
   * sem_var
   */

  @Override public IVariable sem_var (IId id, long offset) {
    return new VariableBuilder(manager, (IdBuilder) id, (int) offset);
  }

  /*
   * sem_linear
   */

  @Override public ILinearExpression sem_lin_var (IVariable var) {
    return new LinearVariableBuilder(manager, (VariableBuilder) var);
  }

  @Override public ILinearExpression sem_lin_imm (long imm) {
    return new LiteralBuilder(manager, BigInt.of(imm));
  }

  @Override public ILinearExpression sem_lin_add (ILinearExpression s0,
      ILinearExpression s1) {
    return new BinopBuilder(manager, LinBinOp.Add, (LinearBuilder) s0,
      (LinearBuilder) s1);
  }

  @Override public ILinearExpression sem_lin_sub (ILinearExpression s0,
      ILinearExpression s1) {
    return new BinopBuilder(manager, LinBinOp.Sub, (LinearBuilder) s0,
      (LinearBuilder) s1);
  }

  @Override public ILinearExpression sem_lin_scale (long scale, ILinearExpression opnd) {
    return new ScaleBuilder(manager, (int) scale, (LinearBuilder) opnd);
  }

  /*
   * sem_sexpr
   */

  @Override public ISimpleExpression sem_sexpr_lin (ILinearExpression lin) {
    return new SexprLinearBuilder(manager, (LinearBuilder) lin);
  }

  @Override public ISimpleExpression sem_sexpr_cmp (long size, ICompare cmp) {
    return new SexprCompareBuilder(manager, (int) size, (CompareBuilder) cmp);
  }

  @Override public ISimpleExpression sem_sexpr_arb () {
    return new ArbitraryBuilder(manager);
  }

  /*
   * sem_op_cmp
   */

  @Override public ICompare sem_cmpeq (ILinearExpression lhs, ILinearExpression rhs) {
    return new CompareBuilder(manager, ComparisonOp.Cmpeq, (LinearBuilder) lhs,
      (LinearBuilder) rhs);
  }

  @Override public ICompare sem_cmpneq (ILinearExpression lhs, ILinearExpression rhs) {
    return new CompareBuilder(manager, ComparisonOp.Cmpneq,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  @Override public ICompare sem_cmples (ILinearExpression lhs, ILinearExpression rhs) {
    return new CompareBuilder(manager, ComparisonOp.Cmples,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  @Override public ICompare sem_cmpleu (ILinearExpression lhs, ILinearExpression rhs) {
    return new CompareBuilder(manager, ComparisonOp.Cmpleu,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  @Override public ICompare sem_cmplts (ILinearExpression lhs, ILinearExpression rhs) {
    return new CompareBuilder(manager, ComparisonOp.Cmplts,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  @Override public ICompare sem_cmpltu (ILinearExpression lhs, ILinearExpression rhs) {
    return new CompareBuilder(manager, ComparisonOp.Cmpltu,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  /*
   * sem_expr
   */

  @Override public IExpression sem_sexpr (ISimpleExpression sexpr) {
    return new ExpressionSexprBuilder(manager, (SexprBuilder) sexpr);
  }

  @Override public IExpression sem_mul (ILinearExpression lhs, ILinearExpression rhs) {
    return new rreil.gdsl.builder.expression.BinopBuilder(manager, BinOp.Mul,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  @Override public IExpression sem_div (ILinearExpression lhs, ILinearExpression rhs) {
    return new rreil.gdsl.builder.expression.BinopBuilder(manager, BinOp.Divu,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  @Override public IExpression sem_divs (ILinearExpression lhs, ILinearExpression rhs) {
    return new rreil.gdsl.builder.expression.BinopBuilder(manager, BinOp.Divs,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  @Override public IExpression sem_mod (ILinearExpression lhs, ILinearExpression rhs) {
    return new rreil.gdsl.builder.expression.BinopBuilder(manager, BinOp.Mod,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  @Override public IExpression sem_mods (ILinearExpression lhs, ILinearExpression rhs) {
    return new rreil.gdsl.builder.expression.BinopBuilder(manager, BinOp.Mods,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  @Override public IExpression sem_shl (ILinearExpression lhs, ILinearExpression rhs) {
    return new rreil.gdsl.builder.expression.BinopBuilder(manager, BinOp.Shl,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  @Override public IExpression sem_shr (ILinearExpression lhs, ILinearExpression rhs) {
    return new rreil.gdsl.builder.expression.BinopBuilder(manager, BinOp.Shr,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  @Override public IExpression sem_shrs (ILinearExpression lhs, ILinearExpression rhs) {
    return new rreil.gdsl.builder.expression.BinopBuilder(manager, BinOp.Shrs,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  @Override public IExpression sem_and (ILinearExpression lhs, ILinearExpression rhs) {
    return new rreil.gdsl.builder.expression.BinopBuilder(manager, BinOp.And,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  @Override public IExpression sem_or (ILinearExpression lhs, ILinearExpression rhs) {
    return new rreil.gdsl.builder.expression.BinopBuilder(manager, BinOp.Or,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  @Override public IExpression sem_xor (ILinearExpression lhs, ILinearExpression rhs) {
    return new rreil.gdsl.builder.expression.BinopBuilder(manager, BinOp.Xor,
      (LinearBuilder) lhs, (LinearBuilder) rhs);
  }

  @Override public IExpression sem_sx (long fromsize, ILinearExpression opnd) {
    return new ExtensionBuilder(manager, true, (int) fromsize,
      (LinearBuilder) opnd);
  }

  @Override public IExpression sem_zx (long fromsize, ILinearExpression opnd) {
    return new ExtensionBuilder(manager, false, (int) fromsize,
      (LinearBuilder) opnd);
  }

  /*
   * sem_varl
   */

  @Override public ILimitedVariable sem_varl (IId id, long offset, long size) {
    return (new VariableBuilder(manager, (IdBuilder) id, (int) offset))
        .size((int) size);
  }

  /*
   * sem_varls
   */

  @Override public IRReilCollection<ILimitedVariable> sem_varls_next (
      ILimitedVariable variable, IRReilCollection<ILimitedVariable> variables) {
    variables.add(variable);
    return variables;
  }

  @Override public IRReilCollection<ILimitedVariable> sem_varls_init () {
    return new VariableListBuilder(manager);
  }

  /*
   * sem_flop
   */

  @Override public IFlop sem_flop_fadd () {
    return new FlopOpBuilder(manager, FlopOp.Fadd);
  }

  @Override public IFlop sem_flop_fsub () {
    return new FlopOpBuilder(manager, FlopOp.Fsub);
  }

  @Override public IFlop sem_flop_fmul () {
    return new FlopOpBuilder(manager, FlopOp.Fmul);
  }

  /*
   * sem_stmt
   */

  @Override public IStatement sem_assign (long size, IVariable var, IExpression exp) {
    AssignBuilder aB = new AssignBuilder(manager, (int) size,
      (VariableBuilder) var, (ExpressionBuilder) exp);
    return aB;
  }

  @Override public IStatement sem_load (long size, IVariable lhs, IAddress address) {
    return new LoadBuilder(manager, (int) size, (VariableBuilder) lhs,
      (AddressBuilder) address);
  }

  @Override public IStatement sem_store (long size, IAddress address, ILinearExpression rhs) {
    return new StoreBuilder(manager, (int) size, (AddressBuilder) address,
      (LinearBuilder) rhs);
  }

  @Override public IStatement sem_ite (ISimpleExpression cond,
      IRReilCollection<IStatement> then_branch,
      IRReilCollection<IStatement> else_branch) {
    return new IteBuilder(manager, (SexprBuilder) cond,
      (StatementCollectionBuilder) then_branch,
      (StatementCollectionBuilder) else_branch);
  }

  @Override public IStatement sem_while (ISimpleExpression cond,
      IRReilCollection<IStatement> body) {
    return new WhileBuilder(manager, (SexprBuilder) cond,
      (StatementCollectionBuilder) body);
  }

  @Override public IStatement sem_cbranch (ISimpleExpression condition,
      IAddress target_true, IAddress target_false) {
    return new ConditionalBranchBuilder(manager, (SexprBuilder) condition,
      (AddressBuilder) target_true, (AddressBuilder) target_false);
  }

  @Override public IStatement sem_branch (IBranchHint hint, IAddress address) {
    return new BranchBuilder(manager, (BranchHintBuilder) hint,
      (AddressBuilder) address);
  }

  @Override public IStatement sem_flop_stmt (IFlop flop, IVariable flags,
      ILimitedVariable lhs, IRReilCollection<ILimitedVariable> rhs) {
    return new FlopBuilder(manager, (FlopOpBuilder) flop,
      (VariableBuilder) flags, (VariableBuilder) lhs,
      (VariableListBuilder) rhs);
  }

  @Override public IStatement sem_prim (String name,
      IRReilCollection<ILimitedVariable> lhs,
      IRReilCollection<ILimitedVariable> rhs) {
    return new PrimitiveBuilder(manager, name, (VariableListBuilder) lhs,
      (VariableListBuilder) rhs);
  }

  @Override public IStatement sem_throw (IException exception) {
    return new ThrowBuilder(manager, (ExceptionBuilder) exception);
  }

  /*
   * branch_hint
   */

  @Override public IBranchHint hint_jump () {
    return new BranchHintBuilder(manager, BranchTypeHint.Jump);
  }

  @Override public IBranchHint hint_call () {
    return new BranchHintBuilder(manager, BranchTypeHint.Call);
  }

  @Override public IBranchHint hint_ret () {
    return new BranchHintBuilder(manager, BranchTypeHint.Return);
  }

  /*
   * sem_stmts
   */

  @Override public IRReilCollection<IStatement> sem_stmts_next (IStatement statement,
      IRReilCollection<IStatement> statements) {
    statements.add(statement);
    return statements;
  }

  @Override public IRReilCollection<IStatement> sem_stmts_init () {
    return new StatementCollectionBuilder(manager);
  }
}
