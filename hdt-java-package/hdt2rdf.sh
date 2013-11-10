#!/bin/bash

java -server -Xms1024M -Xmx1024M -classpath 'bin:lib/*' org.rdfhdt.hdt.tools.HDT2RDF $*
