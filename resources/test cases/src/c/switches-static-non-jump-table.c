int switch_small() {
  int param = 3;
  int retval = 0;
  
  switch(param) {
    case 1: retval = 11; break;
    case 2: retval = 22; break;
    case 3: retval = 33; break;
    case 4: retval = 44; break;
    default: retval = 666;
  }

  return retval;
}

// this would be translated to a jump table by GCC as the values are close and not too many
// int switch_middle() {
//   int param = 5;
//   int retval = 0;
//   
//   switch(param) {
//     case 1: retval = 11; break;
//     case 2: retval = 22; break;
//     case 3: retval = 33; break;
//     case 4: retval = 44; break;
//     case 5: retval = 55; break;
//     default: retval = 666;
//   }
// 
//   return retval;
// }

int switch_big() {
  int param = 2001;
  int retval = 0;
  
  switch(param) {
    case 1: retval = 11; break;
    case 200: retval = 22; break;
    case 3000: retval = 33; break;
    case 40000: retval = 44; break;
    case 500000: retval = 55; break;
    default: retval = 666;
  }

  return retval;
}

int main (void) {
  int result = 0;
  result += switch_small();
//   result += switch_middle();
  result += switch_big();
  return result;
}