if [ -e /usr/bin/systemctl ]
then
    systemctl disable ${oap.service.name} || true
    rm -f /usr/lib/systemd/system/${oap.service.name}.service
    systemctl daemon-reload
else
    chkconfig --del ${oap.service.name}
    rm  -f /etc/init.d/${oap.service.name} || true
fi
