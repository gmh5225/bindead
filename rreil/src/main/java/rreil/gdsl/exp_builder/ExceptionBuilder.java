package rreil.gdsl.exp_builder;

import gdsl.rreil.exception.IException;
import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Builder;

public class ExceptionBuilder extends Builder<String> implements IException {
  private String exception;

  public ExceptionBuilder(BuildingStateManager manager, String exception) {
    super(manager);
    this.exception = exception;
  }

  @Override
  public BuildResult<? extends String> build() {
    return result(exception);
  }

  @Override
  public ExceptionBuilder size(int size) {
    throw new RuntimeException("No size field");
  }

  @Override
  public int getSize() {
    throw new RuntimeException("No size field");
  }
}
