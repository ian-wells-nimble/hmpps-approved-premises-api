package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.util.UUID
import kotlin.RuntimeException

class BookingEntityFactory : Factory<BookingEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringUpperCase(6) }
  private var arrivalDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore() }
  private var departureDate: Yielded<LocalDate> = { LocalDate.now().randomDateAfter() }
  private var keyWorkerStaffId: Yielded<Long?> = { null }
  private var arrival: Yielded<ArrivalEntity>? = null
  private var departure: Yielded<DepartureEntity>? = null
  private var nonArrival: Yielded<NonArrivalEntity>? = null
  private var cancellation: Yielded<CancellationEntity>? = null
  private var extensions: Yielded<MutableList<ExtensionEntity>>? = null
  private var premises: Yielded<PremisesEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withArrivalDate(arrivalDate: LocalDate) = apply {
    this.arrivalDate = { arrivalDate }
  }

  fun withDepartureDate(departureDate: LocalDate) = apply {
    this.departureDate = { departureDate }
  }

  fun withStaffKeyWorkerId(staffKeyWorkerId: Long?) = apply {
    this.keyWorkerStaffId = { staffKeyWorkerId }
  }

  fun withYieldedArrival(arrival: Yielded<ArrivalEntity>) = apply {
    this.arrival = arrival
  }

  fun withArrival(arrival: ArrivalEntity) = apply {
    this.arrival = { arrival }
  }

  fun withYieldedDeparture(departure: Yielded<DepartureEntity>) = apply {
    this.departure = departure
  }

  fun withDeparture(departure: DepartureEntity) = apply {
    this.departure = { departure }
  }

  fun withYieldedNonArrival(nonArrival: Yielded<NonArrivalEntity>) = apply {
    this.nonArrival = nonArrival
  }

  fun withNonArrival(nonArrival: NonArrivalEntity) = apply {
    this.nonArrival = { nonArrival }
  }

  fun withYieldedCancellation(cancellation: Yielded<CancellationEntity>) = apply {
    this.cancellation = cancellation
  }

  fun withCancellation(cancellation: CancellationEntity) = apply {
    this.cancellation = { cancellation }
  }

  fun withYieldedExtensions(extensions: Yielded<MutableList<ExtensionEntity>>) = apply {
    this.extensions = extensions
  }

  fun withExtensions(extensions: MutableList<ExtensionEntity>) = apply {
    this.extensions = { extensions }
  }

  fun withYieldedPremises(premises: Yielded<PremisesEntity>) = apply {
    this.premises = premises
  }

  fun withPremises(premises: PremisesEntity) = apply {
    this.premises = { premises }
  }

  override fun produce(): BookingEntity = BookingEntity(
    id = this.id(),
    crn = this.crn(),
    arrivalDate = this.arrivalDate(),
    departureDate = this.departureDate(),
    keyWorkerStaffId = this.keyWorkerStaffId(),
    arrival = this.arrival?.invoke(),
    departure = this.departure?.invoke(),
    nonArrival = this.nonArrival?.invoke(),
    cancellation = this.cancellation?.invoke(),
    extensions = this.extensions?.invoke() ?: mutableListOf(),
    premises = this.premises?.invoke() ?: throw RuntimeException("Must provide a Premises")
  )
}
