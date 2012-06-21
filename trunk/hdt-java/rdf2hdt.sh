#!/bin/bash

java -server -Xmx20000M -classpath 'bin:lib/*' hdt.tools.RDF2HDT $*
