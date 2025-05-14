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
            "166194" to "UI"
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

        return bankAccounts
    }

    fun readDebitTransactions(): Map<String, List<DebitBankTransaction>> {

        val dir = File("$csvDir/transactions")
        val csvFile = dir.listFiles { file -> file.extension == "csv" }?.firstOrNull()!!

        val transactions = csvFile.readLines()
            .asSequence()
            .drop(1)
            .map {
                val values = it.split(",")
                val debitAmount = values[3].ifBlank { "0" }
                DebitBankTransaction(values[0], ACCOUNT_MAP[values[0].substring(6)] ?: "", BigDecimal(debitAmount), Extensions.parseLocalDate(values[1]))
            }
            .filter { it.entity.isNotBlank() }
            .filter { it.debitAmount > BigDecimal.ZERO }
            .groupBy { it.entity }

        return transactions
    }
}