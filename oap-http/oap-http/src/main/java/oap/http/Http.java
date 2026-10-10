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

package oap.http;

/** Constants for HTTP status codes, headers and content types used by OAP HTTP services. Grouped in the nested classes {@link StatusCode}, {@link Headers} and {@link ContentType}. */
@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class Http {

    /** HTTP response status codes, taken from {@link io.undertow.util.StatusCodes}. */
    @SuppressWarnings( "checkstyle:AbstractClassName" )
    public abstract static class StatusCode {
        /** HTTP status 100 Continue. */
        public static final int CONTINUE = io.undertow.util.StatusCodes.CONTINUE;
        /** HTTP status 101 Switching Protocols. */
        public static final int SWITCHING_PROTOCOLS = io.undertow.util.StatusCodes.SWITCHING_PROTOCOLS;
        /** HTTP status 102 Processing. */
        public static final int PROCESSING = io.undertow.util.StatusCodes.PROCESSING;
        /** HTTP status 200 OK. */
        public static final int OK = io.undertow.util.StatusCodes.OK;
        /** HTTP status 201 Created. */
        public static final int CREATED = io.undertow.util.StatusCodes.CREATED;
        /** HTTP status 202 Accepted. */
        public static final int ACCEPTED = io.undertow.util.StatusCodes.ACCEPTED;
        /** HTTP status 203 Non-Authoritative Information. */
        public static final int NON_AUTHORITATIVE_INFORMATION = io.undertow.util.StatusCodes.NON_AUTHORITATIVE_INFORMATION;
        /** HTTP status 204 No Content. */
        public static final int NO_CONTENT = io.undertow.util.StatusCodes.NO_CONTENT;
        /** HTTP status 205 Reset Content. */
        public static final int RESET_CONTENT = io.undertow.util.StatusCodes.RESET_CONTENT;
        /** HTTP status 206 Partial Content. */
        public static final int PARTIAL_CONTENT = io.undertow.util.StatusCodes.PARTIAL_CONTENT;
        /** HTTP status 207 Multi-Status. */
        public static final int MULTI_STATUS = io.undertow.util.StatusCodes.MULTI_STATUS;
        /** HTTP status 207 Multi-Status. */
        public static final int ALREADY_REPORTED = io.undertow.util.StatusCodes.MULTI_STATUS;
        /** HTTP status 226 IM Used. */
        public static final int IM_USED = io.undertow.util.StatusCodes.IM_USED;
        /** HTTP status 300 Multiple Choices. */
        public static final int MULTIPLE_CHOICES = io.undertow.util.StatusCodes.MULTIPLE_CHOICES;
        /** HTTP status 301 Moved Permanently. */
        public static final int MOVED_PERMANENTLY = io.undertow.util.StatusCodes.MOVED_PERMANENTLY;
        /** HTTP status 302 Found. */
        public static final int FOUND = io.undertow.util.StatusCodes.FOUND;
        /** HTTP status 303 See Other. */
        public static final int SEE_OTHER = io.undertow.util.StatusCodes.SEE_OTHER;
        /** HTTP status 304 Not Modified. */
        public static final int NOT_MODIFIED = io.undertow.util.StatusCodes.NOT_MODIFIED;
        /** HTTP status 305 Use Proxy. */
        public static final int USE_PROXY = io.undertow.util.StatusCodes.USE_PROXY;
        /** HTTP status 307 Temporary Redirect. */
        public static final int TEMPORARY_REDIRECT = io.undertow.util.StatusCodes.TEMPORARY_REDIRECT;
        /** HTTP status 308 Permanent Redirect. */
        public static final int PERMANENT_REDIRECT = io.undertow.util.StatusCodes.PERMANENT_REDIRECT;
        /** HTTP status 400 Bad Request. */
        public static final int BAD_REQUEST = io.undertow.util.StatusCodes.BAD_REQUEST;
        /** HTTP status 401 Unauthorized. */
        public static final int UNAUTHORIZED = io.undertow.util.StatusCodes.UNAUTHORIZED;
        /** HTTP status 403 Forbidden. */
        public static final int FORBIDDEN = io.undertow.util.StatusCodes.FORBIDDEN;
        /** HTTP status 404 Not Found. */
        public static final int NOT_FOUND = io.undertow.util.StatusCodes.NOT_FOUND;
        /** HTTP status 405 Method Not Allowed. */
        public static final int METHOD_NOT_ALLOWED = io.undertow.util.StatusCodes.METHOD_NOT_ALLOWED;
        /** HTTP status 406 Not Acceptable. */
        public static final int NOT_ACCEPTABLE = io.undertow.util.StatusCodes.NOT_ACCEPTABLE;
        /** HTTP status 407 Proxy Authentication Required. */
        public static final int PROXY_AUTHENTICATION_REQUIRED = io.undertow.util.StatusCodes.PROXY_AUTHENTICATION_REQUIRED;
        /** HTTP status 408 Request Time-out. */
        public static final int REQUEST_TIME_OUT = io.undertow.util.StatusCodes.REQUEST_TIME_OUT;
        /** HTTP status 409 Conflict. */
        public static final int CONFLICT = io.undertow.util.StatusCodes.CONFLICT;
        /** HTTP status 410 Gone. */
        public static final int GONE = io.undertow.util.StatusCodes.GONE;
        /** HTTP status 411 Length Required. */
        public static final int LENGTH_REQUIRED = io.undertow.util.StatusCodes.LENGTH_REQUIRED;
        /** HTTP status 412 Precondition Failed. */
        public static final int PRECONDITION_FAILED = io.undertow.util.StatusCodes.PRECONDITION_FAILED;
        /** HTTP status 413 Request Entity Too Large. */
        public static final int REQUEST_ENTITY_TOO_LARGE = io.undertow.util.StatusCodes.REQUEST_ENTITY_TOO_LARGE;
        /** HTTP status 414 Request-URI Too Large. */
        public static final int REQUEST_URI_TOO_LARGE = io.undertow.util.StatusCodes.REQUEST_URI_TOO_LARGE;
        /** HTTP status 415 Unsupported Media Type. */
        public static final int UNSUPPORTED_MEDIA_TYPE = io.undertow.util.StatusCodes.UNSUPPORTED_MEDIA_TYPE;
        /** HTTP status 416 Requested range not satisfiable. */
        public static final int REQUEST_RANGE_NOT_SATISFIABLE = io.undertow.util.StatusCodes.REQUEST_RANGE_NOT_SATISFIABLE;
        /** HTTP status 417 Expectation Failed. */
        public static final int EXPECTATION_FAILED = io.undertow.util.StatusCodes.EXPECTATION_FAILED;
        /** HTTP status 422 Unprocessable Entity. */
        public static final int UNPROCESSABLE_ENTITY = io.undertow.util.StatusCodes.UNPROCESSABLE_ENTITY;
        /** HTTP status 423 Locked. */
        public static final int LOCKED = io.undertow.util.StatusCodes.LOCKED;
        /** HTTP status 424 Failed Dependency. */
        public static final int FAILED_DEPENDENCY = io.undertow.util.StatusCodes.FAILED_DEPENDENCY;
        /** HTTP status 426 Upgrade Required. */
        public static final int UPGRADE_REQUIRED = io.undertow.util.StatusCodes.UPGRADE_REQUIRED;
        /** HTTP status 428 Precondition Required. */
        public static final int PRECONDITION_REQUIRED = io.undertow.util.StatusCodes.PRECONDITION_REQUIRED;
        /** HTTP status 429 Too Many Requests. */
        public static final int TOO_MANY_REQUESTS = io.undertow.util.StatusCodes.TOO_MANY_REQUESTS;
        /** HTTP status 431 Request Header Fields Too Large. */
        public static final int REQUEST_HEADER_FIELDS_TOO_LARGE = io.undertow.util.StatusCodes.REQUEST_HEADER_FIELDS_TOO_LARGE;
        /** HTTP status 500 Internal Server Error. */
        public static final int INTERNAL_SERVER_ERROR = io.undertow.util.StatusCodes.INTERNAL_SERVER_ERROR;
        /** HTTP status 501 Not Implemented. */
        public static final int NOT_IMPLEMENTED = io.undertow.util.StatusCodes.NOT_IMPLEMENTED;
        /** HTTP status 502 Bad Gateway. */
        public static final int BAD_GATEWAY = io.undertow.util.StatusCodes.BAD_GATEWAY;
        /** HTTP status 503 Service Unavailable. */
        public static final int SERVICE_UNAVAILABLE = io.undertow.util.StatusCodes.SERVICE_UNAVAILABLE;
        /** HTTP status 504 Gateway Time-out. */
        public static final int GATEWAY_TIME_OUT = io.undertow.util.StatusCodes.GATEWAY_TIME_OUT;
        /** HTTP status 505 HTTP Version not supported. */
        public static final int HTTP_VERSION_NOT_SUPPORTED = io.undertow.util.StatusCodes.HTTP_VERSION_NOT_SUPPORTED;
        /** HTTP status 507 Insufficient Storage. */
        public static final int INSUFFICIENT_STORAGE = io.undertow.util.StatusCodes.INSUFFICIENT_STORAGE;
        /** HTTP status 508 Loop Detected. */
        public static final int LOOP_DETECTED = io.undertow.util.StatusCodes.LOOP_DETECTED;
        /** HTTP status 510 Not Extended. */
        public static final int NOT_EXTENDED = io.undertow.util.StatusCodes.NOT_EXTENDED;
        /** HTTP status 511 Network Authentication Required. */
        public static final int NETWORK_AUTHENTICATION_REQUIRED = io.undertow.util.StatusCodes.NETWORK_AUTHENTICATION_REQUIRED;

        /**
         * Reason phrase of an HTTP status code, as provided by undertow.
         *
         * @param statusCode HTTP status code, for example {@code 404}
         * @return reason phrase, for example {@code Not Found}
         */
        public static String getReason( int statusCode ) {
            return io.undertow.util.StatusCodes.getReason( statusCode );
        }
    }

    /** HTTP header names, taken from {@link io.undertow.util.Headers}. */
    @SuppressWarnings( "checkstyle:AbstractClassName" )
    public abstract static class Headers {
        /** The Content-Encoding HTTP header. */
        public static final String CONTENT_ENCODING = io.undertow.util.Headers.CONTENT_ENCODING_STRING;
        /** The Accept-Encoding HTTP header. */
        public static final String ACCEPT_ENCODING = io.undertow.util.Headers.ACCEPT_ENCODING_STRING;
        /** The Content-Type HTTP header. */
        public static final String CONTENT_TYPE = io.undertow.util.Headers.CONTENT_TYPE_STRING;
        /** The Location HTTP header. */
        public static final String LOCATION = io.undertow.util.Headers.LOCATION_STRING;
        /** The Authorization HTTP header. */
        public static final String AUTHORIZATION = io.undertow.util.Headers.AUTHORIZATION_STRING;
        /** The WWW-Authenticate HTTP header. */
        public static final String WWW_AUTHENTICATE = io.undertow.util.Headers.WWW_AUTHENTICATE_STRING;
        /** The Date HTTP header. */
        public static final String DATE = io.undertow.util.Headers.DATE_STRING;
        /** The Connection HTTP header. */
        public static final String CONNECTION = io.undertow.util.Headers.CONNECTION_STRING;
    }

    /** MIME media types used by OAP HTTP services. */
    @SuppressWarnings( "checkstyle:AbstractClassName" )
    public abstract static class ContentType {
        /** The text/tab-separated-values media type. */
        public static final String TEXT_TSV = "text/tab-separated-values";
        /** Comma-separated values; Defined in RFC 4180 */
        public static final String TEXT_CSV = "text/csv";
        /** HTML; Defined in RFC 2854 */
        public static final String TEXT_HTML = "text/html";
        /** Textual data; Defined in RFC 2046 and RFC 3676 */
        public static final String TEXT_PLAIN = "text/plain";
        /** Extensible Markup Language; Defined in RFC 3023 */
        public static final String TEXT_XML = "text/xml";
        /** JavaScript Object Notation JSON; Defined in RFC 4627 JavaScript - Defined in and obsoleted by RFC 4329 in order to discourage its usage in favor of application/javascript. However,text/javascript is allowed in HTML 4 and 5 and, unlike application/javascript, has cross-browser support. The "type" attribute of the &lt;script&gt; tag in HTML5 is optional and there is no need to use it at all since all browsers have always assumed the correct default (even in HTML 4 where it was required by the specification). [Obsolete] */
        public static final String TEXT_JAVASCRIPT = "text/javascript";
        /** The application/json media type. */
        public static final String APPLICATION_JSON = "application/json";
        /** Extensible Markup Language; Defined in RFC 3023 */
        public static final String APPLICATION_XML = "application/xml";
        /** Arbitrary binary data.[5] Generally speaking this type identifies files that are not associated with a specific application. Contrary to past assumptions by software packages such as Apache this is not a type that should be applied to unknown files. In such a case, a server or application should not indicate a content type, as it may be incorrect, but rather, should omit the type in order to allow the recipient to guess the type.[6] SOAP; Defined by RFC 3902 */
        public static final String APPLICATION_SOAP_XML = "application/soap+xml";
        /** The application/octet-stream media type. */
        public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
        /** MIME Email; Defined in RFC 2045 and RFC 2046 */
        public static final String MULTIPART_ALTERNATIVE = "multipart/alternative";
        /** MIME Email; Defined in RFC 2045 and RFC 2046 */
        public static final String MULTIPART_MIXED = "multipart/mixed";
        /** MIME Email; Defined in RFC 2387 and used by MHTML (HTML mail) */
        public static final String MULTIPART_RELATED = "multipart/related";
        /** Defined in RFC 1847 */
        public static final String MULTIPART_ENCRYPTED = "multipart/encrypted";
        /** Defined in RFC 1847 */
        public static final String MULTIPART_SIGNED = "multipart/signed";
        /** MIME Webform; Defined in RFC 2388 */
        public static final String MULTIPART_FORM_DATA = "multipart/form-data";
        /** Body contains a URL-encoded query string as per RFC 1867 */
        public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
        /** Tarball files */
        public static final String APPLICATION_X_TAR = "application/x-tar";

        /** Used to denote the encoding necessary for files containing JavaScript source code. The alternative MIME type for this file type is text/javascript. */
        public static final String ApplicationXJavascript = "application/x-javascript";
        /** 24bit Linear PCM audio at 8-48kHz, 1-N channels; Defined in RFC 3190 */
        public static final String AudioL24 = "audio/L24";
        /** Adobe Flash files for example with the extension .swf */
        public static final String ApplicationXShockwaveFlash = "application/x-shockwave-flash";
        /** Atom feeds */
        public static final String ApplicationAtomXml = "application/atom+xml";
        /** Cascading Style Sheets; Defined in RFC 2318 */
        public static final String TextCss = "text/css";
        /** commands; subtype resident in Gecko browsers like Firefox 3.5 */
        public static final String TextCmd = "text/cmd";
        /** deb (file format), a software package format used by the Debian project */
        public static final String ApplicationXDeb = "application/x-deb";
        /** Defined in RFC 2616 */
        public static final String MessageHttp = "message/http";
        /** Defined in RFC 4735 */
        public static final String ModelExample = "model/example";
        /** device-independent document in DVI format */
        public static final String ApplicationXDvi = "application/x-dvi";
        /** DTD files; Defined by RFC 3023 */
        public static final String ApplicationXmlDtd = "application/xml-dtd";
        /** ECMAScript/JavaScript; Defined in RFC 4329 (equivalent to application/ecmascript but with looser processing rules) It is not accepted in IE 8 or earlier - text/javascript is accepted but it is defined as obsolete in RFC 4329. The "type" attribute of the &lt;script&gt; tag in HTML5 is optional and in practice omitting the media type of JavaScript programs is the most interoperable solution since all browsers have always assumed the correct default even before HTML5. */
        public static final String ApplicationJavascript = "application/javascript";
        /** ECMAScript/JavaScript; Defined in RFC 4329 (equivalent to application/javascript but with stricter processing rules) */
        public static final String ApplicationEcmascript = "application/ecmascript";
        /** EDI EDIFACT data; Defined in RFC 1767 */
        public static final String ApplicationEdifact = "application/EDIFACT";
        /** EDI X12 data; Defined in RFC 1767 */
        public static final String ApplicationEdiX12 = "application/EDI-X12";
        /** Email; Defined in RFC 2045 and RFC 2046 */
        public static final String MessagePartial = "message/partial";
        /** Email; EML files, MIME files, MHT files, MHTML files; Defined in RFC 2045 and RFC 2046 */
        public static final String MessageRfc822 = "message/rfc822";
        /** Flash video (FLV files) */
        public static final String VideoXFlv = "video/x-flv";
        /** GIF image; Defined in RFC 2045 and RFC 2046 */
        public static final String ImageGif = "image/gif";
        /** GoogleWebToolkit data */
        public static final String TextXGwtRpc = "text/x-gwt-rpc";
        /** Gzip */
        public static final String ApplicationXGzip = "application/x-gzip";
        /** ICO image; Registered[9] */
        public static final String ImageVndMicrosoftIcon = "image/vnd.microsoft.icon";
        /** IGS files, IGES files; Defined in RFC 2077 */
        public static final String ModelIges = "model/iges";
        /** IMDN Instant Message Disposition Notification; Defined in RFC 5438 */
        public static final String MessageImdnXml = "message/imdn+xml";
        /** JavaScript Object Notation (JSON) Patch; Defined in RFC 6902 */
        public static final String ApplicationJsonPatch = "application/json-patch+json";
        /** JPEG JFIF image; Associated with Internet Explorer; Listed in ms775147(v=vs.85) - Progressive JPEG, initiated before global browser support for progressive JPEGs (Microsoft and Firefox). */
        public static final String ImagePjpeg = "image/pjpeg";
        /** JPEG JFIF image; Defined in RFC 2045 and RFC 2046 */
        public static final String ImageJpeg = "image/jpeg";
        /** jQuery template data */
        public static final String TextXJqueryTmpl = "text/x-jquery-tmpl";
        /** KML files (e.g. for Google Earth) */
        public static final String ApplicationVndGoogleEarthKmlXml = "application/vnd.google-earth.kml+xml";
        /** LaTeX files */
        public static final String ApplicationXLatex = "application/x-latex";
        /** Matroska open media format */
        public static final String VideoXMatroska = "video/x-matroska";
        /** Microsoft Excel 2007 files */
        public static final String ApplicationVndOpenxmlformatsOfficedocumentSpreadsheetmlSheet = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        /** Microsoft Excel files */
        public static final String ApplicationVndMsExcel = "application/vnd.ms-excel";
        /** Microsoft Powerpoint 2007 files */
        public static final String ApplicationVndOpenxmlformatsOfficedocumentPresentationmlPresentation = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        /** Microsoft Powerpoint files */
        public static final String ApplicationVndMsPowerpoint = "application/vnd.ms-powerpoint";
        /** Microsoft Word 2007 files */
        public static final String ApplicationVndOpenxmlformatsOfficedocumentWordprocessingmlDocument = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        /** Microsoft Word files[15] */
        public static final String ApplicationMsword = "application/msword";
        /** Mozilla XUL files */
        public static final String ApplicationVndMozillaXulXml = "application/vnd.mozilla.xul+xml";
        /** MP3 or other MPEG audio; Defined in RFC 3003 */
        public static final String AudioMpeg = "audio/mpeg";
        /** MP4 audio */
        public static final String AudioMp4 = "audio/mp4";
        /** MP4 video; Defined in RFC 4337 */
        public static final String VideoMp4 = "video/mp4";
        /** MPEG-1 video with multiplexed audio; Defined in RFC 2045 and RFC 2046 */
        public static final String VideoMpeg = "video/mpeg";
        /** MSH files, MESH files; Defined in RFC 2077, SILO files */
        public static final String ModelMesh = "model/mesh";
        /** mulaw audio at 8 kHz, 1 channel; Defined in RFC 2046 */
        public static final String AudioBasic = "audio/basic";
        /** Ogg Theora or other video (with audio); Defined in RFC 5334 */
        public static final String VideoOgg = "video/ogg";
        /** Ogg Vorbis, Speex, Flac and other audio; Defined in RFC 5334 */
        public static final String AudioOgg = "audio/ogg";
        /** Ogg, a multimedia bitstream container format; Defined in RFC 5334 */
        public static final String ApplicationOgg = "application/ogg";
        /** OP */
        public static final String ApplicationXopXml = "application/xop+xml";
        /** OpenDocument Graphics; Registered[14] */
        public static final String ApplicationVndOasisOpendocumentGraphics = "application/vnd.oasis.opendocument.graphics";
        /** OpenDocument Presentation; Registered[13] */
        public static final String ApplicationVndOasisOpendocumentPresentation = "application/vnd.oasis.opendocument.presentation";
        /** OpenDocument Spreadsheet; Registered[12] */
        public static final String ApplicationVndOasisOpendocumentSpreadsheet = "application/vnd.oasis.opendocument.spreadsheet";
        /** OpenDocument Text; Registered[11] */
        public static final String ApplicationVndOasisOpendocumentText = "application/vnd.oasis.opendocument.text";
        /** p12 files */
        public static final String ApplicationXPkcs12 = "application/x-pkcs12";
        /** p7b and spc files */
        public static final String ApplicationXPkcs7Certificates = "application/x-pkcs7-certificates";
        /** p7c files */
        public static final String ApplicationXPkcs7Mime = "application/x-pkcs7-mime";
        /** p7r files */
        public static final String ApplicationXPkcs7Certreqresp = "application/x-pkcs7-certreqresp";
        /** p7s files */
        public static final String ApplicationXPkcs7Signature = "application/x-pkcs7-signature";
        /** Portable Document Format, PDF has been in use for document exchange on the Internet since 1993; Defined in RFC 3778 */
        public static final String ApplicationPdf = "application/pdf";
        /** Portable Network Graphics; Registered,[8] Defined in RFC 2083 */
        public static final String ImagePng = "image/png";
        /** PostScript; Defined in RFC 2046 */
        public static final String ApplicationPostscript = "application/postscript";
        /** QuickTime video; Registered[10] */
        public static final String VideoQuicktime = "video/quicktime";
        /** RAR archive files */
        public static final String ApplicationXRarCompressed = "application/x-rar-compressed";
        /** RealAudio; Documented in RealPlayer Customer Support Answer 2559 */
        public static final String AudioVndRnRealaudio = "audio/vnd.rn-realaudio";
        /** Resource Description Framework; Defined by RFC 3870 */
        public static final String ApplicationRdfXml = "application/rdf+xml";
        /** RSS feeds */
        public static final String ApplicationRssXml = "application/rss+xml";
        /** StuffIt archive files */
        public static final String ApplicationXStuffit = "application/x-stuffit";
        /** SVG vector image; Defined in SVG Tiny 1.2 Specification Appendix M */
        public static final String ImageSvgXml = "image/svg+xml";
        /** Tag Image File Format (only for Baseline TIFF); Defined in RFC 3302 */
        public static final String ImageTiff = "image/tiff";
        /** TrueType Font No registered MIME type, but this is the most commonly used */
        public static final String ApplicationXFontTtf = "application/x-font-ttf";
        /** vCard (contact information); Defined in RFC 6350 */
        public static final String TextVcard = "text/vcard";
        /** Vorbis encoded audio; Defined in RFC 5215 */
        public static final String AudioVorbis = "audio/vorbis";
        /** WAV audio; Defined in RFC 2361 */
        public static final String AudioVndWave = "audio/vnd.wave";
        /** Web Open Font Format; (candidate recommendation; use application/x-font-woff until standard is official) */
        public static final String ApplicationFontWoff = "application/font-woff";
        /** WebM Matroska-based open media format */
        public static final String VideoWebm = "video/webm";
        /** WebM open media format */
        public static final String AudioWebm = "audio/webm";
        /** Windows Media Audio Redirector; Documented in Microsoft help page */
        public static final String AudioXMsWax = "audio/x-ms-wax";
        /** Windows Media Audio; Documented in Microsoft KB 288102 */
        public static final String AudioXMsWma = "audio/x-ms-wma";
        /** Windows Media Video; Documented in Microsoft KB 288102 */
        public static final String VideoXMsWmv = "video/x-ms-wmv";
        /** WRL files, VRML files; Defined in RFC 2077 */
        public static final String ModelVrml = "model/vrml";
        /** X3D ISO standard for representing 3D computer graphics, X3D XML files */
        public static final String ModelX3DXml = "model/x3d+xml";
        /** X3D ISO standard for representing 3D computer graphics, X3DB binary files */
        public static final String ModelX3DBinary = "model/x3d+binary";
        /** X3D ISO standard for representing 3D computer graphics, X3DV VRML files */
        public static final String ModelX3DVrml = "model/x3d+vrml";
        /** XHTML; Defined by RFC 3236 */
        public static final String ApplicationXhtmlXml = "application/xhtml+xml";
        /** ZIP archive files; Registered[7] */
        public static final String ApplicationZip = "application/zip";

        /** AWS S3 Folder */
        public static final String ApplicationXDirectory = "application/x-directory";
    }
}
