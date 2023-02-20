
#include <stdlib.h>

int main(int argc, char** argv) {
   int a[10],n,m,c=0,l,u,mid;

   n=10;
   m=argc;
   l=0,u=n-1;
   while(l<=u) {
      mid=(l+u)/2;
      if (m==a[mid]) {
         c=1;
         break;
      } else if (m<a[mid]) {
         u=mid-1;
      } else
         l=mid+1;
   }
   return c;
}