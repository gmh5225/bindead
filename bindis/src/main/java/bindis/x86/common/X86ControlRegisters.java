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
public class X86ControlRegisters {
  public static final int NUM_MMX_REGISTERS = 8;
  public static final X86RegOpnd CR0;
  public static final X86RegOpnd CR1;
  public static final X86RegOpnd CR2;
  public static final X86RegOpnd CR3;
  public static final X86RegOpnd CR4;
  public static final X86RegOpnd CR5;
  public static final X86RegOpnd CR6;
  public static final X86RegOpnd CR7;
  private static X86RegOpnd controlRegisters[];

  static {
    //Control registers
    CR0 = new X86RegOpnd("cr0", 32);
    CR1 = new X86RegOpnd("cr1", 32);
    CR2 = new X86RegOpnd("cr2", 32);
    CR3 = new X86RegOpnd("cr3", 32);
    CR4 = new X86RegOpnd("cr4", 32);
    CR5 = new X86RegOpnd("cr5", 32);
    CR6 = new X86RegOpnd("cr6", 32);
    CR7 = new X86RegOpnd("cr7", 32);

    controlRegisters =
        new X86RegOpnd[]{
      CR0, CR1, CR2, CR3, CR4, CR5, CR6, CR7
    };
  }

  public static int getNumberOfRegisters () {
    return NUM_MMX_REGISTERS;
  }

  public static String getRegisterName (int regNum) {
    assert (regNum > -1 && regNum < NUM_MMX_REGISTERS) : "invalid control register number";
    return controlRegisters[regNum].toString();
  }

  public static X86RegOpnd getRegister (int regNum) {
    assert (regNum > -1 && regNum < NUM_MMX_REGISTERS) : "invalid control register number";
    return controlRegisters[regNum];
  }
}
