/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
package bindis.x86.x32;

import bindis.x86.common.X86RegOpnd;

public class X32Registers {
  public static final int NUM_REGISTERS = 8;
  public static final X86RegOpnd EAX;
  public static final X86RegOpnd ECX;
  public static final X86RegOpnd EDX;
  public static final X86RegOpnd EBX;
  public static final X86RegOpnd ESP;
  public static final X86RegOpnd EBP;
  public static final X86RegOpnd ESI;
  public static final X86RegOpnd EDI;
  public static final X86RegOpnd AX;
  public static final X86RegOpnd CX;
  public static final X86RegOpnd DX;
  public static final X86RegOpnd BX;
  public static final X86RegOpnd SP;
  public static final X86RegOpnd BP;
  public static final X86RegOpnd SI;
  public static final X86RegOpnd DI;
  public static final X86RegOpnd AL;
  public static final X86RegOpnd CL;
  public static final X86RegOpnd DL;
  public static final X86RegOpnd BL;
  public static final X86RegOpnd AH;
  public static final X86RegOpnd CH;
  public static final X86RegOpnd DH;
  public static final X86RegOpnd BH;
  private static X86RegOpnd registers8[];
  private static X86RegOpnd registers16[];
  private static X86RegOpnd registers32[];

  static {
    EAX = new X86RegOpnd("eax", 32);
    ECX = new X86RegOpnd("ecx", 32);
    EDX = new X86RegOpnd("edx", 32);
    EBX = new X86RegOpnd("ebx", 32);
    ESP = new X86RegOpnd("esp", 32);
    EBP = new X86RegOpnd("ebp", 32);
    ESI = new X86RegOpnd("esi", 32);
    EDI = new X86RegOpnd("edi", 32);

    AX = new X86RegOpnd("ax", 16);
    CX = new X86RegOpnd("cx", 16);
    DX = new X86RegOpnd("dx", 16);
    BX = new X86RegOpnd("bx", 16);
    SP = new X86RegOpnd("sp", 16);
    BP = new X86RegOpnd("bp", 16);
    SI = new X86RegOpnd("si", 16);
    DI = new X86RegOpnd("di", 16);

    AL = new X86RegOpnd("al", 8);
    CL = new X86RegOpnd("cl", 8);
    DL = new X86RegOpnd("dl", 8);
    BL = new X86RegOpnd("bl", 8);
    AH = new X86RegOpnd("ah", 8);
    CH = new X86RegOpnd("ch", 8);
    DH = new X86RegOpnd("dh", 8);
    BH = new X86RegOpnd("bh", 8);

    registers32 =
        new X86RegOpnd[]{
      EAX, ECX, EDX, EBX, ESP, EBP, ESI, EDI
    };
    registers16 =
        new X86RegOpnd[]{
      AX, CX, DX, BX, SP, BP, SI, DI
    };
    registers8 =
        new X86RegOpnd[]{
      AL, CL, DL, BL, AH, CH, DH, BH
    };
  }

  public static int getNumberOfRegisters () {
    return NUM_REGISTERS;
  }

  public static X86RegOpnd getRegister8 (int regNum) {
    assert (regNum > -1 && regNum < NUM_REGISTERS) : "invalid integer register number";
    return registers8[regNum];
  }

  public static X86RegOpnd getRegister16 (int regNum) {
    assert (regNum > -1 && regNum < NUM_REGISTERS) : "invalid integer register number";
    return registers16[regNum];
  }

  public static X86RegOpnd getRegister32 (int regNum) {
    assert (regNum > -1 && regNum < NUM_REGISTERS) : "invalid integer register number";
    return registers32[regNum];
  }

  //Return the 32bit register name
  public static String getRegisterName (int regNum) {
    assert (regNum > -1 && regNum < NUM_REGISTERS) : "invalid integer register number";
    return registers32[regNum].toString();
  }
}
