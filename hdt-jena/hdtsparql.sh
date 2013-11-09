#!/bin/bash

export hdtFile=$1
shift

java -d64 -server -Xmx1024M -classpath 'bin:lib/*:../hdt-java/bin:../hdt-java/lib/*' org.rdfhdt.hdtjena.HDTSparql $hdtFile "$*"
