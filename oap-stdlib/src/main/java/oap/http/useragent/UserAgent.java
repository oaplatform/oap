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

import oap.util.Strings;

public class UserAgent {
    public final String userAgent;
    public final Browser browser;

    public UserAgent( String userAgent ) {
        this.userAgent = userAgent;
        this.browser = parseBrowser( userAgent );
    }

    public static Browser parseBrowser( String userAgent ) {
        if( Strings.isEmpty( userAgent ) ) return Browser.UNKNOWN;

        if( userAgent.contains( "Edge" ) )
            return Browser.EDGE;
        else if( userAgent.contains( "Chrome" ) )
            return userAgent.contains( "Mobile" ) ? Browser.CHROME_MOBILE : Browser.CHROME;
        else if( userAgent.contains( "Firefox" ) || userAgent.contains( "FxiOS" ) )
            return Browser.FIREFOX;
        else if( userAgent.contains( "Trident" ) || userAgent.contains( "MSIE" ) )
            return Browser.INTERNET_EXPLORER;
        else if( !userAgent.contains( "Chrome" ) && ( userAgent.contains( "Safari" ) || userAgent.contains( "AppleWebKit" ) ) ) {
            return userAgent.contains( "Mobile" ) ? Browser.SAFARI_MOBILE : Browser.SAFARI;
        }
        else if( userAgent.contains( "OPR" ) || userAgent.contains( "Presto" ) )
            return userAgent.contains( "Mobile" ) ? Browser.OPERA_MOBILE : Browser.OPERA;
        else if( userAgent.contains( "UCBrowser" ) || userAgent.contains( "UCWeb" ) )
            return Browser.UC_BROWSER; //It is not safari and has its own engine
        else return Browser.UNKNOWN;
    }

    public enum Browser {
        UNKNOWN,
        CHROME,
        CHROME_MOBILE,
        INTERNET_EXPLORER,
        FIREFOX,
        SAFARI,
        SAFARI_MOBILE,
        OPERA,
        OPERA_MOBILE,
        UC_BROWSER,
        EDGE
    }
}
