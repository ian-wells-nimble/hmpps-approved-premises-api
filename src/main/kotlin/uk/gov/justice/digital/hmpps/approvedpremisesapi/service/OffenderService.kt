package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.AssessRisksAndNeedsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.ExcludedCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.RiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registrations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.shouldNotBeReached
import java.time.LocalDate

@Service
class OffenderService(
  private val communityApiClient: CommunityApiClient,
  private val assessRisksAndNeedsApiClient: AssessRisksAndNeedsApiClient,
  private val hmppsTierApiClient: HMPPSTierApiClient,
  private val prisonsApiClient: PrisonsApiClient,
  prisonCaseNotesConfigBindingModel: PrisonCaseNotesConfigBindingModel
) {
  private val ignoredRegisterTypesForFlags = listOf("RVHR", "RHRH", "RMRH", "RLRH", "MAPP")
  private val prisonCaseNotesConfig: PrisonCaseNotesConfig

  init {
    val excludedCategories = prisonCaseNotesConfigBindingModel.excludedCategories
      ?: throw RuntimeException("No prison-case-notes.excluded-categories provided")

    prisonCaseNotesConfig = PrisonCaseNotesConfig(
      lookbackDays = prisonCaseNotesConfigBindingModel.lookbackDays ?: throw RuntimeException("No prison-case-notes.lookback-days configuration provided"),
      prisonApiPageSize = prisonCaseNotesConfigBindingModel.prisonApiPageSize ?: throw RuntimeException("No prison-api-page-size configuration provided"),
      excludedCategories = excludedCategories.mapIndexed { index, categoryConfig ->
        ExcludedCategory(
          category = categoryConfig.category ?: throw RuntimeException("No category provided for prison-case-notes.excluded-categories at index $index"),
          subcategory = categoryConfig.subcategory
        )
      }
    )
  }

  fun getOffenderByCrn(crn: String, userDistinguishedName: String): AuthorisableActionResult<OffenderDetailSummary> {
    val offender = when (val offenderResponse = communityApiClient.getOffenderDetailSummary(crn)) {
      is ClientResult.Success -> offenderResponse.body
      is ClientResult.StatusCodeFailure -> if (offenderResponse.status == HttpStatus.NOT_FOUND) return AuthorisableActionResult.NotFound() else offenderResponse.throwException()
      is ClientResult.Failure -> offenderResponse.throwException()
      else -> shouldNotBeReached()
    }

    if (offender.currentExclusion || offender.currentRestriction) {
      val access =
        when (val accessResponse = communityApiClient.getUserAccessForOffenderCrn(userDistinguishedName, crn)) {
          is ClientResult.Success -> accessResponse.body
          is ClientResult.StatusCodeFailure -> {
            if (accessResponse.status == HttpStatus.FORBIDDEN) {
              try {
                accessResponse.deserializeTo<UserOffenderAccess>()
                return AuthorisableActionResult.Unauthorised()
              } catch (exception: Exception) {
                accessResponse.throwException()
              }
            }

            accessResponse.throwException()
          }
          is ClientResult.Failure -> accessResponse.throwException()
          else -> shouldNotBeReached()
        }

      if (access.userExcluded || access.userRestricted) {
        return AuthorisableActionResult.Unauthorised()
      }
    }

    return AuthorisableActionResult.Success(offender)
  }

  fun getInmateDetailByNomsNumber(nomsNumber: String): AuthorisableActionResult<InmateDetail> {
    val inmateDetail = when (val offenderResponse = prisonsApiClient.getInmateDetails(nomsNumber)) {
      is ClientResult.Success -> offenderResponse.body
      is ClientResult.StatusCodeFailure -> when (offenderResponse.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> offenderResponse.throwException()
      }
      is ClientResult.Failure -> offenderResponse.throwException()
      else -> shouldNotBeReached()
    }

    return AuthorisableActionResult.Success(inmateDetail)
  }

  fun getRiskByCrn(crn: String, jwt: String, userDistinguishedName: String): AuthorisableActionResult<PersonRisks> {
    return when (getOffenderByCrn(crn, userDistinguishedName)) {
      is AuthorisableActionResult.NotFound -> AuthorisableActionResult.NotFound()
      is AuthorisableActionResult.Unauthorised -> AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.Success -> {
        val registrationsResponse = communityApiClient.getRegistrationsForOffenderCrn(crn)

        val risks = PersonRisks(
          crn = crn,
          roshRisks = getRoshRisksEnvelope(crn, jwt),
          mappa = getMappaEnvelope(registrationsResponse),
          tier = getRiskTierEnvelope(crn),
          flags = getFlagsEnvelope(registrationsResponse)
        )

        AuthorisableActionResult.Success(
          risks
        )
      }
      else -> shouldNotBeReached()
    }
  }

  fun getPrisonCaseNotesByNomsNumber(nomsNumber: String): AuthorisableActionResult<List<CaseNote>> {
    val allCaseNotes = mutableListOf<CaseNote>()

    val fromDate = LocalDate.now().minusDays(prisonCaseNotesConfig.lookbackDays.toLong())

    var currentPage: CaseNotesPage?
    var currentPageIndex: Int? = null
    do {
      if (currentPageIndex == null) currentPageIndex = 0
      else currentPageIndex += 1

      val caseNotesPageResponse = prisonsApiClient.getCaseNotesPage(nomsNumber, fromDate, currentPageIndex, prisonCaseNotesConfig.prisonApiPageSize)
      currentPage = when (caseNotesPageResponse) {
        is ClientResult.Success -> caseNotesPageResponse.body
        is ClientResult.StatusCodeFailure -> when (caseNotesPageResponse.status) {
          HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
          HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
          else -> caseNotesPageResponse.throwException()
        }
        is ClientResult.Failure -> caseNotesPageResponse.throwException()
        else -> shouldNotBeReached()
      }

      allCaseNotes.addAll(
        currentPage.content.filter { caseNote ->
          prisonCaseNotesConfig.excludedCategories.none { it.excluded(caseNote.type, caseNote.subType) }
        }
      )
    } while (currentPage != null && currentPage.totalPages > currentPageIndex!! + 1)

    return AuthorisableActionResult.Success(allCaseNotes)
  }

  private fun getRoshRisksEnvelope(crn: String, jwt: String): RiskWithStatus<RoshRisks> {
    when (val roshRisksResponse = assessRisksAndNeedsApiClient.getRoshRisks(crn, jwt)) {
      is ClientResult.Success -> {
        val summary = roshRisksResponse.body.summary
        return RiskWithStatus(
          status = RiskStatus.Retrieved,
          value = RoshRisks(
            overallRisk = getOrThrow("overallRiskLevel") { summary.overallRiskLevel?.value },
            riskToChildren = getRiskOrThrow("Children", summary.riskInCommunity),
            riskToPublic = getRiskOrThrow("Public", summary.riskInCommunity),
            riskToKnownAdult = getRiskOrThrow("Known Adult", summary.riskInCommunity),
            riskToStaff = getRiskOrThrow("Staff", summary.riskInCommunity),
            lastUpdated = summary.assessedOn?.toLocalDate()
          )
        )
      }
      is ClientResult.StatusCodeFailure -> return if (roshRisksResponse.status == HttpStatus.NOT_FOUND) {
        RiskWithStatus(
          status = RiskStatus.NotFound,
          value = null
        )
      } else {
        RiskWithStatus(
          status = RiskStatus.Error,
          value = null
        )
      }
      is ClientResult.Failure -> return RiskWithStatus(
        status = RiskStatus.Error,
        value = null
      )
      else -> shouldNotBeReached()
    }
  }

  private fun getMappaEnvelope(registrationsResponse: ClientResult<Registrations>): RiskWithStatus<Mappa> {
    when (registrationsResponse) {
      is ClientResult.Success -> {
        return RiskWithStatus(
          value = registrationsResponse.body.registrations.firstOrNull { it.type.code == "MAPP" }?.let { registration ->
            Mappa(
              level = "CAT ${registration.registerCategory!!.code}/LEVEL ${registration.registerLevel!!.code}",
              lastUpdated = registration.registrationReviews?.filter { it.completed }?.maxOfOrNull { it.reviewDate } ?: registration.startDate
            )
          }
        )
      }
      is ClientResult.StatusCodeFailure -> return if (registrationsResponse.status == HttpStatus.NOT_FOUND) {
        RiskWithStatus(status = RiskStatus.NotFound)
      } else {
        RiskWithStatus(status = RiskStatus.Error)
      }
      is ClientResult.Failure -> {
        return RiskWithStatus(status = RiskStatus.Error)
      }
      else -> shouldNotBeReached()
    }
  }

  private fun getFlagsEnvelope(registrationsResponse: ClientResult<Registrations>): RiskWithStatus<List<String>> {
    when (registrationsResponse) {
      is ClientResult.Success -> {
        return RiskWithStatus(
          value = registrationsResponse.body.registrations.filter { !ignoredRegisterTypesForFlags.contains(it.type.code) }.map { it.type.description }
        )
      }
      is ClientResult.StatusCodeFailure -> return if (registrationsResponse.status == HttpStatus.NOT_FOUND) {
        RiskWithStatus(status = RiskStatus.NotFound)
      } else {
        RiskWithStatus(status = RiskStatus.Error)
      }
      is ClientResult.Failure -> {
        return RiskWithStatus(status = RiskStatus.Error)
      }
      else -> shouldNotBeReached()
    }
  }

  private fun getRiskTierEnvelope(crn: String): RiskWithStatus<RiskTier> {
    when (val tierResponse = hmppsTierApiClient.getTier(crn)) {
      is ClientResult.Success -> {
        return RiskWithStatus(
          status = RiskStatus.Retrieved,
          value = RiskTier(
            level = tierResponse.body.tierScore,
            lastUpdated = tierResponse.body.calculationDate.toLocalDate()
          )
        )
      }
      is ClientResult.StatusCodeFailure -> return if (tierResponse.status == HttpStatus.NOT_FOUND) {
        RiskWithStatus(status = RiskStatus.NotFound)
      } else {
        RiskWithStatus(status = RiskStatus.Error)
      }
      is ClientResult.Failure -> {
        return RiskWithStatus(status = RiskStatus.Error)
      }
      else -> shouldNotBeReached()
    }
  }

  private fun <T> getOrThrow(thing: String, getter: () -> T?): T {
    return getter() ?: throw RuntimeException("Value unexpectedly missing when getting $thing")
  }

  private fun getRiskOrThrow(category: String, risks: Map<RiskLevel?, List<String>>): String {
    risks.forEach {
      if (it.value.contains(category)) {
        return it.key?.value ?: throw RuntimeException("Risk level unexpectedly null when getting $category")
      }
    }

    throw RuntimeException("Category not present in any Risk level when getting $category")
  }
}
