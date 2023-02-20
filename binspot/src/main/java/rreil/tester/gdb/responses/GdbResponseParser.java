package rreil.tester.gdb.responses;

import rreil.tester.gdb.GdbPacket;
import rreil.tester.gdb.commands.IGdbCommand;

public class GdbResponseParser {
  public static final GdbResponseParser $ = new GdbResponseParser();

  public IGdbResponse parse(IGdbCommand command, GdbPacket response) {
    String data = response.getPacketData();
    switch(data.charAt(0)) {
      case 'T':
        return new GdbTResponse(data);
      default:
        switch(command.getType()) {
          case ReadRegisters:
            return new GdbRegistersResponse(data);
          case ReadMemory:
            return new GdbMemoryResponse(data);
          default:
            throw new RuntimeException("Unable to parse...");
        }

    }
  }
}
