package ui.clickupservice.bankexport.service

import org.springframework.stereotype.Service
import ui.clickupservice.bankexport.data.BankAccount
import ui.clickupservice.bankexport.data.DebitBankTransaction
import ui.clickupservice.shared.extension.Extensions
import java.io.File
import java.math.BigDecimal

@Service
class BankExportService {

    companion object {
        val ACCOUNT_MAP: Map<String, String> = mapOf(
            "508462" to "BAB",
            "159960" to "CF",
            "537677" to "CF2",
            "602950" to "CF3",
            "653889" to "CF6",
            "675092" to "CF7",
            "581985" to "LPJP",
            "297781" to "PHKA",
            "291419" to "PHKD",
            "706231" to "RBM",
            "566910" to "RBHP",
            "728262" to "UI-SAVING",
            "166194" to "UI",
            "132947" to "PER"
        )

        private val csvDir = "/Users/joelin/Downloads/csv"
    }

    fun readBankBalance(): Map<String, BankAccount> {

        val dir = File(csvDir)
        val csvFile = dir.listFiles { file -> file.extension == "csv" }?.firstOrNull()!!

        val bankAccounts = csvFile.readLines()
            .asSequence()
            .drop(1)
            .map {
                val values = it.split(",")

                BankAccount(values[3], ACCOUNT_MAP[values[3]] ?: "", BigDecimal(values[4]), Extensions.parseLocalDate(values[5]))
            }
            .filter { it.entity.isNotBlank() }
            .distinctBy { it.account }
            .associateBy { it.entity }

        csvFile.delete()
        println("Bank balance file has been deleted: ${csvFile.name}")

        return bankAccounts
    }

    fun readDebitTransactions(): List<DebitBankTransaction> {

        val dir = File("$csvDir/transactions")
        val csvFile = dir.listFiles { file -> file.extension == "csv" }?.firstOrNull()!!

        val transactions = csvFile.readLines()
            .asSequence()
            .drop(1)
            .map { it.split(",") }
            .filter { it[0].length > 4 }
            .map {
                val accountNumber = it[0]
                val debitAmount = it[3].ifBlank { "0" }
                DebitBankTransaction(accountNumber, ACCOUNT_MAP[accountNumber.substring(6)] ?: "", BigDecimal(debitAmount), Extensions.parseLocalDate(it[1]))
            }
            .filter { it.entity.isNotBlank() }
            .filter { it.debitAmount > BigDecimal.ZERO }
            .toList()

        csvFile.delete()
        println("Transactions file has been deleted: ${csvFile.name}")

        return transactions
    }
}