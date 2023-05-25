[ INJECTION via constructor and public non-final field ]

During startup Kernel loads configuration file and initializes beans.
/META-INF/oap-module.conf is the starting point for manipulating OAP dependencies and other application settings. That file uses HOCON format. Main point is services section, where beans described (like spring beans). 
Each described bean inside is called ‘service’ and has 2 obligatory parameters - its name (not-null-objects-factory) and implementation class (io.xenoss.example.NotNull). 
For example

~~~
not-null-objects-factory {
    implementation = io.xenoss.example.NotNull
}
~~~

That bean MUST have default constructor to be set up while application starting up.
Kernel calls constructor during initialization phase. 
Beans also can have constructors with parameters (moreover, even public fields might be initialized). 
For example
~~~
other-objects-factory {
    implementation = io.xenoss.example.NotNull
}
not-null-objects-factory {
    implementation = io.xenoss.example.NotNull
    parameters {
        internalField = 3
        constructorParameter1 = Text
        constructorParameter2 = modules.this.other-object-factory
    }
}
~~~
Usually parameters are simple classes (int, string, bool), but also can be used
with map loader like below (.conf files are in HOCON format)
~~~
parameters {
    constructorParameter1 = classpath(/oap/application/KernelTest/beanFromClasspathFile.conf)
}
~~~
In this example we may see how the NotNull class (below) can be used in configuration above
~~~
public class NotNull {
     public int internalField;
     private String p1;
     private NotNull p2;

     public NotNull(){
     }
     
     public NotNull(String constructorParameter1, NotNull constructorParameter2) {
         this.p1 = constructorParameter1;
         this.p2 = constructorParameter2;
     }
}
~~~
After initialization these both beans (not-null-objects-factory,other-objects-factory) will be ready.

[ DEPENDENCIES between modules/beans ]

Some beans(services) may depend on each other, 
see example below where service s1 depends on s2, so if s2 is enabled then s1 is also enabled, otherwise both are not enabled
~~~
services {  
    s1 {    
        implementation = ...
        dependsOn = [s2]
    }  
    s2 {    
        implementation = ...    
        profile = enabled
    }
}
~~~

[ SERVICES/BEANS AS THREADS ]
~~~
thread-service {  
    implementation = org.example.thread.SimpleThread
    supervision.thread = true
}
~~~
where SimpleThread class looks like shown below
~~~
public class SimpleThread implements Runnable {    
    @Override    
    public void run() {
    ...
    }
}
~~~
This configuration will run service thread each 5 seconds by cron. Cron configuration may be defined as ‘cron’ config parameter (see https://www.freeformatter.com/cron-expression-generator-quartz.html). 
In the example below the thread will run every hour
~~~
thread-service {  
    implementation = ...
    supervision {  
        schedule = true  
        cron = "0 0 * ? * *"
    }
}
~~~
It’s also possible to setup period via 'period' parameter ignoring tough cron config
~~~
supervision {  
    supervise = true  
    schedule = true  
    delay = 5s
}
~~~

[ LINKING/INJECTION via method call]
Services/beans may also be linked via its parameters. For instance we need to provide some parameter to a service those real value depends on enable/disable flag or profile. In hat case we may provide valid parameter via linking. 
Let’s suppose we have
~~~
services {  
    some-service {    
        implementation = ...    
        parameters {
            consructorParameter = null
        }  
    }
    fake-param1 {    
        implementation = ...
        enabled = false
        link {
            consructorParameter = modules.this.some-service
        }
    }
    fake-param2 {    
        implementation = ...
        profile = [missing]
        link {
            consructorParameter = modules.this.some-service
        }
    }
    real-param {    
        implementation = ...
        link {
            consructorParameter = modules.this.some-service
        }
    }
}
~~~
In the example above we may see ‘some-service’ which has constructor parameter ‘consructorParameter’ and its description is null. 
It means that while initialization phase of Kernel real value will be null, then during linking phase we will try to resolve real value. 
If provided profile is not ‘missing’, then both ‘fake-param1’ and ‘fake-param2’ disabled and there is only one option to substitute the consructorParameter. 
So the real-param will be set for some-service.consructorParameter

Let’s take a look at another (real) example from xenoss-geo:
~~~
geo-enricher {  
    implementation = io.xenoss.bidder.enricher.GeoEnricher  
    parameters {    
        geoService = modules.xenoss-geo.geo    
        priorities = [ MAXMIND, GOOGLE, GEONAMES ]  
    }  
    link.enricher = modules.xenoss-platform-dsp-bidder.bid-request-processor
}
~~~
as we can see, here we have linked geo-enricher to bid-request-processor. The linkage works via calling the method
~~~
public void addEnricher( Object filter ) throws IllegalAccessException
~~~
in BidRequestProcessor class. Of course, it might be done in opposite way with Enricher constructor parameter. 
So this is just a second way to link services/beans.

[ PROGRAMM ACCESS to services/beans ]

- Get service/bean name from configuration from class.
~~~
var fServiceName = kernel.serviceOfClass( TestServiceNameField.class ).orElseThrow();
~~~
-