/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */
package bindis.x86.common;

public class X86SegmentRegisters {
  public static final int NUM_SEGMENT_REGISTERS = 6;
  public static final X86RegOpnd ES;
  public static final X86RegOpnd CS;
  public static final X86RegOpnd SS;
  public static final X86RegOpnd DS;
  public static final X86RegOpnd FS;
  public static final X86RegOpnd GS;
  private static X86RegOpnd segmentRegisters[];

  static {
    //Segment registers
    ES = new X86RegOpnd("es", 32);
    CS = new X86RegOpnd("cs", 32);
    SS = new X86RegOpnd("ss", 32);
    DS = new X86RegOpnd("ds", 32);
    FS = new X86RegOpnd("fs", 32);
    GS = new X86RegOpnd("gs", 32);

    segmentRegisters =
        new X86RegOpnd[]{
      ES, CS, SS, DS, FS, GS
    };
  }

  public static int getNumberOfRegisters () {
    return NUM_SEGMENT_REGISTERS;
  }

  public static X86RegOpnd getSegmentRegister (int regNum) {
    assert (regNum > -1 && regNum < NUM_SEGMENT_REGISTERS) : "invalid segment register number";
    return segmentRegisters[regNum];
  }
}
