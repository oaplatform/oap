package oap.application.remote;

import oap.system.Env;
import org.apache.fory.Fory;
import org.apache.fory.ThreadSafeFory;
import org.apache.fory.config.CompatibleMode;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class ForyConsts {
    public static final int MIN_POOL_SIZE = Integer.parseInt( Env.get( "FORY_MIN_POOL_SIZE", "128" ) );
    public static final int MAX_POOL_SIZE = Integer.parseInt( Env.get( "FORY_MAX_POOL_SIZE", "1024" ) );

    public static ThreadSafeFory fory;

    static {
        fory = Fory
            .builder()
            .withCompatibleMode( CompatibleMode.COMPATIBLE )
            .withAsyncCompilation( true )
            .requireClassRegistration( false )
            .withRefTracking( true )
            .buildThreadSafeForyPool( ForyConsts.MIN_POOL_SIZE, ForyConsts.MAX_POOL_SIZE );
    }
}
