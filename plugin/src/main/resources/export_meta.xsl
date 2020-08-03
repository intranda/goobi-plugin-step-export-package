<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:a="http://www.lunaimaging.com/xsd" xsi:schemaLocation="http://www.loc.gov/standards/premis/ http://www.loc.gov/standards/premis/v2/premis-v2-0.xsd http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-7.xsd http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/mets.xsd http://www.loc.gov/standards/mix/ http://www.loc.gov/standards/mix/mix.xsd" xmlns:mets="http://www.loc.gov/METS/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:goobi="http://meta.goobi.org/v1.5.1/" xmlns:xlink="http://www.w3.org/1999/xlink">

<xsl:variable name="date" select="/mets:mets/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods/mods:extension/goobi:goobi/goobi:metadata[@name='PublicationYear']" />
<xsl:variable name="cat_id" select="/mets:mets/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods/mods:extension/goobi:goobi/goobi:metadata[@name='CatalogIDSource']" />
<xsl:variable name="title" select="/mets:mets/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods/mods:extension/goobi:goobi/goobi:metadata[@name='TitleDocMain']" />
<xsl:variable name="creator" select="/mets:mets/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods/mods:extension/goobi:goobi/goobi:metadata[@name='CreatorsAllOrigin']" />
<xsl:variable name="creator_name" select="/mets:mets/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods/mods:extension/goobi:goobi/goobi:metadata/goobi:displayName" />

	<xsl:template match="/">
		<record>
		
			<xsl:for-each select="/mets:mets/mets:dmdSec">
				<element>
					<xsl:for-each select="mets:mdWrap/mets:xmlData/mods:mods/mods:extension/goobi:goobi/goobi:metadata">
						<metadata>
							<name>
								<xsl:value-of select="./@name"/>
							</name>
							<value>
								<xsl:value-of select="."/>
							</value>
						</metadata>
					</xsl:for-each>
				</element>
			</xsl:for-each>
		
			<pages>
				<xsl:for-each select="mets:mets/mets:structLink/mets:smLink">
					<xsl:variable name="counter" select= "position()" />
					<page name="xlinkTo">
						<value>
							<xsl:value-of select='./@xlink:to'/>
						</value>	
					</page>
				</xsl:for-each> 
			</pages>
				
			</record>
		</xsl:template>



		

</xsl:stylesheet>