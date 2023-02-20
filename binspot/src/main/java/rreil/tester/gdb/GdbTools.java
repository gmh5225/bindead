package rreil.tester.gdb;

public class GdbTools {
  public static String changeStringEndianess(String hexInput) {
    StringBuilder result = new StringBuilder();
    
    for(int i = 0; i < hexInput.length()/2; i++)
      result.append(hexInput.subSequence(hexInput.length() - 2 - 2*i, hexInput.length() - 2*i));
    
    return result.toString();
  }
}
