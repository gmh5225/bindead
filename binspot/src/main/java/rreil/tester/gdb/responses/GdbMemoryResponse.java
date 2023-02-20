package rreil.tester.gdb.responses;

public class GdbMemoryResponse implements IGdbResponse {
  private final byte[] bytes;
  
  public byte[] getBytes() {
    return bytes;
  }
  
  public GdbMemoryResponse (String data) {
    bytes = new byte[data.length()/2];
    
    for(int i = 0; i < bytes.length; i++)
      bytes[i] = (byte)Short.parseShort(data.substring(2*i, 2*i + 2), 16);
  }

  @Override public GdbResponseType getType () {
    return GdbResponseType.Memory;
  }
}
