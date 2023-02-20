package binparse;

import java.io.IOException;

/**
 * Interface to be able to instantiate various binary types.
 */
public interface BinaryFactory {
  public Binary getBinary (String path) throws IOException;
}
