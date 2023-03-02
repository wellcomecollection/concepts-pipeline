package weco.concepts.common.model.matchers

import weco.concepts.common.model.CatalogueConcept
import org.scalatest.Assertions.fail

trait CatalogueConceptMatchers {

  implicit class CatalogueConceptTestOps(concept: CatalogueConcept) {

    def onlyCanonicalId: String =
      concept.canonicalId match {
        case Seq(singleId) => singleId
        case _ =>
          fail(
            s"Concept expected to have exactly one canonicalId, found: ${concept.canonicalId}"
          )
      }

    def onlyOntologyType: String =
      concept.ontologyType match {
        case Seq(singleType) => singleType
        case _ =>
          fail(
            s"Concept expected to have exactly one ontologyType, found: ${concept.ontologyType}"
          )
      }
  }

}
