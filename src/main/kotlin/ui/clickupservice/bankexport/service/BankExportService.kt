package ui.clickupservice.bankexport.service

import com.opencsv.CSVReader
import org.springframework.stereotype.Service
import ui.clickupservice.bankexport.data.BankAccount
import ui.clickupservice.bankexport.data.DebitBankTransaction
import ui.clickupservice.shared.extension.Extensions
import java.io.InputStream
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
            "522619" to "CFC",
            "581985" to "LPJP",
            "297781" to "PHKA",
            "291419" to "PHKD",
            "706231" to "RBM",
            "566910" to "RBHP",
            "728262" to "UI-SAVING",
            "166194" to "UI",
            "132947" to "PER"
        )
    }

    fun readBankBalance(csvInputStream: InputStream): Map<String, BankAccount> {

        val bankAccounts = csvInputStream.bufferedReader().useLines { lines ->
            lines
                .drop(1)
                .map {
                    val values = it.split(",")

                    BankAccount(values[3], ACCOUNT_MAP[values[3]] ?: "", BigDecimal(values[4]), Extensions.parseLocalDate(values[5]))
                }
                .filter { it.entity.isNotBlank() }
                .distinctBy { it.account }
                .associateBy { it.entity }
        }
        return bankAccounts
    }

    fun readDebitTransactions(csvInputStream: InputStream): List<DebitBankTransaction> {

        val transactions = CSVReader(csvInputStream.bufferedReader()).use { reader ->
            val rows = reader.readAll()
            rows.asSequence()
                .drop(1)
                .filter { it[0].length > 4 }
                .map {
                    val accountNumber = it[0]
                    val debitAmount = it[3].ifBlank { "0" }
                    DebitBankTransaction(accountNumber, ACCOUNT_MAP[accountNumber.substring(6)] ?: "", BigDecimal(debitAmount), Extensions.parseLocalDate(it[1]))
                }
                .filter { it.entity.isNotBlank() }
                .filter { it.debitAmount > BigDecimal.ZERO }
                .toList()
        }

        return transactions
    }
}