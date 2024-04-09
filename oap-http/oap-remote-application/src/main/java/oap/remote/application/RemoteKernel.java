package oap.remote.application;

import oap.application.Kernel;
import oap.application.ModuleItem;
import oap.remote.RemoteServices;

import javax.annotation.Nullable;
import java.util.List;

public class RemoteKernel implements RemoteServices {
    private final Kernel kernel;

    public RemoteKernel( Kernel kernel ) {
        this.kernel = kernel;
    }

    @Nullable
    @Override
    public Object get( String name ) {
        return kernel.service( name ).orElse( null );
    }

    @Override
    public int count() {
        return kernel.services.size();
    }

    @Override
    public List<String> keySet() {
        return kernel.services.keySet().stream().map( ModuleItem.ServiceItem::getName ).toList();
    }

    @Override
    public String getName() {
        return kernel.toString();
    }
}
