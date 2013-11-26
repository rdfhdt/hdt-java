#!/bin/bash

source `dirname $0`/javaenv.sh

$JAVA $JAVA_OPTIONS -cp $CP:$CLASSPATH org.rdfhdt.hdt.tools.HDTVerify $*

exit $?
