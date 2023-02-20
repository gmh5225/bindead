package rreil.tester.gdb.responses;

import rreil.tester.gdb.GdbTools;
import rreil.tester.gdb.Tuple;

public class GdbTResponse implements IGdbResponse {
  public final int aa;
  @SuppressWarnings("unchecked") public final Tuple<Integer, Long>[] rn = new Tuple[3];

  public GdbTResponse (String data) {
    aa = Integer.parseInt(data.substring(1, 3), 16);

    String rns = data.substring(3);
    String[] rnsArray = rns.split(";");
    for(int i = 0; i < rnsArray.length; i++) {
      String[] entry = rnsArray[i].split(":");
      rn[i] = new Tuple<Integer, Long>(Integer.parseInt(GdbTools.changeStringEndianess(entry[0])), Long.parseLong(GdbTools.changeStringEndianess(entry[1]), 16));
    }
  }

  public long getAddress () {
    return rn[2].y;
  }

  @Override public GdbResponseType getType () {
    return GdbResponseType.T;
  }
}
