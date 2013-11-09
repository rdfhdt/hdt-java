#!/bin/bash

java -server -XX:NewRatio=1 -XX:SurvivorRatio=9 -Xms1024M -Xmx1024M -classpath 'bin:../hdt-disk/bin:../hdt-disk/lib/*:lib/*' org.rdfhdt.hdt.tools.RDF2HDT $*
