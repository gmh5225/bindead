package rreil.lang.lowlevel;

public enum OperandSize {
  BIT,
  BYTE,
  WORD,
  DWORD,
  QWORD,
  OWORD;

  public int getBits () {
    return this == BIT ? 1 : getSizeInBytes() * 8;
  }

  public int getSizeInBytes () {
    switch (this) {
      case BIT:
      case BYTE:
        return 1;
      case WORD:
        return 2;
      case DWORD:
        return 4;
      case QWORD:
        return 8;
      case OWORD:
        return 16;
      default:
        throw new IllegalStateException("Unknown size: " + this);
    }
  }
}
