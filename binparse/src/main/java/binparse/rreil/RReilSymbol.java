package binparse.rreil;

import binparse.AbstractSymbol;

/**
 * A symbol implementation for labels in a RREIL assembler file.
 *
 * @author Bogdan Mihaila
 */
class RReilSymbol extends AbstractSymbol {

  public RReilSymbol (String name, long address, long size) {
    super(name, address, size, new byte[0]);
  }

}
