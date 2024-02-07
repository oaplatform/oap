# oap-ws-openapi-plugin

## Generating OpenAPI (swagger)
## Automatic generation during build
1. Add plugin in pom.xml
~~~
 <plugin>
    <groupId>oap</groupId>
    <artifactId>oap-ws-openapi-maven-plugin</artifactId>
    <version>${project.parent.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>openapi</goal>
            </goals>
        </execution>
    </executions>
</plugin>
~~~

Some web-services can be excluded with property **excludeModules**

~~~
<excludeModules>oap-ws,oap-ws-sso-api,oap-ws-sso,xenoss-platform-dsp-missioncontrol,oap-ws-openapi-ws,oap-ws-admin-ws</excludeModules>
~~~

Additional properties can be configured in plugin **outputPath**, **outputType**:

~~~
<configuration>
    <outputPath>swagger</outputPath>
    <outputType>JSON_OPENAPI</outputType>
</configuration>
~~~
2. Run command 
~~~
mvn clean install
~~~

3. Lookup for generated swagger.json in directory target/classes

## manual generation via running maven goal
~~~
mvn oap:oap-ws-openapi-maven-plugin:<version>:openapi
~~~