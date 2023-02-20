package bindead.analyses;

import bindead.domains.liveness.FieldLiveness;
import bindead.domains.liveness.LivenessFinitePart;
import bindead.domains.liveness.LivenessRootPart;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.domainnetwork.combinators.RootDomainProxy;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.exceptions.DomainStateException;
import javalx.data.BigInt;
import org.junit.Test;
import rreil.abstractsyntax.BinOp;
import rreil.abstractsyntax.Field;
import rreil.abstractsyntax.RReil;
import rreil.abstractsyntax.RReilAddr;
import rreil.abstractsyntax.RelOp;
import rreil.abstractsyntax.Rhs;
import rreil.abstractsyntax.util.CRReilFactory;
import rreil.abstractsyntax.util.RReilFactory;
import rreil.abstractsyntax.util.RootVariables;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LivenessTest {
  private static final RReilFactory rreilFactory = CRReilFactory.getInstance();
  private static final int idOfX = RootVariables.getIdOrFresh("x");
  private static final int idOfY = RootVariables.getIdOrFresh("y");
  private static final int idOfZ = RootVariables.getIdOrFresh("z");
  private static final int idOfFlag = RootVariables.getIdOrFresh("f");
  private static final Rhs.Rvar x = rreilFactory.variable(32, 0, idOfX);
  private static final Rhs.Rvar y = rreilFactory.variable(32, 0, idOfY);
  private static final Rhs.Rvar z = rreilFactory.variable(32, 0, idOfZ);
  private static final Rhs.Rvar z_64 = rreilFactory.variable(64, 0, idOfZ);
  private static final Rhs.Rvar z_32_0 = z;
  private static final Rhs.Rvar z_32_32 = rreilFactory.variable(32, 32, idOfZ);
  private static final Rhs.Rvar flag = rreilFactory.variable(1, 0, idOfFlag);

  @Test public void livenessOfOverlappingFields () {
    RootDomainProxy<?> domain = build(z_64.asUnresolvedField());
    domain.begin();
    assertTrue(queryLiveness(domain.getMutableDomain(), z_32_0.asUnresolvedField()));
    assertTrue(queryLiveness(domain.getMutableDomain(), z_32_32.asUnresolvedField()));
    assertTrue(queryLiveness(domain.getMutableDomain(), z_64.asUnresolvedField()));
    domain.eval(assign(z_32_32, rreilFactory.literal(32, BigInt.ZERO)));
    assertTrue(queryLiveness(domain.getMutableDomain(), z_32_0.asUnresolvedField()));
    assertFalse(queryLiveness(domain.getMutableDomain(), z_32_32.asUnresolvedField()));
    assertTrue(queryLiveness(domain.getMutableDomain(), z_64.asUnresolvedField()));
  }

  @Test public void deadAfterAssignments () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.eval(assign(z, binary(x, BinOp.Add, y)));
    assertTrue(queryLiveness(domain.getMutableDomain(), x.asUnresolvedField()));
    assertTrue(queryLiveness(domain.getMutableDomain(), y.asUnresolvedField()));
    domain.eval(assign(x, literal32(1)));
    domain.eval(assign(y, literal32(2)));
    assertFalse(queryLiveness(domain.getMutableDomain(), y.asUnresolvedField()));
    assertFalse(queryLiveness(domain.getMutableDomain(), x.asUnresolvedField()));
    assertFalse(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.unroll();
  }

  @Test public void shouldNotBeLife000 () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    domain.eval(assign(z, rreilFactory.arbitrary(32)));
    domain = domain.commit();
    assertFalse(queryLiveness(domain.get(), z.asUnresolvedField()));
    domain.begin();
    domain.eval(assign(x, rreilFactory.arbitrary(32)));
    domain = domain.commit();
    domain.begin();
    domain.eval(assign(y, rreilFactory.arbitrary(32)));
    domain = domain.commit();
    assertFalse(queryLiveness(domain.get(), z.asUnresolvedField()));
    assertFalse(queryLiveness(domain.get(), x.asUnresolvedField()));
    assertFalse(queryLiveness(domain.get(), y.asUnresolvedField()));
    domain.begin();
    domain.eval(assign(z, binary(x, BinOp.Add, y)));
    domain = domain.commit();
    assertFalse(queryLiveness(domain.get(), z.asUnresolvedField()));
    assertFalse(queryLiveness(domain.get(), x.asUnresolvedField()));
    assertFalse(queryLiveness(domain.get(), y.asUnresolvedField()));
  }

  @Test public void shouldNotBeLife001 () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    domain.eval(assign(flag, rreilFactory.comparision(x, RelOp.Cmpeq, y)));
    domain = domain.commit();
    assertTrue(queryLiveness(domain.get(), z.asUnresolvedField()));
    assertFalse(queryLiveness(domain.get(), x.asUnresolvedField()));
    assertFalse(queryLiveness(domain.get(), y.asUnresolvedField()));
    assertFalse(queryLiveness(domain.get(), flag.asUnresolvedField()));
  }

  @Test public void lifeAfterTest () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.eval(testIfNonZero(flag));
    assertTrue(queryLiveness(domain.getMutableDomain(), flag.asUnresolvedField()));
    domain.unroll();
  }

  /**
   * Self assignment of a live variable should keep it live.
   */
  @Test public void liveAfterSelfAssign () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.eval(assign(z, z));
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.unroll();
  }

  /**
   * Self assignment of a dead variable should not make it live.
   */
  @Test public void deadAfterSelfAssign () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.eval(assign(x, x));
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    assertFalse(queryLiveness(domain.getMutableDomain(), x.asUnresolvedField()));
    domain.unroll();
  }

  @Test public void liveAfterSelfAssignOfOverlappingField001 () {
    RootDomainProxy<?> domain = build(z_64.asUnresolvedField());
    domain.begin();
    assertTrue(queryLiveness(domain.getMutableDomain(), z_64.asUnresolvedField()));
    domain.eval(assign(z_32_0, z_32_0));
    assertTrue(queryLiveness(domain.getMutableDomain(), z_32_0.asUnresolvedField()));
    assertTrue(queryLiveness(domain.getMutableDomain(), z_64.asUnresolvedField()));
    domain.unroll();
  }

  @Test public void liveAfterSelfAssignOfOverlappingField002 () {
    RootDomainProxy<?> domain = build(z_64.asUnresolvedField());
    domain.begin();
    assertTrue(queryLiveness(domain.getMutableDomain(), z_64.asUnresolvedField()));
    domain.eval(assign(z_32_32, z_32_0));
    assertTrue(queryLiveness(domain.getMutableDomain(), z_32_0.asUnresolvedField()));
    assertFalse(queryLiveness(domain.getMutableDomain(), z_32_32.asUnresolvedField()));
    assertTrue(queryLiveness(domain.getMutableDomain(), z_64.asUnresolvedField()));
    domain.unroll();
  }

  @Test public void liveAfterSelfAssignAndAddition1 () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.eval(assign(z, binary(z, BinOp.Add, x)));
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    assertTrue(queryLiveness(domain.getMutableDomain(), x.asUnresolvedField()));
    domain.unroll();
  }

  @Test public void liveAfterSelfAssignAndAddition2 () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.eval(assign(z, binary(x, BinOp.Add, z)));
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    assertTrue(queryLiveness(domain.getMutableDomain(), x.asUnresolvedField()));
    domain.unroll();
  }

  @Test public void liveAfterSelfAssignAndAddition3 () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.eval(assign(z, binary(z, BinOp.Add, z)));
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.unroll();
  }

  @Test public void shouldBeDeadWithOffsets () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.eval(assign(z_32_32, z));
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    assertFalse(queryLiveness(domain.getMutableDomain(), z_32_32.asUnresolvedField()));
    domain.unroll();
  }

  @SuppressWarnings("unchecked")
  @Test public <D extends RootDomain<D>> void argumentsShouldBeLiveAfterJoin () {
    RootDomainProxy<D> left = (RootDomainProxy<D>) build(z.asUnresolvedField());
    RootDomainProxy<D> right = (RootDomainProxy<D>) build(z.asUnresolvedField());
    left.begin();
    right.begin();
    left.eval(assign(z, binary(x, BinOp.Add, y)));
    assertFalse(queryLiveness(left.getMutableDomain(), z.asUnresolvedField()));
    right.eval(assign(z, binary(x, BinOp.Add, y)));
    assertFalse(queryLiveness(right.getMutableDomain(), z.asUnresolvedField()));
    left.join(right);
    assertTrue(queryLiveness(left.getMutableDomain(), x.asUnresolvedField()));
    assertTrue(queryLiveness(left.getMutableDomain(), y.asUnresolvedField()));
    assertFalse(queryLiveness(left.getMutableDomain(), z.asUnresolvedField()));
    left.unroll();
    right.unroll();
  }

  @SuppressWarnings("unchecked")
  @Test public <D extends RootDomain<D>> void disjointRegionsShouldBeLiveAfterJoin () {
    RootDomainProxy<D> left = (RootDomainProxy<D>) build(z.asUnresolvedField());
    RootDomainProxy<D> right = (RootDomainProxy<D>) build(y.asUnresolvedField());
    left.begin();
    right.begin();
    left.join(right);
    assertTrue(queryLiveness(left.getMutableDomain(), y.asUnresolvedField()));
    assertTrue(queryLiveness(left.getMutableDomain(), z.asUnresolvedField()));
    right.unroll();
    left.unroll();
  }

  @SuppressWarnings("unchecked")
  @Test public <D extends RootDomain<D>> void disjointFieldsShouldBeLiveAfterJoin () {
    RootDomainProxy<D> left = (RootDomainProxy<D>) build(z.asUnresolvedField());
    RootDomainProxy<D> right = (RootDomainProxy<D>) build(z_32_32.asUnresolvedField());
    left.begin();
    right.begin();
    left.join(right);
    assertTrue(queryLiveness(left.getMutableDomain(), z_32_32.asUnresolvedField()));
    assertTrue(queryLiveness(left.getMutableDomain(), z.asUnresolvedField()));
    left.unroll();
    right.unroll();
  }

  @Test(expected = DomainStateException.class) public void reintroductionOfFieldShouldKeepItLive () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    domain.introduceRegionCtx(z.asUnresolvedField().getRegion(), RegionCtx.registerCtx());
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.eval(assign(z, z));
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.unroll();
  }

  @Test(expected = DomainStateException.class) public void reintroductionOfDeadFieldShouldNotMakeItLive () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.eval(assign(z, x));
    assertFalse(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    assertTrue(queryLiveness(domain.getMutableDomain(), x.asUnresolvedField()));
    domain.introduceRegionCtx(z.asUnresolvedField().getRegion(), RegionCtx.registerCtx());
    assertFalse(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    assertTrue(queryLiveness(domain.getMutableDomain(), x.asUnresolvedField()));
    domain.unroll();
  }

  @Test public void touchingOfDeadFieldShouldNotMakeItLive () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.eval(assign(z, x));
    assertFalse(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    assertTrue(queryLiveness(domain.getMutableDomain(), x.asUnresolvedField()));
    domain.touchField(z.asUnresolvedField());
    assertFalse(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    assertTrue(queryLiveness(domain.getMutableDomain(), x.asUnresolvedField()));
    domain.unroll();
  }

  @Test public void targetOfLoadFromMemoryShouldBeDeadButAddressContainerLive () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    domain.introduceRegionCtx(x.asUnresolvedField().getRegion(), RegionCtx.registerCtx());
    domain.eval(load(z, x));
    assertFalse(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    assertTrue(queryLiveness(domain.getMutableDomain(), x.asUnresolvedField()));
    domain.unroll();
  }

  @Test public void loadFromMemoryShouldBeDead () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    domain.introduceRegionCtx(x.asUnresolvedField().getRegion(), RegionCtx.registerCtx());
    domain.introduceRegionCtx(y.asUnresolvedField().getRegion(), RegionCtx.registerCtx());
    domain.eval(load(x, y));
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    assertFalse(queryLiveness(domain.getMutableDomain(), y.asUnresolvedField()));
    assertFalse(queryLiveness(domain.getMutableDomain(), x.asUnresolvedField()));
    domain.unroll();
  }

  @Test public void targetOfLoadFromMemoryShouldBeDead () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    domain.introduceRegionCtx(x.asUnresolvedField().getRegion(), RegionCtx.registerCtx());
    domain.eval(load(x, x));
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    assertFalse(queryLiveness(domain.getMutableDomain(), x.asUnresolvedField()));
    domain.unroll();
  }

  @Test public void targetOfLoadFromMemoryShouldStayLive () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    domain.eval(load(z, z));
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    domain.unroll();
  }

  @Test public void bothAddressAndValueShouldBeLiveAfterStore () {
    RootDomainProxy<?> domain = build(z.asUnresolvedField());
    domain.begin();
    domain.introduceRegionCtx(x.asUnresolvedField().getRegion(), RegionCtx.registerCtx());
    domain.introduceRegionCtx(y.asUnresolvedField().getRegion(), RegionCtx.registerCtx());
    domain.eval(store(y, x));
    assertTrue(queryLiveness(domain.getMutableDomain(), z.asUnresolvedField()));
    assertTrue(queryLiveness(domain.getMutableDomain(), x.asUnresolvedField()));
    assertTrue(queryLiveness(domain.getMutableDomain(), y.asUnresolvedField()));
    domain.unroll();
  }

  private static RootDomainProxy<?> build (Field live) {
    @SuppressWarnings({"rawtypes", "unchecked"})
    RootDomainProxy<?> proxy = new RootDomainProxy(initialDomainHierarchy());
    proxy.begin();
    proxy.introduceRegionCtx(live.getRegion(), RegionCtx.registerCtx());
    proxy.touchField(live);
    return proxy.commit();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static RootDomain<?> initialDomainHierarchy () {
    return new LivenessRootPart(new FieldLiveness(new LivenessFinitePart()));
  }

  private static boolean queryLiveness (QueryChannel channel, Field field) {
    return channel.queryLiveness(field);
  }

  private static RReil.Assign assign (Rhs.Rvar lhs, Rhs expr) {
    return rreilFactory.assign(null, RReilAddr.valueOf(0), lhs.asLhs(), expr);
  }

  private static rreil.abstractsyntax.Test testIfNonZero (Rhs.Rval rhs) {
    return rreilFactory.testIfNonZero(rhs);
  }

  private static RReil.Load load (Rhs.Rvar lhs, Rhs.Rval rhs) {
    return rreilFactory.load(null, RReilAddr.valueOf(0), lhs.asLhs(), rhs);
  }

  private static RReil.Store store (Rhs.Rval lhs, Rhs.Rval rhs) {
    return rreilFactory.store(null, RReilAddr.valueOf(0), lhs, rhs);
  }

  private static Rhs literal32 (int value) {
    return rreilFactory.literal(32, BigInt.valueOf(value));
  }

  private static Rhs binary (Rhs.Rval left, BinOp op, Rhs.Rval right) {
    return rreilFactory.binary(left, op, right);
  }
}
