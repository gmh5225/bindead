package binparse;

import java.util.Set;

import javalx.data.Option;

/**
 * A segment in a binary file. A segment is a fragment of the file with a certain semantics.
 */
public interface Segment {

  /**
   * @return The start address in bytes of the segment.
   */
  public long getAddress ();

  /**
   * @return The size in bytes of the data that the segment contains.
   */
  public long getSize ();

  /**
   * @return The name of the segment if it has one.
   * @see #getNameOrAddress()
   */
  public Option<String> getName ();

  /**
   * @return The name of the segment or its address as string if it has no name.
   */
  public String getNameOrAddress ();

  /**
   * Return the name of the file this segment originated from if it belongs to a file.
   */
  public Option<String> getFileName ();

  /**
   * @return The data that this segment contains.
   */
  public byte[] getData ();

  /**
   * @return The endianness of the data in this segment.
   */
  public Endianness getEndianness ();

  /**
   * @return The allowed operations on this segment, i.e. read, write, execute.
   */
  public Set<Permission> getPermissions ();

}
