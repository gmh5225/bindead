package binparse.trace;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import javalx.data.products.P3;
import javalx.numeric.BigInt;
import binparse.Endianness;
import binparse.Permission;
import binparse.Segment;
import binparse.SegmentImpl;
import binparse.UncheckedIOException;
import binparse.trace.TraceSerializer.CPUState;
import binparse.trace.TraceSerializer.ControlFlowTrace;
import binparse.trace.TraceSerializer.ControlFlowTrace.ControlFlow;
import binparse.trace.TraceSerializer.TraceInfo;

class TraceParser {

  public static TraceInfo parseInfo (String file) {
    return parseTraceDumps(file, new Parser<TraceInfo>() {
      @Override public TraceInfo parse (InputStream input) {
        try {
          return TraceInfo.parseFrom(input);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    });
  }

  public static Map<String, BigInt> parseRegisters (String file) {
    return parseTraceDumps(file, new Parser<Map<String, BigInt>>() {
      @Override public Map<String, BigInt> parse (InputStream input) {
        return parseRegisters(input);
      }
    });
  }

  private static Map<String, BigInt> parseRegisters (InputStream input) {
    Map<String, BigInt> registers = new HashMap<String, BigInt>();
    try {
      CPUState cpuState = CPUState.parseFrom(input);
      for (CPUState.Register reg : cpuState.getRegistersList()) {
        registers.put(reg.getName(), BigInt.of(reg.getValue()));
      }
      return registers;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Segment parseSegment (String file) {
    return parseTraceDumps(file, new Parser<Segment>() {
      @Override public Segment parse (InputStream input) {
        return parseSegment(input);
      }
    });
  }

  private static Segment parseSegment (InputStream input) {
    try {
      TraceSerializer.Module.Segment rawSegment = TraceSerializer.Module.Segment.parseFrom(input);
      return convertSegment(rawSegment);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static List<Segment> parseModule (String file) {
    return parseTraceDumps(file, new Parser<List<Segment>>() {
      @Override public List<Segment> parse (InputStream input) {
        return parseModule(input);
      }
    });
  }

  private static List<Segment> parseModule (InputStream input) {
    List<Segment> segments = new ArrayList<Segment>();
    try {
      TraceSerializer.Module module = TraceSerializer.Module.parseFrom(input);
      for (TraceSerializer.Module.Segment rawSegment : module.getSegmentsList()) {
        Segment segment = convertSegment(rawSegment);
        segments.add(segment);
      }
      return segments;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static List<P3<Long, Long, Boolean>> parseControlFlow (String file) {
    return parseTraceDumps(file, new Parser<List<P3<Long, Long, Boolean>>>() {
      @Override public List<P3<Long, Long, Boolean>> parse (InputStream input) {
        return parseControlFlow(input);
      }
    });
  }

  private static List<P3<Long, Long, Boolean>> parseControlFlow (InputStream input) {
    List<P3<Long, Long, Boolean>> jumps = new ArrayList<P3<Long, Long, Boolean>>();
    try {
      ControlFlowTrace trace = ControlFlowTrace.parseFrom(input);
      for (ControlFlow jump : trace.getJumpsList()) {
        long currentPC = jump.getCurrentPC();
        long nextPC = jump.getNextPC();
        boolean jumpTaken = jump.getJumpConditionWas();
        jumps.add(P3.tuple3(currentPC, nextPC, jumpTaken));
      }
      return jumps;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static String parseControlFlowDebug (String file) {
    return parseTraceDumps(file, new Parser<String>() {
      @Override public String parse (InputStream input) {
        return parseControlFlowDebug(input);
      }
    });
  }

  private static String parseControlFlowDebug (InputStream input) {
    try { // trick taken from http://stackoverflow.com/a/5445161
      return new java.util.Scanner(input).useDelimiter("\\A").next();
    } catch (java.util.NoSuchElementException e) {
      return "";
    }
  }

  private static Segment convertSegment (TraceSerializer.Module.Segment rawSegment) {
    long address = rawSegment.getAddress();
    int size = rawSegment.getSize();
    Endianness endianness = convertEndianness(rawSegment.getEndianness());
    byte[] data;
    if (rawSegment.hasData())
      data = rawSegment.getData().toByteArray();
    else
      data = new byte[0];
    Set<Permission> permissions;
    if (rawSegment.hasPermissions())
      permissions = decodePermissions(rawSegment.getPermissions());
    else
      permissions = EnumSet.allOf(Permission.class);
    String name = null;
    if (rawSegment.hasName())
      name = rawSegment.getName();
    String fileName = null;
    if (rawSegment.hasFileName())
      fileName = rawSegment.getFileName();
    return new SegmentImpl(fileName, name, address, size, data, endianness, permissions);
  }

  private static Endianness convertEndianness (TraceSerializer.Module.Segment.Endianness endianness) {
    switch (endianness) {
    case LITTLE:
      return Endianness.LITTLE;
    case BIG:
      return Endianness.BIG;
    default:
      throw new IllegalArgumentException("Endianness enumeration value unknown.");
    }
  }

  private static Set<Permission> decodePermissions (int permissions) {
    Set<Permission> decodedPermissions = EnumSet.noneOf(Permission.class);
    if ((permissions & 1) == 1)
      decodedPermissions.add(Permission.Execute);
    if ((permissions >> 1 & 1) == 1)
      decodedPermissions.add(Permission.Write);
    if ((permissions >> 2 & 1) == 1)
      decodedPermissions.add(Permission.Read);
    return decodedPermissions;
  }

  private static <T> T parseTraceDumps (String file, Parser<T> parser) {
    InputStream input = null;
    try {
      input = new BufferedInputStream(new FileInputStream(file));
      return parser.parse(input);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
        }
      }
    }
  }

  private static interface Parser<T> {
    public T parse (InputStream input);
  }
}
