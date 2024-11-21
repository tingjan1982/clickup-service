package ui.clickupservice.sheet.service

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest
import com.google.api.services.sheets.v4.model.ValueRange
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import ui.clickupservice.shared.extension.formatNumber
import ui.clickupservice.shared.extension.toDateFormat
import ui.clickupservice.taskreminder.data.LoanTask
import ui.clickupservice.taskreminder.data.PaymentTask
import ui.clickupservice.taskreminder.service.TaskService
import java.io.File
import java.io.InputStreamReader


@Service
class UICashSheetService(@Value("classpath:/credentials.json") val resource: Resource, val taskService: TaskService) {

    companion object {
        private const val APPLICATION_NAME: String = "UI Sheet"
        private const val SHEET_ID = "106RJju-J-NNvnu_TdfbxbZZUFUgIAp_Xheu3KKzE2dU"
        private const val TOKENS_DIRECTORY_PATH: String = "tokens"
        private const val TRANSACTIONS_RANGE = "Cashflow Planning!Transactions"

        private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()
        private val SCOPES: List<String> = listOf(SheetsScopes.SPREADSHEETS)

        private val TAGS_MAP: Map<String, String> = mapOf(
            "ui-harbour" to "UI",
            "cf-bribie" to "CF",
            "cf2-gympie" to "CF2",
            "cf3-bundaberg" to "CF3",
            "cf6-tannum" to "CF6",
            "cf7-townsville" to "CF7",
            "phkd" to "PHKD",
            "phka" to "PHKA",
            "rbm" to "RBM",
            "rbhp" to "RBHP",
            "super" to "SUPER",
            "bab" to "BAB",
            "lpjp" to "LPJP"
        )

    }

    /**
     * Prints the names and majors of students in a sample spreadsheet:
     * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
     */
    fun readCashPosition() {

        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val range = "Cashflow Planning!A3:M6"
        val service = Sheets.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
            .setApplicationName(APPLICATION_NAME)
            .build()

        val response = service.spreadsheets().values()[SHEET_ID, range].execute()
        val values: List<List<Any>> = response.getValues()

        if (values.isEmpty()) {
            println("No data found.")
        } else {
            for (row in values) {
                println(row)
            }
        }
    }

    fun syncPlannedPayments() {

        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val service = Sheets.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
            .setApplicationName(APPLICATION_NAME)
            .build()

        val paymentTasks = taskService.getPlannedPaymentTasks()
            .groupBy {
                return@groupBy "${it.type.displayName}:${convertTag(it.task.toTagString())}:${it.task.dueDate.toDateFormat()}"
            }
            .toMutableMap()

        val loanTasks = taskService.getLoanTasks()
            .groupBy {
                return@groupBy "${convertTag(it.task.toTagString())}:${it.task.dueDate.toDateFormat()}:${it.loan}"
            }
            .toMutableMap()

        println("Total planned payments: ${paymentTasks.size}")
        println("Total planned loans: ${loanTasks.size}")

        val data: MutableList<ValueRange> = mutableListOf()

        service.spreadsheets().values()[SHEET_ID, TRANSACTIONS_RANGE].execute().getValues().onEachIndexed { idx, row ->

            val rowsBeforeTransactions = 11

            if (row[4] != "PAID") {
                val paymentSk = "${row[0]}:${row[3]}:${row[1]}"

                paymentTasks[paymentSk]?.let {
                    val rowIdx = idx + 11
                    val cellRange = "E${idx + rowsBeforeTransactions}"
                    println("Found payment on row $rowIdx - $row")
                    val task = it.first().task
                    data.add(ValueRange().setRange(cellRange).setValues(listOf(listOf(task.taskStatus?.uppercase(), task.name, task.id))))

                    paymentTasks.remove(paymentSk)
                }

                val loanSk = buildString {
                    append("${row[3]}:${row[1]}:")

                    if (row.size >= 6) {
                        append(row[5])
                    }
                }

                loanTasks[loanSk]?.let {
                    val rowIdx = idx + 11
                    val cellRange = "E${idx + rowsBeforeTransactions}"
                    println("Found loan on row $rowIdx - $row")
                    val task = it.first().task
                    data.add(
                        ValueRange().setRange(cellRange)
                            .setValues(listOf(listOf(task.taskStatus?.uppercase(), it.first().loan.formatNumber(), task.id)))
                    )

                    loanTasks.remove(loanSk)
                }
            }
        }

        updateTasks(data, service)
        createTasks(paymentTasks, loanTasks, service)
    }

    private fun updateTasks(data: MutableList<ValueRange>, service: Sheets) {

        print("Found ${data.size} records, updating..... ")
        val body = BatchUpdateValuesRequest()
            .setValueInputOption("USER_ENTERED")
            .setData(data)

        val result = service.spreadsheets().values().batchUpdate(SHEET_ID, body).execute()
        println("Total updated rows: ${result.totalUpdatedRows}")
    }

    private fun createTasks(
        paymentTasks: MutableMap<String, List<PaymentTask>>,
        loanTasks: MutableMap<String, List<LoanTask>>,
        service: Sheets
    ) {

        println("Total planned payments to process: ${paymentTasks.size}")

        paymentTasks.filter {
            it.value.first().task.taskStatus !in arrayOf("payment plan", "paid")
        }.forEach {
            val task = it.value.first()
            val body = ValueRange().setValues(
                listOf(
                    listOf(
                        task.type.displayName,
                        task.task.dueDate.toDateFormat(),
                        task.payment,
                        convertTag(task.task.toTagString()),
                        task.task.taskStatus?.uppercase(),
                        task.task.name,
                        task.task.id
                    )
                )
            )

            val result = service.spreadsheets().values().append(SHEET_ID, TRANSACTIONS_RANGE, body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute()

            println("Created row (${result.updates.updatedRows}): ${task.task.name}")
        }

        println("Total loans to process: ${loanTasks.size}")

        loanTasks.filter {
            it.value.first().task.taskStatus !in arrayOf("payment plan", "paid")
        }.forEach {
            val task = it.value.first()
            val body = ValueRange().setValues(
                listOf(
                    listOf(
                        PaymentTask.Type.INTEREST.displayName,
                        task.task.dueDate.toDateFormat(),
                        task.payment,
                        convertTag(task.task.toTagString()),
                        task.task.taskStatus?.uppercase(),
                        task.loan,
                        task.task.id
                    )
                )
            )

            val result = service.spreadsheets().values().append(SHEET_ID, TRANSACTIONS_RANGE, body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute()

            println("Created row (${result.updates.updatedRows}): ${task.task.name}")
        }

    }

    private fun convertTag(tag: String): String {
        return TAGS_MAP[tag] ?: throw Exception("tag is not found: $tag")
    }

    private fun getCredentials(transport: NetHttpTransport): Credential {

        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(resource.inputStream))

        // Build flow and trigger user authorization request.
        val flow = GoogleAuthorizationCodeFlow.Builder(transport, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()

        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }
}