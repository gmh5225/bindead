package cli;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javalx.data.Option;
import rreil.lang.RReilAddr;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.AnalysisFactory.AnalysisDebugHooks;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.analyses.algorithms.CallStringAnalysis;
import bindead.debug.DebugHelper;
import bindead.debug.StringHelpers;
import binparse.Binary;
import binparse.BinaryFileFormat;
import binparse.Segment;
import binparse.Symbol;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

/**
 * The main class to use the analyzer from the command line.
 *
 * @author Bogdan Mihaila
 */
public class Bindead {
  public static final String defaultDomainHierarchy =
    "SegMem Processor Stack -Null Data -Heap Fields "
      + "-Undef Predicates(F) -SupportSet PointsTo -Disjunction Wrapping "
      + "DelayedWidening -DelayedWidening(Thresholds) -Phased ThresholdsWidening "
      + "-Predicates(Z) RedundantAffine -Affine Congruences Intervals -IntervalSets "
      + "-Apron(Polyhedra) -Apron(Octagons) -Apron(Intervals) ";

  @Parameters(separators = "=")
  private static class ClOptions {
    @Parameter(description = "file")
    private final List<String> files = new ArrayList<String>();

    @Parameter(names = {"-h", "--help"}, help = true, description = "Print the usage description of the tool.")
    private boolean help = false;

    @Parameter(names = {"-s", "--start-point"}, description = "Choose the start point of the disassembly/analysis."
      + " Can be either the name of a symbol or an address in hexadecimal with prefix \"0x\"")
    private String startPoint;

    @Parameter(names = {"-d", "--disassemble"},
        description = "Disassemble but do not analyze the binary. Performs a recursive descent disassembly, "
          + "following jump targets that can statically be resolved, that is, without a value analysis.")
    private boolean disassemble = false;

    @Parameter(names = {"-a", "--analyze"},
        description = "Disassemble and analyze the binary. Performs a recursive descent disassembly, "
          + "following jump targets inferred by the value analysis.")
    private boolean analyze = false;

    @Parameter(names = {"-ss", "--analysis-summary"}, description = "Print a summary of the analysis results.")
    private boolean analysisSummary = true;

    @Parameter(names = {"-sr", "--show-rreil-code"}, description = "Print the disassembled RREIL code.")
    private boolean printRReilCode = false;

    @Parameter(names = {"-sn", "--show-native-code"}, description = "Print the disassembled native code.")
    private boolean printNativeCode = true;

    @Parameter(names = {"-sw", "--show-warnings"}, description = "Print the warning messages in detail.")
    private boolean printWarnings = false;


    @Parameter(names = {"-i", "--file-info"}, description = "Display info about the binary.")
    private boolean printFileInfo = false;

    @Parameter(names = {"-sym", "--show-symbols"}, description = "Display the symbols in the binary.")
    private boolean printSymbols = false;

    @Parameter(names = {"-dyn", "--show-dynamic-symbols"}, description = "Display the dynamic symbols in the binary.")
    private boolean printDynamicSymbols = false;

    @Parameter(names = {"-sec", "--show-sections"}, description = "Display the sections in the binary.")
    private boolean printSections = false;


    @Parameter(names = {"-df", "--diss-frontend"}, description = "Choose the disassembler frontend. Can be the "
      + "\"builtin\" Java disassembler or the newer GDSL disassembler (requires native libraries).",
        validateWith = DisFrontendValidator.class)
    private String disFrontend = "builtin";

    public static class DisFrontendValidator implements IParameterValidator {
      @Override public void validate (String name, String value) throws ParameterException {
        if (!value.equals("gdsl") && !value.equals("builtin"))
          throw new ParameterException("Parameter " + name + " must be one of {builtin, gdsl} but was " + value + ".");
      }
    }

    @Parameter(names = {"-or", "--optimize-rreil"}, description = "Optimize the RREIL code produced during the"
      + "disassembly process (requires the GDSL frontend). Optimizations to RREIL will result in less RREIL code"
      + " that needs to be analyzed.")
    private boolean optimizeRREIL = false;

    // FIXME: summarization should be merged into a call-string analysis with parameter 0
    @Parameter(names = {"-ai", "--analyzer-interprocedural"}, description = "Choose the interprocedural analyzer mode. "
      + "Can be either \"callstring\" or \"summarization\".",
        validateWith = InterproceduralAnalyzerValidator.class)
    private String interprocAnalyzer = "callstring";

    public static class InterproceduralAnalyzerValidator implements IParameterValidator {
      @Override public void validate (String name, String value) throws ParameterException {
        if (!value.equals("callstring") && !value.equals("sumarization"))
          throw new ParameterException("Parameter " + name + " must be one of {callstring, sumarization} but was "
            + value + ".");
      }
    }

    @Parameter(names = {"-cs", "--callstring-length"},
        description = "Maximum length of the callstring in interprocedural analyses.",
        validateWith = PositiveIntegerValidator.class)
    private Integer callstringLength = 5;

    public static class PositiveIntegerValidator implements IParameterValidator {
      @Override public void validate (String name, String value) throws ParameterException {
        int n = Integer.parseInt(value);
        if (n < 0)
          throw new ParameterException("Parameter " + name + " should be positive (found " + value + ")");
      }
    }

    @Parameter(names = {"--debug"}, description = "Show debug output of the disassembler/analyzer.\n"
      + "The output levels are:\n"
      + "0:\t none\n"
      + "1:\t print processed instruction\n"
      + "2:\t print (a compact) analysis state at each instruction\n"
      + "3:\t print (a full) analysis state at each instruction\n"
      + "4:\t print (a full) analysis state (with internal variable ids) at each instruction",
        validateWith = PositiveIntegerValidator.class)
    private Integer debug = 0;

    @Parameter(names = {"-pad", "--print-default-abstract-domains"},
        description = "Print the default abstract domain hierarchy used by the analyzer. Note that a '-' in "
          + "front of a domain name means that the domain is disabled and 'Apron' domains require the native library.")
    private boolean printDefaultDomains = false;

    @Parameter(names = {"-ad", "--abstract-domains"},
        description = "A list of abstract domains to be used in the analysis "
          + "(see --print-default-abstract-domains for a list of available domains). "
          + "Note that a '-' in front of a domain name means that the domain is disabled.",
          variableArity=true)
    private List<String> abstractDomains;

    // TODO:
    // * use stubs for a list of functions (this might require a file to provide the RREIL stub code or
    //          options to choose from predefined stubs (e.g. return top)
  }

  private static Comparator<? super ParameterDescription> getParameterDeclarationOrderComparator (
      Class<?> parameterDeclaration) {
    final Map<Parameter, Integer> indexNumber = new HashMap<>();
    int i = 0;
    for (Field field : parameterDeclaration.getDeclaredFields()) {
      // assumes that the fields in the class are
      // iterated in the order they are declared in the class.
      // This is however not necessary but works at least on Java 7.
      Parameter parameterAnnotation = field.getAnnotation(Parameter.class);
      if (parameterAnnotation != null) {
        indexNumber.put(parameterAnnotation, i);
        i++;
      }
    }

    Comparator<? super ParameterDescription> comparator = new Comparator<ParameterDescription>() {
      @Override public int compare (ParameterDescription p0, ParameterDescription p1) {
        if (indexNumber.containsKey(p0.getParameter().getParameter())
          && indexNumber.containsKey(p1.getParameter().getParameter())) {
          Integer p0Index = indexNumber.get(p0.getParameter().getParameter());
          Integer p1Index = indexNumber.get(p1.getParameter().getParameter());
          return p0Index.compareTo(p1Index);
        }
        return p0.getLongestName().compareTo(p1.getLongestName());
      }
    };
    return comparator;
  }


  public static void main (String[] args) {
    ClOptions options = new ClOptions();
    JCommander parser = new JCommander();
    parser.addObject(options);
    parser.setProgramName("bindead");
    parser.setColumnSize(Integer.MAX_VALUE);
    parser.setParameterDescriptionComparator(getParameterDeclarationOrderComparator(options.getClass()));
    String usageMessage = formatUsageMessage(parser);
    if (args.length == 0) {
      System.err.println(usageMessage);
      System.exit(1);
    }
    try {
      parser.parse(args);
    } catch (ParameterException e) {
      System.err.println("Error parsing command line:\n" + e);
      System.err.println(usageMessage);
    }
    if (options.help) {
      System.out.println(usageMessage);
      System.exit(0);
    }
    process(usageMessage, options);
  }

  private static String formatUsageMessage (JCommander parser) {
    StringBuilder builder = new StringBuilder();
    parser.usage(builder);
    return builder.toString();
  }

  private static void process (String usageMessage, ClOptions options) {
    if (options.files.isEmpty()) {
      System.err.println("No file specified.");
      System.err.println(usageMessage);
      System.exit(1);
    }
    if (options.files.size() > 1) {
      System.err.println("Specified more than one file:\n" + options.files);
      System.err.println(usageMessage);
      System.exit(1);
    }
    for (String fileName : options.files) {
      Binary file = null;
      try {
        file = BinaryFileFormat.getBinary(fileName);
      } catch (IOException e) {
        System.err.println("Error reading file:\n" + fileName + "\n" + e);
        System.exit(1);
      }
      if (file == null) {
        System.err.println("Error reading file:\n" + fileName);
        System.exit(1);
      }
      processFile(file, usageMessage, options);
    }
  }

  private static void processFile (Binary file, String usageMessage, ClOptions options) {
    if (options.printDefaultDomains)
      printDefaultDomains();
    if (options.printFileInfo)
      printFileInfo(file);
    if (options.printSections)
      printSections(file);
    if (options.printSymbols)
      printSymbols(file);
    if (options.printDynamicSymbols)
      printDynamicSymbols(file);

    if (options.disassemble) {
      disassemble(file, options);
    } else if (options.analyze) {
      analyze(file, options);
    }
    System.exit(0);
  }

  private static void printDefaultDomains () {
    System.out.println(defaultDomainHierarchy);
    System.exit(0);
  }


  private static void printSections (Binary file) {
    System.out.println("Sections:");
    List<List<String>> table = new ArrayList<>();
    ArrayList<String> header = new ArrayList<>();
    header.add("Address");
    header.add("Name");
    header.add("Size");
    header.add("Permissions");
    table.add(header);
    for (Segment segment : file.getSegments()) {
      if (segment.getName().isSome()) {
        ArrayList<String> row = new ArrayList<>();
        row.add(toHex(segment.getAddress()));
        if (segment.getName().isSome())
          row.add(segment.getName().get());
        else
          row.add("<no name>");
        row.add(Long.toString(segment.getSize()));
        row.add(segment.getPermissions().toString());
        table.add(row);
      }
    }
    StringBuilder builder = new StringBuilder();
    StringHelpers.printFormattedTable(table, null, null, true, builder);
    System.out.println(builder.toString());
    System.out.println();
  }

  private static void printSymbols (Binary file) {
    System.out.println("Symbols:");
    // TODO: implement a more detailed printer for ELF, PE symbols
    List<List<String>> table = new ArrayList<>();
    ArrayList<String> header = new ArrayList<>();
    header.add("Address");
    header.add("Name");
    header.add("Size");
    table.add(header);
    for (Symbol symbol : file.getExportedSymbols()) {
      if (symbol.getName().isSome()) {
        ArrayList<String> row = new ArrayList<>();
        row.add(toHex(symbol.getAddress()));
        row.add(symbol.getName().get());
        row.add(Long.toString(symbol.getSize()));
        table.add(row);
      }
    }
    StringBuilder builder = new StringBuilder();
    StringHelpers.printFormattedTable(table, null, null, true, builder);
    System.out.println(builder.toString());
    System.out.println();
  }

  private static void printDynamicSymbols (Binary file) {
    System.out.println("Dynamic Symbols:");
    // TODO: implement a more detailed printer for ELF, PE symbols
    List<List<String>> table = new ArrayList<>();
    ArrayList<String> header = new ArrayList<>();
    header.add("Address");
    header.add("Name");
    header.add("Size");
    table.add(header);
    for (Symbol symbol : file.getImportedSymbols()) {
      if (symbol.getName().isSome()) {
        ArrayList<String> row = new ArrayList<>();
        row.add(toHex(symbol.getAddress()));
        row.add(symbol.getName().get());
        row.add(Long.toString(symbol.getSize()));
        table.add(row);
      }
    }
    StringBuilder builder = new StringBuilder();
    StringHelpers.printFormattedTable(table, null, null, true, builder);
    System.out.println(builder.toString());
    System.out.println();
  }

  private static String toHex (long address) {
    return RReilAddr.valueOf(address).toShortStringWithHexPrefix();
  }

  private static void printFileInfo (Binary file) {
    System.out.println("Architecture name: " + file.getArchitectureName());
    System.out.println("Architecture size: " + file.getArchitectureSize() + " bit");
    System.out.println("Endianness: " + file.getEndianness());
    System.out.println("Format: " + file.getFileFormat());
    System.out.println("Type: " + file.getType());
    System.out.println("Entry point: " + toHex(file.getEntryAddress()));
    Option<Symbol> main = file.getMainFunction();
    if (main.isSome())
      System.out.println("Main function: " + main.get().getNameOrAddress() + " @ " + toHex(main.get().getAddress()));
    System.out.println("Debug Info: " + file.hasDebugInformation());
    System.out.println();
  }

  @SuppressWarnings("rawtypes") private static void analyze (Binary file, ClOptions options) {
    commonSetup(options);
    AnalysisDebugHooks debugger = null;
    if (options.debug >= 2) {
      debugger = DebugHelper.printers.domainDump();
      DebugHelper.analysisKnobs.printMemVarOnly();
      DebugHelper.analysisKnobs.printCompactDomain();
    }
    if (options.debug >= 3) {
      DebugHelper.analysisKnobs.printFullDomain();
    }
    if (options.debug >= 4) {
      DebugHelper.analysisKnobs.printMemVarAndNumVar();
    }
    if (options.analysisSummary)
      DebugHelper.analysisKnobs.printSummary();

    String domainHierarchy = defaultDomainHierarchy;
    if (options.abstractDomains != null && !options.abstractDomains.isEmpty()) {
      domainHierarchy = "";
      for (String domain : options.abstractDomains) {
        domainHierarchy = domainHierarchy + " " + domain;
      }
    }
    AnalysisFactory factory = new AnalysisFactory(domainHierarchy);
    Analysis<?> analyzer;
    if (options.interprocAnalyzer.toLowerCase().equals("callstring"))
      analyzer = factory.getCallstringAnalysis(file);
    else if (options.interprocAnalyzer.toLowerCase().equals("summarization"))
      analyzer = factory.getFixpointAnalysis(file);
    else
      throw new ParameterException("Unimplemented functionality for parameter:" + options.interprocAnalyzer);

    analyzer.setDebugHooks(debugger);
    RReilAddr startAddress = getStartPoint(file, options);
    if (analyzer instanceof CallStringAnalysis)
      ((CallStringAnalysis) analyzer).runFrom(startAddress, options.callstringLength);
    else
      analyzer.runFrom(startAddress);
  }

  private static void disassemble (Binary file, ClOptions options) {
    commonSetup(options);
    // be more relaxed about errors when only disassembling the code without analyzing it
    AnalysisProperties.INSTANCE.skipDisassembleErrors.setValue(true);
    AnalysisProperties.INSTANCE.ignoreNonExistentJumpTargets.setValue(true);

    // the analysis environment is actually not used in the disassembler below but
    // might still be of use later on when dealing with the results
    AnalysisFactory factory = new AnalysisFactory(defaultDomainHierarchy);
    Analysis<?> disassembler = factory.getRecursiveDisassembler(file);
    RReilAddr startAddress = getStartPoint(file, options);
    disassembler.runFrom(startAddress);
  }

  private static RReilAddr getStartPoint (Binary file, ClOptions options) {
    RReilAddr startAddress;
    if (options.startPoint != null && !options.startPoint.isEmpty()) {
      Option<Symbol> symbol = file.getSymbol(options.startPoint);
      if (symbol.isSome()) {
        return RReilAddr.valueOf(symbol.get().getAddress());
      } else if (options.startPoint.startsWith("0x")) {
        try {
          Long.parseLong(options.startPoint.substring(2), 16);
        } catch (NumberFormatException e) {
          throw new ParameterException("Could not find or parse startpoint: " + options.startPoint);
        }
      } else {
        throw new ParameterException("Could not find or parse startpoint: " + options.startPoint);
      }
    }
    startAddress = AnalysisFactory.getStartAddress(file);
    return startAddress;
  }

  private static void commonSetup (ClOptions options) {
    if (options.debug >= 1)
      DebugHelper.analysisKnobs.printInstructions();
    if (options.disFrontend.toLowerCase().equals("gdsl")) {
      AnalysisProperties.INSTANCE.useGDSLDisassembler.setValue(true);
      if (options.optimizeRREIL)
        AnalysisProperties.INSTANCE.disassembleBlockWise.setValue(true);
    } else if (options.disFrontend.toLowerCase().equals("builtin"))
      AnalysisProperties.INSTANCE.useGDSLDisassembler.setValue(false);
    else
      throw new ParameterException("Unimplemented functionality for parameter:" + options.disFrontend);

    if (options.printRReilCode)
      DebugHelper.analysisKnobs.printRReilCodeListing();
    if (options.printNativeCode)
      DebugHelper.analysisKnobs.printNativeCodeListing();
    if (options.printWarnings)
      DebugHelper.analysisKnobs.printWarnings();
  }

}
