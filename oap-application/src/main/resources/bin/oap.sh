#!/bin/sh

cd `dirname $0`/..;

APPHOME=`pwd`
APPNAME=`basename $APPHOME`

. $APPHOME/bin/functions.sh
[ -f $APPHOME/bin/env.sh ] && . $APPHOME/bin/env.sh

case $1 in
    "--start")
        oap_status_q && exit 0
        oap_start
    ;;
    "--run")
        oap_run
    ;;
    "--stop")
        oap_status_q || exit 0
        oap_stop
    ;;
    *)
        echo "Usage: $0 --start|--run|--stop"
    ;;
esac

