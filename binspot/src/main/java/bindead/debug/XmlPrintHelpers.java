package bindead.debug;

import java.util.Iterator;
import java.util.Properties;

import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.persistentcollections.AVLSet;
import javalx.xml.XmlPrintable;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import bindead.data.NumVar;
import bindead.data.VarSet;

import com.jamesmurty.utils.XMLBuilder;

public class XmlPrintHelpers {
  private static final Properties outputProperties = new Properties();

  static {
    outputProperties.put(javax.xml.transform.OutputKeys.INDENT, "yes");
    outputProperties.put("{http://xml.apache.org/xslt}indent-amount", "2");
  }

  public static String asString (final String root, final XmlPrintable printable) {
    try {
      final XMLBuilder xml = XMLBuilder.create(root);
      return printable.toXML(xml).asString(outputProperties);
    } catch (ParserConfigurationException ex) {
      throw new IllegalStateException("XMLBuilder failed");
    } catch (FactoryConfigurationError ex) {
      throw new IllegalStateException("XMLBuilder failed");
    } catch (TransformerException ex) {
      throw new IllegalStateException("XMLBuilder failed");
    }
  }

  public static <A, B> XMLBuilder p2AsElement (XMLBuilder builder, P2<A, B> ab) {
    return p2AsElement(builder, "product", ab);
  }

  public static <A, B> XMLBuilder p2AsElement (XMLBuilder builder, String name, P2<A, B> ab) {
    /*
     *	<name>
     *		<value>a</value>
     *		<value>b</value>
     *	</name>
     */
    return builder.e(name).e("value").text(ab._1().toString()).up().e("value").text(ab._2().toString()).up();
  }

  public static <A, B, C> XMLBuilder p3AsElement (XMLBuilder builder, P3<A, B, C> ab) {
    return p3AsElement(builder, "product", ab);
  }

  public static <A, B, C> XMLBuilder p3AsElement (XMLBuilder builder, String name, P3<A, B, C> ab) {
    return builder.e(name).e("value").text(ab._1().toString()).up().e("value").text(ab._2().toString()).up().e("value")
        .text(ab._3().toString()).up();
  }

  public static XMLBuilder binding (XMLBuilder builder, Object id, XmlPrintable value) {
    if (value == null)
      return builder.e("Binding").e("Key").t(id.toString()).up().up();
    return value.toXML(builder.e("Binding").e("Key").t(id.toString()).up().e("Value")).up().up();
  }

  public static XMLBuilder binding (XMLBuilder builder, Object id, VarSet set) {
    if (set == null)
      return builder.e("Binding").e("Key").t(id.toString()).up().up();
    return variableSet(builder.e("Binding").e("Key").t(id.toString()).up().e("Value"), set).up().up();
  }

  public static XMLBuilder variableSet (XMLBuilder builder, VarSet variables) {
    for (NumVar variable : variables) {
      builder = builder.e("Variable").t(variable.toString()).up();
    }
    return builder;
  }

  public static XMLBuilder set (XMLBuilder builder, AVLSet<?> set) {
    XMLBuilder xml = builder;
    xml = xml.e("Set");
    final StringBuilder str = new StringBuilder("{");
    final Iterator<?> it = set.iterator();
    while (it.hasNext()) {
      str.append(it.next().toString());
      if (it.hasNext())
        str.append(", ");
    }
    str.append('}');
    xml = xml.t(set.toString()).up();
    return xml;
  }

  public static XMLBuilder openDomain (XMLBuilder builder, String name, DomainType type) {
    return builder.e(type.name()).a("name", name);
  }

  public static XMLBuilder openDomain (XMLBuilder builder, String name, DomainType type, DomainKind kind) {
    return builder.e(type.name()).a("name", name).a("kind", kind.name());
  }

  public static XMLBuilder domain (XMLBuilder builder, String name, DomainType type, XmlPrintable domain) {
    return domain.toXML(builder.e(name).a("type", type.name()).a("kind", DomainKind.Leaf.name())).up();
  }

  public static XMLBuilder domainFunctor (
      XMLBuilder builder, String name, DomainType type, XmlPrintable outer, XmlPrintable inner) {
    XMLBuilder xml = builder;
    if (outer != null) {
      xml = xml.e(type.name()).a("name", name).a("kind", DomainKind.Functor.name());
      xml = outer.toXML(xml).up();
    }
    xml = inner.toXML(xml);
    return xml;
  }

  public static XMLBuilder closeDomain (XMLBuilder builder) {
    return builder.up();
  }

  public static enum DomainType {
    Root,
    Memory,
    Finite,
    Zeno;
  }

  public static enum DomainKind {
    Leaf,
    Functor,
    Product,
    PartiallyReducedProduct
  }

  /**
   * Sanitize a string to not contain some of the common known XML special characters.
   */
  public static String sanitize (String string) {
    string = remove(string, "(");
    string = remove(string, ")");
    return string;
  }

  private static String remove (String fromString, String what) {
    return fromString.replace(what, "");
  }
}
