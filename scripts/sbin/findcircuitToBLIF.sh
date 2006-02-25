
awk "`cat ${SOLVER}/circuitToBLIF.awk| sed 's/PROCMODE/"FIND"/'`"

