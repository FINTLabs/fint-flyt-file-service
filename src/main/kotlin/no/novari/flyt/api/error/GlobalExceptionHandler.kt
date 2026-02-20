package no.novari.flyt.api.error

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import no.novari.flyt.files.domain.exception.FileNotFoundException
import no.novari.flyt.files.domain.exception.FileStorageException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = KotlinLogging.logger {}

    @ExceptionHandler(FileNotFoundException::class)
    fun handleFileNotFound(
        exception: FileNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        log.atWarn {
            message = "File not found for request path={}"
            arguments = arrayOf(request.requestURI)
            cause = exception
        }
        return buildResponse(HttpStatus.NOT_FOUND, "File not found", request)
    }

    @ExceptionHandler(
        IllegalArgumentException::class,
        MethodArgumentNotValidException::class,
        ConstraintViolationException::class,
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun handleBadRequest(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        log.atWarn {
            message = "Invalid request for path={}"
            arguments = arrayOf(request.requestURI)
            cause = exception
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request", request)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(
        exception: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        log.atWarn {
            message = "Method not allowed for path={}"
            arguments = arrayOf(request.requestURI)
            cause = exception
        }
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", request)
    }

    @ExceptionHandler(FileStorageException::class)
    fun handleStorageException(
        exception: FileStorageException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        log.atError {
            message = "File storage operation failed for path={}"
            arguments = arrayOf(request.requestURI)
            cause = exception
        }
        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Failed to process file operation",
            request,
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        log.atError {
            message = "Unexpected error for path={}"
            arguments = arrayOf(request.requestURI)
            cause = exception
        }
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request)
    }

    private fun buildResponse(
        status: HttpStatus,
        message: String,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatusAndDetail(status, message)
        problemDetail.title = status.reasonPhrase
        problemDetail.setProperty("path", request.requestURI)
        problemDetail.setProperty("timestamp", Instant.now().toString())
        return ResponseEntity.status(status).body(problemDetail)
    }
}
