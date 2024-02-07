
package oap.ws.openapi;

import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.sso.WsSecurity;

import java.util.Optional;
import java.util.stream.Stream;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.ws.WsParam.From.PATH;
import static oap.ws.WsParam.From.QUERY;

public class ExtTestWS {

    @WsMethod( method = GET, path = "/test/empty_optional_param/{id}", description = "This method returns nothing (Void)" )
    public void testVoid( @WsParam( from = PATH ) String id,
                          @WsParam( from = QUERY, description = "Non obligatory limit, MIGHT be skipped" ) Optional<Integer> limit ) {
    }

    @WsMethod( method = GET, path = "/test/empty_required_param/", description = "This method returns nothing (Void)" )
    @Deprecated( since = "not applicable" )
    @WsSecurity( realm = "organizationId", permissions = { "account:read", "permissions:read" } )
    public void testVoid( @WsParam( from = QUERY, description = "Required parameter, MUST be specified" ) Integer limit ) {
    }

    public Stream<CampaignInfo> campaignList() {
        return Stream.of( new CampaignInfo() );
    }

    public static class CampaignInfo {
        public String field;
    }
}
