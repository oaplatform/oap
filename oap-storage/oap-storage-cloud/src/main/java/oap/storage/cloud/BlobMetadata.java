package oap.storage.cloud;

import lombok.Builder;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@ToString
@Builder
public class BlobMetadata implements Serializable {
    @Serial
    private static final long serialVersionUID = -3448280826165820410L;

    public Map<String, String> userMetadata;
    public Map<String, String> tags;
    public String contentType;

}
