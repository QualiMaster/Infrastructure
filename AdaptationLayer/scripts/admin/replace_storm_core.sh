#!/bin/bash
SCRIPTDIR=`dirname $0`
source $SCRIPTDIR/env.sh

FILE_NAME=storm-core-0.9.5.jar
ORIGINAL_FILE=/var/nfs/lib/storm-core-libs/original/$FILE_NAME
ADAPTIVE_FILE=/var/nfs/lib/storm-core-libs/adaptive/$FILE_NAME
STORM_LIB=/usr/local/storm/lib

FILE=
if [ "$1" == "original" ]; then
       FILE=$ORIGINAL_FILE
else
       FILE=$ADAPTIVE_FILE
fi

for host in $NIMBUS $WORKER
do
        scp $FILE nisstorm@$host:/tmp
        ssh nisstorm@$host "sudo cp /tmp/$FILE_NAME $STORM_LIB/$FILE_NAME"
        echo $host ok
done
echo Please execute restart-storm.sh
