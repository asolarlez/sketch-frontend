#!/bin/bash
rm -f script *.h *.exe *.stackdump *.c *.cpp *.cc *.sout *.output
for FILE in *.sk; do rm -f ${FILE%.sk}; done
