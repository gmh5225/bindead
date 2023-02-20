package rreil.gdsl.builder.statement;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.ExceptionBuilder;
import rreil.lang.RReil;
import rreil.lang.util.RReilFactory;

public class ThrowBuilder extends StatementBuilder {
  ExceptionBuilder exception;

  public ThrowBuilder(BuildingStateManager manager, ExceptionBuilder exception) {
    super(manager);
    this.exception = exception;
  }

  @Override
  public BuildResult<? extends RReil> build() {
    RReil expRReil = RReilFactory.instance.throw_(manager.nextAddress(), exception.build()
        .getResult());
    return result(expRReil);
  }
}
