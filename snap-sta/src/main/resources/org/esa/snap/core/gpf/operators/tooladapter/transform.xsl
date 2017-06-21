<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
    <xsl:strip-space elements="*" />

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="operatorClass">
        <xsl:element name="operatorClass">
            <xsl:text>org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterOp</xsl:text>
        </xsl:element>
    </xsl:template>

    <xsl:template match="operator[not(templateType)]">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:element name="templateType">
                <xsl:choose>
                    <xsl:when test="contains(., '.js')">JAVASCRIPT</xsl:when>
                    <xsl:otherwise>VELOCITY</xsl:otherwise>
                </xsl:choose>
            </xsl:element>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="templateFileLocation">
        <xsl:element name="template">
            <xsl:attribute name="type">file</xsl:attribute>
            <xsl:element name="file">
                <xsl:value-of select="."/>
            </xsl:element>
        </xsl:element>
    </xsl:template>

    <xsl:template match="template">
        <xsl:element name="template">
            <xsl:if test="not(@type)">
                <xsl:attribute name="type">file</xsl:attribute>
            </xsl:if>
            <xsl:element name="file">
                <xsl:value-of select="."/>
            </xsl:element>
        </xsl:element>
    </xsl:template>

    <xsl:template match="org.esa.snap.core.gpf.descriptor.DefaultSourceProductDescriptor">
        <xsl:element name="org.esa.snap.core.gpf.descriptor.SimpleSourceProductDescriptor">
            <xsl:element name="name"><xsl:value-of select="./name"/></xsl:element>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="parameter">
        <xsl:choose>
            <xsl:when test="contains(./parameterType/text(), 'Template')">
                <xsl:element name="templateparameter">
                    <xsl:copy-of select="name"/>
                    <xsl:copy-of select="dataType"/>
                    <xsl:copy-of select="defaultValue"/>
                    <xsl:copy-of select="description"/>
                    <xsl:copy-of select="valueSet"/>
                    <xsl:copy-of select="notNull"/>
                    <xsl:copy-of select="notEmpty"/>
                    <xsl:copy-of select="parameterType"/>
                    <xsl:element name="template">
                        <xsl:element name="file">
                            <xsl:value-of select="defaultValue"/>
                        </xsl:element>
                    </xsl:element>
                    <xsl:copy-of select="outputFile"/>
                    <xsl:element name="parameters">
                        <xsl:copy-of select="toolParameterDescriptors/*"/>
                    </xsl:element>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:element name="parameter">
                    <xsl:copy-of select="name"/>
                    <xsl:copy-of select="dataType"/>
                    <xsl:copy-of select="defaultValue"/>
                    <xsl:copy-of select="description"/>
                    <xsl:copy-of select="valueSet"/>
                    <xsl:copy-of select="notNull"/>
                    <xsl:copy-of select="notEmpty"/>
                    <xsl:copy-of select="parameterType"/>
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>