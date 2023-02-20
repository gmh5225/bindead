package binparse;

import java.util.Set;

import javalx.data.Option;

/**
 * A segment in an binary file.
 */
public class SegmentImpl implements Segment {
  private final String name;
  private final String fileName;
  private final long address;
  private final long size;
  private final byte[] data;
  private final Endianness endianness;
  private final Set<Permission> permissions;

  public SegmentImpl (String fileName, String name, long address, long size, byte[] data, Endianness endianness,
      Set<Permission> permissions) {
    if (name == null || name.isEmpty())
      this.name = null;
    else
      this.name = name;
    if (fileName == null || fileName.isEmpty())
      this.fileName = null;
    else
      this.fileName = fileName;
    this.address = address;
    this.size = size;
    this.data = data;
    this.endianness = endianness;
    this.permissions = permissions;
  }

  @Override public long getAddress () {
    return address;
  }

  @Override public Option<String> getName () {
    return Option.fromNullable(name);
  }

  @Override public String getNameOrAddress () {
    if (name == null)
      return String.format("(unnamed)@0x%08X", address);
    else
      return name;
  }

  @Override public Option<String> getFileName () {
    return Option.fromNullable(fileName);
  }

  @Override public byte[] getData () {
    return data;
  }

  @Override public long getSize () {
    return size;
  }

  @Override public Endianness getEndianness () {
    return endianness;
  }

  @Override public Set<Permission> getPermissions () {
    return permissions;
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append(getNameOrAddress());
    builder.append("(");
    builder.append("[" + Permission.unixStylePrettyPrint(permissions) + "]");
    builder.append(" ");
    builder.append(String.format("0x%x", address));
    builder.append("-");
    builder.append(String.format("0x%x", address + size));
    builder.append(")");
    return builder.toString();
  }
}
