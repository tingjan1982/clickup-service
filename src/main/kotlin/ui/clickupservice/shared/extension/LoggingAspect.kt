package ui.clickupservice.shared.extension

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ui.clickupservice.taskreminder.service.TaskReminderService

@Component
@Aspect
class LoggingAspect {

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(TaskReminderService::class.java)
    }


    @Around("execution(* ui.clickupservice..*(..)) && @target(org.springframework.stereotype.Service)")
    fun serviceMethod(pjp: ProceedingJoinPoint): Any? {

        LOGGER.info("Invoking function ${pjp.signature.name}")

        return pjp.proceed()
    }
}