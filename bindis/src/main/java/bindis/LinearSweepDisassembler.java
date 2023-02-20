package bindis;


/**
 * Interface for linear sweep disassemblers.
 *
 * @param <T> The type of the returned instructions.
 *
 * @author Bogdan Mihaila
 */
public interface LinearSweepDisassembler<T> {
  /**
   * Decode one instruction.
   *
   * @param in A byte stream containing the code to be disassembled and pointing at the next location to be read
   * @param pc The value of the program counter (address) for the decoded instruction
   * @return The decoded instruction
   */
  public T decodeOne (DecodeStream in, long pc);

  /**
   * Decode one instruction.
   *
   * @param code A byte array containing the code to be disassembled
   * @param offset The offset into the byte array at which the disassembly should begin
   * @param pc The value of the program counter (address) for the decoded instruction
   * @return The decoded instruction
   */
  public T decodeOne (byte[] code, int offset, long pc);

  /**
   * Continuously decode instructions until a certain condition is met.
   *
   * @param code A byte array containing the code to be disassembled
   * @param offset The offset into the byte array at which the disassembly should begin
   * @param startPc The value of the program counter (address) for the first decoded instruction
   * @param cb The callback will signal depending on the current instruction if the disassembly should continue or not.
   *          Additionally the callback must be used to collect the disassembled instructions
   */
  public void decodeLinearSweep (byte[] code, int offset, long startPc, Callback<? super T> cb);

  /**
   * Continuously decode instructions until a certain condition is met.
   *
   * @param in A byte stream containing the code to be disassembled and pointing at the next location to be read
   * @param startPc The value of the program counter (address) for the first decoded instruction
   * @param cb The callback will signal depending on the current instruction if the disassembly should continue or not.
   *          Additionally the callback must be used to collect the disassembled instructions
   */
  public void decodeLinearSweep (DecodeStream in, long startPc, Callback<? super T> cb);

}