#!/bin/bash

java -d64 -server -Xmx1024M -classpath 'bin:lib/*:../hdt-java/bin:../hdt-java/lib/*' JenaHDTTest $*
