#!/bin/bash
changequote({ ,} )
SBIT=SBitII
AJAR="ANTLR_JAR";
SJAR="SKETCH_JAR";
if [ $OSTYPE == cygwin ]; then

AJAR="`cygpath -w \"$AJAR\"`";
SJAR="`cygpath -w "$SJAR"`";
TMPCLASSPATH="$AJAR;$SJAR;."
else
TMPCLASSPATH="$AJAR:$SJAR:."
fi

TMPCLASSPATH="ANTLR_JAR:SKETCH_JAR:."

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
