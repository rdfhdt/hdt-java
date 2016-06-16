#!/bin/bash
source `dirname $0`/javaenv.sh

# Memory default settings
INDEX_MEM=8g
SERVER_MEM=16g

# Save given arguments
orig_args=$*

# Find the '--hdt' argument value
while (( "$#" )); do
  option=${1:0:5}
  if [ "$1" == "$option" ]; then value=$2
  else value=${1:6}; fi
  if [ "$option" == "--hdt" ]; then
      hdtfile=$value
      break
  fi
  shift
done

# Check if the HDT file needs an index file
if [ -n "$hdtfile" -a ! -f "$hdtfile.index" ]; then
    echo "One-time index file creation, please be patient ..."
    $JAVA -Xmx$INDEX_MEM -cp $CP:$CLASSPATH org.rdfhdt.hdt.fuseki.HDTGenerateIndex $hdtfile || exit $?
fi

# Set default Java options
if [ "$JAVA_OPTIONS" = "" ] ; then
    JAVA_OPTIONS="-Xmx$SERVER_MEM"
fi

# Launch server
$JAVA $JAVA_OPTIONS -cp $CP:$CLASSPATH org.rdfhdt.hdt.fuseki.FusekiHDTCmd --pages=$BASE/pages $orig_args || exit $?
