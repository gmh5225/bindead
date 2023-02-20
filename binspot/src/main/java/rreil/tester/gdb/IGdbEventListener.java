package rreil.tester.gdb;

import rreil.tester.gdb.responses.GdbMemoryResponse;
import rreil.tester.gdb.responses.GdbRegistersResponse;
import rreil.tester.gdb.responses.GdbTResponse;

public interface IGdbEventListener {
  public void registersResponseReceived(GdbCommunicator gC, GdbRegistersResponse response);
  public void tResponseReceived(GdbCommunicator gC, GdbTResponse response);
  public void memoryResponseReceived(GdbCommunicator gC, GdbMemoryResponse response);
}
