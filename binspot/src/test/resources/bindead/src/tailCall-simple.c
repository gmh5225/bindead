#include <stdlib.h>
#include <stdio.h>

int f (int i);
int g (int i);
int h (int i);

int f (int i) {
	int array[i];
	struct {
		int x;
		int y;
	} point = {0, 0};

	for (int j = 0; j < i; j++) {
		array[j] = j;
		point.x = j;
	}
	if (i > 2)
		return point.x;
//   printf("in array[i-1]\n", array[i-1]+point.x);
}

int g (int i) {
//   printf("in g(%i)\n", i);
	f(i + 1);
//   printf("after recursive call to f\n");
	return h(i + 1);
}

int h (int i) {
	for (int j = 0; j < i; j++) {
		i = (i & 0x004537) - j;
	}
	return f(i);
}

int main (void) {
	int i = 20;
//     printf("in main()\n");
	return g(i);
}
