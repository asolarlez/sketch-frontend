FNAME=$1
REPORT=$2

echo Test ${FNAME} >> ${REPORT}

grep "ftime" ${FNAME} >> ${REPORT}

grep "c# Original Num Literals" ${FNAME} >> ${REPORT}
grep "f# Original Num Literals" ${FNAME} >> ${REPORT}

grep "c# Original Num Clauses" ${FNAME} >> ${REPORT}
grep "f# Original Num Clauses" ${FNAME} >> ${REPORT}

grep "c# Added Conflict Clauses" ${FNAME} >> ${REPORT}
grep "f# Added Conflict Clauses" ${FNAME} >> ${REPORT}

grep "c# Total Run Time" ${FNAME} >> ${REPORT}
grep "f# Total Run Time" ${FNAME} >> ${REPORT}

grep "c# Num. of Decisions" ${FNAME} >> ${REPORT}
grep "f# Num. of Decisions" ${FNAME} >> ${REPORT}

grep "c# Max Decision Level" ${FNAME} >> ${REPORT}
grep "f# Max Decision Level" ${FNAME} >> ${REPORT}



