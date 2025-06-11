package ui.clickupservice.bankexport

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ui.clickupservice.bankexport.service.BankExportService
import java.io.File

@SpringBootTest
class BankExportServiceTest(@Autowired val service: BankExportService) {

    companion object {
        const val CSV_DIR = "/Users/joelin/Downloads/import-source"

        val getCsvFile = fun(dir: File): File {
            val csvFile = dir.listFiles { file -> file.extension == "csv" && !file.nameWithoutExtension.endsWith("processed") }?.firstOrNull()

            return csvFile ?: throw Exception("File does not exist or it has been mark as processed: $dir")
        }
    }

    @Test
    fun readBankBalance() {

        val csvFile = getCsvFile(File(CSV_DIR))

        service.readBankBalance(csvFile.inputStream()).forEach {
            println(it)
        }

        val newFile = File(csvFile.parentFile, "${csvFile.nameWithoutExtension} - processed.${csvFile.extension}")
        csvFile.renameTo(newFile)
        println("Bank balance file has been marked as processed")

    }

    @Test
    fun readTransactions() {

        val csvFile = getCsvFile(File("$CSV_DIR/transactions"))

        service.readDebitTransactions(csvFile.inputStream()).forEach {
            println(it)
        }

        val newFile = File(csvFile.parentFile, "${csvFile.nameWithoutExtension} - processed.${csvFile.extension}")
        csvFile.renameTo(newFile)
        println("Transactions file has been marked as processed")
    }
}