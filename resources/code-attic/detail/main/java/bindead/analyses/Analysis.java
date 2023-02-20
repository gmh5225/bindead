package bindead.analyses;

import java.util.Map;

import javalx.data.Interval;
import javalx.data.Option;
import javalx.data.products.P2;
import javalx.digraph.Digraph.Edge;
import javalx.digraph.Digraph.Vertex;
import rreil.abstractsyntax.RReilAddr;
import rreil.cfa.Cfa;
import bindead.domainnetwork.channels.Message;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.domainnetwork.interfaces.SegmentCtx;
import java.util.List;
import rreil.cfa.CfaEdgeTypeVisitor;

public interface Analysis<D extends RootDomain<D>> {
  public Option<D> getState (Vertex v);

  public Option<D> getState (RReilAddr address);

  public Map<Vertex, D> getStates ();

  public Map<Vertex, List<Message>> getWarnings ();

  public Cfa getCfa ();

  public void setStartVertex (Vertex v);

  public Vertex getStartVertex ();

  public void setInitialState (D initial);

  public void bootstrapState (Vertex v, Bootstrap bootstrap, SegmentCtx... segments);

  public Iterable<Vertex> solve (Vertex v);

  public Iterable<P2<Vertex, Edge>> dependencies (Vertex v);

  public Iterable<Vertex> influences (Vertex v);

  public void dumpState (RReilAddr addr);

  public Option<Interval> query (RReilAddr address, String name, int size, int offset);

  public CfaEdgeTypeVisitor<D, D> getEvaluator();
}
