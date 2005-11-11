
BASE=$1
FNAME=$1.out

grep "!%" ${FNAME} > ${BASE}.inputs.out
grep "!+" ${FNAME} > ${BASE}.ctrls.out
grep "********" ${FNAME} > ${BASE}.fctime.out


grep "c# Original Num Literals" ${FNAME} > ${BASE}.C.literals.out
grep "f# Original Num Literals" ${FNAME} > ${BASE}.F.literals.out

grep "c# Original Num Clauses" ${FNAME} > ${BASE}.C.clauses.out
grep "f# Original Num Clauses" ${FNAME} > ${BASE}.F.clauses.out

grep "c# Added Conflict Clauses" ${FNAME} > ${BASE}.C.confclauses.out
grep "f# Added Conflict Clauses" ${FNAME} > ${BASE}.F.confclauses.out

grep "c# Total Run Time" ${FNAME} > ${BASE}.C.sattime.out
grep "f# Total Run Time" ${FNAME} > ${BASE}.F.sattime.out

grep "c# Num. of Decisions" ${FNAME} > ${BASE}.C.numDec.out
grep "f# Num. of Decisions" ${FNAME} > ${BASE}.F.numDec.out

grep "c# Max Decision Level" ${FNAME} > ${BASE}.C.maxDec.out
grep "f# Max Decision Level" ${FNAME} > ${BASE}.F.maxDec.out



