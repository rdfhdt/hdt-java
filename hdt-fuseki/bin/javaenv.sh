#!/bin/bash

# Warning load this file using:
# source `dirname $0`/javaenv.sh

BASE=`dirname $0`/../
version=$(grep -m 1 -oP '<version>.+</version>' $BASE/../pom.xml);
JAR=target/hdt-fuseki-${version:9:-10}.jar
echo $JAR
# Find Java
if [ "$JAVA_HOME" = "" ] ; then
    JAVA="java -server"
else
    JAVA="$JAVA_HOME/bin/java -server"
fi
