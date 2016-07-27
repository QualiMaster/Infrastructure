######
#!/bin/bash
MYAPP_PATH="/var/nfs/qm/main.sh"
MYAPP_NAME="qualimaster"
case "$1" in
  start)
        java -jar $MYAPP_PATH
        ;;
  stop)
        pid=`ps aux | grep $MYAPP_NAME | awk '{print $2}'`
        kill -9 $pid
        ;;
  restart)
        pid=`ps aux | grep $MYAPP_NAME | awk '{print $2}'`
        kill -9 $pid
        sleep 2
        java -jar $MYAPP_PATH
        ;;
esac
######