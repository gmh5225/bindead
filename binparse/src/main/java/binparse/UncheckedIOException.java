package binparse;

import java.io.IOException;

import javalx.exceptions.UncheckedExceptionWrapper;

/**
 * A wrapper for the IOException that does not have to be declared and caught.
 * 
 * @author Bogdan Mihaila
 */
public class UncheckedIOException extends UncheckedExceptionWrapper {

	private static final long serialVersionUID = 1L;

	public UncheckedIOException(IOException exception) {
		super(exception);
	}

}
