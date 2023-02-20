#include "../include/reach.h"
int a[20]={0};

int main(){
   int x,y;
/*    ASSUME(x >= 0); */
/*    ASSUME(y >= 0); */
/*    ASSUME(x< 9); */
/*    ASSUME(y < 10); */
   if(x>=0 || y>=0 || x<9||y<10)
     return 1;
   if (x * y - x*x >= 50){
      x=x+1;
   }
   if(x>=20)
     UNREACHABLE();
   a[x]=1;// no overflow
   return 1;
}
