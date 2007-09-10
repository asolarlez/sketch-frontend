#!/bin/bash

# Infer sbin directory location
#
MYDIR="${0%/*}"
[ "${MYDIR:0:1}" == '/' ] || MYDIR="$PWD/$MYDIR"

SOLVER="$MYDIR"
FRONTEND="${MYDIR%/*}"

# Add sbin directory to PATH
#
export PATH="$PATH:$MYDIR"

# Convert pathnames (for Cygwin) and export
#
if [ "$OSTYPE" == cygwin ]; then
  SOLVER=`cygpath -w "$SOLVER"`
  FRONTEND=`cygpath -w "$FRONTEND"`
fi
export SOLVER
export FRONTEND

