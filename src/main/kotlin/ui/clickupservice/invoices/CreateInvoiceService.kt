package ui.clickupservice.invoices

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.sheets.v4.model.ValueRange
import org.springframework.stereotype.Service
import ui.clickupservice.shared.GoogleApiUtils
import ui.clickupservice.shared.exception.BusinessException
import ui.clickupservice.shared.extension.toDateFormat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Service
class CreateInvoiceService(val pdfInvoiceService: ExtractInvoiceService, val googleApiUtils: GoogleApiUtils) {

    companion object {
        const val RBG_PENDING_FOLDER_ID = "1lXMXBu4n0hUT3sGhLRTRs9tagoDAjJ-i"
        const val RBG_FOLDER_ID = "1OKsjIntwO2m8E3f67dX-ZvKsoMc6MnHt"
    }

    fun writeInvoicesFromGoogleDrive(): Int {

        val service = googleApiUtils.getDriveService()

        val result = service.files().list()
            .setQ("'$RBG_PENDING_FOLDER_ID' in parents and trashed = false")
            .setFields("files(id, name)")
            .execute()

        val files = result.files
        if (files.isNullOrEmpty()) {
            println("No files found.")
            throw BusinessException("No files in RBG Pending folder")
        } else {
            for (file in files) {
                println("Found file: ${file.name} (${file.id})")

                val outputStream = ByteArrayOutputStream()
                service.files().get(file.id).executeMediaAndDownloadTo(outputStream)

                val inputStream = ByteArrayInputStream(outputStream.toByteArray())
                val invoice = pdfInvoiceService.extractInvoiceFromPDF(inputStream)
                println(invoice)

                writeToInvoiceSheet(invoice)
                updateAndMoveFile(service, file)
            }

            return files.count()
        }
    }

    private fun updateAndMoveFile(service: Drive, file: File) {

        if (!file.name.endsWith("processed")) {
            println("Mark file as processed: ${file.name}")
            service.files().update(file.id, File().setName("${file.name} - processed")).execute()
        }

        val getParent = service.files().get(file.id)
            .setFields("parents")
            .execute()
        val previousParents = getParent.parents?.joinToString(separator = ",")

        val movedFile = service.files().update(file.id, null)
            .setAddParents(RBG_FOLDER_ID)
            .setRemoveParents(previousParents)
            .setFields("id, parents")
            .execute()

        println("Moved to folder ID: ${movedFile.parents}")
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
                    "FILL IN",
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