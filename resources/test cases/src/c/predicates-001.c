
// @return eax \in [0, 9]
int main() {
  int n;
  if (n > 0 && n < 10)
    // n \in [1, 9]
    return n;
  return 0;
}
