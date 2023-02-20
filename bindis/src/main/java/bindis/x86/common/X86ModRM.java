package bindis.x86.common;

import bindis.DecodeStream;

/**
 * ModRM operand format decode helper.
 *
 * @author mb0
 */
public class X86ModRM {
  public static final int ModRM_MOD_OFFSET = 6;
  public static final int ModRM_MOD_MASK = 3;
  public static final int ModRM_REG_OR_OPCODE_OFFSET = 3;
  public static final int ModRM_REG_OR_OPCODE_MASK = 7;
  public static final int ModRM_RM_MASK = 7;
  public final int ModRM;

  private X86ModRM (int ModRM) {
    this.ModRM = ModRM;
  }

  public int getModRM () {
    return ModRM;
  }

  public int getRegOrOpcode () {
    return (ModRM >> ModRM_REG_OR_OPCODE_OFFSET) & ModRM_REG_OR_OPCODE_MASK;
  }

  public int getMod () {
    return (ModRM >> ModRM_MOD_OFFSET) & ModRM_MOD_MASK;
  }

  public int getRm () {
    return ModRM & ModRM_RM_MASK;
  }

  public static X86ModRM decode (DecodeStream in) {
    return new X86ModRM(in.read8());
  }

  public static X86ModRM peek (DecodeStream in) {
    return new X86ModRM(in.peek8());
  }
}
