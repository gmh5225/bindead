package bindead.analyses;

import static javalx.data.products.Tuple2.tuple2;

import bindead.analysis.util.ForwardEvaluator;
import bindead.analysis.util.WideningPointCollector;
import bindead.domainnetwork.channels.SynthChannel;
import bindead.domainnetwork.channels.Message;
import bindead.domainnetwork.combinators.RootDomainProxy;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.domainnetwork.interfaces.SegmentCtx;
import bindead.exceptions.Unreachable;
import bindead.platforms.Platforms.AnalysisPlatform;
import java.util.ArrayList;
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
import javalx.data.products.Tuple2;
import javalx.digraph.Digraph.Edge;
import javalx.digraph.Digraph.Vertex;
import javalx.fn.Predicate;
import rreil.abstractsyntax.Field;
import rreil.abstractsyntax.RReilAddr;
import rreil.abstractsyntax.util.RootVariables;
import rreil.cfa.Cfa;
import rreil.cfa.Cfa.CfaExtensionListener;
import rreil.cfa.Cfa.TransitionType;
import rreil.cfa.CfaEdgeTypeVisitor;
import rreil.disassembly.DisassemblyProvider;

public class ForwardAnalysis<D extends RootDomain<D>> implements Analysis<D> {
  private final AnalysisPlatform platform;
  private final Map<Vertex, D> states = new HashMap<Vertex, D>();
  private final Map<Vertex, List<Message>> warnings = new HashMap<Vertex, List<Message>>();
  private final Map<RReilAddr, Vertex> addresses = new HashMap<RReilAddr, Vertex>();
  private final ForwardEvaluator<D> evaluator;
  private RootDomainProxy<D> proxy;
  private final WideningPointCollector wideningPointCollector;
  private final Cfa cfa;
  private Vertex initialVertex;
  private final Predicate<Edge> nonNullStateAtDependency = new Predicate<Edge>() {
    @Override public Boolean apply (Edge e) {
      Vertex v = e.getSource();
      return states.get(v) != null;
    }
  };

  public ForwardAnalysis (AnalysisPlatform platform, Cfa cfa, D initial) {
    this(platform, cfa, null, initial);
  }

  public ForwardAnalysis (AnalysisPlatform platform, Cfa cfa, DisassemblyProvider provider, D initial) {
    this(platform, new ForwardEvaluator<D>(cfa, provider), initial);
  }

  public ForwardAnalysis (AnalysisPlatform platform, ForwardEvaluator<D> evaluator, D initial) {
    this.platform = platform;
    this.cfa = evaluator.getCfa();
    this.proxy = new RootDomainProxy<D>(initial);
    this.evaluator = evaluator;
    wideningPointCollector = new WideningPointCollector(cfa);
    wideningPointCollector.run();
    evaluator.getCfa().addExtensionListener(new CfaExtensionListener() {
      @Override public void cfaExtended (Cfa cfa) {
        wideningPointCollector.run();
      }
    });
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

  @Override public void setInitialState (D initial) {
    this.proxy = new RootDomainProxy<D>(initial);
  }

  @Override public void setStartVertex (Vertex v) {
    initialVertex = v;
    putState(v, proxy.get(), proxy.querySynth());
  }

  @Override public Vertex getStartVertex () {
    return initialVertex;
  }

  @Override public void bootstrapState (Vertex v, Bootstrap bootstrapEval, SegmentCtx... segments) {
    proxy.begin();
    bootstrapEval.bootstrap(proxy);
    for (SegmentCtx segment : segments) {
      proxy.introduceSegmentCtx(segment);
    }
    RootDomainProxy<D> state = proxy.commit();
    putState(v, state.get(), state.querySynth());
  }

  @Override public Map<Vertex, D> getStates () {
    return states;
  }

  @Override public Iterable<Vertex> solve (Vertex v) {
    LinkedList<D> currentStates = new LinkedList<D>();
    Collection<Edge> dependencies = CollectionHelpers.filter(v.incoming(), nonNullStateAtDependency);

    for (Edge e : dependencies) {
      D incoming = states.get(e.getSource());
      D state = applyTransferFunction(e, incoming);
      if (state != null)
        currentStates.add(state);
    }

    if (!currentStates.isEmpty()) {
      D oldState = states.get(v);
      P2<D, SynthChannel> currentStateAndSynth = merge(currentStates);
      D currentState = currentStateAndSynth._1();
      SynthChannel synth = currentStateAndSynth._2();
      if (oldState == null) {
        putState(v, currentState, synth);
        return influences(v);
      } else if (oldState != currentState && !currentState.subsetOrEqual(oldState)) {
        if (currentStates.size() > 1 && isWideningPoint(v))
          currentState = oldState.widen(currentState);
        putState(v, currentState, synth);
        return influences(v);
      }
    }

    return Collections.<Vertex>emptyList();
  }

  private void putState (Vertex v, D state, SynthChannel synth) {
    Option<RReilAddr> addr = cfa.getAddress(v);
    if (addr.isSome())
      addresses.put(addr.get(), v);
    warnings.put(v, new ArrayList<Message>(synth.getWarnings()));
    states.put(v, state);
  }

  private P2<D, SynthChannel> merge (List<D> states) {
    Iterator<D> it = states.iterator();
    D current = it.next();
    SynthChannel channel = current.querySynth();
    while (it.hasNext()) {
      D next = it.next();
      channel.update(next.querySynth());
      current = current.join(next);
    }
    return Tuple2.tuple2(current, channel);
  }

  private boolean isWideningPoint (Vertex v) {
    return wideningPointCollector.isWideningPoint(v);
  }

  @Override public Iterable<P2<Vertex, Edge>> dependencies (final Vertex v) {
    return new Iterable<P2<Vertex, Edge>>() {
      @Override public Iterator<P2<Vertex, Edge>> iterator () {
        final Iterator<Edge> it = v.incoming().iterator();

        return new Iterator<P2<Vertex, Edge>>() {
          @Override public boolean hasNext () {
            return it.hasNext();
          }

          @Override public P2<Vertex, Edge> next () {
            final Edge e = it.next();
            return tuple2(e.getSource(), e);
          }

          @Override public void remove () {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  @Override public Iterable<Vertex> influences (Vertex v) {
    return v.successors();
  }

  public AnalysisPlatform getPlatform () {
    return platform;
  }

  private D applyTransferFunction (Edge e, D incoming) {
    D state = null;
    TransitionType transition = cfa.getTransitionType(e);
    if (transition instanceof Cfa.CfaTest) {
      try {
        state = transition.accept(evaluator, e, incoming);
      } catch (Unreachable _) {
        incoming.unrollUnreachable();
      }
    } else {
      state = transition.accept(evaluator, e, incoming);
    }
    return state;
  }

  @Override public void dumpState (RReilAddr address) {
    Option<D> stateOption = getState(address);
    if (stateOption.isNone())
      return;
    D state = stateOption.get();
    System.out.println(state);
  }

  @Override public Option<Interval> query (RReilAddr address, String name, int size, int offset) {
    Option<D> stateOption = getState(address);
    if (stateOption.isNone())
      return Option.none();
    D state = stateOption.get();
    return Option.some(state.queryRange(new Field(size, offset, RootVariables.getIdOrNull(name))).convexHull());
  }

  @Override public CfaEdgeTypeVisitor<D, D> getEvaluator () {
    return evaluator;
  }
}
