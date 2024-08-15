package oap.json.schema;

public class NodeResponse {
    public final JsonSchemaParserContext context;
    public final AbstractSchemaASTWrapper schema;

    public NodeResponse( JsonSchemaParserContext context ) {
        this.context = context;
        this.schema = null;
    }

    public NodeResponse( AbstractSchemaASTWrapper schema ) {
        this.schema = schema;
        this.context = null;
    }
}
