int switch_middle_nested(int par1, int par2) {
  int retval = 0;
  
  switch(par1) {
    case 1:
      switch(par2) {
	case 10: retval = 111; break;
	case 20: retval = 222; break;
	case 30: retval = 333; break;
	case 40: retval = 444; break;
	case 50: retval = 555; break;
	default: retval = 6666;
      }
      break;
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
  result += switch_middle_nested(parameter, parameter + 5);
  return result;
}