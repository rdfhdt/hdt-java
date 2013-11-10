#!/bin/bash

java -server -XX:NewRatio=1 -XX:SurvivorRatio=9 -Xmx1024M -classpath 'lib/*.jar' org.rdfhdt.hdt.tools.RDF2HDT $*
