package oap.application.remote;

import oap.http.Uri;
import oap.util.Maps;

import java.nio.file.Paths;
import java.util.Optional;

/**
 * Created by macchiatow on 10/21/16.
 */
public class RemoteService {

   @SuppressWarnings( value = "unchecked")
   public static <T> T get(Class<T> clazz, String url, long timeout, String serviceName, String certificateLocation, String password ){
      return  (T) RemoteInvocationHandler.proxy(
         Uri.uri( url, Maps.empty() ), serviceName, clazz, Paths.get( certificateLocation ), password,
         Optional.of( timeout ), Optional.empty()
      );
   }
}
