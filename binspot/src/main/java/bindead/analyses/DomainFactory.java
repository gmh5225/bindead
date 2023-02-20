package bindead.analyses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import bindead.analyses.AnalysisFactory.DomainHierarchyFactory;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.domainnetwork.interfaces.ZenoDomain;
import bindead.domains.affine.Affine;
import bindead.domains.affine.RedundantAffine;
import bindead.domains.apron.ApronIntervals;
import bindead.domains.apron.ApronOctagons;
import bindead.domains.apron.ApronPolyhedra;
import bindead.domains.congruences.Congruences;
import bindead.domains.fields.Fields;
import bindead.domains.fields.FieldsDisjunction;
import bindead.domains.finiteDisjunction.FiniteDisjunction;
import bindead.domains.finitesupportset.FiniteSupportSet;
import bindead.domains.gauge.Gauge;
import bindead.domains.intervals.IntervalSets;
import bindead.domains.intervals.Intervals;
import bindead.domains.phased.Phased;
import bindead.domains.pointsto.PointsTo;
import bindead.domains.root.Root;
import bindead.domains.sat.Sat;
import bindead.domains.segments.SegMem;
import bindead.domains.segments.basics.Segment;
import bindead.domains.segments.heap.HeapSegment;
import bindead.domains.segments.machine.DataSegments;
import bindead.domains.segments.machine.NullSegment;
import bindead.domains.segments.machine.Processor;
import bindead.domains.segments.machine.StackSegment;
import bindead.domains.segments.machine.ThreadLocalStorage;
import bindead.domains.syntacticstripes.Stripes;
import bindead.domains.undef.Undef;
import bindead.domains.widening.delayed.DelayedWidening;
import bindead.domains.widening.delayed.DelayedWideningWithThresholds;
import bindead.domains.widening.thresholds.ThresholdsWidening;
import bindead.domains.wrapping.Wrapping;

/**
 * A convenience factory to build domain stacks.
 */
public class DomainFactory {
  // allowed separators to delimit domains in a domain hierarchy string
  private final static List<String> domainSeparators = Arrays.asList("/", ":", "!", ">", "\\|", " ");
  private final static String disableDomainToken = "-";
  private final static Map<String, DomainBuilder> domainBuilders = new HashMap<String, DomainBuilder>();
  static {
    registerDomainBuilders();
    SegmentFactory.registerSegmentBuilders();
  }

  /**
   * Parses a string of domains separated by one of "/:!>| ".
   * If you want to not include a domain then prefix it with "-". Here an example domain hierarchy specification:<br>
   * "Root Fields -Predicate PointsTo Wrapping -Narrowing Affine Intervals"
   *
   * @param domainHierarchy A string of domain hierarchies.
   * @return A builder to be passed to the analysis that instantiates the domains in the given order.
   */
  static DomainHierarchyFactory parseFactory (String domainHierarchy) {
    final List<String> domains = tokenize(domainHierarchy.trim());
    if (domains.isEmpty())
      throw new IllegalArgumentException("Could not parse the domain hierarchy string: " + domainHierarchy);
    final Object cachedDomain = DomainFactory.buildNext(domains);
    return new DomainHierarchyFactory() {
      @SuppressWarnings("unchecked") @Override public <D extends RootDomain<D>> D build () {
        return (D) cachedDomain;
      }
    };
  }

  /**
   * Parses a string of domains separated by one of "/:!>| " to a finite domain.
   * If you want to not include a domain then prefix it with "-". Here an example domain hierarchy specification:<br>
   * "Wrapping Affine Intervals -IntervalSet"
   *
   * @param domainHierarchy A string of domain hierarchies.
   * @return The instantiated finite domain hierarchy in the given order.
   */
  @SuppressWarnings({"rawtypes"}) public static FiniteDomain parseFiniteDomain (String domainHierarchy) {
    final List<String> domains = tokenize(domainHierarchy);
    if (domains.isEmpty())
      throw new IllegalArgumentException("Could not parse the domain hierarchy string: " + domainHierarchy);
    return (FiniteDomain) DomainFactory.buildNext(domains);
  }

  /**
   * Parses a string of domains separated by one of "/:!>| " to a zeno domain.
   * If you want to not include a domain then prefix it with "-". Here an example domain hierarchy specification:<br>
   * "Affine Intervals -IntervalSet"
   *
   * @param domainHierarchy A string of domain hierarchies.
   * @return The instantiated finite domain hierarchy in the given order.
   */
  @SuppressWarnings({"rawtypes"}) public static ZenoDomain parseZenoDomain (String domainHierarchy) {
    final List<String> domains = tokenize(domainHierarchy);
    if (domains.isEmpty())
      throw new IllegalArgumentException("Could not parse the domain hierarchy string: " + domainHierarchy);
    return (ZenoDomain) DomainFactory.buildNext(domains);
  }

  @SuppressWarnings({"rawtypes", "unchecked"}) private static void registerDomainBuilders () {
    addBuilder(Root.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new Root((MemoryDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(SegMem.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        Segment[] segArray = SegmentFactory.buildSegments(childHierarchy);
        return new SegMem((MemoryDomain) buildNext(childHierarchy), segArray);
      }
    });
    addBuilder(Fields.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new Fields((FiniteDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(FieldsDisjunction.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new FieldsDisjunction((MemoryDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(bindead.domains.predicates.finite.Predicates.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new bindead.domains.predicates.finite.Predicates((FiniteDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(bindead.domains.predicates.zeno.Predicates.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new bindead.domains.predicates.zeno.Predicates((ZenoDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(Undef.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        FiniteDomain buildNext = (FiniteDomain) buildNext(childHierarchy);
        return new Undef(buildNext);
      }
    });
    addBuilder(PointsTo.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new PointsTo((FiniteDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(FiniteSupportSet.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new FiniteSupportSet((FiniteDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(FiniteDisjunction.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new FiniteDisjunction((FiniteDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(Sat.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new Sat((FiniteDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(Wrapping.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new Wrapping((ZenoDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(RedundantAffine.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new RedundantAffine((ZenoDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(ThresholdsWidening.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new ThresholdsWidening((ZenoDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(Phased.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new Phased((ZenoDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(DelayedWidening.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new DelayedWidening((ZenoDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(DelayedWideningWithThresholds.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new DelayedWideningWithThresholds((ZenoDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(Stripes.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new Stripes((ZenoDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(Congruences.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new Congruences((ZenoDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(Intervals.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new Intervals();
      }
    });
    addBuilder(Gauge.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new Gauge((ZenoDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(IntervalSets.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new IntervalSets();
      }
    });
    addBuilder(ApronIntervals.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new ApronIntervals();
      }
    });
    addBuilder(ApronOctagons.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new ApronOctagons();
      }
    });
    addBuilder(ApronPolyhedra.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new ApronPolyhedra();
      }
    });

    // domains that are superseded but still around for reference and useful in some special cases
    addBuilder(Affine.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new Affine((ZenoDomain) buildNext(childHierarchy));
      }
    });
    addBuilder(bindead.domains.widening.oldthresholds.ThresholdsWidening.NAME, new DomainBuilder() {
      @Override public Object instantiate (List<String> childHierarchy) {
        return new bindead.domains.widening.oldthresholds.ThresholdsWidening((ZenoDomain) buildNext(childHierarchy));
      }
    });
  }

  private static Object buildNext (List<String> domains) {
    if (domains.isEmpty())
      throw new IllegalArgumentException("You did not specify the right domain for the bottom of the hierarchy." +
        " It still needs a child.");
    String nextDomain = domains.remove(0);
    DomainBuilder builder = getDomainBuilder(nextDomain);
    if (builder == null)
      throw new IllegalArgumentException("Could not find a builder for domain: " + nextDomain);
    return builder.instantiate(domains);
  }

  private static void addBuilder (String name, DomainBuilder domainBuilder) {
    domainBuilders.put(name.toLowerCase(), domainBuilder);
  }

  private static DomainBuilder getDomainBuilder (String domainName) {
    return domainBuilders.get(domainName);
  }

  private static List<String> tokenize (String domainHierarchy) {
    domainHierarchy = domainHierarchy.toLowerCase();
    for (String separator : domainSeparators) {
      String[] domainsArray = domainHierarchy.split(separator);
      if (domainsArray.length > 1) {
        List<String> domains = new LinkedList<String>();
        for (String ds : domainsArray)
          if (!ds.startsWith(disableDomainToken))
            domains.add(ds);
        return domains;
      }
    }
    throw new IllegalArgumentException("cannot parse domain string: " + domainHierarchy);
  }

  private static class SegmentFactory {

    final private static Map<String, DomainFactory.SegmentBuilder> segmentBuilders =
      new HashMap<String, DomainFactory.SegmentBuilder>();

    @SuppressWarnings("rawtypes") private static Segment[] buildSegments (List<String> childHierarchy) {
      LinkedList<Segment> segs = new LinkedList<Segment>();
      while (!childHierarchy.isEmpty() && isSegment(childHierarchy.get(0))) {
        String domainName = childHierarchy.remove(0);
        segs.add(segmentBuilders.get(domainName).instantiate());
      }
      Segment[] segArray = new Segment[0];
      segArray = segs.toArray(segArray);
      return segArray;
    }

    private static boolean isSegment (String segName) {
      return segmentBuilders.containsKey(segName);
    }

    private static void addSegmentBuilder (String name, DomainFactory.SegmentBuilder segmentBuilder) {
      segmentBuilders.put(name.toLowerCase(), segmentBuilder);
    }

    @SuppressWarnings({"rawtypes"}) private static void registerSegmentBuilders () {
      DomainFactory.SegmentFactory.addSegmentBuilder(DataSegments.NAME, new DomainFactory.SegmentBuilder() {
        @Override public Segment instantiate () {
          return new DataSegments();
        }
      });
      DomainFactory.SegmentFactory.addSegmentBuilder(NullSegment.NAME, new DomainFactory.SegmentBuilder() {
        @Override public Segment instantiate () {
          return new NullSegment();
        }
      });
      DomainFactory.SegmentFactory.addSegmentBuilder(HeapSegment.NAME, new DomainFactory.SegmentBuilder() {
        @Override public Segment instantiate () {
          return new HeapSegment();
        }
      });
      DomainFactory.SegmentFactory.addSegmentBuilder(Processor.NAME, new DomainFactory.SegmentBuilder() {
        @Override public Segment instantiate () {
          return new Processor();
        }
      });
      DomainFactory.SegmentFactory.addSegmentBuilder(StackSegment.NAME, new DomainFactory.SegmentBuilder() {
        @Override public Segment instantiate () {
          return new StackSegment();
        }
      });
      DomainFactory.SegmentFactory.addSegmentBuilder(ThreadLocalStorage.NAME, new DomainFactory.SegmentBuilder() {
        @Override public Segment instantiate () {
          return new ThreadLocalStorage();
        }
      });
    }

  }

  private static interface DomainBuilder {
    public Object instantiate (List<String> childHierarchy);
  }

  private static interface SegmentBuilder {
    @SuppressWarnings("rawtypes") public Segment instantiate ();
  }
}
