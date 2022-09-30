package weco.concepts.ingestor

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.model.{Concept, Identifier, IdentifierType}

class ConceptFormatterTest extends AnyFunSpec with Matchers {
  it("does not return documents for geographic subdivision records") {
    val concept = Concept(
      identifier = Identifier(
        value = "sh2014000619-781",
        identifierType = IdentifierType.LCSubjects
      ),
      label = "Gabon--Parc national de Loango",
      alternativeLabels = Nil
    )
    ConceptFormatter.format("test")(concept) shouldBe None
  }
}
