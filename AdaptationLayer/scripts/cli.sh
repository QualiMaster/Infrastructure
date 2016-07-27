#!/bin/bash
source /etc/profile
CLASSPATH=$(find $QM_LIBS -maxdepth 1 -name '*.jar' -printf '%p:' | sed 's/:$//'):$(find $PROVIDED_LIBS -maxdepth 1 -name '*.jar' -printf '%p:' | sed 's/:$//'):$(find $STORM_HOME/lib -name '*.jar' -printf '%p:' | sed 's/:$//')
java -Dstorm.conf.file=conf/storm.yaml -Dqm.home.dir=$QM_HOME -classpath $CLASSPATH:$STORM_HOME eu.qualimaster.adaptation.platform.Cli $*