package ui.clickupservice.shared.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ui.clickupservice.shared.exception.BusinessException

@RestControllerAdvice
class ExceptionController {

    @ExceptionHandler(BusinessException::class)
    fun handleControllerException(request: HttpServletRequest, ex: Throwable): ResponseEntity<*> {

        return ResponseEntity(MyErrorBody(400, ex.message.toString()), HttpStatus.BAD_REQUEST)
    }

    data class MyErrorBody(val code: Int, val message: String)
}