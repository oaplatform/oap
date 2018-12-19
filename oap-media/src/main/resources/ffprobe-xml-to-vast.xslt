<!--
  ~ The MIT License (MIT)
  ~
  ~ Copyright (c) Open Application Platform Authors
  ~
  ~ Permission is hereby granted, free of charge, to any person obtaining a copy
  ~ of this software and associated documentation files (the "Software"), to deal
  ~ in the Software without restriction, including without limitation the rights
  ~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  ~ copies of the Software, and to permit persons to whom the Software is
  ~ furnished to do so, subject to the following conditions:
  ~
  ~ The above copyright notice and this permission notice shall be included in all
  ~ copies or substantial portions of the Software.
  ~
  ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  ~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  ~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  ~ SOFTWARE.
  -->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:param name="id"/>
    <xsl:param name="contentType"/>

    <xsl:template match="/">
        <VAST version="2.0">
            <Ad id="{$id}">
                <InLine>
                    <AdSystem version="v2.2.0">Madberry</AdSystem>
                    <AdTitle>
                        <xsl:value-of select="/ffprobe/model/@filename"/>
                    </AdTitle>
                    <Impression id="someImpId">
                        https://tests.quple.rocks/rtb/someImpId/i?price=AAAAAAAMZu8AAAAAAAQ5UUiijIwjs5bDMYa6Kg==
                    </Impression>
                </InLine>
                <Creatives>
                    <Creative sequence="1" id="${$id}">
                        <Linear>
                            <Duration>
                                <xsl:value-of select="/ffprobe/model/@duration"/>
                            </Duration>
                            <VideoClicks/>
                            <TrackingEvents/>
                            <MediaFiles>
                                <xsl:for-each select="/ffprobe/streams/stream[@codec_type='video']">
                                    <MediaFile delivery="progressive" width="{current()/@width}"
                                               height="{current()/@height}" type="{$contentType}">${STORAGE_URL}
                                        <xsl:value-of select="$id"/>
                                    </MediaFile>
                                </xsl:for-each>
                            </MediaFiles>
                        </Linear>
                    </Creative>
                </Creatives>
                <Extensions>
                    <Extension>
                        <CustomTracking>
                            <Tracking event="skip">
                                https://test.quple.rocks/rtb/someImpId_AAAAAAAMZu8AAAAAAAQ5UUiijIwjs5bDMYa6Kg==/e?name=skip
                            </Tracking>
                        </CustomTracking>
                    </Extension>
                </Extensions>
            </Ad>
        </VAST>
    </xsl:template>
</xsl:stylesheet>
