CFLAGS=-Wall
OBJECTS=bitvec.o
BINARIES=bvtest

.PHONY: clean all

all: $(OBJECTS) $(BINARIES)

clean:
	rm -f $(BINARIES) $(OBJECTS)

$(OBJECTS):%.o: Makefile

bvtest: bitvec.h

