package ui.clickupservice.statement.web

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ui.clickupservice.shared.web.ApiResponse
import ui.clickupservice.statement.CreditStatementService
import java.io.File

@RestController
@RequestMapping("/statements")
class StatementController(val service: CreditStatementService) {

    @PostMapping("/card1212")
    fun importCard1212(): ApiResponse {

        File("/Users/joelin/Downloads/import-source/estatement - 1212.pdf").let {
            service.extractTransaction(it)

            return ApiResponse("Statement 1212 has been imported.")
        }
    }

    @PostMapping("/card0296")
    fun importCard0296(): ApiResponse {

        File("/Users/joelin/Downloads/import-source/estatement - 0296.pdf").let {
            service.extractTransaction(it)

            return ApiResponse("Statement 0296 has been imported.")
        }
    }
}