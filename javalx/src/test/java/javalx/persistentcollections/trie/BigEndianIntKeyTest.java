package javalx.persistentcollections.trie;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

@SuppressWarnings("deprecation") // REFACTOR assertEquals is deprecated
public class BigEndianIntKeyTest {
  @Test
  public void testHighestBitMaskOfZero () {
    final int mask = BigEndianIntKey.highestOneBit(0);

    assertEquals(Integer.valueOf(0), Integer.valueOf(mask));
  }

  @Test
  public void testHighestBitMaskOfOne () {
    final int mask = BigEndianIntKey.highestOneBit(1);

    assertEquals(Integer.valueOf(1), Integer.valueOf(mask));
  }

  @Test
  public void testHighestBitMaskOf2Pow10 () {
    final int mask = BigEndianIntKey.highestOneBit(1 << 10);

    assertEquals(Integer.valueOf(1024), Integer.valueOf(mask));
  }

  @Test
  public void testHighestBitMaskOf2Pow10PlusOne () {
    final int mask = BigEndianIntKey.highestOneBit((1 << 10) + 1);

    assertEquals(Integer.valueOf(1024), Integer.valueOf(mask));
    assertEquals(Integer.valueOf(1024), Integer.valueOf(Integer.highestOneBit(mask)));
  }

  @Test
  public void testHighestBitMaskWithMsbSet () {
    final int mask = BigEndianIntKey.highestOneBit(1 << 31);

    assertEquals(Integer.valueOf(-2147483648), Integer.valueOf(mask));
    assertEquals(Integer.valueOf(-2147483648), Integer.valueOf(Integer.highestOneBit(mask)));
  }

  @Test
  public void testHighestBitMaskOfMinusOne () {
    final int mask = BigEndianIntKey.highestOneBit(-1);

    assertEquals(Integer.valueOf(-2147483648), Integer.valueOf(mask));
  }

  @Test
  public void testLargestCommonPrefix0 () {
    final int p1 = 2;
    final int p2 = 3;
    final int m = BigEndianIntKey.branchMask(p1, p2);
    final int p = BigEndianIntKey.mask(p1, m);

    assertEquals(Integer.valueOf(2), Integer.valueOf(p));
  }

  @Test
  public void testLargestCommonPrefix1 () {
    final int p1 = 2;
    final int p2 = 4;
    final int m = BigEndianIntKey.branchMask(p1, p2);
    final int p = BigEndianIntKey.mask(p1, m);

    assertEquals(Integer.valueOf(0), Integer.valueOf(p));
  }
}
