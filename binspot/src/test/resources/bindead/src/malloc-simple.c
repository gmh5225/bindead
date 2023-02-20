#include <stdlib.h>

int main(int argc, char **argv) {
	char* knownSizeField = malloc(16);
	char* unknownSizeField = malloc(argc);
	return (long) knownSizeField;
}

