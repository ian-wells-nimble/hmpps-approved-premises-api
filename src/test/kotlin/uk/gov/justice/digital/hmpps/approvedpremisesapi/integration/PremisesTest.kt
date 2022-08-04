package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.LocalAuthorityAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.PremisesTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ProbationRegionTestRepository

class PremisesTest : IntegrationTestBase() {
  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var probationRegionRepository: ProbationRegionTestRepository

  @Autowired
  lateinit var apAreaRepository: ApAreaTestRepository

  @Autowired
  lateinit var localAuthorityAreaRepository: LocalAuthorityAreaTestRepository

  @Autowired
  lateinit var premisesRepository: PremisesTestRepository

  private lateinit var probationRegionEntityFactory: ProbationRegionEntityFactory
  private lateinit var apAreaEntityFactory: ApAreaEntityFactory
  private lateinit var localAuthorityEntityFactory: LocalAuthorityEntityFactory
  private lateinit var premisesEntityFactory: PremisesEntityFactory

  @BeforeEach
  fun setupFactories() {
    probationRegionEntityFactory = ProbationRegionEntityFactory(probationRegionRepository)
    apAreaEntityFactory = ApAreaEntityFactory(apAreaRepository)
    localAuthorityEntityFactory = LocalAuthorityEntityFactory(localAuthorityAreaRepository)
    premisesEntityFactory = PremisesEntityFactory(premisesRepository)
  }

  @Test
  fun `Get all Premises returns OK with correct body`() {
    val premises = premisesEntityFactory
      .withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
      .withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      .withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist() }
      .produceAndPersistMultiple(10)

    val expectedJson = objectMapper.writeValueAsString(premises.map(::premisesEntityToExpectedApiResponse))

    webTestClient.get()
      .uri("/premises")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  private fun premisesEntityToExpectedApiResponse(premises: PremisesEntity) = Premises(
    id = premises.id,
    name = premises.name,
    apCode = premises.apCode,
    postcode = premises.postcode,
    bedCount = premises.totalBeds,
    probationRegion = ProbationRegion(id = premises.probationRegion.id, identifier = premises.probationRegion.identifier, name = premises.probationRegion.name),
    apArea = ApArea(id = premises.apArea.id, name = premises.apArea.name),
    localAuthorityArea = LocalAuthorityArea(id = premises.localAuthorityArea.id, identifier = premises.localAuthorityArea.identifier, name = premises.localAuthorityArea.name)
  )
}