package javalx.xml;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Print the contents of objects in XML format.
 */
public interface XmlPrintable {

  public XMLBuilder toXML (XMLBuilder builder);
}
