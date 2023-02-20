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
public class X86DebugRegisters {
  public static final int NUM_MMX_REGISTERS = 8;
  public static final X86RegOpnd DR0;
  public static final X86RegOpnd DR1;
  public static final X86RegOpnd DR2;
  public static final X86RegOpnd DR3;
  public static final X86RegOpnd DR4;
  public static final X86RegOpnd DR5;
  public static final X86RegOpnd DR6;
  public static final X86RegOpnd DR7;
  private static X86RegOpnd controlRegisters[];

  static {
    //Control registers
    DR0 = new X86RegOpnd("dr0", 32);
    DR1 = new X86RegOpnd("dr1", 32);
    DR2 = new X86RegOpnd("dr2", 32);
    DR3 = new X86RegOpnd("dr3", 32);
    DR4 = new X86RegOpnd("dr4", 32);
    DR5 = new X86RegOpnd("dr5", 32);
    DR6 = new X86RegOpnd("dr6", 32);
    DR7 = new X86RegOpnd("dr7", 32);

    controlRegisters =
      new X86RegOpnd[]{
        DR0, DR1, DR2, DR3, DR4, DR5, DR6, DR7
      };
  }

  public static int getNumberOfRegisters () {
    return NUM_MMX_REGISTERS;
  }

  public static String getRegisterName (int regNum) {
    assert (regNum > -1 && regNum < NUM_MMX_REGISTERS) : "invalid debug register number";
    return controlRegisters[regNum].toString();
  }

  public static X86RegOpnd getRegister (int regNum) {
    assert (regNum > -1 && regNum < NUM_MMX_REGISTERS) : "invalid debug register number";
    return controlRegisters[regNum];
  }
}
