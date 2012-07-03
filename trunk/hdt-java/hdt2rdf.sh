#!/bin/bash

java -server -Xmx1024M -classpath 'bin:lib/*' org.rdfhdt.hdt.tools.HDT2RDF $*
