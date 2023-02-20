package binparse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.numeric.FiniteRange;
import javalx.persistentcollections.tree.FiniteRangeTree;
import javalx.persistentcollections.tree.OverlappingRanges;

/**
 * Implements common functionality for the various binary formats.
 */
public abstract class AbstractBinary implements Binary {
  private final static Logger logger = Logger.getLogger(AbstractBinary.class.getName());
  public final static String x86_32 = "x86-32";
  public final static String x86_64 = "x86-64";
  protected List<Symbol> importedSymbols;
  protected List<Symbol> exportedSymbols;
  protected List<Segment> segments;
  protected FiniteRangeTree<Segment> segmentAddresses = FiniteRangeTree.<Segment>empty();
  protected final Map<String, Segment> segmentNames = new HashMap<String, Segment>();
  protected final Map<Long, Symbol> symbolAddresses = new HashMap<Long, Symbol>();
  protected final Map<String, Symbol> symbolNames = new HashMap<String, Symbol>();

  @Override public List<Segment> getSegments () {
    if (segments == null)
      initSegments();
    return this.segments;
  }

  @Override public Option<Segment> getSegment (String name) {
    if (segments == null)
      initSegments();
    return Option.fromNullable(segmentNames.get(name));
  }

  @Override public Option<Symbol> getSymbol (String name) {
    if (exportedSymbols == null)
      initExportedSymbols();
    if (importedSymbols == null)
      initImportedSymbols();
    return Option.fromNullable(symbolNames.get(name));
  }

  @Override public Option<Symbol> getSymbol (long address) {
    if (exportedSymbols == null)
      initExportedSymbols();
    if (importedSymbols == null)
      initImportedSymbols();
    return Option.fromNullable(symbolAddresses.get(address));
  }

  @Override public Segment getSegment (Symbol symbol) {
    return getSegment(symbol.getAddress()).getOrNull();
  }

  @Override public Option<Segment> getSegment (long address) {
    if (segments == null)
      initSegments();
    OverlappingRanges<Segment> overlappings = segmentAddresses.searchOverlaps(FiniteRange.of(address));
    if (overlappings.isEmpty()) {
      return Option.none();
    } else if (overlappings.size() != 1) {
      // XXX: ugly hack for broken or not yet linked binaries where some comment and string sections are marked as
      // starting at zero and the text section, too. Here we assume that mostly one wants the text section when there are
      // overlaps.
      for (P2<FiniteRange, Segment> section : overlappings) {
        if (section._2().getNameOrAddress().equals(".text")) {
          logger.log(Level.WARNING, "The binary \"" + getFileName() +
            "\" contains more than one section for the address: " + Long.toHexString(address) + ". Sections: " +
            overlappings + "\nThe .text section was returned for the address.");
          return Option.some(section._2());
        }
      }
      throw new IllegalStateException("The binary \"" + getFileName() +
        "\" contains more than one section for the address: " + Long.toHexString(address) + ". Sections: " +
        overlappings);
    } else {
      return Option.some(overlappings.getFirst()._2());
    }
  }

  @Override public List<Symbol> getExportedSymbols () {
    if (exportedSymbols == null)
      initExportedSymbols();
    return exportedSymbols;
  }

  @Override public List<Symbol> getImportedSymbols () {
    if (importedSymbols == null)
      initImportedSymbols();
    return importedSymbols;
  }

  /**
   * Retrieve and initialize the segments from the raw binary. Called only once when accessing segments for the first
   * time
   */
  protected abstract void initSegments ();

  /**
   * Retrieve and initialize the exported symbols from the raw binary. Called only once when accessing symbols for the
   * first time.
   */
  protected abstract void initExportedSymbols ();

  /**
   * Retrieve and initialize the imported symbols from the raw binary. Called only once when accessing symbols for the
   * first time
   */
  protected abstract void initImportedSymbols ();

}
