#!/bin/bash

java -server -Xmx1024M -classpath 'bin:lib/*' hdt.tools.HDT2RDF $*
