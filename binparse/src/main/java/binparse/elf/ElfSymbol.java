package binparse.elf;

import binparse.AbstractSymbol;

/**
 * A symbol in an ELF file.
 */
class ElfSymbol extends AbstractSymbol {
  private final Type type;
  private final BindingType bindingType;

  public ElfSymbol (String name, long address, long size, byte[] data, Type type, BindingType bindingType) {
    super(name, address, size, data);
    this.type = type;
    this.bindingType = bindingType;
  }


  public Type getType () {
    return type;
  }

  public BindingType getBindingType () {
    return bindingType;
  }

  /**
   * Symbol types taken from the ELF specification.
   */
  public enum Type {
    /**
     * The symbol type is not specified.
     */
    None,

    /**
     * This symbol is associated with a data object, such as a variable, an array, and so forth.
     */
    Object,

    /**
     * This symbol is associated with a function or other executable code.
     */
    Function,

    /**
     * This symbol is associated with a section. Symbol table entries of this type exist primarily for relocation and
     * normally have STB_LOCAL binding.
     */
    Section,

    /**
     * Conventionally, the symbol's name gives the name of the source file that is associated with the object file. A
     * file symbol has STB_LOCAL binding and a section index of SHN_ABS. This symbol, if present, precedes the other
     * STB_LOCAL symbols for the file.
     *
     * Symbol index 1 of the SHT_SYMTAB is an STT_FILE symbol representing the object file. Conventionally, this symbol
     * is followed by the files STT_SECTION symbols. These section symbols are then followed by any global symbols that
     * have been reduced to locals.
     */
    File,

    /**
     * This symbol labels an uninitialized common block. This symbol is treated exactly the same as STT_OBJECT.
     */
    Common,

    /**
     * The symbol specifies a thread-local storage entity. When defined, this symbol gives the assigned offset for the
     * symbol, not the actual address.
     *
     * Thread-local storage relocations can only reference symbols with type STT_TLS. A reference to a symbol of type
     * STT_TLS from an allocatable section, can only be achieved by using special thread-local storage relocations.
     * See Chapter 8, Thread-Local Storage for details. A reference to a symbol of type STT_TLS from a non-allocatable
     * section does not have this restriction.
     */
    ThreadLocalStorage,

    NON_EXISTENT_7,
    NON_EXISTENT_8,
    NON_EXISTENT_9,
    OS_Specific_10,
    OS_Specific_11,
    OS_Specific_12,
    SPARC_Register,
    CPU_Specific_14,
    CPU_Specific_15,
  }

  /**
   * Symbol binding types taken from the ELF specification.
   * @author Bogdan Mihaila
   */
  public enum BindingType {
    /**
     * Local symbol. These symbols are not visible outside the object file containing their definition. Local symbols
     * of the same name can exist in multiple files without interfering with each other.
     */
    Local,

    /**
     * Global symbols. These symbols are visible to all object files being combined. One file's definition of a global
     * symbol satisfies another file's undefined reference to the same global symbol.
     */
    Global,

    /**
     * Weak symbols. These symbols resemble global symbols, but their definitions have lower precedence.
     */
    Weak,

    NON_EXISTENT_3,
    NON_EXISTENT_4,
    NON_EXISTENT_5,
    NON_EXISTENT_6,
    NON_EXISTENT_7,
    NON_EXISTENT_8,
    NON_EXISTENT_9,
    OS_Specific_10,
    OS_Specific_11,
    OS_Specific_12,
    CPU_Specific_13,
    CPU_Specific_14,
    CPU_Specific_15,
  }

}
