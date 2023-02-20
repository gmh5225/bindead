package bindis.avr8.decoders;

/**
 *
 * @author mb0
 */
public class Slice {
  public static final Slice S1 = new Slice(12, 4);
  public static final Slice S2 = new Slice(8, 4);
  public static final Slice S3 = new Slice(4, 4);
  public static final Slice S4 = new Slice(0, 4);
  public static final Slice F1 = new Slice(3, 1).compose(new Slice(7, 1));
  public static final Slice Sr1 = new Slice(7, 2);
  public static final Slice S31 = new Slice(3, 1);
  public static final Slice S3191 = new Slice(3, 1).compose(new Slice(9, 1));
  public static final Slice S111 = new Slice(11, 1);
  
  private int offset;
  private int length;
  private int mask;

  public Slice (int offset, int length) {
    this.offset = offset;
    this.length = length;
    this.mask = (1 << length) - 1;
  }

  public int slice (int word) {
    return (word >> offset) & mask;
  }

  public Slice compose (final Slice other) {
    return new Slice(-1, length + other.length) {
      @Override public int slice (final int word) {
        return Slice.this.slice(word) | (other.slice(word) << Slice.this.length);
      }
    };
  }
}
