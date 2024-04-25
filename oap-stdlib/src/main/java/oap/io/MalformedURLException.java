package oap.io;

public class MalformedURLException extends IOException {
    public MalformedURLException( java.net.MalformedURLException cause ) {
        super( cause );
    }
}
