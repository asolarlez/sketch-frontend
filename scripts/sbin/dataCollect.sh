ODIR=/cygdrive/c/Armando/Workspace/edu.berkeley.asolar.sketchBit/Thesis/ResultsDB

echo $ODIR/RID

RID=`cat "$ODIR"/RID`
VER=`cat "$ODIR"/VER`

echo "RID = "$RID;

echo $((RID + 1)) > $ODIR/RID

awk -v "RUN_ID=$RID" -v "VERSION=$VER" -v "ODIR=$ODIR" -f $FRONTEND/sbin/dataCollect.awk 




