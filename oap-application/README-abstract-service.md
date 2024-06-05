# Example:
```
package oap.test;

public abstract class AbstractService {
}

public class DefaultService extends AbstractService {
}

public class Container {
  public AbstractService fieldParameter;
  public final ArrayList<AbstractService> listParameter = new ArrayList<>();
   
  public Container( AbstractService constructorParameter ) {
  }
}
```

### oap-module.conf:
```
name = module-with-abstract-service

services {
  abstract-service {
    # required
    abstract = true
    # abstract class or interface
    implementation = oap.test.AbstractService
    # optional default implementation
    default = <modules.module-name.default-service>
  }
  
  service {
    implementation = oap.test.Container
    parameters {
      constructorParameter = <modules.this.abstract-service>
      fieldParameter = <modules.this.abstract-service>
      listParameter = [
        <modules.this.abstract-service>
      ]
    }
  }
  
  default-service {
    implementation = oap.test.DefaultService
  }
}
```

### application.conf:
```
boot.maion = module-with-abstract-service

services {
  module-with-abstract-service.abstract-service = <modules.module-with-abstract-service.default-service>
}
```

# mock implementation:

## test module:
### oap-module.conf:
```
name = module-with-abstract-service-TEST

dependsOn = module-with-abstract-service

services {
  mock-service {
    implementation = oap.test.MockService
  }
}
```

### application-fixture.conf:
```
boot.maion = module-with-abstract-service-TEST

services {
  module-with-abstract-service {
    default-service.enabled = false
  }

  module-with-abstract-service.abstract-service = <modules.module-with-abstract-service-TEST.mock-service>
}
```

1. If an implementation is not specified, the oap application will throw an error with a list of services that can be implementations.
2. Cyclic dependencies are not allowed, example __container__ -> __abstract service__ -> __implementation__ -> __container__
