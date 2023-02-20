package binparse;

import java.nio.ByteOrder;

/**
 * Describes the byte order of binary data.
 *
 * @author Bogdan Mihaila
 */
public enum Endianness {
  BIG, LITTLE;

  public static Endianness fromByteOrder (ByteOrder order) {
    if (order == ByteOrder.BIG_ENDIAN)
      return BIG;
    else
      return LITTLE;
  }

  /**
   * Reverses the byte in an array in place--i.e. changes the elements order of the passed in array.
   *
   * @param data The array which elements order should be reversed.
   */
  public static void reverseByteOrder (byte[] data) {
    int left = 0;
    int right = data.length - 1;
    while (left < right) {
      byte temp = data[left];
      data[left] = data[right];
      data[right] = temp;
      left++;
      right--;
    }
  }

}
