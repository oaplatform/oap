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

package oap.io;

import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

import static oap.util.Maps.byValue;

public class MimeTypes {
    public static final String APPLICATION_ANDREW_INSET = "application/andrew-inset";
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_ZIP = "application/zip";
    public static final String APPLICATION_X_GZIP = "application/x-gzip";
    public static final String APPLICATION_TGZ = "application/tgz";
    public static final String APPLICATION_MSWORD = "application/msword";
    public static final String APPLICATION_POSTSCRIPT = "application/postscript";
    public static final String APPLICATION_PDF = "application/pdf";
    public static final String APPLICATION_JNLP = "application/jnlp";
    public static final String APPLICATION_MAC_BINHEX40 = "application/mac-binhex40";
    public static final String APPLICATION_MAC_COMPACTPRO = "application/mac-compactpro";
    public static final String APPLICATION_MATHML_XML = "application/mathml+xml";
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String APPLICATION_ODA = "application/oda";
    public static final String APPLICATION_RDF_XML = "application/rdf+xml";
    public static final String APPLICATION_JAVA_ARCHIVE = "application/java-archive";
    public static final String APPLICATION_RDF_SMIL = "application/smil";
    public static final String APPLICATION_SRGS = "application/srgs";
    public static final String APPLICATION_SRGS_XML = "application/srgs+xml";
    public static final String APPLICATION_VND_MIF = "application/vnd.mif";
    public static final String APPLICATION_VND_MSEXCEL = "application/vnd.ms-excel";
    public static final String APPLICATION_VND_MSPOWERPOINT = "application/vnd.ms-powerpoint";
    public static final String APPLICATION_VND_RNREALMEDIA = "application/vnd.rn-realmedia";
    public static final String APPLICATION_X_BCPIO = "application/x-bcpio";
    public static final String APPLICATION_X_CDLINK = "application/x-cdlink";
    public static final String APPLICATION_X_CHESS_PGN = "application/x-chess-pgn";
    public static final String APPLICATION_X_CPIO = "application/x-cpio";
    public static final String APPLICATION_X_CSH = "application/x-csh";
    public static final String APPLICATION_X_DIRECTOR = "application/x-director";
    public static final String APPLICATION_X_DVI = "application/x-dvi";
    public static final String APPLICATION_X_FUTURESPLASH = "application/x-futuresplash";
    public static final String APPLICATION_X_GTAR = "application/x-gtar";
    public static final String APPLICATION_X_HDF = "application/x-hdf";
    public static final String APPLICATION_X_JAVASCRIPT = "application/x-javascript";
    public static final String APPLICATION_X_KOAN = "application/x-koan";
    public static final String APPLICATION_X_LATEX = "application/x-latex";
    public static final String APPLICATION_X_NETCDF = "application/x-netcdf";
    public static final String APPLICATION_X_OGG = "application/x-ogg";
    public static final String APPLICATION_X_SH = "application/x-sh";
    public static final String APPLICATION_X_SHAR = "application/x-shar";
    public static final String APPLICATION_X_SHOCKWAVE_FLASH = "application/x-shockwave-flash";
    public static final String APPLICATION_X_STUFFIT = "application/x-stuffit";
    public static final String APPLICATION_X_SV4CPIO = "application/x-sv4cpio";
    public static final String APPLICATION_X_SV4CRC = "application/x-sv4crc";
    public static final String APPLICATION_X_TAR = "application/x-tar";
    public static final String APPLICATION_X_RAR_COMPRESSED = "application/x-rar-compressed";
    public static final String APPLICATION_X_TCL = "application/x-tcl";
    public static final String APPLICATION_X_TEX = "application/x-tex";
    public static final String APPLICATION_X_TEXINFO = "application/x-texinfo";
    public static final String APPLICATION_X_TROFF = "application/x-troff";
    public static final String APPLICATION_X_TROFF_MAN = "application/x-troff-man";
    public static final String APPLICATION_X_TROFF_ME = "application/x-troff-me";
    public static final String APPLICATION_X_TROFF_MS = "application/x-troff-ms";
    public static final String APPLICATION_X_USTAR = "application/x-ustar";
    public static final String APPLICATION_X_WAIS_SOURCE = "application/x-wais-source";
    public static final String APPLICATION_VND_MOZZILLA_XUL_XML = "application/vnd.mozilla.xul+xml";
    public static final String APPLICATION_XHTML_XML = "application/xhtml+xml";
    public static final String APPLICATION_XSLT_XML = "application/xslt+xml";
    public static final String APPLICATION_XML = "application/xml";
    public static final String APPLICATION_XML_DTD = "application/xml-dtd";
    public static final String IMAGE_BMP = "image/bmp";
    public static final String IMAGE_CGM = "image/cgm";
    public static final String IMAGE_GIF = "image/gif";
    public static final String IMAGE_IEF = "image/ief";
    public static final String IMAGE_JPEG = "image/jpeg";
    public static final String IMAGE_TIFF = "image/tiff";
    public static final String IMAGE_PNG = "image/png";
    public static final String IMAGE_SVG_XML = "image/svg+xml";
    public static final String IMAGE_VND_DJVU = "image/vnd.djvu";
    public static final String IMAGE_WAP_WBMP = "image/vnd.wap.wbmp";
    public static final String IMAGE_X_CMU_RASTER = "image/x-cmu-raster";
    public static final String IMAGE_X_ICON = "image/x-icon";
    public static final String IMAGE_X_PORTABLE_ANYMAP = "image/x-portable-anymap";
    public static final String IMAGE_X_PORTABLE_BITMAP = "image/x-portable-bitmap";
    public static final String IMAGE_X_PORTABLE_GRAYMAP = "image/x-portable-graymap";
    public static final String IMAGE_X_PORTABLE_PIXMAP = "image/x-portable-pixmap";
    public static final String IMAGE_X_RGB = "image/x-rgb";
    public static final String AUDIO_BASIC = "audio/basic";
    public static final String AUDIO_MIDI = "audio/midi";
    public static final String AUDIO_MPEG = "audio/mpeg";
    public static final String AUDIO_X_AIFF = "audio/x-aiff";
    public static final String AUDIO_X_MPEGURL = "audio/x-mpegurl";
    public static final String AUDIO_X_PN_REALAUDIO = "audio/x-pn-realaudio";
    public static final String AUDIO_X_WAV = "audio/x-wav";
    public static final String CHEMICAL_X_PDB = "chemical/x-pdb";
    public static final String CHEMICAL_X_XYZ = "chemical/x-xyz";
    public static final String MODEL_IGES = "model/iges";
    public static final String MODEL_MESH = "model/mesh";
    public static final String MODEL_VRLM = "model/vrml";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String TEXT_RICHTEXT = "text/richtext";
    public static final String TEXT_RTF = "text/rtf";
    public static final String TEXT_HTML = "text/html";
    public static final String TEXT_CALENDAR = "text/calendar";
    public static final String TEXT_CSS = "text/css";
    public static final String TEXT_SGML = "text/sgml";
    public static final String TEXT_TAB_SEPARATED_VALUES = "text/tab-separated-values";
    public static final String TEXT_VND_WAP_XML = "text/vnd.wap.wml";
    public static final String TEXT_VND_WAP_WMLSCRIPT = "text/vnd.wap.wmlscript";
    public static final String TEXT_X_SETEXT = "text/x-setext";
    public static final String TEXT_X_COMPONENT = "text/x-component";
    public static final String VIDEO_QUICKTIME = "video/quicktime";
    public static final String VIDEO_MPEG = "video/mpeg";
    public static final String VIDEO_VND_MPEGURL = "video/vnd.mpegurl";
    public static final String VIDEO_X_MSVIDEO = "video/x-msvideo";
    public static final String VIDEO_X_MS_WMV = "video/x-ms-wmv";
    public static final String VIDEO_X_SGI_MOVIE = "video/x-sgi-movie";
    public static final String X_CONFERENCE_X_COOLTALK = "x-conference/x-cooltalk";

    private static HashMap<String, String> mimetypes = new HashMap<>();

    static {
        register( "xul", APPLICATION_VND_MOZZILLA_XUL_XML );
        register( "json", APPLICATION_JSON );
        register( "ice", X_CONFERENCE_X_COOLTALK );
        register( "movie", VIDEO_X_SGI_MOVIE );
        register( "avi", VIDEO_X_MSVIDEO );
        register( "wmv", VIDEO_X_MS_WMV );
        register( "m4u", VIDEO_VND_MPEGURL );
        register( "mxu", VIDEO_VND_MPEGURL );
        register( "htc", TEXT_X_COMPONENT );
        register( "etx", TEXT_X_SETEXT );
        register( "wmls", TEXT_VND_WAP_WMLSCRIPT );
        register( "wml", TEXT_VND_WAP_XML );
        register( "tsv", TEXT_TAB_SEPARATED_VALUES );
        register( "sgm", TEXT_SGML );
        register( "sgml", TEXT_SGML );
        register( "css", TEXT_CSS );
        register( "ifb", TEXT_CALENDAR );
        register( "ics", TEXT_CALENDAR );
        register( "wrl", MODEL_VRLM );
        register( "vrlm", MODEL_VRLM );
        register( "silo", MODEL_MESH );
        register( "mesh", MODEL_MESH );
        register( "msh", MODEL_MESH );
        register( "iges", MODEL_IGES );
        register( "igs", MODEL_IGES );
        register( "rgb", IMAGE_X_RGB );
        register( "ppm", IMAGE_X_PORTABLE_PIXMAP );
        register( "pgm", IMAGE_X_PORTABLE_GRAYMAP );
        register( "pbm", IMAGE_X_PORTABLE_BITMAP );
        register( "pnm", IMAGE_X_PORTABLE_ANYMAP );
        register( "ico", IMAGE_X_ICON );
        register( "ras", IMAGE_X_CMU_RASTER );
        register( "wbmp", IMAGE_WAP_WBMP );
        register( "djv", IMAGE_VND_DJVU );
        register( "djvu", IMAGE_VND_DJVU );
        register( "svg", IMAGE_SVG_XML );
        register( "ief", IMAGE_IEF );
        register( "cgm", IMAGE_CGM );
        register( "bmp", IMAGE_BMP );
        register( "xyz", CHEMICAL_X_XYZ );
        register( "pdb", CHEMICAL_X_PDB );
        register( "ra", AUDIO_X_PN_REALAUDIO );
        register( "ram", AUDIO_X_PN_REALAUDIO );
        register( "m3u", AUDIO_X_MPEGURL );
        register( "aifc", AUDIO_X_AIFF );
        register( "aif", AUDIO_X_AIFF );
        register( "aiff", AUDIO_X_AIFF );
        register( "mp3", AUDIO_MPEG );
        register( "mp2", AUDIO_MPEG );
        register( "mp1", AUDIO_MPEG );
        register( "mpga", AUDIO_MPEG );
        register( "kar", AUDIO_MIDI );
        register( "mid", AUDIO_MIDI );
        register( "midi", AUDIO_MIDI );
        register( "dtd", APPLICATION_XML_DTD );
        register( "xsl", APPLICATION_XML );
        register( "xml", APPLICATION_XML );
        register( "xslt", APPLICATION_XSLT_XML );
        register( "xht", APPLICATION_XHTML_XML );
        register( "xhtml", APPLICATION_XHTML_XML );
        register( "src", APPLICATION_X_WAIS_SOURCE );
        register( "ustar", APPLICATION_X_USTAR );
        register( "ms", APPLICATION_X_TROFF_MS );
        register( "me", APPLICATION_X_TROFF_ME );
        register( "man", APPLICATION_X_TROFF_MAN );
        register( "roff", APPLICATION_X_TROFF );
        register( "tr", APPLICATION_X_TROFF );
        register( "t", APPLICATION_X_TROFF );
        register( "texi", APPLICATION_X_TEXINFO );
        register( "texinfo", APPLICATION_X_TEXINFO );
        register( "tex", APPLICATION_X_TEX );
        register( "tcl", APPLICATION_X_TCL );
        register( "sv4crc", APPLICATION_X_SV4CRC );
        register( "sv4cpio", APPLICATION_X_SV4CPIO );
        register( "sit", APPLICATION_X_STUFFIT );
        register( "swf", APPLICATION_X_SHOCKWAVE_FLASH );
        register( "shar", APPLICATION_X_SHAR );
        register( "sh", APPLICATION_X_SH );
        register( "cdf", APPLICATION_X_NETCDF );
        register( "nc", APPLICATION_X_NETCDF );
        register( "latex", APPLICATION_X_LATEX );
        register( "skm", APPLICATION_X_KOAN );
        register( "skt", APPLICATION_X_KOAN );
        register( "skd", APPLICATION_X_KOAN );
        register( "skp", APPLICATION_X_KOAN );
        register( "js", APPLICATION_X_JAVASCRIPT );
        register( "hdf", APPLICATION_X_HDF );
        register( "gtar", APPLICATION_X_GTAR );
        register( "spl", APPLICATION_X_FUTURESPLASH );
        register( "dvi", APPLICATION_X_DVI );
        register( "dxr", APPLICATION_X_DIRECTOR );
        register( "dir", APPLICATION_X_DIRECTOR );
        register( "dcr", APPLICATION_X_DIRECTOR );
        register( "csh", APPLICATION_X_CSH );
        register( "cpio", APPLICATION_X_CPIO );
        register( "pgn", APPLICATION_X_CHESS_PGN );
        register( "vcd", APPLICATION_X_CDLINK );
        register( "bcpio", APPLICATION_X_BCPIO );
        register( "rm", APPLICATION_VND_RNREALMEDIA );
        register( "ppt", APPLICATION_VND_MSPOWERPOINT );
        register( "mif", APPLICATION_VND_MIF );
        register( "grxml", APPLICATION_SRGS_XML );
        register( "gram", APPLICATION_SRGS );
        register( "smil", APPLICATION_RDF_SMIL );
        register( "smi", APPLICATION_RDF_SMIL );
        register( "rdf", APPLICATION_RDF_XML );
        register( "ogg", APPLICATION_X_OGG );
        register( "oda", APPLICATION_ODA );
        register( "dmg", APPLICATION_OCTET_STREAM );
        register( "lzh", APPLICATION_OCTET_STREAM );
        register( "so", APPLICATION_OCTET_STREAM );
        register( "lha", APPLICATION_OCTET_STREAM );
        register( "dms", APPLICATION_OCTET_STREAM );
        register( "bin", APPLICATION_OCTET_STREAM );
        register( "mathml", APPLICATION_MATHML_XML );
        register( "cpt", APPLICATION_MAC_COMPACTPRO );
        register( "hqx", APPLICATION_MAC_BINHEX40 );
        register( "jnlp", APPLICATION_JNLP );
        register( "ez", APPLICATION_ANDREW_INSET );
        register( "txt", TEXT_PLAIN );
        register( "rtf", TEXT_RTF );
        register( "rtx", TEXT_RICHTEXT );
        register( "html", TEXT_HTML );
        register( "htm", TEXT_HTML );
        register( "zip", APPLICATION_ZIP );
        register( "rar", APPLICATION_X_RAR_COMPRESSED );
        register( "gzip", APPLICATION_X_GZIP );
        register( "gz", APPLICATION_X_GZIP );
        register( "tgz", APPLICATION_TGZ );
        register( "tar", APPLICATION_X_TAR );
        register( "gif", IMAGE_GIF );
        register( "jpeg", IMAGE_JPEG );
        register( "jpg", IMAGE_JPEG );
        register( "jpe", IMAGE_JPEG );
        register( "tiff", IMAGE_TIFF );
        register( "tif", IMAGE_TIFF );
        register( "png", IMAGE_PNG );
        register( "au", AUDIO_BASIC );
        register( "snd", AUDIO_BASIC );
        register( "wav", AUDIO_X_WAV );
        register( "mov", VIDEO_QUICKTIME );
        register( "qt", VIDEO_QUICKTIME );
        register( "mpeg", VIDEO_MPEG );
        register( "mpg", VIDEO_MPEG );
        register( "mpe", VIDEO_MPEG );
        register( "abs", VIDEO_MPEG );
        register( "doc", APPLICATION_MSWORD );
        register( "xls", APPLICATION_VND_MSEXCEL );
        register( "eps", APPLICATION_POSTSCRIPT );
        register( "ai", APPLICATION_POSTSCRIPT );
        register( "ps", APPLICATION_POSTSCRIPT );
        register( "pdf", APPLICATION_PDF );
        register( "exe", APPLICATION_OCTET_STREAM );
        register( "dll", APPLICATION_OCTET_STREAM );
        register( "class", APPLICATION_OCTET_STREAM );
        register( "jar", APPLICATION_JAVA_ARCHIVE );
    }

    public static void register( String key, String value ) {
        if( mimetypes.put( key, value ) != null ) throw new IllegalArgumentException( "duplicate extension: " + key );
    }

    public static Optional<String> extensionOf( String mimeType ) {
        return byValue( mimetypes, mimeType );
    }

    public static Optional<String> mimetypeOf( String ext ) {
        return Optional.ofNullable( mimetypes.get( ext ) );
    }

    public static Optional<String> mimetypeOf( Path path ) {

        return Optional.ofNullable( mimetypes.get( FilenameUtils.getExtension( path.toString() ) ) );
    }
}
