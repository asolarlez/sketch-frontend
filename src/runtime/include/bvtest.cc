#include "bitvec.h"

int
main (int argc, char **argv)
{
    bitvec<35> bv1 = 0;
    cout << "bv1=" << bv1 << endl;

    bitvec<35> bv2;
    bv2.reset (true);
    cout << "bv2=" << bv2 << endl;

    bitvec<35> bv3 = 42;
    cout << "bv3=" << bv3 << endl;

    bitvec<35> bv4 = bv1 + bv3;
    cout << "bv4=" << bv4 << endl;

    bitvec<35> bv5 = bv2 + bv3;
    cout << "bv5=" << bv5 << endl;

    bitvec<35> bv6 = bv5 ^ bv2;
    cout << "bv6=" << bv6 << endl;

    bitvec<35> bv7 = bv6 & bv4;
    cout << "bv7=" << bv7 << endl;

    bitvec<35> bv8 = bv5 | bv7;
    cout << "bv8=" << bv8 << endl;

    bitvec<35> bv9 = bv6 >> 4;
    cout << "bv9=" << bv9 << endl;

    bitvec<15> bv10 = bv9.sub<15> (18);
    cout << "bv10=" << bv10 << endl;

    bitvec<35> bv11;
    bv11[3] = bv10;
    cout << "bv11=" << bv11 << endl;

    return 0;
}

