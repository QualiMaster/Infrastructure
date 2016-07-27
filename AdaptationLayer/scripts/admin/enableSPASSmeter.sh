#!/bin/bash
SCRIPTDIR=`dirname $0`
source $SCRIPTDIR/env.sh

NAME=$(date +"%F_%T")

for host in $WORKER
do
	echo 'enabling SPASS-meter for '$host
	ssh nisstorm@$host 'sudo java -jar /var/nfs/spassMeter_new/Yaml.jar worker.childopts -javaagent:/var/nfs/lib/qm-libs/spass-meter-ia.jar=out=/var/nfs/spassMeter_new/log/'$NAME'.log,xmlconfig=/var/nfs/spass/storm.xml'
done
