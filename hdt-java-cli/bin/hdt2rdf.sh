#!/bin/bash

source `dirname $0`/javaenv.sh

#$JAVA $JAVA_OPTIONS -cp $CP:$CLASSPATH org.rdfhdt.hdt.tools.HDT2RDF $*

mvn exec:java -Dexec.mainClass="org.rdfhdt.hdt.tools.HDT2RDF" -Dexec.args="$*"

exit $?
