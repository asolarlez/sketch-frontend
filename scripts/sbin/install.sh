#!/bin/bash

MYDIR="$PWD"

# Add sbin directory to PATH
#
export PATH="$PATH:$MYDIR"

# Set SBit environment variables (optionally convert for Cygwin)
#
SOLVER="$MYDIR"
FRONTEND="${MYDIR%/*}"
if [ "$OSTYPE" == cygwin ]; then
  SOLVER=`cygpath -w "$SOLVER"`
  FRONTEND=`cygpath -w "$FRONTEND"`
fi
export SOLVER
export FRONTEND

