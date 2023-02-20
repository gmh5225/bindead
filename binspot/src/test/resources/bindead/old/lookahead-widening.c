
#include <stdlib.h>

int main(int argc, char** argv) {
   int x = 0;
   int y = 0;

   while (1) {
      if (x <= 50)
         y++;
      else
         y--;
      if (y < 0)
         break;
      x++;
   }
   return x;
}