package bindead.domains.segments.heap;

import java.util.Iterator;
import java.util.LinkedList;

import javalx.data.products.P2;
import javalx.numeric.Range;
import javalx.persistentcollections.AVLMap;
import javalx.persistentcollections.ThreeWaySplit;
import rreil.lang.MemVar;
import bindead.data.MemVarSet;
import bindead.debug.PrettyDomain;

public class ConnectorSet implements Iterable<Connector> {

  private static final ConnectorSet EMPTY = new ConnectorSet();
  final AVLMap<ConnectorId, ConnectorData> connectors;

  public ConnectorSet () {
    this.connectors = AVLMap.<ConnectorId, ConnectorData>empty();
  }

  private ConnectorSet (AVLMap<ConnectorId, ConnectorData> connectors) {
    this.connectors = connectors;
  }

  // hsi: a bit inefficient, does anybody want to implement a better Iterator?
  @Override public Iterator<Connector> iterator () {
    LinkedList<Connector> r = new LinkedList<Connector>();
    for (P2<ConnectorId, ConnectorData> connector : connectors)
      r.add(new Connector(connector._1(), connector._2()));
    return r.iterator();
  }

  public boolean isEmpty () {
    return connectors.isEmpty();
  }

  ConnectorSet add (Connector connector) {
    return add(connector.id, connector.data);
  }

  ConnectorSet add (ConnectorId id, ConnectorData data) {
    assert !connectors.contains(id) : "Error addding " + id + ": EdgeNodeSet already contains Data "
      + connectors.get(id);
    return new ConnectorSet(connectors.bind(id, data));
  }

  @Override public String toString () {
    return connectors.toString();
  }

  ConnectorSet getConnectorComingFrom (MemVar from) {
    ConnectorSet m = ConnectorSet.EMPTY;
    for (final P2<ConnectorId, ConnectorData> connector : connectors)
      if (from.equals(connector._1().src))
        m = m.add(connector._1(), connector._2());
    return m;
  }

  ConnectorSet getConnectorGoingTo (MemVar to) {
    ConnectorSet m = new ConnectorSet();
    for (final P2<ConnectorId, ConnectorData> connector : connectors) {
      if (!to.equals(connector._1().tgt))
        continue;
      m = m.add(connector._1(), connector._2());
    }
    return m;
  }

  boolean contains (ConnectorId id) {
    return connectors.contains(id);
  }

  @SuppressWarnings("unused") private static void msg (String string) {
    System.out.println("EdgeNodeSet: " + string);
  }

  ThreeWaySplit<ConnectorSet> split (ConnectorSet other) {
    ThreeWaySplit<AVLMap<ConnectorId, ConnectorData>> split = connectors.split(other.connectors);
    return ThreeWaySplit.<ConnectorSet>make(
        new ConnectorSet(split.onlyInFirst()),
        new ConnectorSet(split.inBothButDiffering()),
        new ConnectorSet(split.onlyInSecond()));
  }

  public MemVarSet getChildSupportSet () {
    MemVarSet css = MemVarSet.empty();
    for (P2<ConnectorId, ConnectorData> en : connectors)
      css = css.insertAll(en._2().getChildSupportSet());
    return css;
  }

  public static ConnectorSet empty () {
    return EMPTY;
  }

  public Connector get (ConnectorId key) {
    ConnectorData data = connectors.getOrNull(key);
    assert data != null;
    return new Connector(key, data);
  }

  Connector get (MemVar src, PathString transitivePathString, MemVar tgt) {
    return get(new ConnectorId(src, transitivePathString, tgt));
  }

  public ConnectorSet remove (ConnectorId id) {
    return new ConnectorSet(connectors.remove(id));
  }

  public ConnectorSet join (ConnectorSet other) {
    return new ConnectorSet(connectors.union(other.connectors));
  }

  public ConnectorSet removeConnectorsFrom (MemVar mv, Range offset) {
    ConnectorSet m = ConnectorSet.EMPTY;
    for (final P2<ConnectorId, ConnectorData> en1 : connectors) {
      if (!en1._1().comesFrom(mv, offset))
        m = m.add(en1._1(), en1._2());
    }
    return m;
  }

  public ConnectorSet getEdgenodesAttachedTo (MemVar region) {
    ConnectorSet m = ConnectorSet.EMPTY;
    for (final P2<ConnectorId, ConnectorData> e : connectors) {
      ConnectorId id = e._1();
      if (id.attachedTo(region))
        m = m.add(id, e._2());
    }
    return m;

  }

  public void toCompactString (StringBuilder builder, PrettyDomain childDomain) {
    builder.append("Connectors\n");
    for (Connector en : this) {
      en.printCompact(builder, childDomain);
      builder.append('\n');
    }
  }
}
