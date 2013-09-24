#! /usr/bin/bash 
rm -f script *.h *.exe *.stackdump *.c *.cpp *.cc *.sout *.output  *.blif
for FILE in *.sk; do rm -f ${FILE%.sk}; done


