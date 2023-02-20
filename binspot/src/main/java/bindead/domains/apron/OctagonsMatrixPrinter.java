package bindead.domains.apron;

import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.persistentcollections.BiMap;
import apron.Abstract1;
import apron.ApronException;
import apron.Coeff;
import apron.Lincons0;
import apron.Linterm0;
import apron.MpqScalar;
import apron.Scalar;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;

class OctagonsMatrixPrinter {
  final OctagonsMatrix data;
  protected final BiMap<NumVar, String> variablesMapping;
  final String[] names;

  OctagonsMatrixPrinter (Abstract1 state, BiMap<NumVar, String> varMapping) throws ApronException {
    this.variablesMapping = varMapping;
    this.names = state.getEnvironment().getVars();
    assert this.names != null;
    int dim = state.getEnvironment().getVars().length;
    data = new OctagonsMatrix(dim);
    Lincons0[] lincons = state.getAbstract0Ref().toLincons(state.getCreationManager());
//    System.out.print("<init " + dim);
    for (Lincons0 t : lincons)
      setLim(t);
//    System.out.println(">");
  }

  private void removeAddresses () {
    for (int i = 0; i < data.dim; i++)
      if (isAddress(i))
        data.removeVarFromEquations(i);
  }

  private boolean isAddress (int i) {
    String n = names[i];
    NumVar v = variablesMapping.getKey(n).get();
    return v instanceof AddrVar;
  }

  private void removeScalars () {
    for (int i = 0; i < data.dim; i++)
      if (data.isConstant(i))
        data.removeVarFromEquations(i);
  }

  private String showInequality (String p, int i, String p1, int j, Bound content) {
    return p + showVar(i) + " " + p1 + " " + showVar(j) + " >= " + content.negate() + "; ";
  }

  private String showVar (int x) {
    String n = names[x];
    NumVar v = variablesMapping.getKey(n).get();
    return v.toString();
  }

  void setLim (Lincons0 t) {
    BigInt ofs = getOffsetOf(t);
    if (ofs != null) {
      Linterm0[] terms = t.getLinterms();
      int idx1 = indexOf(terms[0]);
      int idx2;
      if (terms.length == 1) {
        idx2 = data.indexOfZero();
      } else {
        assert terms.length == 2;
        idx2 = indexOf(terms[1]);
      }
      data.setLim(idx1, idx2, ofs);
    }
  }

  // assert that t is >= constraint
  private static BigInt getOffsetOf (Lincons0 t) {
    Coeff cst = t.getCst();
    return cst.isScalar() ? Marshaller.fromApronScalarRoundDown((Scalar) cst) : null;
  }

  private int indexOf (Linterm0 linterm) {
    //hsi:  unfortunately, coefficient are inefficient MpqScalars. But at least we can rely on that...
    int sgn = ((MpqScalar) linterm.coeff).cmp(0);
    // we simply assume that abs(linterm.coeff) is always 1, since it must hold for Octagons.
    if (sgn > 0)
      return data.posIdx(linterm.dim);
    else
      return data.negIdx(linterm.dim);
  }

  private StringBuilder reduceAndPrintEqualities () {
    StringBuilder builder = new StringBuilder();
    // System.err.println("r and s mat \n" + this);
    for (int i = 0; i < data.dim; i++)
      for (int j = i + 1; j < data.dim; j++) {
        Bound pp = data.ppBound(i, j);
        Bound nn = data.nnBound(i, j);
        if (pp.isFinite() && nn.isFinite() && pp.isEqualTo(nn.negate())) {
          if (!data.isConstant(i) && !data.isConstant(j)) {
            builder.append(equality(i, j, nn, " - "));
          }
          data.removeVarFromEquations(i);
        }
        Bound np = data.npBound(i, j);
        Bound pn = data.pnBound(i, j);
        if (np.isFinite() && pn.isFinite() && np.isEqualTo(pn.negate())) {
          if (!data.isConstant(i) && !data.isConstant(j)) {
            builder.append(equality(i, j, pn, " + "));
          }
          data.removeVarFromEquations(i);
        }
      }
    return builder;
  }

  private String equality (int i, int j, Bound constant, String operator) {
    if (constant.isZero()) {
      if (operator.equals("+"))
        return showVar(j) + " == " + operator + showVar(i) + ", ";
      else
        return showVar(j) + " == " + showVar(i) + ", ";
    } else {
      return showVar(j) + " == " + constant + operator + showVar(i) + ", ";
    }
  }

  private StringBuilder printBinaryRelations () {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < data.dim; i++) {
      for (int j = i + 1; j < data.dim; j++) {
        Bound content1 = data.ppBound(i, j);
        if (content1.isFinite())
          builder.append(showInequality(" ", i, "+", j, content1));
        Bound content2 = data.nnBound(i, j);
        if (content2.isFinite())
          builder.append(showInequality("-", i, "-", j, content2));
        Bound content3 = data.pnBound(i, j);
        if (content3.isFinite())
          builder.append(showInequality(" ", i, "-", j, content3));
        Bound content4 = data.npBound(i, j);
        if (content4.isFinite())
          builder.append(showInequality("-", i, "+", j, content4));
      }
    }
    return builder;
  }

  private void unrequire () {
    for (int i = 0; i < data.dim; i++)
      for (int j = 0; j < data.dim; j++) {
        Bound iu = data.nnBound(i, -1);
        Bound il = data.pnBound(i, -1).negate();
        Bound ju = data.nnBound(j, -1);
        Bound jl = data.pnBound(j, -1).negate();
        Bound iju = data.nnBound(i, j);
        Bound ijl = data.ppBound(i, j).negate();
        if (ju.add(iu).isLessThanOrEqualTo(iju))
          data.setLim(data.negIdx(i), data.negIdx(j), Bound.NEGINF);
        if (ijl.isLessThanOrEqualTo(jl.add(il)))
          data.setLim(data.posIdx(i), data.posIdx(j), Bound.NEGINF);
        Bound ijnp = data.npBound(i, j).negate();
        if (ijnp.isLessThanOrEqualTo(jl.sub(iu)))
          data.setLim(data.negIdx(i), data.posIdx(j), Bound.NEGINF);
      }
  }

  public String printMinimalContraints () {
    StringBuilder builder = new StringBuilder();
    removeAddresses();
    StringBuilder equalitiesBuilder = reduceAndPrintEqualities();
    unrequire();
    removeScalars();
    StringBuilder binariesBuilder = printBinaryRelations();
    if (equalitiesBuilder.length() > 0 || binariesBuilder.length() > 0) {
      if (equalitiesBuilder.length() > 0) {
        builder.append('{');
        builder.append(equalitiesBuilder);
        builder.append('}');
        if (binariesBuilder.length() > 0)
          builder.append('\n');
      }
      if (binariesBuilder.length() > 0) {
        builder.append('{');
        builder.append(binariesBuilder);
        builder.append("}");
      }
    }
    return builder.toString();
  }

  @Override public String toString () {
    return data.toString();
  }
}