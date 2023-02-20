int switch_middle() {
  int param = 5;
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

int main (void) {
  int result = 0;
  result += switch_middle();
  return result;
}