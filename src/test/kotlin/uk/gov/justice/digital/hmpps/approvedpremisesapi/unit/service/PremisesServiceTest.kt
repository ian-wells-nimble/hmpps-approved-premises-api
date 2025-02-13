package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Availability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import java.time.LocalDate
import java.util.UUID

class PremisesServiceTest {
  private val premisesRepositoryMock = mockk<PremisesRepository>()
  private val lostBedsRepositoryMock = mockk<LostBedsRepository>()
  private val bookingRepositoryMock = mockk<BookingRepository>()
  private val lostBedReasonRepositoryMock = mockk<LostBedReasonRepository>()

  private val premisesService = PremisesService(
    premisesRepositoryMock,
    lostBedsRepositoryMock,
    bookingRepositoryMock,
    lostBedReasonRepositoryMock
  )

  @Test
  fun `getAvailabilityForRange returns correctly when there are no bookings or lost beds`() {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(3)

    val premises = PremisesEntityFactory()
      .withTotalBeds(30)
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory().withYieldedApArea { ApAreaEntityFactory().produce() }.produce()
      }.produce()

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf()
    every { lostBedsRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf()

    val result = premisesService.getAvailabilityForRange(premises, startDate, endDate)

    assertThat(result).containsValues(
      Availability(date = startDate, 0, 0, 0, 0, 0),
      Availability(date = startDate.plusDays(1), 0, 0, 0, 0, 0),
      Availability(date = startDate.plusDays(2), 0, 0, 0, 0, 0)
    )
  }

  @Test
  fun `getAvailabilityForRange returns correctly when there are bookings`() {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(6)

    val premises = PremisesEntityFactory()
      .withTotalBeds(30)
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory().withYieldedApArea { ApAreaEntityFactory().produce() }.produce()
      }.produce()

    val lostBedEntity = LostBedsEntityFactory()
      .withPremises(premises)
      .withStartDate(startDate.plusDays(1))
      .withEndDate(startDate.plusDays(2))
      .withYieldedReason { LostBedReasonEntityFactory().produce() }
      .withNumberOfBeds(5)
      .produce()

    val pendingBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate.plusDays(1))
      .withDepartureDate(startDate.plusDays(3))
      .withStaffKeyWorkerId(null)
      .produce()

    val arrivedBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate)
      .withDepartureDate(startDate.plusDays(2))
      .withStaffKeyWorkerId(123)
      .produce()

    val arrivalEntity = ArrivalEntityFactory()
      .withBooking(arrivedBookingEntity)
      .produce()

    arrivedBookingEntity.arrival = arrivalEntity

    val nonArrivedBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate.plusDays(3))
      .withDepartureDate(startDate.plusDays(5))
      .withStaffKeyWorkerId(null)
      .produce()

    val nonArrivalEntity = NonArrivalEntityFactory()
      .withBooking(nonArrivedBookingEntity)
      .withYieldedReason { NonArrivalReasonEntityFactory().produce() }
      .produce()

    nonArrivedBookingEntity.nonArrival = nonArrivalEntity

    val cancelledBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate.plusDays(4))
      .withDepartureDate(startDate.plusDays(6))
      .withStaffKeyWorkerId(null)
      .produce()

    val cancelledArrivalEntity = CancellationEntityFactory()
      .withYieldedReason { CancellationReasonEntityFactory().produce() }
      .withBooking(cancelledBookingEntity)
      .produce()

    cancelledBookingEntity.cancellation = cancelledArrivalEntity

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf(
      pendingBookingEntity,
      arrivedBookingEntity,
      nonArrivedBookingEntity,
      cancelledBookingEntity
    )
    every { lostBedsRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf(
      lostBedEntity
    )

    val result = premisesService.getAvailabilityForRange(premises, startDate, endDate)

    assertThat(result).containsValues(
      Availability(date = startDate, pendingBookings = 0, arrivedBookings = 1, nonArrivedBookings = 0, cancelledBookings = 0, lostBeds = 0),
      Availability(date = startDate.plusDays(1), pendingBookings = 1, arrivedBookings = 1, nonArrivedBookings = 0, cancelledBookings = 0, lostBeds = 5),
      Availability(date = startDate.plusDays(2), pendingBookings = 1, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, lostBeds = 0),
      Availability(date = startDate.plusDays(3), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 1, cancelledBookings = 0, lostBeds = 0),
      Availability(date = startDate.plusDays(4), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 1, cancelledBookings = 1, lostBeds = 0),
      Availability(date = startDate.plusDays(5), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 1, lostBeds = 0)
    )
  }

  @Test
  fun `createLostBeds returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
    val premisesEntity = PremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val reasonId = UUID.randomUUID()

    every { lostBedReasonRepositoryMock.findByIdOrNull(reasonId) } returns null

    val result = premisesService.createLostBeds(
      premises = premisesEntity,
      startDate = LocalDate.parse("2022-08-28"),
      endDate = LocalDate.parse("2022-08-25"),
      numberOfBeds = 0,
      reasonId = reasonId,
      referenceNumber = "12345",
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.endDate", "beforeStartDate"),
      entry("$.numberOfBeds", "isZero"),
      entry("$.reason", "doesNotExist")
    )
  }

  @Test
  fun `createLostBeds returns Success with correct result when validation passed`() {
    val premisesEntity = PremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val lostBedReason = LostBedReasonEntityFactory().produce()

    every { lostBedReasonRepositoryMock.findByIdOrNull(lostBedReason.id) } returns lostBedReason

    every { lostBedsRepositoryMock.save(any()) } answers { it.invocation.args[0] as LostBedsEntity }

    val result = premisesService.createLostBeds(
      premises = premisesEntity,
      startDate = LocalDate.parse("2022-08-25"),
      endDate = LocalDate.parse("2022-08-28"),
      numberOfBeds = 5,
      reasonId = lostBedReason.id,
      referenceNumber = "12345",
      notes = "notes"
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.premises).isEqualTo(premisesEntity)
    assertThat(result.entity.reason).isEqualTo(lostBedReason)
    assertThat(result.entity.startDate).isEqualTo(LocalDate.parse("2022-08-25"))
    assertThat(result.entity.endDate).isEqualTo(LocalDate.parse("2022-08-28"))
    assertThat(result.entity.numberOfBeds).isEqualTo(5)
    assertThat(result.entity.referenceNumber).isEqualTo("12345")
    assertThat(result.entity.notes).isEqualTo("notes")
  }
}
