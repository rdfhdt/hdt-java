#!/bin/bash

source `dirname $0`/javaenv.sh

export MAVEN_OPTS="-Xmx6g"
mvn exec:java -Dexec.mainClass="org.rdfhdt.hdt.tools.HDTCat" -Dexec.args="$*"

exit $?