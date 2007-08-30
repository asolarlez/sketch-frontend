
BASE=$1
FNAME=$1

grep "!%" ${FNAME} > ${BASE}.inputs
grep "!+" ${FNAME} > ${BASE}.ctrls
grep "********" ${FNAME} > ${BASE}.fctime


grep "c# Original Num Literals" ${FNAME} > ${BASE}.C.literals
grep "f# Original Num Literals" ${FNAME} > ${BASE}.F.literals

grep "c# Original Num Clauses" ${FNAME} > ${BASE}.C.clauses
grep "f# Original Num Clauses" ${FNAME} > ${BASE}.F.clauses

grep "c# Added Conflict Clauses" ${FNAME} > ${BASE}.C.confclauses
grep "f# Added Conflict Clauses" ${FNAME} > ${BASE}.F.confclauses

grep "c# Total Run Time" ${FNAME} > ${BASE}.C.sattime
grep "f# Total Run Time" ${FNAME} > ${BASE}.F.sattime

grep "c# Num. of Decisions" ${FNAME} > ${BASE}.C.numDec
grep "f# Num. of Decisions" ${FNAME} > ${BASE}.F.numDec

grep "c# Max Decision Level" ${FNAME} > ${BASE}.C.maxDec
grep "f# Max Decision Level" ${FNAME} > ${BASE}.F.maxDec



