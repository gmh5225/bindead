package bindead.analyses;

import static javalx.data.products.Tuple2.tuple2;

import bindead.analysis.util.BackwardEvaluator;
import bindead.analysis.util.WideningPointCollector;
import bindead.domainnetwork.channels.Message;
import bindead.domainnetwork.combinators.RootDomainProxy;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.domainnetwork.interfaces.SegmentCtx;
import bindead.exceptions.DomainStateException;
import bindead.exceptions.Unreachable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javalx.data.CollectionHelpers;
import javalx.data.Interval;
import javalx.data.Option;
import javalx.data.products.P2;
import javalx.digraph.Digraph.Edge;
import javalx.digraph.Digraph.Vertex;
import javalx.fn.Predicate;
import rreil.abstractsyntax.RReilAddr;
import rreil.cfa.Cfa;
import rreil.cfa.CfaEdgeTypeVisitor;

public class BackwardAnalysis<D extends RootDomain<D>> implements Analysis<D> {
  private final HashMap<Vertex, D> states = new HashMap<Vertex, D>();
  private final HashMap<Vertex, List<Message>> warnings = new HashMap<Vertex, List<Message>>();
  private final Map<RReilAddr, Vertex> addresses = new HashMap<RReilAddr, Vertex>();
  private final BackwardEvaluator<D> evaluator;
  private RootDomainProxy<D> proxy;
  private final WideningPointCollector wideningPointCollector;
  private final Cfa cfa;
  private Vertex initialVertex;
  private final Predicate<Edge> nonNullStateAtDependency = new Predicate<Edge>() {
    @Override public Boolean apply (Edge e) {
      Vertex v = e.getTarget();
      return states.get(v) != null;
    }
  };

  public BackwardAnalysis (Cfa cfa, D initial) {
    this.cfa = cfa;
    this.proxy = new RootDomainProxy<D>(initial);
    this.evaluator = new BackwardEvaluator<D>();
    wideningPointCollector = new WideningPointCollector(cfa);
    wideningPointCollector.run();
  }

  @Override public Cfa getCfa () {
    return cfa;
  }

  @Override public Option<D> getState (Vertex v) {
    return Option.fromNullable(states.get(v));
  }

  @Override public Option<D> getState (RReilAddr address) {
    Vertex v = addresses.get(address);
    if (v == null)
      return Option.none();
    return getState(v);
  }

  @Override public Map<Vertex, List<Message>> getWarnings () {
    return warnings;
  }

  @Override public void setStartVertex (Vertex v) {
    initialVertex = v;
    proxy.begin();
    putState(v, proxy.commit().get());
  }

  @Override public void setInitialState (D initial) {
    this.proxy = new RootDomainProxy<D>(initial);
  }

  @Override public void bootstrapState (Vertex v, Bootstrap bootstrapEval, SegmentCtx... segments) {
    proxy.begin();
    bootstrapEval.bootstrap(proxy);
    for (SegmentCtx segment : segments) {
      proxy.introduceSegmentCtx(segment);
    }
    putState(v, proxy.commit().get());
  }

  @Override public Vertex getStartVertex () {
    return initialVertex;
  }

  @Override public Map<Vertex, D> getStates () {
    return states;
  }

  @Override public Iterable<Vertex> solve (Vertex v) {
    LinkedList<D> currentStates = new LinkedList<D>();
    Collection<Edge> dependencies = CollectionHelpers.filter(v.outgoing(), nonNullStateAtDependency);

    for (Edge e : dependencies) {
      D incoming = states.get(e.getTarget());
      try {
        currentStates.add(cfa.getTransitionType(e).accept(evaluator, e, incoming));
      } catch (Unreachable _) {
        incoming.unrollUnreachable();
      }
    }

    if (!currentStates.isEmpty()) {
      D oldState = states.get(v);
      D currentState = merge(currentStates);

      if (oldState == null) {
        putState(v, currentState);
        return influences(v);
      } else {
        if (isWideningPoint(v))
          currentState = oldState.widen(currentState);
        else
          currentState = oldState.join(currentState);

        if (!currentState.subsetOrEqual(oldState)) {
          putState(v, currentState);
          return influences(v);
        }
      }
    }

    return Collections.<Vertex>emptyList();
  }

  private void putState (Vertex v, D state) {
    Option<RReilAddr> addr = cfa.getAddress(v);
    if (addr.isSome())
      addresses.put(addr.get(), v);
    states.put(v, state);
  }

  private D merge (List<D> states) {
    Iterator<D> it = states.iterator();
    D current = it.next();
    while (it.hasNext()) {
      current = current.join(it.next());
    }
    return current;
  }

  private boolean isWideningPoint (Vertex v) {
    return wideningPointCollector.isWideningPoint(v);
  }

  @Override public Iterable<P2<Vertex, Edge>> dependencies (final Vertex v) {
    return new Iterable<P2<Vertex, Edge>>() {
      @Override public Iterator<P2<Vertex, Edge>> iterator () {
        final Iterator<Edge> it = v.outgoing().iterator();
        return new Iterator<P2<Vertex, Edge>>() {
          @Override public boolean hasNext () {
            return it.hasNext();
          }

          @Override public P2<Vertex, Edge> next () {
            final Edge e = it.next();
            return tuple2(e.getTarget(), e);
          }

          @Override public void remove () {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  @Override public Iterable<Vertex> influences (Vertex v) {
    return v.predecessors();
  }

  @Override public void dumpState (RReilAddr address) {
    Option<D> stateOption = getState(address);
    if (stateOption.isNone())
      return;
    D state = stateOption.get();
    System.out.println(state);
  }

  @Override public Option<Interval> query (RReilAddr address, String name, int size, int offset) {
    throw new DomainStateException(DomainStateException.ErrObj.UNIMPLEMENTED);
  }

  @Override public CfaEdgeTypeVisitor<D, D> getEvaluator () {
    return evaluator;
  }
}
