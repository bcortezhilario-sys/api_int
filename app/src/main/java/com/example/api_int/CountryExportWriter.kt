package com.example.api_int

import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object CountryExportWriter {
    private val integerFormat = DecimalFormat("#,##0")
    private val decimalFormat = DecimalFormat("#,##0.0")

    fun createExcelFile(cacheDir: File, country: MainActivity.CountryResult): File {
        val file = exportFile(cacheDir, country, "xlsx")
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            zip.putTextEntry("[Content_Types].xml", excelContentTypes())
            zip.putTextEntry("_rels/.rels", excelRootRelationships())
            zip.putTextEntry("xl/workbook.xml", excelWorkbook())
            zip.putTextEntry("xl/_rels/workbook.xml.rels", excelWorkbookRelationships())
            zip.putTextEntry("xl/styles.xml", excelStyles())
            zip.putTextEntry("xl/worksheets/sheet1.xml", excelSheet(country))
        }
        return file
    }

    fun createPowerPointFile(cacheDir: File, country: MainActivity.CountryResult): File {
        val file = exportFile(cacheDir, country, "pptx")
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            zip.putTextEntry("[Content_Types].xml", powerPointContentTypes())
            zip.putTextEntry("_rels/.rels", powerPointRootRelationships())
            zip.putTextEntry("docProps/app.xml", powerPointAppProperties())
            zip.putTextEntry("docProps/core.xml", powerPointCoreProperties(country))
            zip.putTextEntry("ppt/presentation.xml", powerPointPresentation())
            zip.putTextEntry("ppt/_rels/presentation.xml.rels", powerPointPresentationRelationships())
            zip.putTextEntry("ppt/presProps.xml", powerPointPresentationProperties())
            zip.putTextEntry("ppt/viewProps.xml", powerPointViewProperties())
            zip.putTextEntry("ppt/tableStyles.xml", powerPointTableStyles())
            zip.putTextEntry("ppt/slides/slide1.xml", powerPointSlide(country))
            zip.putTextEntry("ppt/slides/_rels/slide1.xml.rels", powerPointSlideRelationships())
            zip.putTextEntry("ppt/slideLayouts/slideLayout1.xml", powerPointSlideLayout())
            zip.putTextEntry("ppt/slideLayouts/_rels/slideLayout1.xml.rels", powerPointSlideLayoutRelationships())
            zip.putTextEntry("ppt/slideMasters/slideMaster1.xml", powerPointSlideMaster())
            zip.putTextEntry("ppt/slideMasters/_rels/slideMaster1.xml.rels", powerPointSlideMasterRelationships())
            zip.putTextEntry("ppt/theme/theme1.xml", powerPointTheme())
        }
        return file
    }

    private fun exportFile(cacheDir: File, country: MainActivity.CountryResult, extension: String): File {
        val exportsDir = File(cacheDir, "exports").apply { mkdirs() }
        return File(exportsDir, "${country.displayName.safeFileSegment()}_pais.$extension").apply {
            if (exists()) {
                delete()
            }
        }
    }

    private fun excelSheet(country: MainActivity.CountryResult): String {
        val rows = country.exportRows()
        val rowXml = buildString {
            append(
                """
                <row r="1">
                    ${excelTextCell("A1", "Campo", 1)}
                    ${excelTextCell("B1", "Valor", 1)}
                </row>
                """.trimIndent()
            )
            rows.forEachIndexed { index, row ->
                val rowNumber = index + 2
                val valueCell = row.rawNumber?.let { excelNumberCell("B$rowNumber", it) }
                    ?: excelTextCell("B$rowNumber", row.displayValue)
                append(
                    """
                    <row r="$rowNumber">
                        ${excelTextCell("A$rowNumber", row.label)}
                        $valueCell
                    </row>
                    """.trimIndent()
                )
            }
        }

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <dimension ref="A1:B${rows.size + 1}"/>
                <sheetViews>
                    <sheetView workbookViewId="0"/>
                </sheetViews>
                <sheetFormatPr defaultRowHeight="18"/>
                <cols>
                    <col min="1" max="1" width="24" customWidth="1"/>
                    <col min="2" max="2" width="72" customWidth="1"/>
                </cols>
                <sheetData>
                    $rowXml
                </sheetData>
                <pageMargins left="0.7" right="0.7" top="0.75" bottom="0.75" header="0.3" footer="0.3"/>
            </worksheet>
        """.trimIndent()
    }

    private fun excelTextCell(reference: String, text: String, style: Int? = null): String {
        val styleAttribute = style?.let { """ s="$it"""" }.orEmpty()
        return """<c r="$reference"$styleAttribute t="inlineStr"><is><t>${text.xmlEscape()}</t></is></c>"""
    }

    private fun excelNumberCell(reference: String, number: String): String {
        return """<c r="$reference"><v>$number</v></c>"""
    }

    private fun excelContentTypes(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
            </Types>
        """.trimIndent()
    }

    private fun excelRootRelationships(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent()
    }

    private fun excelWorkbook(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                <sheets>
                    <sheet name="Pais" sheetId="1" r:id="rId1"/>
                </sheets>
            </workbook>
        """.trimIndent()
    }

    private fun excelWorkbookRelationships(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
            </Relationships>
        """.trimIndent()
    }

    private fun excelStyles(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <fonts count="2">
                    <font><sz val="11"/><name val="Calibri"/></font>
                    <font><b/><sz val="11"/><name val="Calibri"/></font>
                </fonts>
                <fills count="2">
                    <fill><patternFill patternType="none"/></fill>
                    <fill><patternFill patternType="gray125"/></fill>
                </fills>
                <borders count="1">
                    <border><left/><right/><top/><bottom/><diagonal/></border>
                </borders>
                <cellStyleXfs count="1">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
                </cellStyleXfs>
                <cellXfs count="2">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                </cellXfs>
                <cellStyles count="1">
                    <cellStyle name="Normal" xfId="0" builtinId="0"/>
                </cellStyles>
            </styleSheet>
        """.trimIndent()
    }

    private fun powerPointSlide(country: MainActivity.CountryResult): String {
        val title = listOf(country.flagEmoji, country.displayName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val subtitle = listOf(country.officialName, country.code)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
        val leftLines = listOf(
            "Capital: ${country.capital.valueOrUnavailable()}",
            "Region: ${country.region.valueOrUnavailable()}",
            "Subregion: ${country.subregion.valueOrUnavailable()}",
            "Poblacion: ${country.population.displayPopulation()}",
            "Area: ${country.area.displayArea()}",
            "Densidad: ${country.displayDensity()}"
        )
        val rightLines = listOf(
            "Idiomas: ${country.languages.displayList()}",
            "Monedas: ${country.currencies.displayList()}",
            "Zonas horarias: ${country.timezones.displayList()}",
            "Continentes: ${country.continents.displayList()}",
            "Mapa: ${country.mapUrl.valueOrUnavailable()}"
        )

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
                <p:cSld>
                    <p:bg>
                        <p:bgPr>
                            <a:solidFill><a:srgbClr val="F8FAFC"/></a:solidFill>
                            <a:effectLst/>
                        </p:bgPr>
                    </p:bg>
                    <p:spTree>
                        ${powerPointGroupShape()}
                        ${powerPointTextBox(2, "Titulo", 548640, 365760, 11033760, 762000, powerPointParagraph(title, 3600, "0F172A", true))}
                        ${powerPointTextBox(3, "Nombre oficial", 548640, 1097280, 11033760, 457200, powerPointParagraph(subtitle.ifBlank { "Datos del pais" }, 1700, "475467"))}
                        ${powerPointTextBox(4, "Datos principales", 685800, 1859280, 5090160, 3352800, powerPointParagraph("Datos principales", 2200, "0F172A", true) + leftLines.joinToString("") { powerPointParagraph(it, 1800, "344054") })}
                        ${powerPointTextBox(5, "Contexto", 6248400, 1859280, 5090160, 3352800, powerPointParagraph("Contexto", 2200, "0F172A", true) + rightLines.joinToString("") { powerPointParagraph(it, 1650, "344054") })}
                        ${powerPointTextBox(6, "Fuente", 685800, 5791200, 10515600, 365760, powerPointParagraph("Fuente: Rest Countries API", 1350, "667085"))}
                    </p:spTree>
                </p:cSld>
                <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
            </p:sld>
        """.trimIndent()
    }

    private fun powerPointTextBox(
        id: Int,
        name: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        paragraphs: String
    ): String {
        return """
            <p:sp>
                <p:nvSpPr>
                    <p:cNvPr id="$id" name="${name.xmlEscape()}"/>
                    <p:cNvSpPr txBox="1"/>
                    <p:nvPr/>
                </p:nvSpPr>
                <p:spPr>
                    <a:xfrm>
                        <a:off x="$x" y="$y"/>
                        <a:ext cx="$width" cy="$height"/>
                    </a:xfrm>
                    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
                    <a:noFill/>
                    <a:ln><a:noFill/></a:ln>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr wrap="square" anchor="t"><a:spAutoFit/></a:bodyPr>
                    <a:lstStyle/>
                    $paragraphs
                </p:txBody>
            </p:sp>
        """.trimIndent()
    }

    private fun powerPointParagraph(
        text: String,
        fontSize: Int,
        color: String,
        bold: Boolean = false
    ): String {
        val boldAttribute = if (bold) """ b="1"""" else ""
        return """
            <a:p>
                <a:r>
                    <a:rPr lang="es-ES" sz="$fontSize"$boldAttribute>
                        <a:solidFill><a:srgbClr val="$color"/></a:solidFill>
                    </a:rPr>
                    <a:t>${text.xmlEscape()}</a:t>
                </a:r>
            </a:p>
        """.trimIndent()
    }

    private fun powerPointGroupShape(): String {
        return """
            <p:nvGrpSpPr>
                <p:cNvPr id="1" name=""/>
                <p:cNvGrpSpPr/>
                <p:nvPr/>
            </p:nvGrpSpPr>
            <p:grpSpPr>
                <a:xfrm>
                    <a:off x="0" y="0"/>
                    <a:ext cx="0" cy="0"/>
                    <a:chOff x="0" y="0"/>
                    <a:chExt cx="0" cy="0"/>
                </a:xfrm>
            </p:grpSpPr>
        """.trimIndent()
    }

    private fun powerPointContentTypes(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
                <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
                <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
                <Override PartName="/ppt/presProps.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presProps+xml"/>
                <Override PartName="/ppt/viewProps.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.viewProps+xml"/>
                <Override PartName="/ppt/tableStyles.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.tableStyles+xml"/>
                <Override PartName="/ppt/slides/slide1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
                <Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
                <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
                <Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
            </Types>
        """.trimIndent()
    }

    private fun powerPointRootRelationships(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
                <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
                <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
            </Relationships>
        """.trimIndent()
    }

    private fun powerPointPresentation(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
                <p:sldMasterIdLst>
                    <p:sldMasterId id="2147483648" r:id="rId1"/>
                </p:sldMasterIdLst>
                <p:sldIdLst>
                    <p:sldId id="256" r:id="rId2"/>
                </p:sldIdLst>
                <p:sldSz cx="12192000" cy="6858000" type="wide"/>
                <p:notesSz cx="6858000" cy="9144000"/>
                <p:defaultTextStyle/>
            </p:presentation>
        """.trimIndent()
    }

    private fun powerPointPresentationRelationships(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>
                <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide1.xml"/>
                <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/presProps" Target="presProps.xml"/>
                <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/viewProps" Target="viewProps.xml"/>
                <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/tableStyles" Target="tableStyles.xml"/>
            </Relationships>
        """.trimIndent()
    }

    private fun powerPointSlideRelationships(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
            </Relationships>
        """.trimIndent()
    }

    private fun powerPointSlideLayoutRelationships(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>
            </Relationships>
        """.trimIndent()
    }

    private fun powerPointSlideMasterRelationships(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
                <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>
            </Relationships>
        """.trimIndent()
    }

    private fun powerPointSlideLayout(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank" preserve="1">
                <p:cSld name="Blank">
                    <p:spTree>
                        ${powerPointGroupShape()}
                    </p:spTree>
                </p:cSld>
                <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
            </p:sldLayout>
        """.trimIndent()
    }

    private fun powerPointSlideMaster(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
                <p:cSld>
                    <p:spTree>
                        ${powerPointGroupShape()}
                    </p:spTree>
                </p:cSld>
                <p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/>
                <p:sldLayoutIdLst>
                    <p:sldLayoutId id="2147483649" r:id="rId1"/>
                </p:sldLayoutIdLst>
                <p:txStyles/>
            </p:sldMaster>
        """.trimIndent()
    }

    private fun powerPointAppProperties(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
                <Application>Pasaporte Mundial</Application>
                <PresentationFormat>Widescreen</PresentationFormat>
                <Slides>1</Slides>
            </Properties>
        """.trimIndent()
    }

    private fun powerPointCoreProperties(country: MainActivity.CountryResult): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                <dc:title>${country.displayName.xmlEscape()}</dc:title>
                <dc:creator>Pasaporte Mundial</dc:creator>
                <cp:lastModifiedBy>Pasaporte Mundial</cp:lastModifiedBy>
            </cp:coreProperties>
        """.trimIndent()
    }

    private fun powerPointPresentationProperties(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:presentationPr xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"/>
        """.trimIndent()
    }

    private fun powerPointViewProperties(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:viewPr xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"/>
        """.trimIndent()
    }

    private fun powerPointTableStyles(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <a:tblStyleLst xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" def="{5C22544A-7EE6-4342-B048-85BDC9FD1C3A}"/>
        """.trimIndent()
    }

    private fun powerPointTheme(): String {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Office Theme">
                <a:themeElements>
                    <a:clrScheme name="Office">
                        <a:dk1><a:sysClr val="windowText" lastClr="000000"/></a:dk1>
                        <a:lt1><a:sysClr val="window" lastClr="FFFFFF"/></a:lt1>
                        <a:dk2><a:srgbClr val="1F2937"/></a:dk2>
                        <a:lt2><a:srgbClr val="F8FAFC"/></a:lt2>
                        <a:accent1><a:srgbClr val="2563EB"/></a:accent1>
                        <a:accent2><a:srgbClr val="059669"/></a:accent2>
                        <a:accent3><a:srgbClr val="F59E0B"/></a:accent3>
                        <a:accent4><a:srgbClr val="DB2777"/></a:accent4>
                        <a:accent5><a:srgbClr val="7C3AED"/></a:accent5>
                        <a:accent6><a:srgbClr val="0EA5E9"/></a:accent6>
                        <a:hlink><a:srgbClr val="2563EB"/></a:hlink>
                        <a:folHlink><a:srgbClr val="7C3AED"/></a:folHlink>
                    </a:clrScheme>
                    <a:fontScheme name="Office">
                        <a:majorFont><a:latin typeface="Aptos Display"/><a:ea typeface=""/><a:cs typeface=""/></a:majorFont>
                        <a:minorFont><a:latin typeface="Aptos"/><a:ea typeface=""/><a:cs typeface=""/></a:minorFont>
                    </a:fontScheme>
                    <a:fmtScheme name="Office">
                        <a:fillStyleLst>
                            <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
                            <a:gradFill rotWithShape="1"><a:gsLst><a:gs pos="0"><a:schemeClr val="phClr"/></a:gs><a:gs pos="100000"><a:schemeClr val="phClr"/></a:gs></a:gsLst><a:lin ang="5400000" scaled="0"/></a:gradFill>
                            <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
                        </a:fillStyleLst>
                        <a:lnStyleLst>
                            <a:ln w="6350"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln>
                            <a:ln w="12700"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln>
                            <a:ln w="19050"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln>
                        </a:lnStyleLst>
                        <a:effectStyleLst>
                            <a:effectStyle><a:effectLst/></a:effectStyle>
                            <a:effectStyle><a:effectLst/></a:effectStyle>
                            <a:effectStyle><a:effectLst/></a:effectStyle>
                        </a:effectStyleLst>
                        <a:bgFillStyleLst>
                            <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
                            <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
                            <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
                        </a:bgFillStyleLst>
                    </a:fmtScheme>
                </a:themeElements>
            </a:theme>
        """.trimIndent()
    }

    private fun MainActivity.CountryResult.exportRows(): List<ExportRow> {
        return listOf(
            ExportRow("Pais", displayName.valueOrUnavailable()),
            ExportRow("Nombre oficial", officialName.valueOrUnavailable()),
            ExportRow("Codigo", code.valueOrUnavailable()),
            ExportRow("Capital", capital.valueOrUnavailable()),
            ExportRow("Region", region.valueOrUnavailable()),
            ExportRow("Subregion", subregion.valueOrUnavailable()),
            ExportRow("Poblacion", population.displayPopulation(), population.takeIf { it > 0 }?.toString()),
            ExportRow("Area (km2)", area.displayArea(), area.takeIf { it > 0.0 }?.asXmlNumber()),
            ExportRow("Densidad (hab/km2)", displayDensity(), density.takeIf { population > 0 && area > 0.0 }?.asXmlNumber()),
            ExportRow("Idiomas", languages.displayList()),
            ExportRow("Monedas", currencies.displayList()),
            ExportRow("Zonas horarias", timezones.displayList()),
            ExportRow("Continentes", continents.displayList()),
            ExportRow("Mapa", mapUrl.valueOrUnavailable()),
            ExportRow("Fuente", "Rest Countries API")
        )
    }

    private fun MainActivity.CountryResult.displayDensity(): String {
        return if (population > 0 && area > 0.0) {
            "${decimalFormat.format(density)} hab/km2"
        } else {
            "No disponible"
        }
    }

    private fun Long.displayPopulation(): String {
        return if (this > 0) integerFormat.format(this) else "No disponible"
    }

    private fun Double.displayArea(): String {
        return if (this > 0.0) "${integerFormat.format(this)} km2" else "No disponible"
    }

    private fun List<String>.displayList(): String {
        return takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "No disponible"
    }

    private fun String.valueOrUnavailable(): String {
        return if (isBlank()) "No disponible" else this
    }

    private fun Double.asXmlNumber(): String {
        return String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
    }

    private fun String.safeFileSegment(): String {
        return trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "pais" }
    }

    private fun String.xmlEscape(): String {
        return replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun ZipOutputStream.putTextEntry(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private data class ExportRow(
        val label: String,
        val displayValue: String,
        val rawNumber: String? = null
    )
}
