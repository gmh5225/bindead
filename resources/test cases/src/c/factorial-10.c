int fac (int n) {
  if (n <= 0)
    return 1;
  else
    return n * fac (n - 1);
}

int main (void) {
  int value = 10;
  return fac (value);
}