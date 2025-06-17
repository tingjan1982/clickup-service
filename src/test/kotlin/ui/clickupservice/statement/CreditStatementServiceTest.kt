package ui.clickupservice.statement

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ui.clickupservice.shared.FileProcessingUtils
import java.io.File

@SpringBootTest
class CreditStatementServiceTest(@Autowired val service: CreditStatementService) {

    @Test
    fun extractTransactions1212() {

        File("/Users/joelin/Downloads/estatement - 1212.pdf").let {
            service.extractTransaction(it.inputStream())

            FileProcessingUtils.markFileAsProcessed(it)
        }

    }

    @Test
    fun extractTransactions0296() {

        File("/Users/joelin/Downloads/estatement - 0296.pdf").let {
            service.extractTransaction(it.inputStream())

            FileProcessingUtils.markFileAsProcessed(it)
        }
    }
}