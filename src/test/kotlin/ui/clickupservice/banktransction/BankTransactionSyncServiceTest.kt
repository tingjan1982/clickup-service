package ui.clickupservice.banktransction

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import ui.clickupservice.bankexport.data.DebitBankTransaction
import ui.clickupservice.bankexport.service.BankExportService
import ui.clickupservice.banktransction.service.BankTransactionSyncService
import ui.clickupservice.shared.extension.toDate
import ui.clickupservice.sheet.service.UICashSheetService
import ui.clickupservice.taskreminder.data.LoanTask
import ui.clickupservice.taskreminder.data.PaymentTask
import ui.clickupservice.taskreminder.data.Tasks
import ui.clickupservice.taskreminder.service.TaskService
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
class BankTransactionSyncServiceTest(@Autowired val service: BankTransactionSyncService, @Autowired val bankExportService: BankExportService) {

    @MockitoBean
    lateinit var taskService: TaskService

    @MockitoBean
    lateinit var uiCashSheetService: UICashSheetService

    val csv = """
            Bank Account,Date,Narrative,Debit Amount,Credit Amount
            000000508462,01/01/2026,Debit payment,123.45,
            000000159960,02/01/2026,Credit payment,,456.78
            000000537677,03/01/2026,Zero row,,
        """.trimIndent()

    @Test
    fun syncBankTransactions() {

        `when`(taskService.getLoanTasks()).thenReturn(
            listOf(
                loanTask("loan-1", "bab", "100000", "111.11"),
                loanTask("loan-2", "cf-bribie", "200000", "222.22")
            )
        )
        `when`(taskService.getPlannedPaymentTasks()).thenReturn(
            listOf(
                paymentTask("payment-1", "bab", LocalDate.of(2026, 1, 1), "123.45"),
                paymentTask("refund-2", "cf-bribie", LocalDate.of(2026, 1, 2), "-456.78")
            )
        )

        bankExportService.readDebitTransactions(csv.byteInputStream()).let {
            service.syncBankTransactions(it)
        }

        verify(taskService).getLoanTasks()
        verify(taskService).getPlannedPaymentTasks()
        verify(taskService, times(2)).updateTaskStatus(anyTask(), eqPaidStatus())
        verify(uiCashSheetService).syncPlannedPayments()

    }

    @Test
    fun syncBankTransactionsMatchesLoanPaymentOnNextBusinessDay() {

        `when`(taskService.getLoanTasks()).thenReturn(
            listOf(
                loanTask("loan-1", "bab", LocalDate.of(2026, 1, 2), "100000", "123.45"),
                loanTask("loan-2", "cf-bribie", LocalDate.of(2026, 1, 2), "200000", "456.78")
            )
        )
        `when`(taskService.getPlannedPaymentTasks()).thenReturn(emptyList())

        service.syncBankTransactions(
            listOf(
                debitTransaction("BAB", LocalDate.of(2026, 1, 5), "123.45")
            )
        )

        verify(taskService, times(1)).updateTaskStatus(anyTask(), eqPaidStatus())
    }

    @Test
    fun syncBankTransactionsMatchesCombinedLoanPaymentAtEndOfMonth() {

        `when`(taskService.getLoanTasks()).thenReturn(
            listOf(
                loanTask("loan-1", "bab", LocalDate.of(2026, 1, 15), "100000", "123.45"),
                loanTask("loan-2", "bab", LocalDate.of(2026, 1, 20), "200000", "456.78"),
                loanTask("loan-3", "cf-bribie", LocalDate.of(2026, 1, 20), "300000", "100.00")
            )
        )
        `when`(taskService.getPlannedPaymentTasks()).thenReturn(emptyList())

        service.syncBankTransactions(
            listOf(
                debitTransaction("BAB", LocalDate.of(2026, 1, 30), "580.23")
            )
        )

        verify(taskService, times(2)).updateTaskStatus(anyTask(), eqPaidStatus())
    }

    @Test
    fun syncBankTransactionsMatchesCombinedLoanPaymentOnDueDate() {

        `when`(taskService.getLoanTasks()).thenReturn(
            listOf(
                loanTask("loan-1", "bab", LocalDate.of(2026, 1, 15), "100000", "123.45"),
                loanTask("loan-2", "bab", LocalDate.of(2026, 1, 15), "200000", "456.78"),
                loanTask("loan-3", "cf-bribie", LocalDate.of(2026, 1, 15), "300000", "100.00")
            )
        )
        `when`(taskService.getPlannedPaymentTasks()).thenReturn(emptyList())

        service.syncBankTransactions(
            listOf(
                debitTransaction("BAB", LocalDate.of(2026, 1, 15), "580.23")
            )
        )

        verify(taskService, times(2)).updateTaskStatus(anyTask(), eqPaidStatus())
    }

    private fun loanTask(id: String, tag: String, loan: String, payment: String): LoanTask {
        return loanTask(id, tag, LocalDate.of(2026, 1, 1), loan, payment)
    }

    private fun loanTask(id: String, tag: String, dueDate: LocalDate, loan: String, payment: String): LoanTask {
        return LoanTask(task(id, tag, dueDate), BigDecimal(loan), BigDecimal(payment))
    }

    private fun paymentTask(id: String, tag: String, dueDate: LocalDate, payment: String): PaymentTask {
        return PaymentTask(task(id, tag, dueDate), PaymentTask.Type.NA, BigDecimal(payment))
    }

    private fun task(id: String, tag: String, dueDate: LocalDate = LocalDate.of(2026, 1, 1)): Tasks.Task {
        return Tasks.Task(
            id = id,
            name = id,
            dueDate = dueDate.toDate(),
            taskStatus = "scheduled",
            tags = listOf(Tasks.Task.Tag(tag)),
            customFields = emptyList()
        )
    }

    private fun anyTask(): Tasks.Task {
        any(Tasks.Task::class.java)
        return task("any-task", "bab")
    }

    private fun eqPaidStatus(): String {
        eq("PAID")
        return "PAID"
    }

    private fun debitTransaction(entity: String, date: LocalDate, amount: String): DebitBankTransaction {
        return DebitBankTransaction(
            account = "",
            entity = entity,
            debitAmount = BigDecimal(amount),
            creditAmount = BigDecimal.ZERO,
            date = date
        )
    }
}
