package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "probation_regions")
data class ProbationRegionEntity(
  @Id
  val id: UUID,
  val name: String,
  @ManyToOne
  @JoinColumn(name = "ap_area_id")
  val apArea: ApAreaEntity,
  @OneToMany(mappedBy = "probationRegion")
  val premises: MutableList<PremisesEntity>
)
