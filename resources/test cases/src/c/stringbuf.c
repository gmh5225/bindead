
#include <stdio.h>

int main (int argc, char** argv) {
	char *p;
	char t[33] = "AAAAAAABBBBBBBCCCDDDDD";
	char *u = argv[0];
	p = t+4;
	int i = 0;
	while (*p=*u) {i++; p++; u++;};
	return i;
}