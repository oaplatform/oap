# OAP StdLib

Lightweight and simple DI/IoC tool.<br> 

## oap-module.conf structure
..

## Kernel
Please check functionality in [Kernel.class](oap-stdlib/src/main/java/oap/application/Kernel.java) 
and [KernelTest](oap-stdlib/src/test/java/oap/application/KernelTest.java) and [KernelFixtureTest](oap-stdlib/src/test/java/oap/application/testng/KernelFixtureTest.java)

## Kernel services
See main Kernel services in [oap-module.conf](oap-stdlib/src/main/resources/META-INF/oap-module.conf):
* oap-time-java
* oap-time-joda
* oap-http-server
* oap-prometheus-metrics
* oap-http-health-handler
* remoting
* prometheus-jvm-exporter
* prometheus-application-info-exporter

_todo: add javadoc for the above services(classes)_
