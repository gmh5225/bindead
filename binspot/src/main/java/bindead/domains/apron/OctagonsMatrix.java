package bindead.domains.apron;

import javalx.numeric.Bound;

class OctagonsMatrix {
  final int dim;
   final Bound[][] contents;

  public OctagonsMatrix (int d) {
    dim = d;
    int size = d * 2 + 1;
    contents = new Bound[size][];
    for (int i = 0; i < size; i++) {
      contents[i] = new Bound[size];
      for (int j = 0; j < size; j++) {
        contents[i][j] = Bound.NEGINF;
      }
    }
  }

  void setLim (int idx1, int idx2, Bound ofs) {
    contents[idx1][idx2] = ofs;
    contents[idx2][idx1] = ofs;
  }

  // j = posIdx(x)
  // j = dim+x+1
  // x = j-dim-1
  // j = posIdx(j-dim-1)
  int posIdx (int x) {
    return dim + x + 1;
  }

  // j = negIdx(x)
  // j = dim-x-1
  // x = dim-j-1
  // j = negIdx(dim-j-1)
  int negIdx (int x) {
    return dim - x - 1;
  }

  Bound pnBound (int x, int y) {
    return contents[posIdx(x)][negIdx(y)];
  }

  Bound npBound (int x, int y) {
    return pnBound(y, x);
  }

  Bound nnBound (int i, int j) {
    return contents[negIdx(i)][negIdx(j)];
  }

  Bound ppBound (int i, int j) {
    return contents[posIdx(i)][posIdx(j)];
  }

  int indexOfZero () {
    return dim;
  }

  void removeVarFromEquations (int var) {
    for (int x = 0; x < contents.length; x++) {
      contents[posIdx(var)][x] = Bound.NEGINF;
      contents[negIdx(var)][x] = Bound.NEGINF;
      contents[x][posIdx(var)] = Bound.NEGINF;
      contents[x][negIdx(var)] = Bound.NEGINF;
    }
  }

  boolean isConstant (int i) {
    //System.err.println("isScalar " + i + "\n");
    Bound pn = pnBound(i, -1);
    //System.err.println("pn " + pn);
    Bound nn = nnBound(i, -1);
    //System.err.println("nn " + nn);
    boolean s = pn.equals(nn.negate());
    //System.err.println("s " + s);
    return s;
  }

  boolean isZeroVar (int i) {
    return i == dim;
  }

  @Override public String toString () {
    String s = "";
    for (Bound[] b : contents)
      s = s + java.util.Arrays.toString(b) + "\n";
    s = s + "\n";
    return s;
  }
}