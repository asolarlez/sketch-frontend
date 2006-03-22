#!/bin/bash
rm -f script *.h *.exe *.c *.cc *.sout *.output
for FILE in *.sk; do rm -f ${FILE%.sk}; done
