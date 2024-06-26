package oap.remote.application;

import oap.application.Kernel;
import oap.application.ModuleItem;
import oap.application.ServiceKernelListener;
import oap.application.ServiceTree;
import oap.reflect.Reflection;
import oap.remote.RemoteInvocationHandler;
import oap.remote.RemoteLocation;

import java.util.List;

public class RemoteLocationExt extends RemoteLocation implements ServiceKernelListener {
    @Override
    public Object newInstance( Kernel kernel, ServiceTree retModules, ModuleItem.ServiceItem serviceItem, Reflection reflect ) {
        return RemoteInvocationHandler.proxy( serviceItem.getModuleName() + ":" + serviceItem.serviceName, this, reflect.underlying );
    }

    @Override
    public List<String> validate( ModuleItem.ServiceItem serviceItem ) {
        if( url == null ) {
            return List.of( "remote.url == null: service " + serviceItem.toString() );
        }

        return List.of();
    }
}
