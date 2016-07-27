#!/bin/bash
#$1: file name to download
FILE=`wget -q -O - http://jenkins.sse.uni-hildesheim.de/view/QualiMaster/job/FullInfrastructure/ | grep -o 'infra-[0-9]*-[0-9]*.zip' | head -1`
echo Downloading $FILE
wget http://jenkins.sse.uni-hildesheim.de/view/QualiMaster/job/FullInfrastructure/lastSuccessfulBuild/artifact/FullInfrastructure/$FILE
echo Installing $FILE
unzip -o $FILE
