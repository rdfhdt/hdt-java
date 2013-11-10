#!/bin/bash

DIR=`dirname $0`

export hdtFile=$1
shift

java -server -Xmx1G -classpath "${DIR}/lib/*" org.rdfhdt.hdtjena.cmd.HDTSparql $hdtFile "$*"
