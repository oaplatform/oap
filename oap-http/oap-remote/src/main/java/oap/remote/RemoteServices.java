package oap.remote;

import java.util.List;

public interface RemoteServices {
    Object get( String name );

    int count();

    List<String> keySet();
}
