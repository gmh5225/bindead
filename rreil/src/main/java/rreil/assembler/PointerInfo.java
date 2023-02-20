package rreil.assembler;

import javalx.data.Option;
import javalx.numeric.BigInt;

public class PointerInfo {
  private final Integer size;
  private final String id;
  private final Option<BigInt> address;

  public PointerInfo (Integer size, String id, Option<BigInt> address) {
    this.size = size;
    this.id = id;
    this.address = address;
  }
  
  public Option<BigInt> getAddress () {
    return address;
  }
  
  public String getId () {
    return id;
  }
  
  public Integer getSize () {
    return size;
  }
}
