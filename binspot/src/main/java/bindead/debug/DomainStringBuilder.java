package bindead.debug;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A string builder that can filter the output of domains by their name.
 *
 * @author Bogdan Mihaila
 */
public class DomainStringBuilder {
  private final Map<String, String> elements = new LinkedHashMap<String, String>();

  public void append (String elementName, String elementValue) {
    elements.put(elementName, elementValue);
  }

  public String toFilteredString (String... elementsToShow) {
    Set<String> elementsSet = new HashSet<String>(Arrays.asList(elementsToShow));
    return toFilteredString(elementsSet);
  }

  public String toFilteredString (Set<String> elementsToShow) {
    StringBuilder builder = new StringBuilder();
    for (Entry<String, String> element : elements.entrySet()) {
      if (elementsToShow.contains(element.getKey()))
        builder.append(element.getValue());
    }
    return builder.toString();
  }

  @Override public String toString () {
    return toFilteredString();
  }
}
