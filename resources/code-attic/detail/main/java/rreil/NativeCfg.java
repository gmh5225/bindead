package rreil;

import rreil.NativeCfgBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javalx.data.Option;
import javalx.digraph.Digraph;
import rreil.cfa.util.GmlWriter;
import binspot.asm.NativeInstruction;

/**
 * @author Bogdan Mihaila
 */
public class NativeCfg extends Digraph implements GmlWriter.VertexLabelRenderer, GmlWriter.EdgeLabelRenderer {
  private final Map<Vertex, List<NativeInstruction>> blocks = new HashMap<Vertex, List<NativeInstruction>>();
  private final Vertex entryVertex = createVertex();

  public NativeCfg () {
    super();
  }

  public Vertex getEntry () {
    return entryVertex;
  }

  public List<NativeInstruction> getBlockInstructions (Vertex v) {
    return blocks.get(v);
  }

  public void setBlockInstructions (Vertex v, List<NativeInstruction> instructions) {
    blocks.put(v, instructions);
  }

  public Option<Long> getAddress (Vertex v) {
    List<NativeInstruction> instructions = blocks.get(v);
    if (instructions == null || instructions.isEmpty())
      return Option.none();
    return Option.some(instructions.get(0).address());
  }

  @Override public String labelOfEdge (Edge e) {
    return "";
  }

  @Override public String labelOfVertex (Vertex v) {
    StringBuilder builder = new StringBuilder();
    List<NativeInstruction> instructions = blocks.get(v);
    if (instructions == null || instructions.isEmpty())
      return "<empty>";
    int maxOpcodeLength = 0;
    for (NativeInstruction insn : instructions) {
      StringBuilder opcodeString = new StringBuilder();
      insn.opcode(opcodeString);
      maxOpcodeLength = Math.max(maxOpcodeLength, opcodeString.length());
    }
    for (NativeInstruction insn : instructions) {
      insn.address(builder);
      builder.append(": ");
      StringBuilder opcodeString = new StringBuilder();
      insn.opcode(opcodeString);
      builder.append(String.format("%-" + maxOpcodeLength + "s: ", opcodeString.toString()));
      insn.asString(builder);
      builder.append("\n");
    }
    builder.deleteCharAt(builder.length() - 1); // remove the last newline character
    return builder.toString();
  }

  @Override protected String asString (Vertex v) {
    return labelOfVertex(v);
  }

  @Override protected String asString (Edge e) {
    return labelOfEdge(e);
  }

  /**
   * @return A new CFG that is equal to this but transformed to group consecutive instructions as basic blocks.
   */
  public NativeCfg withBasicBlocks () {
    return NativeCfgBuilder.mkBasicBlocks(this);
  }
}
