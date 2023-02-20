package bindead.analyses;

import bindead.analysis.util.CallString;
import bindead.domainnetwork.channels.Message;
import bindead.domainnetwork.interfaces.RootDomain;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javalx.data.products.P3;
import javalx.data.products.Tuple3;
import javalx.digraph.Digraph.Vertex;

/**
 * Contains the contexts for an analysis.
 */
public class AnalysisCtx<D extends RootDomain<D>> implements Map<CallString, Analysis<D>> {
  private final Map<CallString, Analysis<D>> map;
  private final static Comparator<CallString> lengthComparator = new Comparator<CallString>() {
    @Override public int compare (CallString o1, CallString o2) {
      return o1.size() == o2.size() ? 0 : o1.size() < o2.size() ? 1 : -1;
    }
  };

  public AnalysisCtx () {
    this(new HashMap<CallString, Analysis<D>>());
  }

  private AnalysisCtx (Map<CallString, Analysis<D>> map) {
    this.map = map;
  }

  @Override public int size () {
    return map.size();
  }

  @Override public boolean isEmpty () {
    return map.isEmpty();
  }

  @Override public boolean containsKey (Object key) {
    return map.containsKey(key);
  }

  @Override public boolean containsValue (Object value) {
    return map.containsValue(value);
  }

  @Override public Analysis<D> get (Object key) {
    return map.get(key);
  }

  @Override public Analysis<D> put (CallString key, Analysis<D> value) {
    return map.put(key, value);
  }

  @Override public Analysis<D> remove (Object key) {
    return map.remove(key);
  }

  @Override public void putAll (Map<? extends CallString, ? extends Analysis<D>> m) {
    map.putAll(m);
  }

  @Override public void clear () {
    map.clear();
  }

  @Override public Set<CallString> keySet () {
    return map.keySet();
  }

  @Override public Collection<Analysis<D>> values () {
    return map.values();
  }

  @Override public Set<Entry<CallString, Analysis<D>>> entrySet () {
    return map.entrySet();
  }

  public List<P3<CallString, Vertex, Message>> getWarnings () {
    List<P3<CallString, Vertex, Message>> flatWarnings = new ArrayList<P3<CallString, Vertex, Message>>();
    for (Entry<CallString, Analysis<D>> entry : this.entrySet()) {
      CallString callString = entry.getKey();
      Analysis<D> analysis = entry.getValue();
      for (Entry<Vertex, List<Message>> warningsEntry : analysis.getWarnings().entrySet()) {
        Vertex v = warningsEntry.getKey();
        for (Message warning : warningsEntry.getValue()) {
          flatWarnings.add(Tuple3.tuple3(callString, v, warning));
        }
      }
    }
    return flatWarnings;
  }

  public void dumpWarnings (PrintStream out) {
    for (Entry<CallString, Analysis<D>> entry : this.entrySet()) {
      CallString callString = entry.getKey();
      Analysis<D> analysis = entry.getValue();
      out.println("call-context: " + callString);
      for (Entry<Vertex, List<Message>> warningsEntry : analysis.getWarnings().entrySet()) {
        Vertex v = warningsEntry.getKey();
        List<Message> warnings = warningsEntry.getValue();
        if (warnings.isEmpty())
          continue;
        out.println("  Vertex: " + v);
        for (Message warning : warnings) {
          out.println("    " + warning.message());
        }
      }
    }
  }

  public void dumpWarnings (PrintWriter out) {
    for (Entry<CallString, Analysis<D>> entry : this.entrySet()) {
      CallString callString = entry.getKey();
      Analysis<D> analysis = entry.getValue();
      out.println("call-context: " + callString);
      for (Entry<Vertex, List<Message>> warningsEntry : analysis.getWarnings().entrySet()) {
        Vertex v = warningsEntry.getKey();
        List<Message> warnings = warningsEntry.getValue();
        if (warnings.isEmpty())
          continue;
        out.println("  Vertex: " + v);
        for (Message warning : warnings) {
          out.println("    " + warning.message());
        }
      }
    }
  }

  /**
   * Returns this context but sorted according to the call-string length.
   */
  public AnalysisCtx<D> sorted () {
    Map<CallString, Analysis<D>> analyses = new TreeMap<CallString, Analysis<D>>(lengthComparator);
    for (Entry<CallString, Analysis<D>> entry : entrySet()) {
      analyses.put(entry.getKey(), entry.getValue());
    }
    return new AnalysisCtx<D>(analyses);
  }

  @Override public boolean equals (Object obj) {
    return map.equals(obj);
  }

  @Override public int hashCode () {
    return map.hashCode();
  }

  @Override public String toString () {
    return map.toString();
  }
}
