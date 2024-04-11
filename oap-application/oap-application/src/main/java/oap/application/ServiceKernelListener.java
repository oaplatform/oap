package oap.application;

import oap.reflect.Reflection;

import java.util.List;

public interface ServiceKernelListener {
    Object newInstance( Kernel kernel,
                        ServiceInitializationTree retModules,
                        ModuleItem.ServiceItem serviceItem, Reflection reflect );

    default List<String> validate( ModuleItem.ServiceItem serviceItem ) {
        return List.of();
    }
}
