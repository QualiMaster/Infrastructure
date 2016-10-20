SET EVENTHOST=localhost
SET EVENTPORT=9998
SET CLASSPATH=bin;..\QualiMaster.events\bin;..\QualiMaster.events\lib\log4j-over-slf4j-1.6.6.jar;..\QualiMaster.events\lib\slf4j-api-1.7.5.jar
SET SPASSOPT=-javaagent:./dist/spass-meter-ia.jar=logLevel=SEVERE,overhead=false,configDetect=false,xmlconfig=./spass.xml,printStatistics=false,outInterval=500,eventBus.host=%EVENTHOST%,eventBus.port=%EVENTPORT% 
SET TESTCLASS=tests.eu.qualimaster.monitoring.spassMeter.MemTest
echo %CLASSPATH%
REM java -classpath %CLASSPATH% %TESTCLASS%
java %SPASSOPT% -classpath %CLASSPATH% %TESTCLASS%