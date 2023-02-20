package rreil.assembler;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.SortedMap;

import javalx.data.Option;
import javalx.persistentcollections.BiMap;
import rreil.assembler.parser.ASTModule;
import rreil.assembler.parser.ParseException;
import rreil.assembler.parser.RReilParser;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;

public class CompiledAssembler {
  private final SortedMap<RReilAddr, RReil> instructions;
  private final List<PointerInfo> pointers;
  private final BiMap<RReilAddr, String> labels;
  private final int defaultSize;

  CompiledAssembler (SortedMap<RReilAddr, RReil> instructions, List<PointerInfo> pointers,
      BiMap<RReilAddr, String> labels, int defaultSize) {
    this.instructions = instructions;
    this.pointers = pointers;
    this.labels = labels;
    assert defaultSize >= 0;
    this.defaultSize = defaultSize;
  }

  public static CompiledAssembler from (String rreilAssembly) {
    return from(new StringReader(rreilAssembly));
  }

  public static CompiledAssembler from (Reader in) throws UncheckedParseException {
    return compile(new RReilParser(in));
  }

  public static CompiledAssembler from (InputStream in) throws UncheckedParseException {
    return compile(new RReilParser(in));
  }

  private static CompiledAssembler compile (RReilParser parser) {
    try {
      ASTModule module = parser.Module();
      ParseTreeTranslator translator = new ParseTreeTranslator();
      return translator.translate(module);
    } catch (ParseException e) {
      throw new UncheckedParseException(e);
    }
  }

  public SortedMap<RReilAddr, RReil> getInstructions () {
    return instructions;
  }

  public List<PointerInfo> getPointers () {
    return pointers;
  }

  public BiMap<RReilAddr, String> getLabels () {
    return labels;
  }

  public int getDefaultSize () {
    return defaultSize;
  }

  public RReilAddr minInstructionAddress () {
    return instructions.firstKey();
  }

  public RReilAddr maxInstructionAddress () {
    return instructions.lastKey();
  }

  /**
   * Looks for a label named "start" and returns its address.
   * If the label does not exist then the instruction with the lowest addresss is the starting point.
   */
  public RReilAddr entryAddress () {
    Option<RReilAddr> entry = labels.getKey("start");
    if (entry.isSome())
      return entry.get();
    // otherwise just take the first instruction in the list
    return minInstructionAddress();
  }
}
