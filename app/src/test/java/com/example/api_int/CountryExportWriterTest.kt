package com.example.api_int

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile

class CountryExportWriterTest {
    @Test
    fun writesExcelAndPowerPointPackages() {
        val outputDir = Files.createTempDirectory("country-export").toFile()
        val country = MainActivity.CountryResult(
            displayName = "Peru",
            officialName = "Republic of Peru",
            code = "PE",
            capital = "Lima",
            region = "Americas",
            subregion = "South America",
            population = 34_352_719,
            area = 1_285_216.0,
            languages = listOf("Spanish", "Quechua", "Aymara"),
            currencies = listOf("Peruvian sol (PEN, S/)"),
            timezones = listOf("UTC-05:00"),
            continents = listOf("South America"),
            flagEmoji = "\uD83C\uDDF5\uD83C\uDDEA",
            flagPngUrl = "https://flagcdn.com/w320/pe.png",
            mapUrl = "https://goo.gl/maps/uDWEUaXNcZTng1fP6"
        )

        val excelFile = CountryExportWriter.createExcelFile(outputDir, country)
        val powerPointFile = CountryExportWriter.createPowerPointFile(outputDir, country)

        assertTrue(excelFile.exists())
        assertTrue(powerPointFile.exists())
        assertZipContains(
            excelFile,
            "[Content_Types].xml",
            "xl/workbook.xml",
            "xl/worksheets/sheet1.xml",
            "xl/styles.xml"
        )
        assertZipContains(
            powerPointFile,
            "[Content_Types].xml",
            "ppt/presentation.xml",
            "ppt/slides/slide1.xml",
            "ppt/slideLayouts/slideLayout1.xml",
            "ppt/slideMasters/slideMaster1.xml",
            "ppt/theme/theme1.xml"
        )
    }

    private fun assertZipContains(file: File, vararg expectedEntries: String) {
        ZipFile(file).use { zip ->
            val names = mutableSetOf<String>()
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                names.add(entries.nextElement().name)
            }
            expectedEntries.forEach { entry ->
                assertTrue("${file.name} should contain $entry", names.contains(entry))
            }
        }
    }
}
