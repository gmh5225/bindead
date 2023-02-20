package rreil.assembler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestsHelper {

  /**
   * Append newlines after each argument.
   */
  public static String lines (String... lines) {
    StringBuilder builder = new StringBuilder();
    for (String line : lines) {
      builder.append(line + "\n");
    }
    return builder.toString();
  }

  /**
   * Remove any assertion from an assembler program and add newlines after each argument.
   * @see #lines(String...)
   * @see #removeAssertions(String...)
   */
  public static String linesWithoutAssertions (String... lines) {
    return lines(removeAssertions(lines));
  }

  /**
   * Remove the assertions from an assembler program given as one statement per line.
   * @param lines The assembler program
   * @return The same assembler program without the assertion statements
   */
  public static String[] removeAssertions (String... lines) {
    List<String> originalArray = Arrays.asList(lines);
    List<String> resultArray = new ArrayList<String>();
    for (String string : originalArray) {
      if (!string.startsWith("assert"))
        resultArray.add(string);
    }
    return resultArray.toArray(new String[]{});
  }
}
