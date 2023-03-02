package oap.json.schema;

public class JsonSchemaException extends IllegalStateException {
    public JsonSchemaException( String message ) {
        super( message );
    }

    public JsonSchemaException( RuntimeException cause ) {
        super( cause );
    }

    public JsonSchemaException( String message, RuntimeException cause ) {
        super( message, cause );
    }
}
