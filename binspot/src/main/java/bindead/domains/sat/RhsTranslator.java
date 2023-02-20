package bindead.domains.sat;

import java.util.LinkedList;
import java.util.List;

import javalx.numeric.BigInt;
import javalx.numeric.Interval;
import rreil.lang.BinOp;
import bindead.abstractsyntax.finite.Finite.Bin;
import bindead.abstractsyntax.finite.Finite.Cmp;
import bindead.abstractsyntax.finite.Finite.Convert;
import bindead.abstractsyntax.finite.Finite.FiniteRangeRhs;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.Finite.SignExtend;
import bindead.abstractsyntax.finite.Finite.Test;
import bindead.abstractsyntax.finite.FiniteExprVisitor;
import bindead.data.Linear;
import bindead.data.Linear.Term;
import bindead.data.NumVar;

class RhsTranslator implements FiniteExprVisitor<List<List<Integer>>, Boolean> {
  private final SatStateBuilder ssb;

  RhsTranslator (SatStateBuilder s) {
    ssb = s;
  }

  @Override public List<List<Integer>> visit (Bin expr, Boolean data) {
    System.out.println("Visit bin " + expr);
    BinOp op = expr.getOp();
    Rlin l = expr.getLeft();
    Rlin r = expr.getRight();

    if (op == BinOp.Sub || op == BinOp.Add || op == BinOp.Xor) {
      return makeXor(l, r, data);
    } else if (op == BinOp.Mul || op == BinOp.And) {
      return makeAnd(l, r, data);
    } else if (op == BinOp.Or) {
      return makeOr(l, r, data);
    } else
      return null;
  }

  List<List<Integer>> makeOr (Rlin l, Rlin r, Boolean data) {
    List<List<Integer>> ll = l.accept(this, data);
    List<List<Integer>> rl = r.accept(this, data);
    if (data) {
      return makeDisjunction(ll, rl);
    } else {
      return makeConjunction(ll, rl);

    }
  }

  private List<List<Integer>> makeAnd (Rlin l, Rlin r, Boolean data) {
    List<List<Integer>> ll = l.accept(this, data);
    List<List<Integer>> rl = r.accept(this, data);
    if (!data) {
      return makeConjunction(ll, rl);
    } else {
      return makeDisjunction(ll, rl);

    }
  }

  private static List<List<Integer>> makeDisjunction (List<List<Integer>> ll,
      List<List<Integer>> rl) {
    LinkedList<List<Integer>> newL = new LinkedList<List<Integer>>();
    if (ll == null || rl == null)
      return null;
    for (List<Integer> l1 : ll)
      for (List<Integer> r1 : rl) {
        LinkedList<Integer> newC = new LinkedList<Integer>(l1);
        newC.addAll(r1);
        newL.add(newC);
      }
    return newL;
  }

  private static List<List<Integer>> makeConjunction (List<List<Integer>> ll,
      List<List<Integer>> rl) {
    LinkedList<List<Integer>> newL = new LinkedList<List<Integer>>(ll);
    newL.addAll(rl);
    return newL;
  }

  private List<List<Integer>> makeXor (Rlin l, Rlin r, Boolean data) {
    List<List<Integer>> ll1 = l.accept(this, data);
    List<List<Integer>> rl1 = r.accept(this, true);
    List<List<Integer>> ll2 = l.accept(this, !data);
    List<List<Integer>> rl2 = r.accept(this, false);
    List<List<Integer>> o1 = makeDisjunction(ll1, rl1);
    List<List<Integer>> o2 = makeDisjunction(ll2, rl2);
    return makeConjunction(o1, o2);
  }

  @Override public List<List<Integer>> visit (Cmp expr, Boolean data) {
    assert false;
    return null;
    /*
     * Cmples("cmples", "<=s"), Cmpleu("cmpleu", "<=u"), Cmplts("cmplts",
     * "<s"), Cmpltu("cmpltu", "<u"); RelOp op = expr.getOp(); if (op ==
     * RelOp.Cmpeq) { BinValue l = expr.getLeft().accept(this, data); if (l
     * == null) return null; BinValue r = expr.getRight().accept(this,
     * data); if (r == null) return null; return new BinValue(P.iff(l._2(),
     * r._2())); } else if (op == RelOp.Cmpneq) { BinValue l =
     * expr.getLeft().accept(this, data); if (l == null) return null;
     * BinValue r = expr.getRight().accept(this, data); if (r == null)
     * return null; return new BinValue(P.xor(l._2(), r._2())); } else
     * return null;
     */
  }

  @Override public List<List<Integer>> visit (Convert expr, Boolean data) {
    return null;
  }

  @Override public List<List<Integer>> visit (SignExtend expr, Boolean data) {
    return null;
  }

  @Override public List<List<Integer>> visit (FiniteRangeRhs expr, Boolean data) {
    System.out.println("visit Range " + expr + " size=" + expr.getSize());
    // if (expr.getSize() != 1) return null;
    Interval r = expr.getRange();
    System.out.println("range " + r);
    BigInt v = r.getConstant();
    if (v == null)
      return null;
    System.out.println("constant " + v);
    /*
     * if (v.equals(BigInt.ZERO)) return P.no(); else if
     * (v.equals(BigInt.ONE)) return P.yes(); else if
     * (v.equals(BigInt.MINUSONE)) return P.yes();
     */
    LinkedList<List<Integer>> res = new LinkedList<List<Integer>>();
    if (v.isOne()) {
      if (!data)
        res.add(new LinkedList<Integer>());
      return res;
    } else if (v.isZero()) {
      if (!data)
        res.add(new LinkedList<Integer>());
      return res;
    }
    return null;
  }

  @Override public List<List<Integer>> visit (Rlin expr, Boolean data) {
    System.out.println("Visit Rlin " + expr + " size " + expr.getSize());
    Linear l = expr.getLinearTerm();
    return makeLinear(data, l);
  }

  private List<List<Integer>> makeLinear (Boolean data, Linear l) {
    Term[] ts = l.getTerms();
    List<List<Integer>> clauses = new LinkedList<List<Integer>>();
    if (ts.length == 0) {
      if (!data)
        clauses.add(new LinkedList<Integer>());
      //System.err.println("returning " + clauses);
      return clauses;
    }
    if (ts.length != 1) {
      return null;
    }
    Term t = ts[0];
    if (!t.getCoeff().isOne()) {
      return null;
    }
    NumVar v = t.getId();
    if (!ssb.isTracked(v)) {
      return null;
    }
    LinkedList<Integer> clause = new LinkedList<Integer>();
    clause.add(data ? -v.getStamp() : v.getStamp());
    clauses.add(clause);
    return clauses;
  }

  public List<List<Integer>> visitTest (Test test, Boolean data) {
    switch (test.getOperator()) {
    case Equal:
      return makeEqualTo(test.getSize(), test.getLeftExpr(), test.getRightExpr(), data);
    case NotEqual:
      return makeEqualTo(test.getSize(), test.getLeftExpr(), test.getRightExpr(), !data);
    case SignedLessThan:
      return visitSignedLessThan(test, data);
    case SignedLessThanOrEqual:
      return visitSignedLessThanOrEqualTo(test, data);
    case UnsignedLessThan:
      assert false;
      return null;
    case UnsignedLessThanOrEqual:
      assert false;
      return null;
    default:
      throw new IllegalArgumentException();
    }
  }

  private List<List<Integer>> visitSignedLessThanOrEqualTo (Test test, Boolean data) {
    int size = test.getSize();
    if (size != 1)
      return null;
    assert false;

    List<List<Integer>> lhs = makeLinear(data, test.getLeftExpr());
    List<List<Integer>> rhs = makeLinear(data, test.getRightExpr());
    if (lhs == null || rhs == null)
      return null;
    return null;
  }

  private List<List<Integer>> visitSignedLessThan (Test test, Boolean data) {
    int size = test.getSize();
    if (size != 1)
      return null;
    assert false;

    List<List<Integer>> lhs = makeLinear(data, test.getLeftExpr());
    List<List<Integer>> rhs = makeLinear(data, test.getRightExpr());
    if (lhs == null || rhs == null)
      return null;
    return null;
  }

  private List<List<Integer>> makeEqualTo (int size, Linear lhs, Linear rhs, Boolean data) {
    System.out.println("visit EqualTo " + lhs + " == " + rhs + " ~ " + data);
    List<List<Integer>> plhs = makeLinear(data, lhs);
    List<List<Integer>> nlhs = makeLinear(!data, lhs);
    List<List<Integer>> prhs = makeLinear(data, rhs);
    List<List<Integer>> nrhs = makeLinear(!data, rhs);
    List<List<Integer>> clauses = makeDisjunction(plhs, nrhs);
    System.out.println("plhs " + plhs);
    System.out.println("nlhs " + nlhs);
    System.out.println("prhs " + prhs);
    System.out.println("nrhs " + nrhs);
    if (plhs == null || nlhs == null || prhs == null || nrhs == null)
      return null;
    clauses.addAll(makeDisjunction(nlhs, prhs));
    System.out.println("created clauses " + clauses);
    return clauses;
  }
}
