#include <stdlib.h>
#include <stdio.h>

int f (int i);
int g (int i);
int h (int i);

int f (int i) {
	int array[16];
	struct {
		int x;
		int y;
	} point = {0, 0};

	for (int j = 0; j < 16; j++) {
		array[j] = i;
		point.x = j;
	}
	if (i > 2)
		return point.x;
}

int g (int i) {
	f(i + 1);
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
	return g(i);
}
