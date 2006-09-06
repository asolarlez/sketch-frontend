#!/bin/bash

for x in ${*:-*.sk}; do
  echo "RUNNING $x"
  bname="${x%%.sk}"
  rm -f "${bname}".{output,cc,c,h}
  preproc.sh --outputcfiles --incremental 6 --seed 10 "$x" &> "${bname}.output"
done

rm -f *.tmp

grep -n '[0-9]' *.c | cpp | sed 's/:[0-9]*:/::/' > current.output
diff current.output reference

