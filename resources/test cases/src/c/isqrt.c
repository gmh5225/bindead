
#include <stdlib.h>

int main(int n, char **argv)
{
  int x = 1;
  // n \in [0, 10]
  while (x * x <= 100)
    x++;
  return x--;
}
