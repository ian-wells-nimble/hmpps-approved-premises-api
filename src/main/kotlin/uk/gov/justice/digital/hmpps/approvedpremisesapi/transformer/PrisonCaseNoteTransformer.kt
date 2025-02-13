package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PrisonCaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNote
import java.time.ZoneOffset

@Component
class PrisonCaseNoteTransformer {
  fun transformModelToApi(domain: CaseNote) = PrisonCaseNote(
    id = domain.caseNoteId,
    createdAt = domain.creationDateTime.atOffset(ZoneOffset.UTC),
    occurredAt = domain.occurrenceDateTime.atOffset(ZoneOffset.UTC),
    authorName = domain.authorName,
    type = domain.typeDescription ?: domain.type,
    subType = domain.subTypeDescription ?: domain.subType,
    note = domain.text
  )
}
