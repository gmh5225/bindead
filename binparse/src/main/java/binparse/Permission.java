package binparse;

import java.util.EnumSet;
import java.util.Set;

/**
 * Enum for modeling access permissions to data and code segments.
 */
public enum Permission {
  Read, Write, Execute;
  public static final Set<Permission> $ReadOnly = EnumSet.of(Read);
  public static final Set<Permission> $ReadWrite = EnumSet.of(Read, Write);
  public static final Set<Permission> $ReadExecute = EnumSet.of(Read, Execute);
  public static final Set<Permission> $ReadExecuteWrite = EnumSet.of(Read, Execute, Write);

  public static boolean isReadable (Set<Permission> permissions) {
    return permissions.contains(Read);
  }

  public static boolean isWritable (Set<Permission> permissions) {
    return permissions.contains(Write);
  }

  public static boolean isExecutable (Set<Permission> permissions) {
    return permissions.contains(Execute);
  }

  /**
   * Prints a set of permissions as a {@code rwx} triple as used in Unix.
   */
  public static String unixStylePrettyPrint (Set<Permission> permissions) {
    StringBuilder builder = new StringBuilder();
    builder.append(permissions.contains(Read) ? "r" : "-");
    builder.append(permissions.contains(Write) ? "w" : "-");
    builder.append(permissions.contains(Execute) ? "x" : "-");
    return builder.toString();
  }
}
