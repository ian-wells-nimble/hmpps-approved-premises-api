package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PeopleApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PrisonCaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.shouldNotBeReached
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PrisonCaseNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer

@Service
class PeopleController(
  private val httpAuthService: HttpAuthService,
  private val offenderService: OffenderService,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val prisonCaseNoteTransformer: PrisonCaseNoteTransformer
) : PeopleApiDelegate {
  override fun peopleSearchGet(crn: String): ResponseEntity<Person> {
    val principal = httpAuthService.getDeliusPrincipalOrThrow()

    val offenderDetails = when (val offenderResult = offenderService.getOffenderByCrn(crn, principal.name)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> offenderResult.entity
      else -> shouldNotBeReached()
    }

    if (offenderDetails.otherIds.nomsNumber == null) {
      throw InternalServerErrorProblem("No nomsNumber present for CRN")
    }

    val inmateDetail = when (val inmateDetailResult = offenderService.getInmateDetailByNomsNumber(offenderDetails.otherIds.nomsNumber)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(offenderDetails.otherIds.nomsNumber, "Inmate")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> inmateDetailResult.entity
      else -> shouldNotBeReached()
    }

    return ResponseEntity.ok(
      personTransformer.transformModelToApi(offenderDetails, inmateDetail)
    )
  }

  override fun peopleCrnRisksGet(crn: String): ResponseEntity<PersonRisks> {
    val principal = httpAuthService.getDeliusPrincipalOrThrow()

    val risks = when (val risksResult = offenderService.getRiskByCrn(crn, principal.token.tokenValue, principal.name)) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Success -> risksResult.entity
      else -> shouldNotBeReached()
    }

    return ResponseEntity.ok(risksTransformer.transformDomainToApi(risks))
  }

  override fun peopleCrnPrisonCaseNotesGet(crn: String): ResponseEntity<List<PrisonCaseNote>> {
    val principal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = principal.name

    val offenderDetailsResult = offenderService.getOffenderByCrn(crn, username)
    val offenderDetails = when (offenderDetailsResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
      else -> shouldNotBeReached()
    }

    if (offenderDetails.otherIds.nomsNumber == null) {
      throw InternalServerErrorProblem("No nomsNumber present for CRN")
    }

    val nomsNumber = offenderDetails.otherIds.nomsNumber

    val prisonCaseNotesResult = offenderService.getPrisonCaseNotesByNomsNumber(nomsNumber)
    val prisonCaseNotes = when (prisonCaseNotesResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Inmate")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> prisonCaseNotesResult.entity
      else -> shouldNotBeReached()
    }

    return ResponseEntity.ok(prisonCaseNotes.map(prisonCaseNoteTransformer::transformModelToApi))
  }
}
