package ui.clickupservice.statement.web

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import ui.clickupservice.shared.web.ApiResponse
import ui.clickupservice.statement.CreditStatementService

@RestController
@RequestMapping("/statements")
class CreditStatementController(val service: CreditStatementService) {

    @PostMapping("/import")
    fun importCard(@RequestParam("file") file: MultipartFile): ApiResponse {

        service.extractTransactions(file.inputStream).let {
            return ApiResponse("Statement $it has been imported.")
        }
    }

    @PostMapping("/populateExpenseTasks")
    fun populateExpenseTasks(): ApiResponse {

        service.populateExpenseTasks().let { (cardNumber, dueDate) ->
            return ApiResponse("[Card $cardNumber due on $dueDate] Expense tasks have been updated")
        }
    }
}