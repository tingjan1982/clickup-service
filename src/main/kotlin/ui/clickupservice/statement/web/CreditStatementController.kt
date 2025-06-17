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

    @PostMapping("/card1212")
    fun importCard1212(@RequestParam("file") file: MultipartFile): ApiResponse {

        service.extractTransactions(file.inputStream)
        return ApiResponse("Statement 1212 has been imported.")

    }

    @PostMapping("/card0296")
    fun importCard0296(@RequestParam("file") file: MultipartFile): ApiResponse {

        service.extractTransactions(file.inputStream)
        return ApiResponse("Statement 0296 has been imported.")
    }
}