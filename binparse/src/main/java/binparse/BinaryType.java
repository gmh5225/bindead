package binparse;

/**
 * The various types that a collection of binary code can represent (e.g. executable, library).
 */
public enum BinaryType {
  Executable,
  SharedLibrary,
  Object,
  CoreDump,
  TraceDump
}