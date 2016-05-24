/**
 * Copyright
 */
package oap.ws.validate;

import oap.util.Lists;
import oap.ws.WsClientException;

import java.util.ArrayList;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

public class ValidationErrors {
   public final int code;
   public final List<String> errors = new ArrayList<>();
   public static final int DEFAULT_CODE = HTTP_BAD_REQUEST;

   private ValidationErrors( int code, List<String> errors ) {
      this.code = code;
      this.errors.addAll( errors );
   }

   public static ValidationErrors empty() {
      return create( Lists.empty() );
   }

   public static ValidationErrors create( String error ) {
      return create( Lists.of( error ) );
   }

   public static ValidationErrors create( List<String> errors ) {
      return new ValidationErrors( DEFAULT_CODE, errors );
   }

   public static ValidationErrors create( int code, List<String> errors ) {
      return new ValidationErrors( code, errors );
   }

   public static ValidationErrors create( int code, String error ) {
      return create( code, Lists.of( error ) );
   }

   public void merge( ValidationErrors result ) {
      this.errors.addAll( result.errors );
   }

   public boolean isFailed() {
      return !errors.isEmpty();
   }

   public boolean hasDefaultCode() {
      return code == DEFAULT_CODE;
   }

   public WsClientException throwIfInvalid() {
      if( isFailed() )
         throw new WsClientException( errors.size() > 1 ? "validation failed" : errors.get( 0 ), code, errors );
      return null;
   }
}
