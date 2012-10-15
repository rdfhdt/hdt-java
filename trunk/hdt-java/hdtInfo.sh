#!/bin/bash

java -server -Xms512M -Xmx512M -classpath 'bin:lib/*' org.rdfhdt.hdt.tools.HDTInfo $*
