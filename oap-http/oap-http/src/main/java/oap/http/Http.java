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

@SuppressWarnings( "checkstyle:InterfaceIsType" )
public interface Http {

    @SuppressWarnings( "checkstyle:InterfaceIsType" )
    interface StatusCode {
        int CONTINUE = io.undertow.util.StatusCodes.CONTINUE;
        int SWITCHING_PROTOCOLS = io.undertow.util.StatusCodes.SWITCHING_PROTOCOLS;
        int PROCESSING = io.undertow.util.StatusCodes.PROCESSING;
        int OK = io.undertow.util.StatusCodes.OK;
        int CREATED = io.undertow.util.StatusCodes.CREATED;
        int ACCEPTED = io.undertow.util.StatusCodes.ACCEPTED;
        int NON_AUTHORITATIVE_INFORMATION = io.undertow.util.StatusCodes.NON_AUTHORITATIVE_INFORMATION;
        int NO_CONTENT = io.undertow.util.StatusCodes.NO_CONTENT;
        int RESET_CONTENT = io.undertow.util.StatusCodes.RESET_CONTENT;
        int PARTIAL_CONTENT = io.undertow.util.StatusCodes.PARTIAL_CONTENT;
        int MULTI_STATUS = io.undertow.util.StatusCodes.MULTI_STATUS;
        int ALREADY_REPORTED = io.undertow.util.StatusCodes.MULTI_STATUS;
        int IM_USED = io.undertow.util.StatusCodes.IM_USED;
        int MULTIPLE_CHOICES = io.undertow.util.StatusCodes.MULTIPLE_CHOICES;
        int MOVED_PERMANENTLY = io.undertow.util.StatusCodes.MOVED_PERMANENTLY;
        int FOUND = io.undertow.util.StatusCodes.FOUND;
        int SEE_OTHER = io.undertow.util.StatusCodes.SEE_OTHER;
        int NOT_MODIFIED = io.undertow.util.StatusCodes.NOT_MODIFIED;
        int USE_PROXY = io.undertow.util.StatusCodes.USE_PROXY;
        int TEMPORARY_REDIRECT = io.undertow.util.StatusCodes.TEMPORARY_REDIRECT;
        int PERMANENT_REDIRECT = io.undertow.util.StatusCodes.PERMANENT_REDIRECT;
        int BAD_REQUEST = io.undertow.util.StatusCodes.BAD_REQUEST;
        int UNAUTHORIZED = io.undertow.util.StatusCodes.UNAUTHORIZED;
        int FORBIDDEN = io.undertow.util.StatusCodes.FORBIDDEN;
        int NOT_FOUND = io.undertow.util.StatusCodes.NOT_FOUND;
        int METHOD_NOT_ALLOWED = io.undertow.util.StatusCodes.METHOD_NOT_ALLOWED;
        int NOT_ACCEPTABLE = io.undertow.util.StatusCodes.NOT_ACCEPTABLE;
        int PROXY_AUTHENTICATION_REQUIRED = io.undertow.util.StatusCodes.PROXY_AUTHENTICATION_REQUIRED;
        int REQUEST_TIME_OUT = io.undertow.util.StatusCodes.REQUEST_TIME_OUT;
        int CONFLICT = io.undertow.util.StatusCodes.CONFLICT;
        int GONE = io.undertow.util.StatusCodes.GONE;
        int LENGTH_REQUIRED = io.undertow.util.StatusCodes.LENGTH_REQUIRED;
        int PRECONDITION_FAILED = io.undertow.util.StatusCodes.PRECONDITION_FAILED;
        int REQUEST_ENTITY_TOO_LARGE = io.undertow.util.StatusCodes.REQUEST_ENTITY_TOO_LARGE;
        int REQUEST_URI_TOO_LARGE = io.undertow.util.StatusCodes.REQUEST_URI_TOO_LARGE;
        int UNSUPPORTED_MEDIA_TYPE = io.undertow.util.StatusCodes.UNSUPPORTED_MEDIA_TYPE;
        int REQUEST_RANGE_NOT_SATISFIABLE = io.undertow.util.StatusCodes.REQUEST_RANGE_NOT_SATISFIABLE;
        int EXPECTATION_FAILED = io.undertow.util.StatusCodes.EXPECTATION_FAILED;
        int UNPROCESSABLE_ENTITY = io.undertow.util.StatusCodes.UNPROCESSABLE_ENTITY;
        int LOCKED = io.undertow.util.StatusCodes.LOCKED;
        int FAILED_DEPENDENCY = io.undertow.util.StatusCodes.FAILED_DEPENDENCY;
        int UPGRADE_REQUIRED = io.undertow.util.StatusCodes.UPGRADE_REQUIRED;
        int PRECONDITION_REQUIRED = io.undertow.util.StatusCodes.PRECONDITION_REQUIRED;
        int TOO_MANY_REQUESTS = io.undertow.util.StatusCodes.TOO_MANY_REQUESTS;
        int REQUEST_HEADER_FIELDS_TOO_LARGE = io.undertow.util.StatusCodes.REQUEST_HEADER_FIELDS_TOO_LARGE;
        int INTERNAL_SERVER_ERROR = io.undertow.util.StatusCodes.INTERNAL_SERVER_ERROR;
        int NOT_IMPLEMENTED = io.undertow.util.StatusCodes.NOT_IMPLEMENTED;
        int BAD_GATEWAY = io.undertow.util.StatusCodes.BAD_GATEWAY;
        int SERVICE_UNAVAILABLE = io.undertow.util.StatusCodes.SERVICE_UNAVAILABLE;
        int GATEWAY_TIME_OUT = io.undertow.util.StatusCodes.GATEWAY_TIME_OUT;
        int HTTP_VERSION_NOT_SUPPORTED = io.undertow.util.StatusCodes.HTTP_VERSION_NOT_SUPPORTED;
        int INSUFFICIENT_STORAGE = io.undertow.util.StatusCodes.INSUFFICIENT_STORAGE;
        int LOOP_DETECTED = io.undertow.util.StatusCodes.LOOP_DETECTED;
        int NOT_EXTENDED = io.undertow.util.StatusCodes.NOT_EXTENDED;
        int NETWORK_AUTHENTICATION_REQUIRED = io.undertow.util.StatusCodes.NETWORK_AUTHENTICATION_REQUIRED;
    }

    @SuppressWarnings( "checkstyle:InterfaceIsType" )
    interface Headers {
        String CONTENT_ENCODING = io.undertow.util.Headers.CONTENT_ENCODING_STRING;
        String ACCEPT_ENCODING = io.undertow.util.Headers.ACCEPT_ENCODING_STRING;
        String CONTENT_TYPE = io.undertow.util.Headers.CONTENT_TYPE_STRING;
        String LOCATION = io.undertow.util.Headers.LOCATION_STRING;
        String AUTHORIZATION = io.undertow.util.Headers.AUTHORIZATION_STRING;
        String DATE = io.undertow.util.Headers.DATE_STRING;
        String CONNECTION = io.undertow.util.Headers.CONNECTION_STRING;
    }

    @SuppressWarnings( "checkstyle:InterfaceIsType" )
    interface ContentType {
        String TEXT_TSV = "text/tab-separated-values";
        ///<summary>Comma-separated values; Defined in RFC 4180</summary>
        String TEXT_CSV = "text/csv";
        ///<summary>HTML; Defined in RFC 2854</summary>
        String TEXT_HTML = "text/html";
        ///<summary>Textual data; Defined in RFC 2046 and RFC 3676</summary>
        String TEXT_PLAIN = "text/plain";
        ///<summary>Extensible Markup Language; Defined in RFC 3023</summary>
        String TEXT_XML = "text/xml";
        ///<summary>JavaScript Object Notation JSON; Defined in RFC 4627</summary>
        ///<summary>JavaScript - Defined in and obsoleted by RFC 4329 in order to discourage its usage in favor of application/javascript. However,text/javascript is allowed in HTML 4 and 5 and, unlike application/javascript, has cross-browser support. The "type" attribute of the <script> tag in HTML5 is optional and there is no need to use it at all since all browsers have always assumed the correct default (even in HTML 4 where it was required by the specification).</summary>
        /// [Obsolete]
        String TEXT_JAVASCRIPT = "text/javascript";
        String APPLICATION_JSON = "application/json";
        ///<summary>Extensible Markup Language; Defined in RFC 3023</summary>
        String APPLICATION_XML = "application/xml";
        ///<summary>Arbitrary binary data.[5] Generally speaking this type identifies files that are not associated with a specific application. Contrary to past assumptions by software packages such as Apache this is not a type that should be applied to unknown files. In such a case, a server or application should not indicate a content type, as it may be incorrect, but rather, should omit the type in order to allow the recipient to guess the type.[6]</summary>
        ///<summary>SOAP; Defined by RFC 3902</summary>
        String APPLICATION_SOAP_XML = "application/soap+xml";
        String APPLICATION_OCTET_STREAM = "application/octet-stream";
        ///<summary>MIME Email; Defined in RFC 2045 and RFC 2046</summary>
        String MULTIPART_ALTERNATIVE = "multipart/alternative";
        ///<summary>MIME Email; Defined in RFC 2045 and RFC 2046</summary>
        String MULTIPART_MIXED = "multipart/mixed";
        ///<summary>MIME Email; Defined in RFC 2387 and used by MHTML (HTML mail)</summary>
        String MULTIPART_RELATED = "multipart/related";
        ///<summary>Defined in RFC 1847</summary>
        String MULTIPART_ENCRYPTED = "multipart/encrypted";
        ///<summary>Defined in RFC 1847</summary>
        String MULTIPART_SIGNED = "multipart/signed";
        ///<summary>MIME Webform; Defined in RFC 2388</summary>
        String MULTIPART_FORM_DATA = "multipart/form-data";
        /// <summary>Body contains a URL-encoded query string as per RFC 1867</summary>
        String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
        ///<summary>Tarball files</summary>
        String APPLICATION_X_TAR = "application/x-tar";

        ///<summary>Used to denote the encoding necessary for files containing JavaScript source code. The alternative MIME type for this file type is text/javascript.</summary>
        String ApplicationXJavascript = "application/x-javascript";
        ///<summary>24bit Linear PCM audio at 8-48kHz, 1-N channels; Defined in RFC 3190</summary>
        String AudioL24 = "audio/L24";
        ///<summary>Adobe Flash files for example with the extension .swf</summary>
        String ApplicationXShockwaveFlash = "application/x-shockwave-flash";
        ///<summary>Atom feeds</summary>
        String ApplicationAtomXml = "application/atom+xml";
        ///<summary>Cascading Style Sheets; Defined in RFC 2318</summary>
        String TextCss = "text/css";
        ///<summary>commands; subtype resident in Gecko browsers like Firefox 3.5</summary>
        String TextCmd = "text/cmd";
        ///<summary>deb (file format), a software package format used by the Debian project</summary>
        String ApplicationXDeb = "application/x-deb";
        ///<summary>Defined in RFC 2616</summary>
        String MessageHttp = "message/http";
        ///<summary>Defined in RFC 4735</summary>
        String ModelExample = "model/example";
        ///<summary>device-independent document in DVI format</summary>
        String ApplicationXDvi = "application/x-dvi";
        ///<summary>DTD files; Defined by RFC 3023</summary>
        String ApplicationXmlDtd = "application/xml-dtd";
        ///<summary>ECMAScript/JavaScript; Defined in RFC 4329 (equivalent to application/ecmascript but with looser processing rules) It is not accepted in IE 8 or earlier - text/javascript is accepted but it is defined as obsolete in RFC 4329. The "type" attribute of the <script> tag in HTML5 is optional and in practice omitting the media type of JavaScript programs is the most interoperable solution since all browsers have always assumed the correct default even before HTML5.</summary>
        String ApplicationJavascript = "application/javascript";
        ///<summary>ECMAScript/JavaScript; Defined in RFC 4329 (equivalent to application/javascript but with stricter processing rules)</summary>
        String ApplicationEcmascript = "application/ecmascript";
        ///<summary>EDI EDIFACT data; Defined in RFC 1767</summary>
        String ApplicationEdifact = "application/EDIFACT";
        ///<summary>EDI X12 data; Defined in RFC 1767</summary>
        String ApplicationEdiX12 = "application/EDI-X12";
        ///<summary>Email; Defined in RFC 2045 and RFC 2046</summary>
        String MessagePartial = "message/partial";
        ///<summary>Email; EML files, MIME files, MHT files, MHTML files; Defined in RFC 2045 and RFC 2046</summary>
        String MessageRfc822 = "message/rfc822";
        ///<summary>Flash video (FLV files)</summary>
        String VideoXFlv = "video/x-flv";
        ///<summary>GIF image; Defined in RFC 2045 and RFC 2046</summary>
        String ImageGif = "image/gif";
        ///<summary>GoogleWebToolkit data</summary>
        String TextXGwtRpc = "text/x-gwt-rpc";
        ///<summary>Gzip</summary>
        String ApplicationXGzip = "application/x-gzip";
        ///<summary>ICO image; Registered[9]</summary>
        String ImageVndMicrosoftIcon = "image/vnd.microsoft.icon";
        ///<summary>IGS files, IGES files; Defined in RFC 2077</summary>
        String ModelIges = "model/iges";
        ///<summary>IMDN Instant Message Disposition Notification; Defined in RFC 5438</summary>
        String MessageImdnXml = "message/imdn+xml";
        ///<summary>JavaScript Object Notation (JSON) Patch; Defined in RFC 6902</summary>
        String ApplicationJsonPatch = "application/json-patch+json";
        ///<summary>JPEG JFIF image; Associated with Internet Explorer; Listed in ms775147(v=vs.85) - Progressive JPEG, initiated before global browser support for progressive JPEGs (Microsoft and Firefox).</summary>
        String ImagePjpeg = "image/pjpeg";
        ///<summary>JPEG JFIF image; Defined in RFC 2045 and RFC 2046</summary>
        String ImageJpeg = "image/jpeg";
        ///<summary>jQuery template data</summary>
        String TextXJqueryTmpl = "text/x-jquery-tmpl";
        ///<summary>KML files (e.g. for Google Earth)</summary>
        String ApplicationVndGoogleEarthKmlXml = "application/vnd.google-earth.kml+xml";
        ///<summary>LaTeX files</summary>
        String ApplicationXLatex = "application/x-latex";
        ///<summary>Matroska open media format</summary>
        String VideoXMatroska = "video/x-matroska";
        ///<summary>Microsoft Excel 2007 files</summary>
        String ApplicationVndOpenxmlformatsOfficedocumentSpreadsheetmlSheet = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        ///<summary>Microsoft Excel files</summary>
        String ApplicationVndMsExcel = "application/vnd.ms-excel";
        ///<summary>Microsoft Powerpoint 2007 files</summary>
        String ApplicationVndOpenxmlformatsOfficedocumentPresentationmlPresentation = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        ///<summary>Microsoft Powerpoint files</summary>
        String ApplicationVndMsPowerpoint = "application/vnd.ms-powerpoint";
        ///<summary>Microsoft Word 2007 files</summary>
        String ApplicationVndOpenxmlformatsOfficedocumentWordprocessingmlDocument = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        ///<summary>Microsoft Word files[15]</summary>
        String ApplicationMsword = "application/msword";
        ///<summary>Mozilla XUL files</summary>
        String ApplicationVndMozillaXulXml = "application/vnd.mozilla.xul+xml";
        ///<summary>MP3 or other MPEG audio; Defined in RFC 3003</summary>
        String AudioMpeg = "audio/mpeg";
        ///<summary>MP4 audio</summary>
        String AudioMp4 = "audio/mp4";
        ///<summary>MP4 video; Defined in RFC 4337</summary>
        String VideoMp4 = "video/mp4";
        ///<summary>MPEG-1 video with multiplexed audio; Defined in RFC 2045 and RFC 2046</summary>
        String VideoMpeg = "video/mpeg";
        ///<summary>MSH files, MESH files; Defined in RFC 2077, SILO files</summary>
        String ModelMesh = "model/mesh";
        ///<summary>mulaw audio at 8 kHz, 1 channel; Defined in RFC 2046</summary>
        String AudioBasic = "audio/basic";
        ///<summary>Ogg Theora or other video (with audio); Defined in RFC 5334</summary>
        String VideoOgg = "video/ogg";
        ///<summary>Ogg Vorbis, Speex, Flac and other audio; Defined in RFC 5334</summary>
        String AudioOgg = "audio/ogg";
        ///<summary>Ogg, a multimedia bitstream container format; Defined in RFC 5334</summary>
        String ApplicationOgg = "application/ogg";
        ///<summary>OP</summary>
        String ApplicationXopXml = "application/xop+xml";
        ///<summary>OpenDocument Graphics; Registered[14]</summary>
        String ApplicationVndOasisOpendocumentGraphics = "application/vnd.oasis.opendocument.graphics";
        ///<summary>OpenDocument Presentation; Registered[13]</summary>
        String ApplicationVndOasisOpendocumentPresentation = "application/vnd.oasis.opendocument.presentation";
        ///<summary>OpenDocument Spreadsheet; Registered[12]</summary>
        String ApplicationVndOasisOpendocumentSpreadsheet = "application/vnd.oasis.opendocument.spreadsheet";
        ///<summary>OpenDocument Text; Registered[11]</summary>
        String ApplicationVndOasisOpendocumentText = "application/vnd.oasis.opendocument.text";
        ///<summary>p12 files</summary>
        String ApplicationXPkcs12 = "application/x-pkcs12";
        ///<summary>p7b and spc files</summary>
        String ApplicationXPkcs7Certificates = "application/x-pkcs7-certificates";
        ///<summary>p7c files</summary>
        String ApplicationXPkcs7Mime = "application/x-pkcs7-mime";
        ///<summary>p7r files</summary>
        String ApplicationXPkcs7Certreqresp = "application/x-pkcs7-certreqresp";
        ///<summary>p7s files</summary>
        String ApplicationXPkcs7Signature = "application/x-pkcs7-signature";
        ///<summary>Portable Document Format, PDF has been in use for document exchange on the Internet since 1993; Defined in RFC 3778</summary>
        String ApplicationPdf = "application/pdf";
        ///<summary>Portable Network Graphics; Registered,[8] Defined in RFC 2083</summary>
        String ImagePng = "image/png";
        ///<summary>PostScript; Defined in RFC 2046</summary>
        String ApplicationPostscript = "application/postscript";
        ///<summary>QuickTime video; Registered[10]</summary>
        String VideoQuicktime = "video/quicktime";
        ///<summary>RAR archive files</summary>
        String ApplicationXRarCompressed = "application/x-rar-compressed";
        ///<summary>RealAudio; Documented in RealPlayer Customer Support Answer 2559</summary>
        String AudioVndRnRealaudio = "audio/vnd.rn-realaudio";
        ///<summary>Resource Description Framework; Defined by RFC 3870</summary>
        String ApplicationRdfXml = "application/rdf+xml";
        ///<summary>RSS feeds</summary>
        String ApplicationRssXml = "application/rss+xml";
        ///<summary>StuffIt archive files</summary>
        String ApplicationXStuffit = "application/x-stuffit";
        ///<summary>SVG vector image; Defined in SVG Tiny 1.2 Specification Appendix M</summary>
        String ImageSvgXml = "image/svg+xml";
        ///<summary>Tag Image File Format (only for Baseline TIFF); Defined in RFC 3302</summary>
        String ImageTiff = "image/tiff";
        ///<summary>TrueType Font No registered MIME type, but this is the most commonly used</summary>
        String ApplicationXFontTtf = "application/x-font-ttf";
        ///<summary>vCard (contact information); Defined in RFC 6350</summary>
        String TextVcard = "text/vcard";
        ///<summary>Vorbis encoded audio; Defined in RFC 5215</summary>
        String AudioVorbis = "audio/vorbis";
        ///<summary>WAV audio; Defined in RFC 2361</summary>
        String AudioVndWave = "audio/vnd.wave";
        ///<summary>Web Open Font Format; (candidate recommendation; use application/x-font-woff until standard is official)</summary>
        String ApplicationFontWoff = "application/font-woff";
        ///<summary>WebM Matroska-based open media format</summary>
        String VideoWebm = "video/webm";
        ///<summary>WebM open media format</summary>
        String AudioWebm = "audio/webm";
        ///<summary>Windows Media Audio Redirector; Documented in Microsoft help page</summary>
        String AudioXMsWax = "audio/x-ms-wax";
        ///<summary>Windows Media Audio; Documented in Microsoft KB 288102</summary>
        String AudioXMsWma = "audio/x-ms-wma";
        ///<summary>Windows Media Video; Documented in Microsoft KB 288102</summary>
        String VideoXMsWmv = "video/x-ms-wmv";
        ///<summary>WRL files, VRML files; Defined in RFC 2077</summary>
        String ModelVrml = "model/vrml";
        ///<summary>X3D ISO standard for representing 3D computer graphics, X3D XML files</summary>
        String ModelX3DXml = "model/x3d+xml";
        ///<summary>X3D ISO standard for representing 3D computer graphics, X3DB binary files</summary>
        String ModelX3DBinary = "model/x3d+binary";
        ///<summary>X3D ISO standard for representing 3D computer graphics, X3DV VRML files</summary>
        String ModelX3DVrml = "model/x3d+vrml";
        ///<summary>XHTML; Defined by RFC 3236</summary>
        String ApplicationXhtmlXml = "application/xhtml+xml";
        ///<summary>ZIP archive files; Registered[7]</summary>
        String ApplicationZip = "application/zip";
    }
}
