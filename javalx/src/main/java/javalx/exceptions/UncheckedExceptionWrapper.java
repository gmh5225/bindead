package javalx.exceptions;

/**
 * A wrapper for checked Exceptions that does not have to be declared and
 * caught. Use it to wrap and propagate checked exceptions without having to
 * declare them for every method up the call hierarchy. Subclass this to handle
 * your exception by making it an own type.<br>
 * Note that this exception is a lightweight wrapper as it does not use the
 * caused by exception chain to not add to the clutter of "caused by" cascades.
 * Beware that this could mislead which exception to catch just by reading an
 * exception stacktrace. Use own subclasses to make sure you know which
 * exception type to catch.
 * 
 * @author Bogdan Mihaila
 */
public class UncheckedExceptionWrapper extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private final Throwable exception;

	public UncheckedExceptionWrapper(Throwable exception) {
		this.exception = exception;
		setStackTrace(exception.getStackTrace());
	}

	/**
	 * Retrieve the exception that is wrapped by this one.
	 */
	public Throwable getWrappedException() {
		return exception;
	}

	@Override
	public String toString() {
		String message = getLocalizedMessage();
		StringBuilder builder = new StringBuilder();
		if (message == null)
			message = getWrappedException().toString();
		builder.append(message);
		builder.append("\nWrapped in exception: ");
		builder.append(getClass().getName());
		return builder.toString();
	}

}
