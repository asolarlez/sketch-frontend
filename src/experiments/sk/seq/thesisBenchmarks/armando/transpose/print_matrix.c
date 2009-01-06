#include <stdio.h>

void transpose(int *mx, int *result) {
    for (int x = 0; x < 4; x++)
        for (int y = 0; y < 4; y++)
            result[4*x+y] = mx[4*y+x];
}

void print_matrix(int *matrix) {
    printf("[\n");
    for (int row = 0; row < 4; row++) {
        printf("%3d   %3d   %3d   %3d\n",
               matrix[4*row],
               matrix[4*row + 1],
               matrix[4*row + 2],
               matrix[4*row + 3]);
    }
    printf("]\n\n");
}

int main() {
    int matrix[16] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
    int result[16];
    transpose(matrix, result);
    print_matrix(matrix);
    print_matrix(result);
}
