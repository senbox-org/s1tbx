<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="html"
                version="1.0"
                encoding="ISO-8859-1"
                omit-xml-declaration="yes"
                standalone="yes"
                doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
                doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
                cdata-section-elements="value"
                indent="yes"
                media-type="text/html"/>

    <xsl:template match="/">
        <html>
            <head>
                <title>Feature Extraction Result</title>
                <link rel="stylesheet" type="text/css" href="fex-overview.css"/>
            </head>
            <body>
                <xsl:apply-templates/>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="featureExtraction">
        <h2>Feature Extraction Results</h2>
        <table>
            <tr>
                <xsl:apply-templates select="featureType"/>
            </tr>
            <xsl:apply-templates select="patch"/>
        </table>
    </xsl:template>

    <xsl:template match="patch">
        <tr>
            <xsl:apply-templates/>
        </tr>
    </xsl:template>

    <xsl:template match="featureType">
        <th class="fName">
            <xsl:value-of select="@name"/>
        </th>
    </xsl:template>

    <xsl:template match="feature">
        <xsl:choose>
            <xsl:when test="@type = 'img'">
                <td class="fImage">
                    <img>
                        <xsl:attribute name="src">
                            <xsl:value-of select="."/>
                        </xsl:attribute>
                    </img>
                </td>
            </xsl:when>
            <xsl:when test="@type = 'raw'">
                <td class = "fProduct">
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="."/>
                        </xsl:attribute>
                        <xsl:value-of select="."/>
                    </a>
                </td>
            </xsl:when>
            <xsl:otherwise>
                <td class = "fValue">
                    <xsl:choose>
                        <xsl:when test="count(./*) = 0">
                            <p class="fValue">
                                <xsl:value-of select="."/>
                            </p>
                        </xsl:when>
                        <xsl:otherwise>
                            <table>
                                <xsl:for-each select="./*">
                                    <tr>
                                        <td class="fAttrName">
                                            <emp>
                                                <xsl:value-of select="local-name()"/>
                                            </emp>
                                            :
                                        </td>
                                        <td class="fAttrValue">
                                            <xsl:value-of select="."/>
                                        </td>
                                    </tr>
                                </xsl:for-each>
                            </table>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
