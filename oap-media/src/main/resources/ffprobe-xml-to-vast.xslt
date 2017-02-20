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
                    <AdTitle><xsl:value-of select="/ffprobe/format/@filename"/></AdTitle>
                    <Impression id="someImpId">https://tests.quple.rocks/rtb/someImpId/i?price=AAAAAAAMZu8AAAAAAAQ5UUiijIwjs5bDMYa6Kg==</Impression>
                </InLine>
                <Creatives>
                    <Creative sequence="1" id="${$id}">
                        <Linear>
                            <Duration><xsl:value-of select="/ffprobe/format/@duration"/></Duration>
                            <VideoClicks>
                                <ClickThrough>https://tests.quple.rocks/rtb/someImpId_AAAAAAAMZu8AAAAAAAQ5UUiijIwjs5bDMYa6Kg==/e?name=click&amp;landing=http%3A%2F%2Fsome.click.tracker</ClickThrough>
                            </VideoClicks>
                            <TrackingEvents>
                                <Tracking event="start">https://test.quple.rocks/rtb/someImpId_AAAAAAAMZu8AAAAAAAQ5UUiijIwjs5bDMYa6Kg==/e?name=start</Tracking>
                                <Tracking event="firstQuartile">https://test.quple.rocks/rtb/someImpId_AAAAAAAMZu8AAAAAAAQ5UUiijIwjs5bDMYa6Kg==/e?name=firstQuartile</Tracking>
                                <Tracking event="midpoint">https://test.quple.rocks/rtb/someImpId_AAAAAAAMZu8AAAAAAAQ5UUiijIwjs5bDMYa6Kg==/e?name=midpoint</Tracking>
                                <Tracking event="thirdQuartile">https://test.quple.rocks/rtb/someImpId_AAAAAAAMZu8AAAAAAAQ5UUiijIwjs5bDMYa6Kg==/e?name=thirdQuartile</Tracking>
                                <Tracking event="complete">https://test.quple.rocks/rtb/someImpId_AAAAAAAMZu8AAAAAAAQ5UUiijIwjs5bDMYa6Kg==/e?name=complete</Tracking>
                                <Tracking event="close">https://test.quple.rocks/rtb/someImpId_AAAAAAAMZu8AAAAAAAQ5UUiijIwjs5bDMYa6Kg==/e?name=close</Tracking>
                                <Tracking event="pause">https://test.quple.rocks/rtb/someImpId_AAAAAAAMZu8AAAAAAAQ5UUiijIwjs5bDMYa6Kg==/e?name=pause</Tracking>
                                <Tracking event="resume">https://test.quple.rocks/rtb/someImpId_AAAAAAAMZu8AAAAAAAQ5UUiijIwjs5bDMYa6Kg==/e?name=resume</Tracking>
                            </TrackingEvents>
                            <MediaFiles>
                                <xsl:for-each select="/ffprobe/streams/stream[@codec_type='video']">
                                    <MediaFile delivery="PROGRESSIVE" width="{current()/@width}" height="{current()/@height}" type="{$contentType}">https://publicadserver.com/<xsl:value-of select="/ffprobe/format/@filename"/></MediaFile>
                                </xsl:for-each>
                            </MediaFiles>
                        </Linear>
                    </Creative>
                </Creatives>
                <Extensions>
                    <Extension>
                        <CustomTracking>
                            <Tracking event="skip">https://test.quple.rocks/rtb/someImpId_AAAAAAAMZu8AAAAAAAQ5UUiijIwjs5bDMYa6Kg==/e?name=skip</Tracking>
                        </CustomTracking>
                    </Extension>
                </Extensions>
            </Ad>
        </VAST>
    </xsl:template>
</xsl:stylesheet>