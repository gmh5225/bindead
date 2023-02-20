package binparse;

import javalx.data.Option;

/**
 * A symbol in a binary file. A symbol is a named location in the file.
 *
 * @author Bogdan Mihaila
 */
public interface Symbol {

  /**
   * @return The address of the symbol.
   */
  public long getAddress ();

  /**
   * @return The size in bytes of the data that the symbol addresses.
   */
  public long getSize ();

  /**
   * @return The name of the symbol if it has one.
   */
  public Option<String> getName ();

  /**
   * @return The name of the symbol or its address as string if it has no name.
   */
  public String getNameOrAddress ();

  /**
   * @return The data that this symbol addresses.
   */
  public byte[] getData ();

}
