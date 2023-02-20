int switch_middle(int param) {
  int retval = 0;
  
  switch(param) {
    case 1: retval = 11; break;
    case 2: retval = 22; break;
    case 3: retval = 33; break;
    case 4: retval = 44; break;
    case 5: retval = 55; break;
    default: retval = 666;
  }

  return retval;
}

int main(int argc, char **argv) {
  // using argc to suppress GCC's 
  // interproc constant propagation
  int parameter = argc;
  int result = 0;
  result += switch_middle(parameter);
  return result;
}