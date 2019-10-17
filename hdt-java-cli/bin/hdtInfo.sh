#!/bin/bash

source `dirname $0`/javaenv.sh

mvn exec:java -Dexec.mainClass="org.rdfhdt.hdt.tools.HDTInfo" -Dexec.args="$*"

exit $?
