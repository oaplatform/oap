package oap.storage.cloud;

import lombok.Builder;

@Builder
public class ListOptions {
    public String continuationToken;
    public Integer maxKeys;
}
