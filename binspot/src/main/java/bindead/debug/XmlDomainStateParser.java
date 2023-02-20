package bindead.debug;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import bindead.domainnetwork.interfaces.RootDomain;
import bindead.domains.affine.Affine;
import bindead.domains.congruences.Congruences;
import bindead.domains.fields.Fields;
import bindead.domains.intervals.Intervals;
import bindead.domains.predicates.finite.Predicates;
import bindead.domains.syntacticstripes.Stripes;
import bindead.domains.widening.oldthresholds.ThresholdsWidening;

import com.googlecode.jatl.Html;

/**
 * Parses and visualizes domain states as HTML documents or as a formatted string. Uses the XML string representation of
 * the domain hierarchy to parse and retrieve the known domain properties.
 */
public class XmlDomainStateParser {
  private final RootDomain<?> domain;
  private StateParserResult result;

  public XmlDomainStateParser (RootDomain<?> domain) {
    this.domain = domain;
  }

  private void init () {
    if (result == null)
      result = parse(domain.toXml());
  }

  public RootDomain<?> getDomain () {
    return domain;
  }

  private static StateParserResult parse (String domain) {
    StateParserResult result = null;
    try {
      result = new StateParser().run(domain);
    } catch (XPathExpressionException e) {
    }
    return result;
  }

  /**
   * Render the domain as a string of tables.
   *
   * @return The pretty printed domain values.
   */
  public String renderAsString () {
    return renderAsString(".*");
  }

  /**
   * Render the domain as a string of tables.
   *
   * @param regionsFilter A regular expression that will be used to print only regions that match the expression.
   * @return The pretty printed domain values.
   */
  public String renderAsString (String regionsFilter) {
    return renderAsHtml(regionsFilter);
  }

  /**
   * Render the domain as a string of tables.
   *
   * @param regionsFilter A regular expression that will be used to print only regions that match the expression.
   * @return The pretty printed domain values.
   */
  public String renderAsStringOldBroken (String regionsFilter) {
    init();
    StringBuilder output = new StringBuilder();
    for (Entry<String, Map<String, Table>> category : result.valuesTables.entrySet()) {
      if (category.getValue().isEmpty())
        continue;
      for (Table table : category.getValue().values()) {
        String name = table.getName();
        if (!name.matches(regionsFilter))
          continue;
        output.append(name);
        output.append("\n");
        table.asFormattedString(output, true);
        output.append("\n");
      }
      String separator = StringHelpers.repeatString("=", 80);
      output.append(separator);
      output.append("\n");
      output.append("\n");
    }
    for (Table table : result.auxilliaryTables.values()) {
      output.append(table.getName());
      output.append("\n");
      table.asFormattedString(output, true);
      output.append("\n");
    }
    String separator = StringHelpers.repeatString("*", 80);
    output.append(separator);
    output.append("\n");
    return output.toString();
  }

  /**
   * Render the domain as HTML tables.
   *
   * @return A string of the HTML pretty printed domain.
   */
  public String renderAsHtml () {
    return renderAsHtml(".*");
  }

  /**
   * Render the domain as HTML tables.
   *
   * @param regionsFilter A regular expression that will be used to print only regions that match the expression.
   * @return A string of the HTML pretty printed domain.
   */
  public String renderAsHtml (String regionsFilter) {
    String htmlOutput = XmlToHtml.renderAsHtml(domain.toXml());
    return htmlOutput;
  }

  /**
   * Render the domain as HTML tables.
   *
   * @param regionsFilter A regular expression that will be used to print only regions that match the expression.
   * @return A string of the HTML pretty printed domain.
   */
  public String renderAsHtmlOldBroken (String regionsFilter) {
    init();
    StringWriter output = new StringWriter();
    Html html = new Html(output).html();
    html = html.head().style().attr("type", "text/css");
    html = html.text("body {" +
      "font-size: small; " +
      "font-family: sans serif; " +
      "text-align: left;" +
      "margin: auto 20px auto 20px;" +
      "}");
    html = html.text("table {" +
      "border-width: 0px;" +
      "border-spacing: 0px;" +
      "border-style: solid;" +
      "border-color: black;" +
      "border-collapse: collapse;" +
      "background-color: white;" +
      "}");
    html = html.text("th {" +
      "border-width: 1px;" +
      "padding: 3px;" +
      "border-style: solid;" +
      "border-color: gray;" +
      "background-color: #B8CFE5;" + // Swing's Metal LAF color
      "font-weight: normal;" +
      "white-space:nowrap;" +
      "}");
    html = html.text("td {" +
      "border-width: 1px;" +
      "padding: 3px;" +
      "border-style: solid;" +
      "border-color: gray;" +
      "background-color: white;" +
      "white-space:nowrap;" +
      "}");
    html = html.end(2);
    html = html.body();
    for (Entry<String, Map<String, Table>> category : result.valuesTables.entrySet()) {
      if (category.getValue().isEmpty())
        continue;
      for (Table table : category.getValue().values()) {
        String name = table.getName();
        if (!name.matches(regionsFilter))
          continue;
        html = html.p().b().text(name).end();
        html = table.asHtml(html);
        html = html.end();
      }
      html = html.br().br().hr();
    }
    for (Table table : result.auxilliaryTables.values()) {
      html = html.p().b().text(table.getName()).end();
      html = table.asHtml(html);
      html = html.end();
    }
    html = html.end(2);
    return output.toString();
  }

  private static String removeBraces (String string) {
    return string.substring(1, string.length() - 1);
  }

  private static class StateParser {
    private final XPathFactory factory = XPathFactory.newInstance();
    private final XPath xPath = factory.newXPath();
    private XPathExpression queryRegions;
    private XPathExpression queryNameOfBinding;
    private XPathExpression queryKeyOfBinding;
    private XPathExpression queryValueOfBinding;
    private XPathExpression queryIntervalBindings;
    private XPathExpression queryIntervalDomain;
    private XPathExpression queryNarrowingDomain;
    private XPathExpression queryAffineDomain;
    private XPathExpression queryPointsToDomain;
    private XPathExpression querySymbolicAddresses;
    private XPathExpression queryIntervalValueOfBinding;
    private XPathExpression querySetValueOfBinding;
    private XPathExpression queryPredicatesDomain;
    private XPathExpression queryPredicateValueOfBinding;
    private XPathExpression queryLivenessDomain;
    private XPathExpression queryStripesDomain;
    private XPathExpression queryCongruencesDomain;
    private XPathExpression queryCongruencesValueOfBinding;
    private XPathExpression queryVariableName;

    public StateParser () throws XPathExpressionException {
      setupQueryExpressions();
    }

    private void setupQueryExpressions () throws XPathExpressionException {
      queryNameOfBinding = xPath.compile("Name/text()");
      queryKeyOfBinding = xPath.compile("Key/text()");
      queryVariableName = xPath.compile("Variable/text()");
      queryValueOfBinding = xPath.compile("Value/text()");
      querySetValueOfBinding = xPath.compile("Value/Set/text()");
      queryIntervalValueOfBinding = xPath.compile("Value/Interval/text()");
      queryCongruencesValueOfBinding = xPath.compile("Value/Congruence/text()");
      queryPredicateValueOfBinding = xPath.compile("Value/FiniteTest/text()");
      queryIntervalBindings = xPath.compile("IntervalTree/Binding");

      queryRegions = xPath.compile("//Memory[@name='" + Fields.NAME + "']/REGIONS/Region");
      queryCongruencesDomain = xPath.compile(zeno(Congruences.NAME) + "/Binding");
      queryIntervalDomain = xPath.compile(Intervals.NAME + "/Entry");
// TODO: need to parse the XML of the new points-to domain
//      queryPointsToDomain = xPath.compile(finite(PointsTo.NAME) + "/PointsToBinding/Binding");
//      querySymbolicAddresses = xPath.compile(finite(PointsTo.NAME) + "/SymbolicAddresses/Set");
      queryNarrowingDomain = xPath.compile(zeno(ThresholdsWidening.NAME) + "/Set");
      queryPredicatesDomain = xPath.compile(finite(Predicates.NAME) + "/Binding");
      queryAffineDomain = xPath.compile(zeno(Affine.NAME) + "/Binding/Value/Linear/text()");
      queryStripesDomain = xPath.compile(zeno(Stripes.NAME) + "/Linear/text()");
    }

    private static String zeno (String name) {
      return "//Zeno[@name='" + name + "']";
    }

    private static String finite (String name) {
      return "//Finite[@name='" + name + "']";
    }

    public StateParserResult run (String xml) throws XPathExpressionException {
      StateParserResult result = new StateParserResult();
      NodeList affineTerms = (NodeList) queryAffineDomain.evaluate(createInputSource(xml), XPathConstants.NODESET);
      NodeList stripesTerms = (NodeList) queryStripesDomain.evaluate(createInputSource(xml), XPathConstants.NODESET);
      Map<String, String> pointsTo =
        getPointsToSets((NodeList) queryPointsToDomain.evaluate(createInputSource(xml), XPathConstants.NODESET));
      Map<String, String> congruences =
        getMapping((NodeList) queryCongruencesDomain.evaluate(createInputSource(xml), XPathConstants.NODESET),
            queryCongruencesValueOfBinding);
      Map<String, String> intervals =
        getMapping((NodeList) queryIntervalDomain.evaluate(createInputSource(xml), XPathConstants.NODESET),
            queryIntervalValueOfBinding);
      Map<String, String> predicates =
        getMapping((NodeList) queryPredicatesDomain.evaluate(createInputSource(xml), XPathConstants.NODESET),
            queryPredicateValueOfBinding);
      result =
        createVariableTables(xml, result, intervals, congruences, affineTerms, stripesTerms, pointsTo, predicates);
      result = createAuxilliaryTables(xml, result, intervals, congruences, affineTerms, stripesTerms);
      return result;
    }

    private StateParserResult createVariableTables (String xml, StateParserResult result, Map<String, String> intervals,
        Map<String, String> congruences, NodeList affineTerms, NodeList stripesTerms, Map<String, String> pointsToSets,
        Map<String, String> predicates) throws XPathExpressionException {
      Pattern pattern = Pattern.compile("\\((.*)\\)"); //extract the numerical variable name from inside the parentheses
      NodeList regions = (NodeList) queryRegions.evaluate(createInputSource(xml), XPathConstants.NODESET);
      for (int i = 0; i < regions.getLength(); i++) {
        Node region = regions.item(i);
        String name = queryNameOfBinding.evaluate(region);
        if (name.trim().isEmpty())
          continue;
        NodeList bindings = (NodeList) queryIntervalBindings.evaluate(region, XPathConstants.NODESET);
        if (bindings.getLength() == 0)
          continue;
        Table table = result.createValueTable(name);
        table.addColumn("Name");
        table.addColumn("Range");
        for (int j = 0; j < bindings.getLength(); j++) {
          Node kv = bindings.item(j);
          String key = queryKeyOfBinding.evaluate(kv);
          String value = queryValueOfBinding.evaluate(kv);
          String columnID = value.split(":")[0]; // remove size annotation
          Matcher matcher = pattern.matcher(value);
          matcher.find();
          String variableName = matcher.group(1);
          table.bind(columnID, "Name", variableName);
          table.bind(columnID, "Range", prettyPrintInterval(key));
        }
        fillIntervalsColumn(table, intervals);
        fillCongruencesColumn(table, congruences);
        fillAffineColumn(table, affineTerms);
        fillStripesColumn(table, stripesTerms);
        fillPointsToColumn(table, pointsToSets);
        fillPredicatesColumn(table, predicates);
      }
      return result;
    }

    private StateParserResult createAuxilliaryTables (String xml, StateParserResult result,
        Map<String, String> intervals, Map<String, String> congruences, NodeList affineTerms, NodeList stripesTerms)
        throws XPathExpressionException {
      String symbolicAddresses = (String) querySymbolicAddresses.evaluate(createInputSource(xml), XPathConstants.STRING);
      if (!symbolicAddresses.trim().isEmpty()) {
        symbolicAddresses = removeBraces(symbolicAddresses);
        String[] addresses = symbolicAddresses.split(", ");
        Table symbolicTable = result.createAuxilliaryTable("Symbolic Addresses");
        symbolicTable.addColumn("Name");
        for (String address : addresses) {
          symbolicTable.bind(address, "Name", address);
        }
        fillIntervalsColumn(symbolicTable, intervals);
        fillCongruencesColumn(symbolicTable, congruences);
        fillAffineColumn(symbolicTable, affineTerms);
        fillStripesColumn(symbolicTable, stripesTerms);
      }
      String narrowing = (String) queryNarrowingDomain.evaluate(createInputSource(xml), XPathConstants.STRING);
      if (!narrowing.trim().isEmpty()) {
        narrowing = removeBraces(narrowing);
        String[] predicates = narrowing.split(", ");
        Table narrowingTable = result.createAuxilliaryTable("Narrowing");
        narrowingTable.addColumn("Predicate");
        for (String predicate : predicates) {
          narrowingTable.bind(predicate, "Predicate", predicate);
        }
      }
      String liveVariables = (String) queryLivenessDomain.evaluate(createInputSource(xml), XPathConstants.STRING);
      if (!liveVariables.trim().isEmpty()) {
        Table liveVariablesTable = result.createAuxilliaryTable("Live Variables");
        liveVariablesTable.addColumn("Variables");
        liveVariablesTable.bind("", "Variables", liveVariables);
      }
      return result;
    }

    private Map<String, String> getMapping (NodeList keysAndValues, XPathExpression valueQuery)
        throws XPathExpressionException {
      Map<String, String> mapping = new HashMap<String, String>();
      for (int i = 0; i < keysAndValues.getLength(); i++) {
        Node kv = keysAndValues.item(i);
        String key = queryVariableName.evaluate(kv);
        String value = valueQuery.evaluate(kv);
        mapping.put(key, value);
      }
      return mapping;
    }

    private Map<String, String> getPointsToSets (NodeList pointsToSet) throws XPathExpressionException {
      Map<String, String> pointsToSetsMap = new HashMap<String, String>();
      for (int i = 0; i < pointsToSet.getLength(); i++) {
        Node kv = pointsToSet.item(i);
        String key = queryKeyOfBinding.evaluate(kv);
        String value = removeBraces(querySetValueOfBinding.evaluate(kv));
        pointsToSetsMap.put(key, value);
      }
      return pointsToSetsMap;
    }

    private static void fillValueColumn (Table table, String columnName, Map<String, String> variableValues)
        throws XPathExpressionException {
      table.addColumn(columnName);
      for (String variable : table.getRowIds()) {
        if (variableValues.containsKey(variable))
          table.bind(variable, columnName, variableValues.get(variable));
      }
    }

    private static void fillValueSetColumn (Table table, String columnName, NodeList linearTerms) {
      table.addColumn(columnName);
      for (String variable : table.getRowIds()) {
        for (int i = 0; i < linearTerms.getLength(); i++) {
          String linear = linearTerms.item(i).getNodeValue();
          if (linear.contains(variable))
            table.bindToSet(variable, columnName, linear);
        }
      }
    }

    private static void fillCongruencesColumn (Table table, Map<String, String> congruences)
        throws XPathExpressionException {
      fillValueColumn(table, "Congruence", congruences);
    }

    private static void fillIntervalsColumn (Table table, Map<String, String> intervals) throws XPathExpressionException {
      fillValueColumn(table, "Interval", intervals);
    }

    private static void fillPredicatesColumn (Table table, Map<String, String> predicates)
        throws XPathExpressionException {
      fillValueColumn(table, "Predicate", predicates);
    }

    private static void fillPointsToColumn (Table table, Map<String, String> pointsToSets)
        throws XPathExpressionException {
      fillValueColumn(table, "Points to", pointsToSets);
    }

    private static void fillAffineColumn (Table table, NodeList linearTerms) {
      fillValueSetColumn(table, "Affine", linearTerms);
    }

    private static void fillStripesColumn (Table table, NodeList linearTerms) {
      fillValueSetColumn(table, "Stripes", linearTerms);
    }

    private static InputSource createInputSource (String xml) {
      return new InputSource(new StringReader(xml));
    }

    private static String prettyPrintInterval (String value) {
      value = removeBraces(value);
      value = value.replaceFirst(", ", "-");
      return value;
    }

  }

  private static class StateParserResult {
    private final Map<String, Map<String, Table>> valuesTables = new LinkedHashMap<String, Map<String, Table>>();
    private final Map<String, Table> auxilliaryTables = new LinkedHashMap<String, Table>();
    private static final StringHelpers.ArrayOrderedStringsComparator flagSorter = new RReilFlagNamesSorter();
    private static final StringHelpers.ArrayOrderedStringsComparator registerSorter = new X86RegisterNamesSorter();

    public StateParserResult () {
      TreeMap<String, Table> registers = new TreeMap<String, Table>(registerSorter);
      TreeMap<String, Table> flags = new TreeMap<String, Table>(flagSorter);
      TreeMap<String, Table> temporaries = new TreeMap<String, Table>();
      TreeMap<String, Table> dataSections = new TreeMap<String, Table>();
      TreeMap<String, Table> rest = new TreeMap<String, Table>();
      valuesTables.put("registers", registers);
      valuesTables.put("temporaries", temporaries);
      valuesTables.put("rest", rest);
      valuesTables.put("dataSections", dataSections);
      valuesTables.put("flags", flags);
    }

    public Table createValueTable (String name) {
      Table t = new Table(name);
      if (registerSorter.containsString(name))
        valuesTables.get("registers").put(name, t);
      else if (flagSorter.containsString(name))
        valuesTables.get("flags").put(name, t);
      else if (name.startsWith("t"))
        valuesTables.get("temporaries").put(name, t);
      else if (name.startsWith("."))
        valuesTables.get("dataSections").put(name, t);
      else
        valuesTables.get("rest").put(name, t);
      return t;
    }

    public Table createAuxilliaryTable (String name) {
      Table t = new Table(name);
      auxilliaryTables.put(name, t);
      return t;
    }

    @Override public String toString () {
      return "StateParserResult{" + "valuesTables=" + valuesTables + "auxilliaryTables=" + auxilliaryTables + '}';
    }
  }

  private static class Table {
    private final String name;
    private final List<String> columnNames = new ArrayList<String>();
    private final Map<String, Columns> rows = new LinkedHashMap<String, Columns>();

    public Table (String name) {
      this.name = name;
    }

    public String getName () {
      return name;
    }

    public void addColumn (String name) {
      columnNames.add(name);
    }

    public Columns getRow (String name) {
      Columns columns = rows.get(name);
      if (columns != null)
        return columns;
      columns = new Columns();
      rows.put(name, columns);
      return columns;
    }

    public boolean containsRow (String name) {
      return rows.containsKey(name);
    }

    public Set<String> getRowIds () {
      return rows.keySet();
    }

    public void bind (String row, String column, Object data) {
      getRow(row).bind(column, data);
    }

    public void bindToSet (String row, String column, Object data) {
      getRow(row).bindToSet(column, data);
    }

    public Html asHtml (Html html) {
      html = html.table().tr();
      for (String colName : columnNames) {
        html = html.th().text(colName).end();
      }
      html = html.end();
      for (Columns columns : rows.values()) {
        html = html.tr();
        for (String columnName : columnNames) {
          Object data = columns.get(columnName);
          data = data != null ? data : "";
          html = html.td().text(data.toString()).end();
        }
        html = html.end();
      }
      return html.end();
    }

    public StringBuilder asFormattedString (StringBuilder builder) {
      return asFormattedString(builder, false);
    }

    public StringBuilder asFormattedString (StringBuilder builder, boolean cellsContentLeftAligned) {
      String[][] table = new String[rows.keySet().size() + 1][columnNames.size()];
      int rowIndex = 1;
      int columnIndex = 0;
      for (String columnName : columnNames) {
        table[0][columnIndex] = columnName;
        columnIndex++;
      }
      columnIndex = 0;
      for (Columns columns : rows.values()) {
        for (String columnName : columnNames) {
          Object data = columns.get(columnName);
          data = data != null ? data : "";
          table[rowIndex][columnIndex] = data.toString();
          columnIndex++;
        }
        rowIndex++;
        columnIndex = 0;
      }
      StringHelpers.printFormattedTable(table, "\u2016", "–", cellsContentLeftAligned, builder);
      return builder;
    }

    @Override public String toString () {
      return asFormattedString(new StringBuilder(), true).toString();
    }
  }

  private static class Columns {
    private final Map<String, Object> columns = new HashMap<String, Object>();

    public void bind (String name, Object o) {
      columns.put(name, o);
    }

    public void bindToSet (String name, Object o) {
      @SuppressWarnings("unchecked")
      Set<Object> values = (Set<Object>) columns.get(name);
      if (values == null) {
        values = new HashSet<Object>() {
          @Override public String toString () {
            return removeBraces(super.toString());
          }
        };
      }
      values.add(o);
      columns.put(name, values);
    }

    public Object get (String column) {
      return columns.get(column);
    }

    @Override public String toString () {
      return "Columns{" + "columns=" + columns + '}';
    }
  }

  /**
   * Sorts the registers of the x86 architecture for 64bit and lower.
   *
   * @author Bogdan Mihaila
   */
  private static class X86RegisterNamesSorter extends StringHelpers.ArrayOrderedStringsComparator {
    private static final String[] registers = {
      "ah", "al", "ax", "eax", "rax",
      "bh", "bl", "bx", "ebx", "rbx",
      "ch", "cl", "cx", "ecx", "rcx",
      "dh", "dl", "dx", "edx", "rdx",
      "spl", "sp", "esp", "rsp",
      "bpl", "bp", "ebp", "rbp",
      "sil", "si", "esi", "rsi",
      "dil", "di", "edi", "rdi",
      "r8b", "r8w", "r8d", "r8",
      "r9b", "r9w", "r9d", "r9",
      "r10b", "r10w", "r10d", "r10",
      "r11b", "r11w", "r11d", "r11",
      "r12b", "r12w", "r12d", "r12",
      "r13b", "r13w", "r13d", "r13",
      "r14b", "r14w", "r14d", "r14",
      "r15b", "r15w", "r15d", "r15",
      "ip", "eip", "rip",
      "cs", "ds", "es", "fs", "gs", "ss"};

    public X86RegisterNamesSorter () {
      super(registers);
    }
  }

  private static class RReilFlagNamesSorter extends StringHelpers.ArrayOrderedStringsComparator {
    private static final String[] flags = {"CF", "BE", "LT", "LE", "ZF", "SF", "OF"};

    public RReilFlagNamesSorter () {
      super(flags);
    }
  }
}
