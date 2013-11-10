#!/bin/bash

DIR=`dirname $0`

java -server -classpath "${DIR}/lib/*" org.rdfhdt.hdt.tools.HDTInfo $*
