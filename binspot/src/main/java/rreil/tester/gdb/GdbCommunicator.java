package rreil.tester.gdb;

import java.io.IOException;
import java.util.ArrayList;

import javalx.exceptions.UnimplementedException;
import rreil.tester.gdb.commands.GdbContinueCommand;
import rreil.tester.gdb.commands.GdbInsertBreakOrWatchPointCommand;
import rreil.tester.gdb.commands.GdbReadMemoryCommand;
import rreil.tester.gdb.commands.GdbReadRegistersCommand;
import rreil.tester.gdb.commands.GdbStepCommand;
import rreil.tester.gdb.commands.IGdbCommand;
import rreil.tester.gdb.responses.GdbMemoryResponse;
import rreil.tester.gdb.responses.GdbRegistersResponse;
import rreil.tester.gdb.responses.GdbResponseParser;
import rreil.tester.gdb.responses.GdbTResponse;
import rreil.tester.gdb.responses.IGdbResponse;

public class GdbCommunicator {
  public enum MemorySpace {
    Program, Data
  }

  private final ArrayList<IGdbEventListener> listeners = new ArrayList<IGdbEventListener>();
  private final GdbConnection c;

  public GdbCommunicator(GdbConnection c) {
    this.c = c;
  }

  public void addListener(IGdbEventListener listener) {
    listeners.add(listener);
  }

  public void removeListener(IGdbEventListener listener) {
    for(int i = 0; i < listeners.size(); i++)
      if(listeners.get(i) == listener) {
        listeners.remove(i);
        break;
      }
  }

  private void handleResponse(IGdbResponse response) {
    switch(response.getType()) {
      case T:
        GdbTResponse tResponse = (GdbTResponse)response;
        for(int i = 0; i < listeners.size(); i++)
          listeners.get(i).tResponseReceived(this, tResponse);
        break;
      case Registers:
        GdbRegistersResponse registersResponse = (GdbRegistersResponse)response;
        for(int i = 0; i < listeners.size(); i++)
          listeners.get(i).registersResponseReceived(this, registersResponse);
        break;
      case Memory:
        GdbMemoryResponse memoryResponse = (GdbMemoryResponse)response;
        for(int i = 0; i < listeners.size(); i++)
          listeners.get(i).memoryResponseReceived(this, memoryResponse);
      default:
        break;
    }
  }

  public IGdbResponse continueGdb() throws IOException, InterruptedException {
    IGdbCommand command = new GdbContinueCommand();
    c.send(new GdbPacket(command.commandString()));

    GdbPacket p = c.getNextPacket();
    c.acknowledgePacket(p);

    IGdbResponse response = GdbResponseParser.$.parse(command, p);
    handleResponse(response);

    return response;
  }

  public IGdbResponse step() throws IOException, InterruptedException {
    IGdbCommand command = new GdbStepCommand();
    c.send(new GdbPacket(command.commandString()));

    GdbPacket p = c.getNextPacket();
    c.acknowledgePacket(p);

    IGdbResponse response = GdbResponseParser.$.parse(command, p);
    handleResponse(response);

    return response;
  }

  public IGdbResponse queryRegisters() throws IOException, InterruptedException {
    IGdbCommand command = new GdbReadRegistersCommand();
    c.send(new GdbPacket(command.commandString()));

    GdbPacket p = c.getNextPacket();
    c.acknowledgePacket(p);

    IGdbResponse response = GdbResponseParser.$.parse(command, p);
    handleResponse(response);

    return response;
  }

  public IGdbResponse[] readMemory(MemorySpace space, int address, int length) throws IOException, InterruptedException {
    if(space == MemorySpace.Data)
      address += 0x800000;

    int chunk_size = 128;
    int chunks = (length+chunk_size)/chunk_size;

    IGdbResponse[] responses = new IGdbResponse[chunks];

    for(int i = 0; i < responses.length; i++) {
      IGdbCommand command = new GdbReadMemoryCommand(address, length > chunk_size ? chunk_size : length);
      c.send(new GdbPacket(command.commandString()));

      GdbPacket p = c.getNextPacket();
      c.acknowledgePacket(p);

      IGdbResponse response = GdbResponseParser.$.parse(command, p);
      handleResponse(response);

      length -= chunk_size;
      address += chunk_size;

      responses[i] = response;
    }

    return responses;
  }

  public void insertBreakpoint(int address) throws IOException, InterruptedException {
    c.send(new GdbPacket(new GdbInsertBreakOrWatchPointCommand(address).commandString()));

    /*
     * Todo: Handle Ok / Error
     */
    c.acknowledgePacket(c.getNextPacket());
  }

  public void removeBreakpoint(int address) {
    throw new UnimplementedException();
  }
}
