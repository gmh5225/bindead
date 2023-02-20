
#include <avr/io.h>

typedef int8_t i8;

// @return eax \in [0, 9]
i8 main() {
  i8 n = PORTB;
  if (n > 0 && n < 10)
    // n \in [1, 9]
    return n;
  return 0;
}
