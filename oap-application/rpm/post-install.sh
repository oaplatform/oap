if [ -e /usr/bin/systemctl ]
then
    [ -e /usr/lib/systemd/system/${oap.service.name}.service ] && rm /usr/lib/systemd/system/${oap.service.name}.service || true
    cp ${oap.service.home}/bin/${oap.service.name}.service /usr/lib/systemd/system/

    systemctl daemon-reload

    if [ "disabled" = `systemctl is-enabled ${oap.service.name}` ]
    then
        systemctl enable ${oap.service.name}
    fi

    if [ "active" = `systemctl is-active ${oap.service.name}` ]
    then
        systemctl restart ${oap.service.name}
    else
        systemctl start ${oap.service.name}
    fi
else
    [ -e /etc/init.d/${oap.service.name} ] && rm /etc/init.d/${oap.service.name} || true
    ln -s ${oap.service.home}/bin/${oap.service.name} /etc/init.d/

    if [ 1 -eq `chkconfig --list ${oap.service.name}  2>&1 | grep -c "not referenced"` ]
    then
        chkconfig --add ${oap.service.name}
    fi

    if [ 1 -eq `service ${oap.service.name} status | grep -c running` ]
    then
        service ${oap.service.name} restart
    else
        service ${oap.service.name} start
    fi
fi
