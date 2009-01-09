#!/bin/bash
changequote({ ,} )
SBIT=SBitII
AJAR="ANTLR_JAR";
SJAR="SKETCH_JAR";
RJAR="RATS_JAR";
if [ $OSTYPE == cygwin ]; then

AJAR="`cygpath -w \"$AJAR\"`";
SJAR="`cygpath -w "$SJAR"`";
RJAR="`cygpath -w "$RJAR"`";
TMPCLASSPATH="$AJAR;$SJAR;$RJAR;."
else
TMPCLASSPATH="$AJAR:$SJAR:$RJAR:."
fi

echo $TMPCLASSPATH

# Get the last command-line argument as INFILE
#
for INFILE; do continue; done

# Launch
#
java -ea \
  -classpath "$TMPCLASSPATH" \
  streamit.frontend.ToSBit \
  --output "$INFILE.tmp" \
  "$@"
