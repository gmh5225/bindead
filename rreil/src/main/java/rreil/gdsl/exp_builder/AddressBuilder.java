package rreil.gdsl.exp_builder;

import gdsl.rreil.IAddress;
import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;
import rreil.gdsl.builder.Builder;
import rreil.gdsl.exp_builder.linear.LinearBuilder;
import rreil.lang.Rhs.Rval;

/**
 * This class is used to build an {@link Rval} object that represents
 * a Gdsl RReil address AST node.
 * 
 * @author Julian Kranz
 */
public class AddressBuilder extends Builder<Rval> implements IAddress {
  private LinearBuilder address;

  /**
   * Construct the builder object.
   * 
   * @param manager the RReil address manager to use
   * @param size the size of the address, i.e. the pointer size
   * @param address a builder that builds the linear expression calculating the address
   */
  public AddressBuilder(BuildingStateManager manager, int size,
      LinearBuilder address) {
    super(manager);
    this.address = address.size(size);
  }

  @Override
  public BuildResult<? extends Rval> build() {
    return address.buildRval();
  }

  @Override
  public Builder<Rval> size(int size) {
    return this;
  }

  @Override
  public int getSize() {
    throw new RuntimeException("No size field :-(");
  }

}
