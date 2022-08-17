package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.KeyWorkerEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Availability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import java.time.LocalDate

class PremisesServiceTest {
  private val premisesRepositoryMock = mockk<PremisesRepository>()
  private val lostBedsRepositoryMock = mockk<LostBedsRepository>()
  private val bookingRepositoryMock = mockk<BookingRepository>()

  private val premisesService = PremisesService(
    premisesRepositoryMock,
    lostBedsRepositoryMock,
    bookingRepositoryMock
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
      .withNumberOfBeds(5)
      .produce()

    val pendingBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate.plusDays(1))
      .withDepartureDate(startDate.plusDays(3))
      .withKeyWorker(KeyWorkerEntityFactory().produce())
      .produce()

    val arrivedBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate)
      .withDepartureDate(startDate.plusDays(2))
      .withKeyWorker(KeyWorkerEntityFactory().produce())
      .produce()

    val arrivalEntity = ArrivalEntityFactory()
      .withBooking(arrivedBookingEntity)
      .produce()

    arrivedBookingEntity.arrival = arrivalEntity

    val nonArrivedBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate.plusDays(3))
      .withDepartureDate(startDate.plusDays(5))
      .withKeyWorker(KeyWorkerEntityFactory().produce())
      .produce()

    val nonArrivalEntity = NonArrivalEntityFactory()
      .withBooking(nonArrivedBookingEntity)
      .produce()

    nonArrivedBookingEntity.nonArrival = nonArrivalEntity

    val cancelledBookingEntity = BookingEntityFactory()
      .withPremises(premises)
      .withArrivalDate(startDate.plusDays(4))
      .withDepartureDate(startDate.plusDays(6))
      .withKeyWorker(KeyWorkerEntityFactory().produce())
      .produce()

    val cancelledArrivalEntity = CancellationEntityFactory()
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
}