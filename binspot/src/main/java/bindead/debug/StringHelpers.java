package bindead.debug;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class StringHelpers {
  public static final class AnalysisSymbols {
    public static final String leq = "\u2291";
    public static final String join = "\u2294";
    public static final String widen = "\u2207";
    public static final String top = "\u22a4";
    public static final String bottom = "\u22a5";
    public static final String infinity = "\u221e";
    public static final String posInfinity = "+" + infinity;
    public static final String negInfinity = "-" + infinity;
  }

  /**
   * Concatenate a collection of strings and add a separator.
   */
  public static String joinWith (String separator, Iterable<String> strings) {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (String string : strings) {
      if (first)
        first = false;
      else
        builder.append(separator);
      builder.append(string);
    }
    return builder.toString();
  }

  /**
   * Sorts strings like they are naturally sorted (left to right) in a given array. A string not in the array will be
   * appended to the right.
   */
  public static class ArrayOrderedStringsComparator implements Comparator<String> {
    private final Map<String, Integer> stringsOrder = new HashMap<String, Integer>();

    public ArrayOrderedStringsComparator (String[] strings) {
      for (int i = 0; i < strings.length; i++) {
        stringsOrder.put(strings[i], i);
      }
    }

    private static int getStringOrder (String string, Map<String, Integer> order) {
      Integer stringOrder = order.get(string);
      if (stringOrder == null)
        return Integer.MAX_VALUE;
      else
        return stringOrder;
    }

    public boolean containsString (String string) {
      return stringsOrder.containsKey(string);
    }

    @Override public int compare (String o1, String o2) {
      return getStringOrder(o1, stringsOrder) - getStringOrder(o2, stringsOrder);
    }
  }

  /**
   * Prints a table nicely formatted so that all the data will be aligned.
   *
   * @param table The table of strings that should be printed. Table array is organized as rows*columns, hence the first
   *          index iterates over the rows. It is assumed that the table is not sparse, all the rows are of the same
   *          size.
   * @param columnsSeparator A string that will be used as the separator between columns
   * @param rowsSeparator A string that will be used as the separator between rows or {@code null} if there should be
   *          none
   * @param cellsContentLeftAligned {@code true} if the strings in the table should be left aligned or {@code false} if
   *          right aligned
   * @param builder A string builder that will be mutated to contain the printed table
   */
  public static void printFormattedTable (List<List<String>> table, String columnsSeparator, String rowsSeparator,
      boolean cellsContentLeftAligned, StringBuilder builder) {
    String[][] tableArray = new String[table.size()][table.get(0).size()];
    int rowIndex = 0;
    int columnIndex = 0;
    for (List<String> row : table) {
      for (String columnData : row) {
        tableArray[rowIndex][columnIndex] = columnData;
        columnIndex++;
      }
      rowIndex++;
      columnIndex = 0;
    }
    printFormattedTable(tableArray, columnsSeparator, rowsSeparator, cellsContentLeftAligned, builder);
  }

  /**
   * Prints a table nicely formatted so that all the data will be aligned.
   *
   * @param table The table of strings that should be printed. Table array is organized as rows*columns, hence the first
   *          index iterates over the rows. It is assumed that the table is not sparse, not containing {@code null}s.
   * @param columnsSeparator A string that will be used as the separator between columns or {@code null} if there should be
   *          none
   * @param rowsSeparator A string that will be used as the separator between rows or {@code null} if there should be
   *          none
   * @param cellsContentLeftAligned {@code true} if the strings in the table should be left aligned or {@code false} if
   *          right aligned
   * @param builder A string builder that will be mutated to contain the printed table
   */
  public static void printFormattedTable (String[][] table, String columnsSeparator, String rowsSeparator,
      boolean cellsContentLeftAligned, StringBuilder builder) {
    if (table.length == 0)
      return;
    String alignmentModifier = cellsContentLeftAligned ? "-" : "";
    int[] maxColumnLength = new int[table[0].length];
    for (int i = 0; i < table.length; i++) {
      for (int j = 0; j < table[i].length; j++) {
        maxColumnLength[j] = Math.max(table[i][j].length(), maxColumnLength[j]);
      }
    }
    int rowLength = 0;
    for (int i = 0; i < maxColumnLength.length; i++) {
      if (maxColumnLength[i] == 0)
        maxColumnLength[i] = 1;
      rowLength = rowLength + maxColumnLength[i];
    }
    if (columnsSeparator == null)
      columnsSeparator = "";
    // add space for the columns separator character
    rowLength = rowLength + (maxColumnLength.length + 1) * (columnsSeparator.length() + 2);
    String verticalLine = null;
    if (rowsSeparator != null && rowsSeparator.length() > 0) {
      verticalLine = StringHelpers.repeatString(rowsSeparator, rowLength / rowsSeparator.length());
      builder.append(verticalLine);
      builder.append("\n");
    }
    for (int i = 0; i < table.length; i++) {
      builder.append(columnsSeparator + " ");
      for (int j = 0; j < table[i].length; j++) {
        builder.append(String.format("%" + alignmentModifier + maxColumnLength[j] + "s", table[i][j]));
        builder.append(" " + columnsSeparator + " ");
      }
      builder.append("\n");
      if (verticalLine != null && verticalLine.length() > 0) {
        builder.append(verticalLine);
        builder.append("\n");
      }
    }
  }

  public static String repeatChar (char character, int times) {
    if (times < 0)
      return "";
    StringBuilder sb = new StringBuilder(times);
    for (int i = 0; i < times; i++) {
      sb.append(character);
    }
    return sb.toString();
  }

  /**
   * Indents the second argument such that every newline starts after the position of the first string. It returns the
   * first string and the indented second string. Here an example usage:<br>
   * {@code System.out.println(indentMultiline("equations: ", "a = 123\nb = 234\nc = 345"));} produces the output:<br>
   * equations: a = 123<br>
   * ____________b = 234<br>
   * ____________c = 345<br>
   *
   * @param indentationString
   * @param multilineString
   * @return
   */
  public static String indentMultiline (String indentationString, String multilineString) {
    int length = indentationString.length();
    // TODO: do not replace the last newline with the indented one
    return indentationString + multilineString.replaceAll("\n", "\n" + repeatChar(' ', length));
  }

  public static String repeatString (String string, int times) {
    if (times < 0)
      return "";
    StringBuilder sb = new StringBuilder(times * string.length());
    for (int i = 0; i < times; i++) {
      sb.append(string);
    }
    return sb.toString();
  }

  public static StringBuilder padWithWitespace (int amountBefore, StringBuilder builder, int amountAfter) {
    String paddingBefore = StringHelpers.repeatChar(' ', amountBefore);
    String paddingAfter = StringHelpers.repeatChar(' ', amountAfter);
    builder.insert(0, paddingBefore);
    builder.append(paddingAfter);
    return builder;
  }

  public static String padWithWitespace (int amountBefore, String string, int amountAfter) {
    String paddingBefore = StringHelpers.repeatChar(' ', amountBefore);
    String paddingAfter = StringHelpers.repeatChar(' ', amountAfter);
    return paddingBefore + string + paddingAfter;
  }

  /**
   * Sorts a collection by using a case insensitive comparison of the string
   * representation of its elements.
   */
  public static <T> SortedSet<T> sortLexically (Iterable<T> variables) {
    SortedSet<T> sorted = new TreeSet<>(new Comparator<T>() {
      @Override public int compare (T o1, T o2) {
        return o1.toString().compareToIgnoreCase(o2.toString());
      }
    });
    for (T variable : variables) {
      sorted.add(variable);
    }
    return sorted;
  }

}
