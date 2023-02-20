package binparse.trace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import javalx.data.products.P3;
import javalx.numeric.BigInt;
import binparse.Binary;
import binparse.Segment;
import binparse.UncheckedIOException;

/**
 * The main interface to the results of a trace.
 */
public class TraceDump {
  private final String traceDumpPrefix;
  private final static String traceSuffixInfo = ".info";
  private final static String traceSuffixSegments = ".segments";
  private final static String traceSuffixRegisters = ".registers";
  private final static String traceSuffixStack = ".stack";
  private final static String traceSuffixHeap = ".heap";
  private final static String traceSuffixFlow = ".flow";
  private final static String traceSuffixDebug = ".debug";
  private final Binary tracedBinary;

  public TraceDump (String tracesPrefix, Binary tracedBinary) {
    this.traceDumpPrefix = tracesPrefix;
    this.tracedBinary = tracedBinary;
  }

  public Binary getTracedBinary () {
    return tracedBinary;
  }

  public long getTraceStartAddress () {
    return TraceParser.parseInfo(traceDumpPrefix + traceSuffixInfo).getStartAddress();
  }

  public long getTraceEndAddress () {
    return TraceParser.parseInfo(traceDumpPrefix + traceSuffixInfo).getStopAddress();
  }

  public List<Segment> getSegments () {
    List<Segment> segments = TraceParser.parseModule(traceDumpPrefix + traceSuffixSegments);
    // TODO: a hack to avoid TLS segments that alias with other segments.
    removeUnwantedSegmentsHack(segments);
    if (segments == null)
      segments = new ArrayList<Segment>();
    return segments;
  }

  private static void removeUnwantedSegmentsHack (List<Segment> segments) {
    for (Iterator<Segment> iterator = segments.iterator(); iterator.hasNext();) {
      Segment segment = iterator.next();
      if (segment.getNameOrAddress().equals(".tbss") || segment.getNameOrAddress().equals(".tdata"))
        iterator.remove();
    }
  }

  public Segment getStack () {
    return TraceParser.parseSegment(traceDumpPrefix + traceSuffixStack);
  }

  public Segment getHeap () {
    try {
      return TraceParser.parseSegment(traceDumpPrefix + traceSuffixHeap);
    } catch (UncheckedIOException e) {
      return null;
    }
  }

  public Map<String, BigInt> getRegisters () {
    return TraceParser.parseRegisters(traceDumpPrefix + traceSuffixRegisters);
  }

  public List<P3<Long, Long, Boolean>> getControlFlow () {
    return TraceParser.parseControlFlow(traceDumpPrefix + traceSuffixFlow);
  }

  public String getControlFlowDebugOutput () {
    return TraceParser.parseControlFlowDebug(traceDumpPrefix + traceSuffixDebug);
  }

}
