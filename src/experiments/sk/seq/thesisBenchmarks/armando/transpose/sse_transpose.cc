/**
 * using Mark Murphy's sketch "transpose.sk"
 * converted to c by Nicholas Tung (ntung@ntung.com)
 * compile with g++ -g -O3 -msse -o sse_transpose sse_transpose.cc
 */

#include <assert.h>
#include <stdio.h>
#include <xmmintrin.h>
#include <sys/time.h>

#define SSE_SHUFFLE(a, b, c) _mm_shuffle_ps(a, b, c)
#define SHUFFLE_BITS(a, b, c, d) _MM_SHUFFLE(d, c, b, a)
#define NUM_TRIALS (1 << 30)
#define MANUAL_UNROLL_4(a) a; a; a; a;
#define FLOAT_SECS(from, to) ({struct timeval diff; \
    timersub(to, from, &diff); \
    diff.tv_sec + 1e-6 * (float)diff.tv_usec; })

/// specification; separate result matrix is used as memory is
/// usually plentiful (don't need to copy back to mx)
inline void transpose(float *mx, float *result) {
    for (int x = 0; x < 4; x++)
        for (int y = 0; y < 4; y++)
            result[4*x+y] = mx[4*y+x];
}

/// transpose, replacing the original result.
/// note that when casting bit vectors to int's, earlier
/// bits have lower significance.
inline void sse_transpose(__m128 *in) {
// python to generate codes; grep for mx, p0, p1 for offsets
// a = ["01000001", "11101011", "00010100", "11101011",
//      "11001100", "00110110", "10011100", "01100110"]
// print "\n".join([repr(tuple([int(e[0]) + 2*int(e[1])
//                              for e in zip(c[::2], c[1::2])])) for c in a])
    __m128 tmp[4];
    tmp[0]  = SSE_SHUFFLE(in[3], in[2],  SHUFFLE_BITS(2, 0, 0, 2));
    tmp[2]  = SSE_SHUFFLE(in[3], in[2],  SHUFFLE_BITS(3, 1, 1, 3));
    tmp[1]  = SSE_SHUFFLE(in[0], in[1],  SHUFFLE_BITS(0, 2, 2, 0));
    tmp[3]  = SSE_SHUFFLE(in[1], in[0],  SHUFFLE_BITS(3, 1, 1, 3));

    in[3]   = SSE_SHUFFLE(tmp[3], tmp[2], SHUFFLE_BITS(3, 0, 3, 0));
    in[0]   = SSE_SHUFFLE(tmp[1], tmp[0], SHUFFLE_BITS(0, 3, 2, 1));
    in[2]   = SSE_SHUFFLE(tmp[1], tmp[0], SHUFFLE_BITS(1, 2, 3, 0));
    in[1]   = SSE_SHUFFLE(tmp[3], tmp[2], SHUFFLE_BITS(2, 1, 2, 1));
}

void print_matrix(float *m) {
    for (int row = 0; row < 4; row++) {
        printf("%c%4.1f   %4.1f   %4.1f   %4.1f%s\n",
               row == 0 ? '[' : ' ',
               m[4*row], m[4*row + 1], m[4*row + 2], m[4*row + 3],
               row == 3 ? " ]" : ",");
    }
}

void run_spec(float *matrix) {
    printf("=== Specification calculation ===\n");
    static float result[16] __attribute__ ((aligned(16)));
    for (int a = 0; a < NUM_TRIALS / 4; a++) {
        MANUAL_UNROLL_4(transpose(matrix, result));
    }
    print_matrix(matrix);
    print_matrix(result);
}

void run_sse(float *matrix) {
    static float result[16] __attribute__ ((aligned(16)));
    printf("\n=== SSE calculation ===\n");
    __m128 matrix_as_sse[4];
    // in the current implementation, this could just be a memcpy
    for (int a = 0; a < 4; a++) {
        matrix_as_sse[a] = _mm_load_ps(&matrix[4*a]);
    }

    for (int a = 0; a < NUM_TRIALS / 4; a++) {
        MANUAL_UNROLL_4(sse_transpose(matrix_as_sse));
    }

    // convert back to matrix; each __m128 needs to be 16b aligned
    for (int a = 0; a < 4; a++) {
        float *ptr = &result[4*a];
        assert((((long)ptr) & 0xf) == 0);
        _mm_store_ps(ptr, matrix_as_sse[a]);
    }
    print_matrix(matrix);
    print_matrix(result);
    printf("value[0] = %f\n", result[1]);
}

int main() {
    // NOTE - need to align 16 so conversion to __m128 to float works
    float matrix[16] __attribute__ ((aligned(16)));
    for (int a = 0; a < 16; a++) {
        matrix[a] = a;
    }
    struct timeval begin, spec_end, sse_end;
    gettimeofday(&begin, NULL);
    run_spec(matrix);
    gettimeofday(&spec_end, NULL);
    run_sse(matrix);
    gettimeofday(&sse_end, NULL);
    printf("spec time   = %6.2f\n", FLOAT_SECS(&begin, &spec_end));
    printf("sse time    = %6.2f\n", FLOAT_SECS(&spec_end, &sse_end));
    printf("num trials  = %d\n", (NUM_TRIALS / 8 - 1));
}
