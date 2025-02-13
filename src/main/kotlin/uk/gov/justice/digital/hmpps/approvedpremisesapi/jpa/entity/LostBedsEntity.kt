package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Objects
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table

@Repository
interface LostBedsRepository : JpaRepository<LostBedsEntity, UUID> {
  @Query("SELECT lb FROM LostBedsEntity lb WHERE lb.premises.id = :premisesId AND lb.startDate <= :endDate AND lb.endDate >= :startDate")
  fun findAllByPremisesIdAndOverlappingDate(premisesId: UUID, startDate: LocalDate, endDate: LocalDate): List<LostBedsEntity>

  @Query("SELECT MAX(lb.endDate) FROM LostBedsEntity lb WHERE lb.premises.id = :premisesId")
  fun getHighestBookingDate(premisesId: UUID): LocalDate?
}

@Entity
@Table(name = "lost_beds")
data class LostBedsEntity(
  @Id
  val id: UUID,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val numberOfBeds: Int,
  @ManyToOne
  @JoinColumn(name = "lost_bed_reason_id")
  val reason: LostBedReasonEntity,
  val referenceNumber: String?,
  val notes: String?,
  @OneToOne
  @JoinColumn(name = "premises_id")
  var premises: PremisesEntity
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is LostBedsEntity) return false

    if (id != other.id) return false
    if (startDate != other.startDate) return false
    if (endDate != other.endDate) return false
    if (numberOfBeds != other.numberOfBeds) return false
    if (reason != other.reason) return false
    if (referenceNumber != other.referenceNumber) return false
    if (notes != other.notes) return false

    return true
  }

  override fun hashCode() = Objects.hash(id, startDate, endDate, numberOfBeds, reason, referenceNumber, notes)

  override fun toString() = "ArrivalEntity:$id"
}
