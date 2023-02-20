package bindis;

/**
 * Thrown to indicate that an indexed data has been accessed with an illegal index. The index is either negative or
 * greater than or equal to the size of the data. This exception provides more information on the index than Java's
 * {@link #IndexOutOfBoundsException()}.
 *
 * @author Bogdan Mihaila
 */
public class IndexOutOfBoundsException extends java.lang.IndexOutOfBoundsException {
  private static final long serialVersionUID = -7269642095257100431L;
  
  private final int index;

  /**
   * Constructs an <code>IndexOutOfBoundsException</code> with no * detail message.
   */
  public IndexOutOfBoundsException () {
    super();
    this.index = -1;
  }

  /**
   * Constructs a new <code>IndexOutOfBoundsException</code> class with an argument indicating the illegal index.
   *
   * @param index the illegal index.
   */
  public IndexOutOfBoundsException (int index) {
    super("Index out of range: " + index);
    this.index = index;
  }

  /**
   * @return the index at which the exception occurred
   */
  public int getIndex () {
    return index;
  }

}
