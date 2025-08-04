package oap.http.pniov3;

public class PnioForceTerminateException extends RuntimeException {
    public final String asyncTaskType;

    public PnioForceTerminateException( String asyncTaskType ) {
        super( null, null, false, false );
        this.asyncTaskType = asyncTaskType;
    }
}
