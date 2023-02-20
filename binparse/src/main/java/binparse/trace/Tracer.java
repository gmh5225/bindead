package binparse.trace;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.numeric.FiniteRange;
import javalx.persistentcollections.tree.FiniteRangeTree;
import javalx.persistentcollections.tree.OverlappingRanges;
import binparse.Binary;
import binparse.Permission;
import binparse.Segment;
import binparse.Symbol;

/**
 * Utility class to produce and deal with traces.
 */
public class Tracer {

  /**
   * Linux only at the moment (also needs bash)!
   * Executes and traces a program and writes the output to a file for later analysis.
   *
   * @param tracerScript The path to the script file that invokes the tracer
   * @param tracedCommand The executable with its parameters that will be traced
   * @param outputFile The file to write the trace output to
   * @param tracerOptions Switches for the tracer (invoke the tracer with --help for a list of available switches)
   * @return The file name of the trace output
   * @throws IOException if any operation on the necessary files did not succeed
   */
  public static String trace (String tracerScript, String tracedCommand, String outputFile, String... tracerOptions)
      throws IOException {
    if (!fileExists(tracerScript))
      throw new FileNotFoundException("The tracer invocation script: \"" + tracerScript + "\" does not exist.");
    if (outputFile == null || outputFile.isEmpty())
      outputFile = System.getProperty("user.home") + "/tracedump";
    List<String> commands = new ArrayList<String>();
    commands.add("bash");
    commands.add(tracerScript);
    commands.add("--nocompress");
    commands.add("-o ");
    commands.add(outputFile);
    for (String option : tracerOptions) {
      commands.add(option);
    }
    commands.add(tracedCommand);
    Process tracerCall = new ProcessBuilder(commands).start();
    try {
      tracerCall.waitFor();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    String callNormalOutput = getStreamOutput(tracerCall.getInputStream());
    String callErrorOutput = getStreamOutput(tracerCall.getErrorStream());
    if (!callNormalOutput.isEmpty())
      System.out.println("Output:\n" + callNormalOutput);
    if (!callErrorOutput.isEmpty())
      System.err.println("Errors:\n" + callErrorOutput);
    return outputFile;
  }

  public static boolean fileExists (String filePath) {
    return new File(filePath).exists();
  }

  public static void setExecutable (String filePath) {
    new File(filePath).setExecutable(true);
  }

  private static String getStreamOutput (InputStream stream) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
    StringBuilder output = new StringBuilder();
    String line = null;
    while ((line = reader.readLine()) != null)
      output.append(line + "\n");
    reader.close();
    stream.close();
    return output.toString();
  }

  /**
   * Writes a segment to a file to be able to look at it.
   */
  public static void dumpSegment (Segment segment, String fileName) {
    if (fileName == null || fileName.isEmpty())
      fileName = System.getProperty("user.home", "") + "/" + segment.getNameOrAddress() + ".dump";
    try {
      BufferedOutputStream file = new BufferedOutputStream(new FileOutputStream(fileName));
      file.write(segment.getData());
      file.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Dumps the control flow to the console, i.e. the locations and the targets of the control flow changes. If verbose
   * is chosen then it annotates for each address in what symbol, segment and file it relies.
   */
  public static void dumpControlFlow (TraceDump traces, boolean verbose) {
    if (verbose)
      dumpControlFlowVerbose(traces);
    else
      dumpControlFlow(traces);
  }

  /**
   * Dumps the control flow to the console, i.e. the locations and the targets of the control flow changes.
   *
   * @see #dumpControlFlowVerbose(TraceBinary)
   */
  private static void dumpControlFlow (TraceDump traces) {
    for (P3<Long, Long, Boolean> flow : traces.getControlFlow()) {
      String condition = flow._3() ? "t" : "f";
      System.out.println(Long.toHexString(flow._1()) + " -" + condition + "> " + Long.toHexString(flow._2()));
    }
  }

  /**
   * Dumps the control flow to the console and annotates for each address in what segment it relies. If the trace
   * segments were prefixed with the filenames then it is possible to view control transfer between files and symbols.
   */
  private static void dumpControlFlowVerbose (final TraceDump trace) {
    String lastLocationName = null;
    SegmentSymbolResolver resolver = new SegmentSymbolResolver() {
      FiniteRangeTree<Symbol> symbolAddresses = inferSymbolRanges(trace.getTracedBinary());

      @Override public Option<Symbol> getSymbol (long address) {
        OverlappingRanges<Symbol> overlaps = symbolAddresses.searchOverlaps(FiniteRange.of(address));
        if (overlaps.isEmpty())
          return Option.none();
        if (overlaps.size() == 1)
          return Option.some(overlaps.getFirst()._2());
        else
          throw new IllegalStateException("More than one symbol found for address: " + address);
      }

      @Override public Option<Segment> getSegment (long address) {
        return trace.getTracedBinary().getSegment(address);
      }
    };
    for (P3<Long, Long, Boolean> flow : trace.getControlFlow()) {
      String fromLocationName = formatLocationName(resolver, flow._1());
      if (!fromLocationName.equals(lastLocationName))
        System.out.println(" " + fromLocationName);
      String toLocationName = formatLocationName(resolver, flow._2());
      String fromAddress = Long.toHexString(flow._1());
      String toAddress = Long.toHexString(flow._2());
      String condition = flow._3() ? "t" : "f";
      String toLocationChanged = "";
      if (!toLocationName.equals(fromLocationName))
        toLocationChanged = "     " + toLocationName;
      System.out.println(fromAddress + " -" + condition + "> " + toAddress + toLocationChanged);
      lastLocationName = toLocationName;
    }
  }

  private static FiniteRangeTree<Symbol> inferSymbolRanges (Binary binary) {
    FiniteRangeTree<Symbol> symbolAddresses = FiniteRangeTree.<Symbol>empty();
    symbolAddresses = setSymbolStartAddresses(symbolAddresses, binary.getImportedSymbols());
    symbolAddresses = setSymbolStartAddresses(symbolAddresses, binary.getExportedSymbols());

    FiniteRangeTree<Symbol> symbolRanges = FiniteRangeTree.<Symbol>empty();
    Iterator<P2<FiniteRange, Symbol>> iter = symbolAddresses.iterator();
    P2<FiniteRange, Symbol> cachedNextSymbol = null;
    while (iter.hasNext() || cachedNextSymbol != null) {
      P2<FiniteRange, Symbol> symbol = cachedNextSymbol != null ? cachedNextSymbol : iter.next();
      long startAddress;
      long endAddress;
      if (iter.hasNext()) {
        P2<FiniteRange, Symbol> successorSymbol = iter.next();
        startAddress = symbol._1().low().asInteger().longValue();
        endAddress = successorSymbol._1().low().asInteger().longValue() - 1;
        cachedNextSymbol = successorSymbol;
      } else {
        cachedNextSymbol = null;
        startAddress = symbol._1().low().asInteger().longValue();
        endAddress = Math.max(startAddress, getHighestExecutableAddress(binary));
      }
      symbolRanges = symbolRanges.bind(FiniteRange.of(startAddress, endAddress), symbol._2());
    }
    return symbolRanges;
  }

  private static long getHighestExecutableAddress (Binary binary) {
    long maxAddress = 0;
    for (Segment segment : binary.getSegments()) {
      if (Permission.isExecutable(segment.getPermissions()) && segment.getAddress() > maxAddress)
        maxAddress = segment.getAddress();
    }
    return maxAddress;
  }

  private static FiniteRangeTree<Symbol> setSymbolStartAddresses (FiniteRangeTree<Symbol> symbolRanges, List<Symbol> symbols) {
    for (Symbol symbol : symbols) {
      symbolRanges = symbolRanges.bind(FiniteRange.of(symbol.getAddress()), symbol);
    }
    return symbolRanges;
  }

  private static String formatLocationName (SegmentSymbolResolver resolver, Long address) {
    String symbol = formatSymbolName(resolver.getSymbol(address));
    String segment = formatSegmentName(resolver.getSegment(address));
    return symbol + "          " + segment;
  }

  private static String formatSymbolName (Option<Symbol> symbol) {
    if (symbol.isNone())
      return "(unknown symbol)";
    else
      return symbol.get().getNameOrAddress();
  }

  private static String formatSegmentName (Option<Segment> segment) {
    if (segment.isNone())
      return "(unknown segment)";
    else
      return segment.get().toString() + "       " + segment.get().getFileName().getOrElse("");
  }

  private static interface SegmentSymbolResolver {
    public Option<Symbol> getSymbol (long address);

    public Option<Segment> getSegment (long address);
  }

}
