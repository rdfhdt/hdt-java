#!/bin/bash

java -Xmx512M -classpath 'bin:lib/*' org.rdfhdt.hdt.tools.HdtSearch $*
