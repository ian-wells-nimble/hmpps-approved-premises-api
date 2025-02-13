package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.util.ContentCachingRequestWrapper
import org.zalando.problem.Problem
import org.zalando.problem.Status
import org.zalando.problem.StatusType
import org.zalando.problem.ThrowableProblem
import org.zalando.problem.spring.common.AdviceTrait
import org.zalando.problem.spring.web.advice.ProblemHandling
import org.zalando.problem.spring.web.advice.io.MessageNotReadableAdviceTrait
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DeserializationValidationService

@ControllerAdvice
class ExceptionHandling(
  private val objectMapper: ObjectMapper,
  private val deserializationValidationService: DeserializationValidationService
) : ProblemHandling, MessageNotReadableAdviceTrait {
  override fun toProblem(throwable: Throwable, status: StatusType): ThrowableProblem? {
    if (throwable is AuthenticationCredentialsNotFoundException) {
      return UnauthenticatedProblem()
    }

    if (throwable is AccessDeniedException) {
      return ForbiddenProblem()
    }

    return AdviceTraitDefault().toProblem(throwable, status)
  }

  override fun handleMessageNotReadableException(
    exception: HttpMessageNotReadableException,
    request: NativeWebRequest
  ): ResponseEntity<Problem> {
    val responseBuilder = Problem.builder()
      .withStatus(Status.BAD_REQUEST)
      .withTitle("Bad Request")

    when (exception.cause) {
      is MismatchedInputException -> {
        val mismatchedInputException = exception.cause as MismatchedInputException

        val requestBody = request.getNativeRequest(ContentCachingRequestWrapper::class.java)
        val jsonTree = objectMapper.readTree(String(requestBody.contentAsByteArray))

        if (expectedArrayButGotObject(jsonTree, mismatchedInputException)) {
          responseBuilder.withDetail("Expected an array but got an object")
          return ResponseEntity<Problem>(responseBuilder.build(), HttpStatus.BAD_REQUEST)
        }

        if (expectedObjectButGotArray(jsonTree, mismatchedInputException)) {
          responseBuilder.withDetail("Expected an object but got an array")
          return ResponseEntity<Problem>(responseBuilder.build(), HttpStatus.BAD_REQUEST)
        }

        val badRequestProblem = if (rootIsArray(mismatchedInputException)) {
          val arrayItemsType = (mismatchedInputException.path[1].from as Class<*>).kotlin

          BadRequestProblem(
            invalidParams = deserializationValidationService.validateArray(
              targetType = arrayItemsType,
              jsonArray = jsonTree as ArrayNode
            )
          )
        } else {
          val objectType = (mismatchedInputException.path[0].from as Class<*>).kotlin

          BadRequestProblem(
            invalidParams = deserializationValidationService.validateObject(
              targetType = objectType,
              jsonObject = jsonTree as ObjectNode
            )
          )
        }

        return ResponseEntity(badRequestProblem, HttpStatus.BAD_REQUEST)
      }
      else ->
        responseBuilder.withDetail(exception.message)
    }

    return ResponseEntity<Problem>(responseBuilder.build(), HttpStatus.BAD_REQUEST)
  }

  private fun isInputTypeArray(mismatchedInputException: MismatchedInputException): Boolean {
    if (mismatchedInputException.path.isEmpty()) {
      return deserializationValidationService.isArrayType(mismatchedInputException.targetType)
    }

    if (mismatchedInputException.path.first().from is Class<*>) {
      return deserializationValidationService.isArrayType(mismatchedInputException.path.first().from as Class<*>)
    }

    return true
  }

  private fun expectedArrayButGotObject(jsonNode: JsonNode, mismatchedInputException: MismatchedInputException) = jsonNode is ObjectNode && isInputTypeArray(mismatchedInputException)
  private fun expectedObjectButGotArray(jsonNode: JsonNode, mismatchedInputException: MismatchedInputException) = jsonNode is ArrayNode && !isInputTypeArray(mismatchedInputException)
  private fun rootIsArray(mismatchedInputException: MismatchedInputException) = mismatchedInputException.path[0].from !is Class<*>
}

private class AdviceTraitDefault : AdviceTrait
