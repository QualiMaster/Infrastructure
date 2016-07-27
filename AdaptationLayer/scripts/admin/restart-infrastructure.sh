#!/bin/bash

# replace 'master slave0 slave1 slave2 slave3 slave4 slave5 slave6' with
# the host names (or IPs) of your cluster's nodes
# here a priority-based group would be better...
SCRIPTDIR=`dirname $0`
source $SCRIPTDIR/env.sh

for host in $NIMBUS
do
        ssh nisstorm@$host 'sudo supervisorctl stop qm-infra'
        ssh nisstorm@$host 'sudo supervisorctl start qm-infra'
        echo $host ok
done
