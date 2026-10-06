package oap.ws;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode
@ToString
public class ErrorResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = -2847613560193847521L;

    public final int statusCode;
    public final String error;
    public final List<Message> messages;

    public ErrorResponse( int statusCode, String error, List<Message> messages ) {
        this.statusCode = statusCode;
        this.error = error;
        this.messages = messages;
    }

    @EqualsAndHashCode
    @ToString
    public static class Message implements Serializable {
        @Serial
        private static final long serialVersionUID = 6019283746510293847L;

        @JsonInclude( JsonInclude.Include.NON_NULL )
        public final Integer code;
        public final String message;

        @JsonCreator
        public Message( @JsonProperty( "code" ) Integer code, @JsonProperty( "message" ) String message ) {
            this.code = code;
            this.message = message;
        }
    }
}
