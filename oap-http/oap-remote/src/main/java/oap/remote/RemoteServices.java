package oap.remote;

import javax.annotation.Nullable;
import java.util.List;

public interface RemoteServices {
    @Nullable
    Object get( String name );

    int count();

    List<String> keySet();

    String getName();
}
