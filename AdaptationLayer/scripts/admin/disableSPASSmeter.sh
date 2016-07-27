#!/bin/bash
SCRIPTDIR=`dirname $0`
source $SCRIPTDIR/env.sh

#for host in $NIMBUS
#do
#	ssh nisstorm@$host '[ -f /usr/local/storm/conf/storm.yaml_backup ] && ( sudo rm /usr/local/storm/conf/storm.yaml ; sudo mv /usr/local/storm/conf/storm.yaml_backup /usr/local/storm/conf/storm.yaml ) ; [ -f /usr/local/storm/conf/storm.yaml_backup ] || echo "/usr/local/storm/conf/storm.yaml_backup does not exists"'
#	#ssh nisstorm@$host '[ -f /usr/local/storm/conf/storm.yaml_backup ] && ( echo "file exists" ) ; [ -f /usr/local/storm/conf/storm.yaml_backup ] || echo "/usr/local/storm/conf/storm.yaml_backup does not exists"'
#done
for host in $WORKER
do
	ssh nisstorm@$host '[ -f /usr/local/storm/conf/storm.yaml_backup ] && ( sudo rm /usr/local/storm/conf/storm.yaml ; sudo mv /usr/local/storm/conf/storm.yaml_backup /usr/local/storm/conf/storm.yaml ) ; [ -f /usr/local/storm/conf/storm.yaml_backup ] || echo "/usr/local/storm/conf/storm.yaml_backup does not exists"'
	#ssh nisstorm@$host '[ -f /usr/local/storm/conf/storm.yaml_backup ] && ( echo "file exists" ) ; [ -f /usr/local/storm/conf/storm.yaml_backup ] || echo "/usr/local/storm/conf/storm.yaml_backup does not exists"'
done
