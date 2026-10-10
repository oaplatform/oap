package external.validation;

import oap.ws.validate.ValidationErrors;

public final class ExternalValidators {
    private ExternalValidators() {
    }

    public static ValidationErrors tooLarge() {
        return ValidationErrors.empty().statusCode( 400 ).error( "payload too large" ).endCode();
    }
}
