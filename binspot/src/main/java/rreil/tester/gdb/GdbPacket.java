package rreil.tester.gdb;

import java.io.PrintWriter;
import java.io.StringWriter;

public class GdbPacket {
  private String packetData;
  private String checksum;
  
  public static boolean isComplete(StringBuilder packet) {    
    if(packet.length() < 4)
      return false;
    if(packet.charAt(0) != '$')
      return false;
    return packet.charAt(packet.length() - 3) == '#';
  }
  
  public static Tuple<Boolean, Tuple<Integer, Integer>> match(StringBuilder data) {
    Tuple<Boolean, Tuple<Integer, Integer>> r = new Tuple<Boolean, Tuple<Integer, Integer>>();
    
    int pDStart = -1;
    int cStart = -1;
    sLoop: for(int i = 0; i < data.length(); i++) {
      switch(data.charAt(i)) {
        case '$':
          pDStart = i;
          break;
        case '#':
          cStart = i;
          break sLoop;
        default:
          break;
      }
    }
    
    if(pDStart >= 0 && cStart >= 0 && data.length() > cStart + 2) {
      r.x = true;
      r.y = new Tuple<Integer, Integer>(pDStart, cStart);
    }
    else
      r.x = false;
        
    return r;
  }

  public static int calculateChecksum(String input) {
    int myChecksum = 0;
    for(int i = 0; i < input.length(); i++)
      myChecksum += input.charAt(i) % 256;
    myChecksum %= 256;
    return myChecksum;
  }
  
  public String getPacketData () {
    return packetData;
  }

  public void setPacketData (String packetData) {
    this.packetData = packetData;
  }

  public String getChecksum () {
    return checksum;
  }

  public void setChecksum (String checksum) {
    this.checksum = checksum;
  }
  
  public boolean checksumValid() {
    int iChecksum = Integer.parseInt(checksum, 16);
    int myChecksum = calculateChecksum(packetData);
    return myChecksum == iChecksum;
  }
  
  @Override public String toString() {
    StringWriter sW = new StringWriter();
    PrintWriter pW = new PrintWriter(sW);
    
    pW.printf("$%s#%s", packetData, checksum);
    
    return sW.toString();
  }

  public GdbPacket(String packetData, String checksum) {
    this.packetData = packetData;
    this.checksum = checksum;
  }
  
  public GdbPacket(String packetData) {
    this.packetData = packetData;
    
    StringWriter sW = new StringWriter();
    PrintWriter pW = new PrintWriter(sW);
    
    pW.printf("%x", calculateChecksum(packetData));
    
    this.checksum = sW.toString();
  }
}