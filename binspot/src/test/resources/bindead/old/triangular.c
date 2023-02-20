
#include <stdlib.h>

int main(int argc, char** argv) {
   int x, y;
   for (y = 0, x = 0; x<100; x++) {
      if (argc == 1)
         y++;
   }
   return y;
}
