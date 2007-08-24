#ifndef __BITVEC_H
#define __BITVEC_H

#include <cassert>
#include <iostream>
#include <climits>

#define WORDSIZE  (sizeof (unsigned))
#define WORDBITS  (WORDSIZE << 3)
#define BITVECNWORDS(n)  (((n) + WORDBITS - 1) / WORDBITS)

using namespace std;


inline int
word_copy (unsigned &wtag, unsigned w1, unsigned w2, unsigned &carry) {
    wtag = w2;
    return 0;
}

inline int
word_or (unsigned &wtag, unsigned w1, unsigned w2, unsigned &carry) {
    wtag = w1 | w2;
    return 0;
}

inline int
word_and (unsigned &wtag, unsigned w1, unsigned w2, unsigned &carry) {
    wtag = w1 & w2;
    return 0;
}

inline int
word_xor (unsigned &wtag, unsigned w1, unsigned w2, unsigned &carry) {
    wtag = w1 ^ w2;
    return 0;
}

inline int
word_cmp (unsigned &wtag, unsigned w1, unsigned w2, unsigned &carry) {
    return (w1 == w2 ? 0 : (w1 > w2) ? 1 : -1);
}

inline int
word_add (unsigned &wtag, unsigned w1, unsigned w2, unsigned &carry)
{
    const unsigned half_shift = WORDBITS >> 1;
    const unsigned lshalf_mask = (1 << half_shift) - 1;
    const unsigned mshalf_mask = lshalf_mask << half_shift;
    unsigned w1_lshalf = w1 & lshalf_mask;
    unsigned w1_mshalf = (w1 & mshalf_mask) >> half_shift;
    unsigned w2_lshalf = w2 & lshalf_mask;
    unsigned w2_mshalf = (w2 & mshalf_mask) >> half_shift;


    assert (carry < 2);  /* Addition carry is either 0 or 1. */

    unsigned result = w1_lshalf + w2_lshalf + carry;
    unsigned result_lshalf = result & lshalf_mask;
    unsigned result_mshalf = (result & mshalf_mask) >> half_shift;

    result = w1_mshalf + w2_mshalf + result_mshalf;
    result_mshalf = result & lshalf_mask;
    carry = (result & mshalf_mask) >> half_shift;

    result = (result_mshalf << half_shift) | result_lshalf;

    wtag = result;
    return 0;
}

class bitmask {
private:
    unsigned m;

public:
    inline bitmask (unsigned a_m) : m (a_m) { }
    inline bitmask (unsigned length, unsigned shift) {
	assert (length + shift <= WORDBITS);
	/* FIXME for some reason, 1 << 32 evaluates to 1
	 * (instead of 0); hence the special casing below. */
	m = (length < WORDBITS ?
	     (((unsigned) 1 << length) - 1) << shift : (unsigned) -1);
    }

    inline operator unsigned (void) const {
	return m;
    }
};


class const_bitref {
protected:
    unsigned *w;
    size_t i;
    unsigned m;

public:
    inline const_bitref (const unsigned *a_w, size_t a_i) {
	assert (a_w);
	w = (unsigned *) a_w + a_i / WORDBITS;
	i = a_i & (WORDBITS - 1);
	m = 1 << i;
    }

    inline operator bool (void) const {
	return ((*w & m) != 0);
    }    
};

class bitref : public const_bitref {
public:
    inline bitref (unsigned *a_w, size_t a_i) : const_bitref (a_w, a_i) { }

    inline const bitref &operator= (const bool b) {
	unsigned w_old = *w;
	*w = (b ? (w_old | m) : (w_old & ~m));
	return *this;
    }

    inline const bitref &operator= (const bitref &br) {
	return *this = (bool) br;
    }

    inline void flip (void) {
	*w ^= m;
    }
};


template <size_t N>
class bitvec {
private:
    unsigned v[BITVECNWORDS(N)];

    /*
     * Accessors / mutators.
     */
public:
    //struct bitvec *bitvec_set_range (const size_t, const size_t, const bit);

    //struct bitvec *bitvec_cpy_offset (const size_t, const struct bitvec *,
	//			      const size_t, const size_t);

    inline const size_t nwords (void) const {
	return (sizeof (v) / WORDSIZE);
    }

    inline const size_t nbits (void) const {
	return N;
    }

    /*
     * Initializers.
     */
private:
    inline void init (bool bit = false) const;

    template <size_t Ntag>
	int iter (const bitvec<Ntag> &bv, size_t offset,
	    	  int (*func) (unsigned &, unsigned, unsigned, unsigned &),
		  unsigned carry_init);


    /*
     * Operators.
     */
public:
    bitref operator[] (const size_t i);
    const_bitref operator[] (const size_t i) const;

    //template <int S> const bitvec range (size_t i) const;

    const bitvec<N> &operator= (const bitvec<N> &bv);
    /*const bitvec<N> &operator= (unsigned w);
    const bitvec<N> &operator= (bool bit); */
    const bitvec<N> &operator+= (const bitvec &bv);
    const bitvec<N> &operator|= (const bitvec<N> &bv);
    const bitvec<N> &operator&= (const bitvec<N> &bv);
    const bitvec<N> &operator^= (const bitvec<N> &bv);

    const bool operator==(const bitvec<N> bv2) const;
    const bool operator!=(const bitvec<N> bv2) const;
    const bool operator<(const bitvec<N> bv2) const;
    const bool operator<=(const bitvec<N> bv2) const;
    const bool operator>(const bitvec<N> bv2) const;
    const bool operator>=(const bitvec<N> bv2) const;

    const bitvec<N> operator+ (const bitvec<N> &bv2);
    const bitvec<N> operator| (const bitvec<N> &bv2);
    const bitvec<N> operator& (const bitvec<N> &bv2);
    const bitvec<N> operator^ (const bitvec<N> &bv2);
    const bitvec<N> operator<< (size_t s);
    const bitvec<N> operator>> (size_t s);

    /* XXX casting operator */
    //operator char *bitvec_string (char *, size_t, struct bitvec *);


    /*
     * Constructors.
     */
public:
    bitvec (bool val = false);
    //bitvec (const unsigned w);
    bitvec (const bitvec<N> &bv);
    bitvec (const const_bitref &bv);

    /*
     * Friends.
     */

    template <size_t Ntag> friend ostream &operator<< (ostream &out, const bitvec<Ntag> &bv);
};


class bitstream {
private:
    unsigned *v;
    size_t i;
    unsigned cache;
    bool flip;
    bool reversed;

public:
    inline bitstream (const unsigned *a_v, const size_t i = 0,
		      const bool a_flip = false,
		      const bool a_reversed = false);

    inline void advance (const size_t n);
    inline unsigned read (const size_t n);
};
 

bitstream::bitstream (const unsigned *a_v, const size_t a_i, bool a_flip,
		      bool a_reversed)
{
    /* FIXME make it work for reversed streams! */
    assert (! a_reversed);

    flip = a_flip;
    if ((v = (unsigned *) a_v)) {
	v += a_i / WORDBITS;
	i = a_i & (WORDBITS - 1);
	reversed = a_reversed;

	/* Cache head word of offset is non-zero. */
	if (i)
	    cache = *v;
    }
}

inline void
bitstream::advance (size_t n)
{
    size_t itag = i + n;
    size_t v_delta = itag / WORDBITS;
    
    v += v_delta;
    i = itag & (WORDBITS - 1);
}

unsigned
bitstream::read (size_t c)
{
    unsigned w = 0;
    size_t ctag, chead = 0;

    assert (c <= WORDBITS);

    /* Empty stream. */
    if (! v) {
	w = (flip ? (unsigned) -1 : 0);
	w &= bitmask (c, 0);
	return w;
    }

    /* Read offset heading word first (if such exists). */
    if (i) {
	ctag =  WORDBITS - i;
	if (c < ctag)
	    ctag = c;

	w = (cache & bitmask (ctag, i)) >> i;

	advance (ctag);
	c -= chead = ctag;
    }

    /* Read and cache aligned word (if required). */
    if (c) {
	w |= ((cache = *v) & bitmask (c, 0)) << chead;
	advance (c);
    }

    return w;
}



template <size_t N> inline void
bitvec<N>::init (bool bit) const
{
    *this = bit;
}

template <size_t N> template <size_t Ntag> int
bitvec<N>::iter (const bitvec<Ntag> &bv, size_t offset,
	    	 int (*func) (unsigned &, unsigned, unsigned, unsigned &),
	       	 unsigned carry_init)
{
    unsigned w, wtag, m;
    int ret;

    assert (offset + Ntag <= N);

    size_t c = Ntag;
    bitstream st (bv.v);
    unsigned carry = carry_init;

    /* Advance to first word for assigment. */
    unsigned *vtag = v + offset / WORDBITS;
    offset &= WORDBITS - 1;

    /* Set offset heading word (if such exists). */
    if (offset) {
	/* Compute item count in word. */
	size_t ctag = WORDBITS - offset;
	if (c < ctag)
	    ctag = c;

	carry &= bitmask (ctag, 0);

	/* Fetch, clear, set, and store the (offset) read data. */
	m = bitmask (ctag, offset);
	w = *vtag;
	if ((ret = func (wtag, w & m, st.read (ctag) << offset, carry)))
	    return ret;
	*vtag++ = (w & ~m) | (wtag & m);
	c -= ctag;
    }

    /* Set main vector words (if such exist). */
    while (c >= WORDBITS) {
	w = *vtag;
	if ((ret = func (wtag, w, st.read (WORDBITS), carry)))
	    return ret;
	*vtag++ = wtag;
	c -= WORDBITS;
    }

    /* Set trailing word (if such exists). */
    if (c) {
	m = bitmask (c, 0);
	carry &= m;
	w = *vtag;
	if ((ret = func (wtag, w & m, st.read (c), carry)))
	    return ret;
	*vtag = (w & ~m) | (wtag & m);
    }

    return 0;
}


template <size_t N> inline bitref
bitvec<N>::operator[] (const size_t i)
{
    assert (i < N);
    return bitref (v + (i / WORDBITS), i & (WORDBITS - 1));
}

template <size_t N> inline const_bitref
bitvec<N>::operator[] (const size_t i) const
{
    assert (i < N);
    return const_bitref (v + (i / WORDBITS), i & (WORDBITS - 1));
}

template <size_t N> const bitvec<N> &
bitvec<N>::operator= (const bitvec<N> &bv)
{
    iter (bv, 0, word_copy, 0);
    return *this;
}
/*
template <size_t N> const bitvec<N> &
bitvec<N>::operator= (unsigned w)
{
    if (N < WORDBITS) {
	w &= bitmask (N, 0);
	cout << "w=" << w << endl;
    }
    *this = false;
    v[0] = w;
}

template <size_t N> const bitvec<N> &
bitvec<N>::operator= (bool bit)
{
    size_t c = N;
    unsigned *vtag = v;
    unsigned w = (bit ? (unsigned) -1 : 0);

    while (c >= WORDBITS) {
	*vtag++ = w;
	c -= WORDBITS;
    }

    if (c)
	*vtag = w & bitmask (c, 0);

    return *this;
}
*/

template <size_t N> const bitvec<N> &
bitvec<N>::operator+= (const bitvec<N> &bv)
{
    iter<N> (bv, 0, word_add, 0);
    return *this;
}

template <size_t N> const bitvec<N> &
bitvec<N>::operator|= (const bitvec<N> &bv)
{
    iter<N> (bv, 0, word_or, 0);
    return *this;
}

template <size_t N> const bitvec<N> &
bitvec<N>::operator&= (const bitvec<N> &bv)
{
    iter<N> (bv, 0, word_and, 0);
    return *this;
}

template <size_t N> const bitvec<N> &
bitvec<N>::operator^= (const bitvec<N> &bv)
{
    iter (bv, 0, word_xor, 0);
    return *this;
}

template <size_t N> const bool
bitvec<N>::operator==(const bitvec<N> bv) const
{	
    return (((bitvec<N>*)this)->iter (bv, 0, word_cmp, 0) == 0);
}

template <size_t N> const bool
bitvec<N>::operator!=(const bitvec<N> bv) const
{
    return (((bitvec<N>*)this)->iter (bv, 0, word_cmp, 0) != 0);
}

template <size_t N> const bool 
bitvec<N>::operator<(const bitvec<N> bv) const
{
    return (((bitvec<N>*)this)->iter (bv, 0, word_cmp, 0) < 0);
}

template <size_t N> const bool 
bitvec<N>::operator<=(const bitvec<N> bv) const
{
    return (((bitvec<N>*)this)->iter (bv, 0, word_cmp, 0) <= 0);
}

template <size_t N> const bool 
bitvec<N>::operator>(const bitvec<N> bv) const
{
    return (((bitvec<N>*)this)->iter (bv, 0, word_cmp, 0) > 0);
}

template <size_t N> const bool 
bitvec<N>::operator>=(const bitvec<N> bv) const
{
    return (((bitvec<N>*)this)->iter (bv, 0, word_cmp, 0) >= 0);
}


template <size_t N> const bitvec<N> 
bitvec<N>::operator+ (const bitvec<N> &bv)
{
    return (bitvec<N> (*this) += bv);
}

template <size_t N> const bitvec<N> 
bitvec<N>::operator| (const bitvec<N> &bv)
{
    return (bitvec<N> (*this) |= bv);
}

template <size_t N> const bitvec<N> 
bitvec<N>::operator& (const bitvec<N> &bv)
{
    return (bitvec<N> (*this) &= bv);
}

template <size_t N> const bitvec<N> 
bitvec<N>::operator^ (const bitvec<N> &bv)
{
    return (bitvec<N> (*this) ^= bv);
}

template <size_t N> const bitvec<N>
bitvec<N>::operator<< (size_t s)
{
    bitvec<N> tmp = false;
    if (s < N)
	for (int i = 0; i < N - s; i++)
	    tmp[i + s] = (*this)[i];
    return tmp;
}

template <size_t N> const bitvec<N>
bitvec<N>::operator>> (size_t s)
{
    bitvec<N> tmp = false;
    if (s < N)
	for (int i = 0; i < N - s; i++)
	    tmp[i] = (*this)[i + s];
    return tmp;
}


template <size_t N>
bitvec<N>::bitvec (bool val)
{
    size_t c = N;
    unsigned *vtag = v;
    unsigned w = (val ? (unsigned) -1 : 0);

    while (c >= WORDBITS) {
	*vtag++ = w;
	c -= WORDBITS;
    }

    if (c)
	*vtag = w & bitmask (c, 0);
}

/*
template <size_t N>
bitvec<N>::bitvec (unsigned w)
{
    *this = w;
}
*/

template <size_t N>
bitvec<N>::bitvec (const bitvec<N> &bv)
{
    *this = bv;
}

template <size_t N>
bitvec<N>::bitvec (const const_bitref &bv){
	*this = (bool) bv;	
}


template <size_t Ntag> ostream &
operator<< (ostream &out, const bitvec<Ntag> &bv)
{
    for (int i = 0; i < Ntag; i++)
	out << (bv[i] ? "1" : "0");
    return out;
}


#endif /* __BITVEC_H */

