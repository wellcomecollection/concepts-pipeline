package weco.concepts.aggregator.testhelpers

import weco.concepts.aggregator.ConceptExtractor
import weco.concepts.common.model.IdentifierType

import scala.util.Random

trait ValueGenerators {
  private val keys = IdentifierType.types.map(_.id).toList

  def anAuthority: String =
    keys(Random.nextInt(keys.length))

  private val terms = List(
    "Lorem",
    "Ipsum",
    "Dolor",
    "Sit",
    "Amet",
    "Consectetur"
  )

  def aLabel: String =
    terms(Random.nextInt(terms.length))

  def aType: String =
    ConceptExtractor.conceptTypes(
      Random.nextInt(ConceptExtractor.conceptTypes.length)
    )

  def anExternalId: String =
    "EXT_" + Random.alphanumeric.take(10).mkString

  def aCanonicalId: String =
    Random.alphanumeric.take(8).mkString
}
