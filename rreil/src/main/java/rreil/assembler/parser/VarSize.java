package rreil.assembler.parser;


public abstract class VarSize {
  public static final VarSize NOT_SET = new IntegerSize(null, -1);
  private final Token token;

  public VarSize (Token token) {
    this.token = token;
  }

  public abstract boolean isTemlateVar ();

  public abstract Integer asInteger ();

  public boolean isSet () {
    return token != null;
  }

  public Token getToken () {
    return token;
  }


  public static class IntegerSize extends VarSize {
    private final Integer val;

    public IntegerSize (Token token, Integer val) {
      super(token);
      this.val = val;
    }

    @Override public boolean isTemlateVar () {
      return false;
    }

    @Override public Integer asInteger () {
      return val;
    }
  }


  public static class TemplateSize extends VarSize {

    public TemplateSize (Token token) {
      super(token);
    }

    @Override public boolean isTemlateVar () {
      return true;
    }

    @Override public Integer asInteger () {
      return -1;
    }
  }
}
