#!/bin/bash

java -server -Xmx512M -classpath 'bin:lib/*' org.rdfhdt.hdt.tools.HdtSearch $*
