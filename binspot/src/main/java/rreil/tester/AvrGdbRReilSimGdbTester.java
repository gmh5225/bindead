package rreil.tester;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteOrder;

import rreil.disassembler.translators.avr8.implementations.AvrImplementation;
import rreil.interpreter.InterpCtx;
import rreil.interpreter.MachineCtx;
import rreil.interpreter.MemoryModel;
import rreil.interpreter.RReilMachine;
import rreil.interpreter.avr.chipstate.AvrChipState;
import rreil.tester.gdb.GdbCommunicator;
import rreil.tester.gdb.GdbCommunicator.MemorySpace;
import rreil.tester.gdb.responses.GdbMemoryResponse;
import rreil.tester.gdb.responses.IGdbResponse;
import bindead.environment.platform.RReilPlatform;

public class AvrGdbRReilSimGdbTester {
  private final GdbCommunicator communicator;
  private final int startAddress;
  private final int memRangelength;
  private final RReilPlatform avr8;
  private final RReilMachine machine;
  private MachineCtx machCtx;

  public AvrGdbRReilSimGdbTester (GdbCommunicator communicator, RReilPlatform avr8, int startAddress, int memRangelength) throws IOException, InterruptedException {
    this.communicator = communicator;
    this.startAddress = startAddress;
    this.memRangelength = memRangelength;
    this.avr8 = avr8;
    this.machine = new RReilMachine(avr8.getDisassembler());
    init();
  }

  private byte[] combineBytes (IGdbResponse[] responses, int size) {
    byte[] bytes = new byte[size];
    int offset = 0;
    for (int i = 0; i < responses.length; i++) {
      for (int j = 0; j < ((GdbMemoryResponse) responses[i]).getBytes().length; j++) {
        bytes[offset + j] = ((GdbMemoryResponse) responses[i]).getBytes()[j];
      }
      offset += ((GdbMemoryResponse) responses[i]).getBytes().length;
    }
    return bytes;
  }

  private void initGdb () throws IOException, InterruptedException {
    if (startAddress != 0x00) {
      communicator.insertBreakpoint(startAddress);

      communicator.continueGdb();
    }
  }

  private void initSimulator () throws IOException, InterruptedException {
    IGdbResponse[] responses = communicator.readMemory(MemorySpace.Program, 0, memRangelength);

    byte[] bytes = combineBytes(responses, memRangelength);

    System.out.println("Program:");

    StringWriter sW = new StringWriter();
    PrintWriter pW = new PrintWriter(sW);

    pW.printf("{");
    for (int i = 0; i < bytes.length; i++) {
      pW.printf(" (byte)0x%02x", bytes[i]);
      if (i + 1 < bytes.length)
        pW.printf(",");
    }
    pW.printf(" }");

    System.out.println(sW.toString());
// XXX: this method does not exist anymore. Why is there no AVR8 Platform class and a register translator?
//    InterpCtx ctx = avr8.emptyInterpCtx();
// code below is bogus and just here to make things compile. Needs fixing.
    InterpCtx ctx = new InterpCtx(avr8.defaultArchitectureSize(), null, new MemoryModel(ByteOrder.BIG_ENDIAN));
    machCtx = new MachineCtx(ctx, bytes, startAddress, startAddress);
    machine.initPc(machCtx);

    int memSize = AvrImplementation.$ATMEGA32L.getDataSpaceSize();
    IGdbResponse[] memoryResponses = communicator.readMemory(MemorySpace.Data, 0, memSize);

    byte[] memBytes = combineBytes(memoryResponses, memSize);

    AvrChipState state = AvrChipState.fromGdbMemoryDump(memBytes, 0, memBytes.length);

    // System.out.println(state.getJavaArrayString());

    state.toAvrInterpCtx(ctx);

    // final byte[] code = TstHelpers.pack(0x0f, 0x5f);
    // final InterpCtx ctx = SubTranslatorTest.avr8.emptyInterpCtx();
    //
    // ctx.set("r16", BigInt.ONE);
    //
    // final MachineCtx machCtx = new MachineCtx(ctx, code, 0, 0);
    // SubTranslatorTest.avr8.getMachine().evalOne(machCtx);
  }

  public void init () throws IOException, InterruptedException {
    initGdb();
    initSimulator();
  }

  public void step () throws IOException, InterruptedException {
    machine.step(machCtx);
    communicator.step();
  }

  public String diff () throws IOException, InterruptedException {
    int memSize = AvrImplementation.$ATMEGA32L.getDataSpaceSize();
    IGdbResponse[] memoryResponses = communicator.readMemory(MemorySpace.Data, 0, memSize);

    byte[] memBytes = combineBytes(memoryResponses, memSize);

    AvrChipState gdbState = AvrChipState.fromGdbMemoryDump(memBytes, 0, memBytes.length);

    AvrChipState simState = AvrChipState.fromAvrInterpCtx(machCtx.getCtx());

    return gdbState.diff(simState);
  }

  public AvrChipState simulatorState () {
    AvrChipState simState = AvrChipState.fromAvrInterpCtx(machCtx.getCtx());

    return simState;
  }
}
