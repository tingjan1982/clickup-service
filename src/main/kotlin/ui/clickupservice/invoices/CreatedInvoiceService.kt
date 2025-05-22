package ui.clickupservice.invoices

import com.google.api.services.sheets.v4.model.ValueRange
import org.springframework.stereotype.Service
import ui.clickupservice.shared.GoogleApiUtils
import ui.clickupservice.shared.extension.toDateFormat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Service
class CreatedInvoiceService(val pdfInvoiceService: ExtractInvoiceService, val googleApiUtils: GoogleApiUtils) {

    fun readGoogleDriveFile() {

        val service = googleApiUtils.getDriveService()

        val folderId = "1lXMXBu4n0hUT3sGhLRTRs9tagoDAjJ-i"
        val result = service.files().list()
            .setQ("'$folderId' in parents and trashed = false")
            .setFields("files(id, name)")
            .execute()

        val files = result.files
        if (files.isNullOrEmpty()) {
            println("No files found.")
        } else {
            for (file in files) {
                println("Found file: ${file.name} (${file.id})")

                val outputStream = ByteArrayOutputStream()
                service.files().get(file.id).executeMediaAndDownloadTo(outputStream)

                val inputStream = ByteArrayInputStream(outputStream.toByteArray())
                val invoice = pdfInvoiceService.extractInvoiceFromPDF(inputStream)
                println(invoice)

                writeToInvoiceSheet(invoice)
            }
        }
    }

    private fun writeToInvoiceSheet(invoice: Invoice) {

        val sheetId = "1XLHMC8oM8jnfCtdh_KoFmd871lzaIBGOUVQATvzZFoE"
        val service = googleApiUtils.getSheetService()

        val body = ValueRange().setValues(
            listOf(
                listOf(
                    invoice.invoiceDate.toDateFormat(),
                    invoice.entity,
                    invoice.invoiceNumber,
                    invoice.description,
                    "fill in pls",
                    invoice.amount.toString(),
                    "REVIEW"
                )
            )
        )

        val result = service.spreadsheets().values().append(sheetId, "RBG!A1", body)
            .setValueInputOption("USER_ENTERED")
            .setInsertDataOption("INSERT_ROWS")
            .execute()

        println("Created row (${result.updates.updatedRows})")
    }
}