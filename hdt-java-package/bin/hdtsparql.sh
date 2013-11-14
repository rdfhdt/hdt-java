#!/bin/bash

source `dirname $0`/javaenv.sh

export hdtFile=$1
shift

$JAVA $JAVA_OPTIONS -cp $CP:$CLASSPATH org.rdfhdt.hdtjena.cmd.HDTSparql $hdtFile "$*"

exit $?
