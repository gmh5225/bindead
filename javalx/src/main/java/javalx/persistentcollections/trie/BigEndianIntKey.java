package javalx.persistentcollections.trie;

class BigEndianIntKey {
  private BigEndianIntKey () {
  }

  public final static int mask (int k, int m) {
    return k & (~(m - 1) ^ m);
  }

  public final static boolean match (int k, int p, int m) {
    return mask(k, m) == p;
  }

  public final static boolean zero (int k, int m) {
    return (k & m) == 0;
  }

  public final static int highestOneBit (int x) {
    int i = x;

    i |= i >> 1;
    i |= i >> 2;
    i |= i >> 4;
    i |= i >> 8;
    i |= i >> 16;
    // for 64bit: i |= (i >> 32);
    return i - (i >>> 1);
  }

  public final static int branchMask (int p1, int p2) {
    return highestOneBit(p1 ^ p2);
    // return Integer.highestOneBit(p1 ^ p2);
  }
}
