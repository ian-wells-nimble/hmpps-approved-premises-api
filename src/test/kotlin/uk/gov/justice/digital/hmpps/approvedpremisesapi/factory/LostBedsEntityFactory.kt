package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.LostBedsTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class LostBedsEntityFactory(
  lostBedsTestRepository: LostBedsTestRepository
) : PersistedFactory<LostBedsEntity, UUID>(lostBedsTestRepository) {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var startDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(6) }
  private var endDate: Yielded<LocalDate> = { LocalDate.now().randomDateAfter(6) }
  private var numberOfBeds: Yielded<Int> = { randomInt(1, 10) }
  private var reason: Yielded<LostBedReason> = { LostBedReason.values()[randomInt(0, LostBedReason.values().size - 1)] }
  private var referenceNumber: Yielded<String?> = { UUID.randomUUID().toString() }
  private var notes: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var premises: Yielded<PremisesEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withStartDate(startDate: LocalDate) = apply {
    this.startDate = { startDate }
  }

  fun withEndDate(endDate: LocalDate) = apply {
    this.endDate = { endDate }
  }

  fun withNumberOfBeds(numberOfBeds: Int) = apply {
    this.numberOfBeds = { numberOfBeds }
  }

  fun withReason(reason: LostBedReason) = apply {
    this.reason = { reason }
  }

  fun withReferenceNumber(referenceNumber: String?) = apply {
    this.referenceNumber = { referenceNumber }
  }

  fun withNotes(notes: String?) = apply {
    this.notes = { notes }
  }

  fun withYieldedPremises(premises: Yielded<PremisesEntity>) = apply {
    this.premises = premises
  }

  fun withPremises(premises: PremisesEntity) = apply {
    this.premises = { premises }
  }

  override fun produce(): LostBedsEntity = LostBedsEntity(
    id = this.id(),
    startDate = this.startDate(),
    endDate = this.endDate(),
    numberOfBeds = this.numberOfBeds(),
    reason = this.reason(),
    referenceNumber = this.referenceNumber(),
    notes = this.notes(),
    premises = this.premises?.invoke() ?: throw RuntimeException("Must provide a Premises")
  )
}
