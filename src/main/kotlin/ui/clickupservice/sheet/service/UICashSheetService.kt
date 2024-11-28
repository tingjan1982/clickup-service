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
import org.springframework.stereotype.Service
import ui.clickupservice.shared.config.ConfigProperties
import ui.clickupservice.shared.extension.formatNumber
import ui.clickupservice.shared.extension.toDateFormat
import ui.clickupservice.taskreminder.data.LoanTask
import ui.clickupservice.taskreminder.data.PaymentTask
import ui.clickupservice.taskreminder.service.TaskService
import java.io.File
import java.io.StringReader
import java.time.LocalDate


/**
 * This is another way to load resource via @Value annotation.
 * @Value("file:./credentials.json") val resource: Resource,
 */
@Service
class UICashSheetService(val taskService: TaskService, val configProperties: ConfigProperties) {

    companion object {
        private const val APPLICATION_NAME: String = "UI Sheet"
        private const val SHEET_ID = "106RJju-J-NNvnu_TdfbxbZZUFUgIAp_Xheu3KKzE2dU"
        private const val TOKENS_DIRECTORY_PATH: String = "tokens"
        private const val TRANSACTIONS_RANGE = "Cashflow Planning!Transactions"
        private const val ROWS_BEFORE_TRANSACTIONS = 11

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
            "lpjp" to "LPJP",
            "personal" to "PER"
        )

    }

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

        val data: MutableList<ValueRange> = mutableListOf()

        val paymentTasks = taskService.getPlannedPaymentTasks()
            .groupByTo(mutableMapOf()) {
                it.task.id
            }.also {
                println("Total planned payments: ${it.size}")
            }

        val loanTasks = taskService.getLoanTasks()
            .groupByTo(mutableMapOf()) {
                it.task.id
            }.also {
                println("Total planned loans: ${it.size}")

            }

        service.spreadsheets().values()[SHEET_ID, TRANSACTIONS_RANGE].execute().getValues().onEachIndexed { idx, row ->
            if (row[4] != "PAID") {
                syncPaymentTask(row, idx, paymentTasks, data)
                syncLoanTask(row, idx, loanTasks, data)
            }
        }

        updateTasks(data, service)
        createTasks(paymentTasks, loanTasks, service)
    }

    private fun syncPaymentTask(row: MutableList<Any>, idx: Int, paymentTasks: MutableMap<String, MutableList<PaymentTask>>, data: MutableList<ValueRange>) {

        val paymentSk = if (row.size >= 7) { row[6] } else { "" }

        paymentTasks[paymentSk]?.let {
            val rowIdx = idx + 11
            val cellRange = "A${idx + ROWS_BEFORE_TRANSACTIONS}"
            println("Found payment ${row[0]} on row $rowIdx - $row")

            val task = it.first().task
            data.add(
                ValueRange().setRange(cellRange)
                    .setValues(listOf(listOf(
                        it.first().type.displayName,
                        task.dueDate.toDateFormat(),
                        it.first().payment.formatNumber(),
                        convertTag(task.toTagString()),
                        task.taskStatus?.uppercase(),
                        task.name,
                        task.id,
                        "Last updated: ${LocalDate.now().toDateFormat()}")))
            )

            paymentTasks.remove(paymentSk)
        }
    }

    private fun syncLoanTask(row: MutableList<Any>, idx: Int, loanTasks: MutableMap<String, MutableList<LoanTask>>, data: MutableList<ValueRange>) {

        val loanSk = if (row.size >= 7) { row[6] } else { "" }

        loanTasks[loanSk]?.let {
            val rowIdx = idx + 11
            val cellRange = "A${idx + ROWS_BEFORE_TRANSACTIONS}"
            println("Found loan ${row[0]} on row $rowIdx - ${row[6]}")

            val task = it.first().task
            data.add(
                ValueRange().setRange(cellRange)
                    .setValues(listOf(listOf(
                        PaymentTask.Type.INTEREST.displayName,
                        task.dueDate.toDateFormat(),
                        it.first().payment.formatNumber(),
                        convertTag(task.toTagString()),
                        task.taskStatus?.uppercase(),
                        it.first().loan.formatNumber(),
                        task.id,
                        "Last updated: ${LocalDate.now().toDateFormat()}")))
            )

            loanTasks.remove(loanSk)
        }
    }

    private fun updateTasks(data: MutableList<ValueRange>, service: Sheets) {

        print("Found ${data.size} records, updating..... ")
        val body = BatchUpdateValuesRequest()
            .setValueInputOption("USER_ENTERED")
            .setData(data)

        val result = service.spreadsheets().values().batchUpdate(SHEET_ID, body).execute()
        println("Total updated rows: ${result.totalUpdatedRows}")
    }

    private fun createTasks(paymentTasks: MutableMap<String, MutableList<PaymentTask>>, loanTasks: MutableMap<String, MutableList<LoanTask>>, service: Sheets) {

        println("Total planned payments to create: ${paymentTasks.size}")

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
                        task.task.id,
                        "Created: ${LocalDate.now().toDateFormat()}"
                    )
                )
            )

            val result = service.spreadsheets().values().append(SHEET_ID, TRANSACTIONS_RANGE, body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute()

            println("Created row (${result.updates.updatedRows}): ${task.task.name}")
        }

        println("Total loans to create: ${loanTasks.size}")

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
                        task.task.id,
                        "Created: ${LocalDate.now().toDateFormat()}"
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

        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, StringReader(configProperties.googleCredentials))

        // Build flow and trigger user authorization request.
        val flow = GoogleAuthorizationCodeFlow.Builder(transport, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()

        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }
}