
// @return eax \in [-1, 1]
int main() {
  int n;
  if (n >= -1 && n <= 1)
    // n \in [-1, +1]
    return n;
  return 0;
}
