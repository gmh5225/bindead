package satDomain;


public class NativeInverter {
  static {
    loadNativeDependencies();
  }

  public static void loadNativeDependencies () {
    System.loadLibrary("AndyKing");
  }

  public static void invert (SatDomain cls, int lastVar) {
    // System.out.println("invert " + lastVar + " " + cls);
    final NativeInverter p = new NativeInverter();
    cls.addClausesToInverter(p);
    // System.out.println("inverting");
    // p.showcnf(p.internalCnf);
    // System.out.println("inverted");
    p.invert(p.internalCnf, lastVar + 1, p.internalDnf);
    // p.showcnf(dnf);
    int[] c;
    cls.setLastVar(lastVar);
    cls.csetToTop();
    // System.out.println("lastvar " + lastVar);
    while ((c = p.popClause()) != null)
      cls.assumeClause(c);
    p.destroycnf(p.internalCnf);
    p.destroycnf(p.internalDnf);
    // System.out.println("inverted 2: " + dls);
  }

  private final long internalCnf = makecnf();
  private final long internalDnf = makecnf();

  void addclause (int[] clause) {
    addclause(internalCnf, clause);
  }

  int[] popClause () {
    final int[] c = popclause(internalDnf);
    if (c != null)
      for (int i = 0; i < c.length; i++) {
        final int v = c[i];
        c[i] = (v & 1) == 0 ? v / 2 : -(v / 2);
      }
    // System.out.println("popping clause " + Arrays.toString(c));
    return c;
  }

  void showcnf () {
    showcnf(internalCnf);
  }

  native private void addclause (long cnf, int[] clause);

  native private void destroycnf (long cnf);

  native private void invert (long cnf, int scope, long dnf);

  native private long makecnf ();

  native private int[] popclause (long cnf);

  native private void showcnf (long cnf);
}
