package ui.clickupservice.invoices

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ExtractInvoiceService {

    companion object {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

        val entityMap = mapOf(
            "Central Fair Shopping Centre Pty Ltd" to "CF",
            "Central Fair No.2 Pty Ltd" to "CF2",
            "Central Fair No.3 Pty Ltd" to "CF3",
            "Central Fair No.6 Pty Ltd" to "CF6",
            "Central Fair No.7 Pty Ltd" to "CF7",
            "Pacific Hong Kong Development Limited" to "PHKD",
            "Raby Bay Harbour Properties Pty Ltd" to "RBHP",
            "Raby Bay Marina Pty Ltd" to "RBM",
            "UI International Pty Ltd" to "UI",
            "U. I. International Pty. Ltd." to "UI"
        )
    }

    fun extractInvoiceFromPDF(file: File): Invoice {

        FileInputStream(file).use {
            return this.extractInvoiceFromPDF(it)
        }
    }

    fun extractInvoiceFromPDF(inputStream: InputStream): Invoice {

        PDDocument.load(inputStream).use { document ->
            return extractInvoiceContent(document)
        }
    }

    private fun extractInvoiceContent(document: PDDocument): Invoice {

        val pdfStripper = PDFTextStripper()

        var invoiceNumber = ""
        var invoiceDate = LocalDate.now()
        var entity = ""
        var amount = BigDecimal.ZERO

        val content = pdfStripper.getText(document).lines().map { line ->
            if (line.contains("Invoice Due")) {
                return@map "$line\n>>>>"
            }

            if (line.contains("Invoice Summary")) {

                return@map "<<<<\n$line"
            }

            return@map line
        }.joinToString("\n")

        val description = content.substringAfter(">>>>").substringBefore("<<<<").replace("\n", "").trim()

        content
            .lines().forEach { line ->
                if (line.contains("TAX INVOICE Invoice No:")) {
                    invoiceNumber = line.substring(line.indexOf(":") + 2).trim()
                }

                if (line.contains("Invoice Date:")) {
                    invoiceDate = LocalDate.parse(line.substring(line.indexOf(":") + 2).trim(), formatter)
                }

                if (entityMap.containsKey(line.trim())) {
                    entity = entityMap[line.trim()] ?: throw Exception("Entity not found")
                }

                if (line.contains("Balance Payable") && amount == BigDecimal.ZERO) {
                    amount = BigDecimal(
                        line.substring(line.indexOf("Payable") + "Payable".length + 1).trim()
                            .replace("$", "")
                            .replace(",", "")
                    )
                }

                if (line.contains("Total (Inc GST)") && amount == BigDecimal.ZERO) {
                    amount = BigDecimal(
                        line.substring(line.indexOf("Total (Inc GST)") + "Total (Inc GST)".length + 1).trim()
                            .replace("$", "")
                            .replace(",", "")
                    )
                }
            }

        return Invoice(invoiceNumber = invoiceNumber, invoiceDate = invoiceDate, entity = entity, description = description, amount = amount)
    }
}