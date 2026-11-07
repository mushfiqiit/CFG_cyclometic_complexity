#include <stdio.h>

int main(void) {
    int x = 5, y;
    for (int i = 0; i < 3; i++) {
        y = i;
    }
    if (x > y) {
        y = x;
    }
    return 0;
}
