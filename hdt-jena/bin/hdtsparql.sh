#!/bin/bash

source `dirname $0`/javaenv.sh

if [[ $1 =~ ^- ]]; then
	export option=$1
	shift
fi

export hdtFile=$1
shift
mvn exec:java -Dexec.mainClass="org.rdfhdt.hdtjena.cmd.HDTSparql" -Dexec.args="$option $hdtFile '$1'"

exit $?
