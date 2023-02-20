package binparse.pe;

import binparse.AbstractSymbol;

/**
 * A symbol in a PE file.
 */
class PESymbol extends AbstractSymbol {

  public PESymbol (String name, long address, long size, byte[] data) {
    super(name, address, size, data);
  }

}
