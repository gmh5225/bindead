int global = 3;

int main (int argc, char** argv) {
  if (argc == 3)
    global = 1;
  else
    global = 2;
  return global;
}
