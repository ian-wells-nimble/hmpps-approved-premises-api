package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PremisesApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import java.util.UUID

@Service
class PremisesController(
  private val premisesService: PremisesService,
  private val premisesTransformer: PremisesTransformer
) : PremisesApiDelegate {
  override fun premisesGet(): ResponseEntity<List<Premises>> {
    return ResponseEntity.ok(
      premisesService.getAllPremises().map(premisesTransformer::transformJpaToApi)
    )
  }

  override fun premisesPremisesIdGet(premisesId: UUID): ResponseEntity<Premises> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    return ResponseEntity.ok(premisesTransformer.transformJpaToApi(premises))
  }
}
