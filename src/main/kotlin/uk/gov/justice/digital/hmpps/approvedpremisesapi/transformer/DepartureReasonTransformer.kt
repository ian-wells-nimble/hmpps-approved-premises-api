package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.DepartureReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity

@Component
class DepartureReasonTransformer() {
  fun transformJpaToApi(jpa: DepartureReasonEntity) = DepartureReason(
    id = jpa.id,
    name = jpa.name,
    isActive = jpa.isActive
  )
}
