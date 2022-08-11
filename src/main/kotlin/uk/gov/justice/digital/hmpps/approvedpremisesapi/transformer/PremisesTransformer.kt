package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity

@Component
class PremisesTransformer(
  private val probationRegionTransformer: ProbationRegionTransformer,
  private val apAreaTransformer: ApAreaTransformer,
  private val localAuthorityAreaTransformer: LocalAuthorityAreaTransformer
) {
  fun transformJpaToApi(jpa: PremisesEntity) = Premises(
    id = jpa.id,
    name = jpa.name,
    apCode = jpa.apCode,
    postcode = jpa.postcode,
    bedCount = jpa.totalBeds,
    probationRegion = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
    apArea = apAreaTransformer.transformJpaToApi(jpa.apArea),
    localAuthorityArea = localAuthorityAreaTransformer.transformJpaToApi(jpa.localAuthorityArea)
  )
}
