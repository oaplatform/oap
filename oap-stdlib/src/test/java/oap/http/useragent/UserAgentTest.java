/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.http.useragent;

import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

public class UserAgentTest {

    @Test
    public void browser() {
        UserAgent chrome = new UserAgent( "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.81 Safari/537.36" );
        assertThat( chrome.browser ).isEqualTo( UserAgent.Browser.CHROME );
        UserAgent opera = new UserAgent( "Opera/9.80 (Macintosh; Intel Mac OS X 10.14.1) Presto/2.12.388 Version/12.16" );
        assertThat( opera.browser ).isEqualTo( UserAgent.Browser.OPERA );
        UserAgent edge = new UserAgent( "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML like Gecko) Chrome/51.0.2704.79 Safari/537.36 Edge/14.14931" );
        assertThat( edge.browser ).isEqualTo( UserAgent.Browser.EDGE );
    }

    private static class Agent extends UserAgent {
        String version;

        private Agent( String expectedBrowser, String versionAndAgentCsv ) {
            super( versionAndAgentCsv.replaceFirst( "^[^;]++;'(.*)'", "$1" ) );
            version = versionAndAgentCsv.replaceFirst( "^([^;]++);'.*'", "$1" );
            assertEquals( expectedBrowser, browser.name() );
        }
    }

    @Test
    public void safari() {
        new Agent( "SAFARI", "iOS 10.0;'Mozilla/5.0 (iPhone; CPU iPhone OS 10_0_2 like Mac OS X) AppleWebKit/602.1.50 (KHTML, like Gecko) AppleNews/608.0.1 Version/2.0.1'" );
        new Agent( "UNKNOWN", "iOS 10.1;'TestApp/1.0 CFNetwork/808.1.4 Darwin/16.1.0'" );
        new Agent( "SAFARI_MOBILE", "iOS 10.2;'Mozilla/5.0 (iPhone; CPU iPhone OS 10_2_1 like Mac OS X) AppleWebKit/602.4.6 (KHTML, like Gecko) Mobile/14D27 [FBAN/MessengerForiOS;FBAV/124.0.0.50.70;FBBV/63293619;FBDV/iPhone7,1;FBMD/iPhone;FBSN/iOS;FBSV/10.2.1;FBSS/3;FBCR/Viettel;FBID/phone;FBLC/vi_VN;FBOP/5;FBRV/0]'" );
        new Agent( "SAFARI_MOBILE", "iOS 10.3;'Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.3.8 (KHTML, like Gecko) Mobile/14G60 TopBuzz com.topbuzz.videoen/8.2.2 (iPhone; iOS 10.3.3; en; WIFI; CTRadioAccessTechnologyLTE)'" );
        new Agent( "SAFARI_MOBILE", "iOS 11.0;'Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Mobile/15A372 Safari Line/7.12.0'" );
        new Agent( "UNKNOWN", "iOS 11.4;'ArcGISRuntime-iOS/100.4 (iOS 11.4; iPad7,6) arcgis-aurora/18.1.0 (00000000-0000-0000-0000-000000000000)'" );
        new Agent( "UNKNOWN", "iOS 12.0;'App/1.0.0 CFNetwork/958.1 Darwin/18.0.0'" );
        new Agent( "UNKNOWN", "iOS 12.1;'Luminary/70 CFNetwork/975.0.3 Darwin/18.2.0'" );
        new Agent( "UNKNOWN", "iOS 12.4;'Collector/6212016 CFNetwork/978.0.7 Darwin/18.7.0'" );
        new Agent( "UNKNOWN", "iOS 13;'Explorer/1544 CFNetwork/1107.1 Darwin/19.0.0'" );
        new Agent( "UNKNOWN", "iOS 13.6;'App/1.0.0 CFNetwork/1128.0.1 Darwin/19.6.0'" );
        new Agent( "SAFARI_MOBILE", "iOS 14.4;'Mozilla/5.0 (iPad; CPU iPad OS 14_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148'" );
        new Agent( "UNKNOWN", "iOS 14.5;'App/1.0.0 CFNetwork/1233 Darwin/20.4.0'" );
        new Agent( "SAFARI_MOBILE", "iOS 14.7;'Mozilla/5.0 (iPhone; CPU iPhone OS 14_7_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Mobile/15E148 Safari/604.1'" );
        new Agent( "SAFARI_MOBILE", "iOS 15.0;'Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15 Mobile/15E145 Safari/602.4'" );
        new Agent( "SAFARI_MOBILE", "iOS 15.5;'Mozilla/5.0 (iPhone; CPU iPhone OS 15_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Mobile/15E148 Safari/604.1'" );

        new Agent( "UNKNOWN", "Mac OS 15.11;'MacOutlook/15.27.0.161010 (Intelx64 Mac OS X Version 10.11.6 (Build 15G1108))'" );
    }
}
