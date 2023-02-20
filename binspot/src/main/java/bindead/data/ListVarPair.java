package bindead.data;

import java.util.LinkedList;

public class ListVarPair extends LinkedList<VarPair> {

  private static final long serialVersionUID = 1L;

  public ListVarPair () {
  }

  public ListVarPair (ListVarPair nvps) {
    super(nvps);
  }

  public void add (NumVar p, NumVar e) {
    add(new VarPair(p, e));
  }

  public static ListVarPair singleton (NumVar x1, NumVar x2) {
    ListVarPair lvp = new ListVarPair();
    lvp.add(x1, x2);
    return lvp;
  }

}
