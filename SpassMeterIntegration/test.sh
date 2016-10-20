#/bin/bash
#for Jenkins
EVENTHOST=localhost
EVENTPORT=9998
EVENTS=../../../QualiMaster.Events/workspace/QualiMaster.Events
CLASSPATH=build/classes:$EVENTS/build/classes:$EVENTS/lib/log4j-over-slf4j-1.6.6.jar:$EVENTS/lib/slf4j-api-1.7.5.jar
SPASSOPT=-javaagent:./dist/spass-meter-ia.jar=logLevel=SEVERE,overhead=false,configDetect=false,xmlconfig=./spass.xml,printStatistics=false,outInterval=500,eventBus.host=$EVENTHOST,eventBus.port=$EVENTPORT
TESTCLASS=tests.eu.qualimaster.monitoring.spassMeter.MemTest
echo $CLASSPATH
# java -classpath $CLASSPATH $TESTCLASS
java $SPASSOPT -classpath $CLASSPATH $TESTCLASS