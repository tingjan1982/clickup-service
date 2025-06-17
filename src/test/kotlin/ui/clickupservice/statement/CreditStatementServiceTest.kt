package ui.clickupservice.statement

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ui.clickupservice.shared.FileProcessingUtils
import ui.clickupservice.shared.extension.Extensions
import java.io.File

@SpringBootTest
class CreditStatementServiceTest(@Autowired val service: CreditStatementService) {

    @Test
    fun extractTransactions1212() {

        File("/Users/joelin/Downloads/estatement - 1212.pdf").let {
            service.extractTransactions(it.inputStream())

            FileProcessingUtils.markFileAsProcessed(it)
        }

    }

    @Test
    fun extractTransactions0296() {

        File("/Users/joelin/Downloads/estatement - 0296.pdf").let {
            service.extractTransactions(it.inputStream())

            FileProcessingUtils.markFileAsProcessed(it)
        }
    }

    @Test
    fun isStatementUploadedBefore() {

        service.isStatementUploadedBefore("1212", Extensions.parseLocalDate("03/07/2025")).let {
            println("Result is $it")
        }
    }
}