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

/* 8 64-bit registers called MMX registers*/
public class X86MMXRegisters {
  public static final int NUM_MMX_REGISTERS = 8;
  public static final X86RegOpnd MM0;
  public static final X86RegOpnd MM1;
  public static final X86RegOpnd MM2;
  public static final X86RegOpnd MM3;
  public static final X86RegOpnd MM4;
  public static final X86RegOpnd MM5;
  public static final X86RegOpnd MM6;
  public static final X86RegOpnd MM7;
  private static X86RegOpnd mmxRegisters[];

  static {
    // 64-bit MMX registers
    MM0 = new X86RegOpnd("mm0", 64);
    MM1 = new X86RegOpnd("mm1", 64);
    MM2 = new X86RegOpnd("mm2", 64);
    MM3 = new X86RegOpnd("mm3", 64);
    MM4 = new X86RegOpnd("mm4", 64);
    MM5 = new X86RegOpnd("mm5", 64);
    MM6 = new X86RegOpnd("mm6", 64);
    MM7 = new X86RegOpnd("mm7", 64);

    mmxRegisters =
        new X86RegOpnd[]{
      MM0, MM1, MM2, MM3, MM4, MM5, MM6, MM7
    };
  }

  public static int getNumberOfRegisters () {
    return NUM_MMX_REGISTERS;
  }

  public static String getRegisterName (int regNum) {
    assert (regNum > -1 && regNum < NUM_MMX_REGISTERS) : "invalid MMX register number";
    return mmxRegisters[regNum].toString();
  }

  public static X86RegOpnd getRegister (int regNum) {
    assert (regNum > -1 && regNum < NUM_MMX_REGISTERS) : "invalid MMX register number";
    return mmxRegisters[regNum];
  }
}
