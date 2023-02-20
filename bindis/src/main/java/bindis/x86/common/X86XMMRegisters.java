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

/* There are 8 128-bit XMM registers*/
public class X86XMMRegisters {
  public static final int NUM_XMM_REGISTERS = 8;
  public static final X86RegOpnd XMM0;
  public static final X86RegOpnd XMM1;
  public static final X86RegOpnd XMM2;
  public static final X86RegOpnd XMM3;
  public static final X86RegOpnd XMM4;
  public static final X86RegOpnd XMM5;
  public static final X86RegOpnd XMM6;
  public static final X86RegOpnd XMM7;
  private static X86RegOpnd xmmRegisters[];

  static {
    //XMM registers
    XMM0 = new X86RegOpnd("xmm0", 128);
    XMM1 = new X86RegOpnd("xmm1", 128);
    XMM2 = new X86RegOpnd("xmm2", 128);
    XMM3 = new X86RegOpnd("xmm3", 128);
    XMM4 = new X86RegOpnd("xmm4", 128);
    XMM5 = new X86RegOpnd("xmm5", 128);
    XMM6 = new X86RegOpnd("xmm6", 128);
    XMM7 = new X86RegOpnd("xmm7", 128);

    xmmRegisters =
        new X86RegOpnd[]{
      XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7
    };
  }

  public static int getNumberOfRegisters () {
    return NUM_XMM_REGISTERS;
  }

  //Return the register name
  public static String getRegisterName (int regNum) {
    assert (regNum > -1 && regNum < NUM_XMM_REGISTERS) : "invalid XMM register number";
    return xmmRegisters[regNum].toString();
  }

  public static X86RegOpnd getRegister (int regNum) {
    assert (regNum > -1 && regNum < NUM_XMM_REGISTERS) : "invalid XMM register number";
    return xmmRegisters[regNum];
  }
}
