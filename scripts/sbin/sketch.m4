#!/bin/bash

SBIT=SBitII
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
