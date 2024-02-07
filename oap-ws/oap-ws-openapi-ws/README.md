# OAP-WS Openapi module
Generating [OpenApi](https://www.openapis.org) documentation for OAP web services


## General Information 
- Generates OpenApi (version 3.0+) documentation for all classes which are used as web services in an easy way.
- No need to use swagger annotation for api documentation. Module uses data from reflection to form proper and appropriate document.
- Only public methods annotated with @WsMethod are subject to be documented

## Documentation
- All necessary information about OpenApi specification could be found in Swagger [docs](https://swagger.io/resources/open-api/) or in original [OAS](https://spec.openapis.org/oas/latest.html).


## OAP-WS-API Comparison
OAP-WS-Openapi provides functionality similar to OAP-WS-API module.

### Difference: 

##### Schema
- OpenApi module uses [OAS](https://spec.openapis.org/oas/latest.html) for describing web services.
- Api module uses its own format for web services description.

##### Web services

- OpenApi module only describes services which marked as included.
- Api module describes all web services within module.

### Pros & Cons of using OAP-WS-Openapi module:

#### Pros

- Uses [OAS](https://spec.openapis.org/oas/latest.html) for describing web services.
- OpenApi is well known and widespread format.
- Response schema can be used for code generation tools like [Swagger Codegen](https://swagger.io/tools/swagger-codegen/) or others.
- Web services can be manually included/excluded to/from resulting document.

#### Cons

- Requires OAS versions support and update.
- Requires dependency on OAS implementation.
- Web services which are added to module as a dependency can't be easily added to description.

## Usage
Steps to use this module within other oap module

- oap-ws-openapi-ws module depends on oap-ws module.

- add module as dependency to _pom.xml/build.gradle_

```
//Example:

<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-ws-openapi-ws</artifactId>
    <version>17.11.0.2</version>
</dependency>
```
- add 'oap-ws-openapi-ws' module as dependency to _oap.module.conf_

```
name: some-module
dependsOn: oap-ws-openapi-ws
services {
...
```

- [OpenApi Info](https://swagger.io/specification/#info-object) can be specified within application.conf

```
oap-ws-openapi-ws.openapi-info.parameters.title = "Title"
oap-ws-openapi-ws.openapi-info.parameters.description = "Description"
oap-ws-openapi-ws.openapi-info.parameters.version = "0.1.1"
```

- description field may be used in @WsMethod annotation to explain what method really does 
```
class TestWS {

    @WsMethod( method = GET, path = "/", description = "Returns constantly (int32) number 2" )
    public int test() {
        return 2;
    }
}
```

- @WSParam annotation also may have description for a parameter
```
class TestWS {

    @WsMethod( method = GET, path = "/?name=" )
    public int test( @WsParam( from = QUERY, description = "An integer argument to be used as a result" ) String name ) {
        return Integer.parseInt( name );
    }
}
```

- Optional parameter automatically marked as not required ( all other parameters treated as obligatory)
```
class TestWS {

    @WsMethod( method = GET, path = "/?name=" )
    public int test( @WsParam( from = QUERY ) Optional<String> name ) {
        return name == null ? 2 : Integer.parseInt( name );
    }
}
```


