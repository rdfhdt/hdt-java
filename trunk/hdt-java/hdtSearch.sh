#!/bin/bash

java -server -Xmx3500M -classpath 'bin:lib/*' org.rdfhdt.hdt.tools.HdtSearch $*
