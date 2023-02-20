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
package bindis.x86.x64;

import bindis.x86.common.X86RegOpnd;

public class AMD64FloatRegisters {
  public static final X86RegOpnd XMM0;
  public static final X86RegOpnd XMM1;
  public static final X86RegOpnd XMM2;
  public static final X86RegOpnd XMM3;
  public static final X86RegOpnd XMM4;
  public static final X86RegOpnd XMM5;
  public static final X86RegOpnd XMM6;
  public static final X86RegOpnd XMM7;
  public static final X86RegOpnd XMM8;
  public static final X86RegOpnd XMM9;
  public static final X86RegOpnd XMM10;
  public static final X86RegOpnd XMM11;
  public static final X86RegOpnd XMM12;
  public static final X86RegOpnd XMM13;
  public static final X86RegOpnd XMM14;
  public static final X86RegOpnd XMM15;
  public static final int NUM_REGIXMMERS = 16;
  private static final X86RegOpnd[] registers;

  static {
    XMM0 = mk(0);
    XMM1 = mk(1);
    XMM2 = mk(2);
    XMM3 = mk(3);
    XMM4 = mk(4);
    XMM5 = mk(5);
    XMM6 = mk(6);
    XMM7 = mk(7);
    XMM8 = mk(8);
    XMM9 = mk(9);
    XMM10 = mk(10);
    XMM11 = mk(11);
    XMM12 = mk(12);
    XMM13 = mk(13);
    XMM14 = mk(14);
    XMM15 = mk(15);

    registers = new X86RegOpnd[]{
      XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7,
      XMM8, XMM9, XMM10, XMM11, XMM12, XMM13, XMM14, XMM15
    };
  }

  public static int getNumRegisters () {
    return NUM_REGIXMMERS;
  }

  public static X86RegOpnd getRegister (int regNum) {
    assert (regNum > -1 && regNum < NUM_REGIXMMERS) : "invalid float register number";
    return registers[regNum];
  }

  public static String getRegisterName (int i) {
    return "XMM(" + i + ")";
  }

  private static X86RegOpnd mk (int num) {
    return new X86RegOpnd(String.format("xmm%d", num), 128);
  }
}
