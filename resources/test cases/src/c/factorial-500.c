int fac (int n) {
  if (n <= 0)
    return 1;
  else
    return n * fac (n - 1);
}

int main (void) {
  int value = 500;
  return fac (value);
}