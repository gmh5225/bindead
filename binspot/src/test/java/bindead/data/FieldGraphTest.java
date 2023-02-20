package bindead.data;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import javalx.numeric.FiniteRange;
import javalx.persistentcollections.tree.OverlappingRanges;

import org.junit.Test;

import bindead.domains.fields.FieldGraph;
import bindead.domains.fields.VariableCtx;

/**
 * Testing field partitionings {@FieldGraph}.
 */
public class FieldGraphTest {
  @Test public void test001 () {
    OverlappingRanges<VariableCtx> overlapping = build();
    FieldGraph fg = FieldGraph.build(overlapping);
    FieldGraph.Partitioning path = fg.findPartitioning(Bound.ZERO, BigInt.of(63));
    assertThat(path.span().get(), is(FiniteRange.of(0, 63)));
  }

  private static OverlappingRanges<VariableCtx> build () {
    OverlappingRanges<VariableCtx> fields = new OverlappingRanges<VariableCtx>();
    fields.add(FiniteRange.of(0, 31), new VariableCtx(32, NumVar.fresh()));
    fields.add(FiniteRange.of(48, 63), new VariableCtx(16, NumVar.fresh()));
    fields.add(FiniteRange.of(32, 63), new VariableCtx(32, NumVar.fresh()));
    fields.add(FiniteRange.of(32, 47), new VariableCtx(16, NumVar.fresh()));
    fields.add(FiniteRange.of(0, 15), new VariableCtx(16, NumVar.fresh()));
    return fields;
  }
}
