package ui.clickupservice.shared.extension

import mu.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
@Aspect
class LoggingAspect {

    @Around("execution(* ui.clickupservice..*(..)) && @target(org.springframework.stereotype.Service)")
    fun serviceMethod(pjp: ProceedingJoinPoint): Any? {

        logger.info { "Invoking function ${pjp.signature.name}" }

        return pjp.proceed()
    }
}