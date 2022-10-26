package weco.concepts.ingestor.stages

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.model._

import scala.io.Source

class TransformerTest extends AnyFunSpec with Matchers {
  describe("LoC transformer") {
    it("transforms a concept with an ID, a prefLabel, and altLabels") {
      Transformer.subjectsTransformer.transform(
        TestData.completeConcept
      ) should contain(
        AuthoritativeConcept(
          identifier = Identifier(
            value = "sh95000541",
            identifierType = IdentifierType.LCSubjects
          ),
          label = "World Wide Web",
          alternativeLabels = Seq(
            "W3 (World Wide Web)",
            "WWW (World Wide Web)",
            "Web (World Wide Web)",
            "World Wide Web (Information retrieval system)"
          )
        )
      )
    }

    it("transforms a concept with an ID and a prefLabel, but no altLabels") {
      Transformer.subjectsTransformer.transform(
        TestData.noAltLabels
      ) should contain(
        AuthoritativeConcept(
          identifier = Identifier(
            value = "sh2003010454",
            identifierType = IdentifierType.LCSubjects
          ),
          label = "Wellcome Building (London, England)",
          alternativeLabels = Nil
        )
      )
    }

    it("returns None for concepts missing a prefLabel") {
      Transformer.subjectsTransformer.transform(
        TestData.deprecatedConcept
      ) shouldBe None
    }

    it("Handles flat labels for names") {
      Transformer.namesTransformer.transform(
        TestData.flatLabelConcept
      ) should contain(
        AuthoritativeConcept(
          identifier = Identifier(
            value = "n83217500",
            identifierType = IdentifierType.LCNames
          ),
          label = "Wellcome, Henry S. (Henry Solomon), Sir, 1853-1936",
          alternativeLabels = Seq(
            "Wellcome, H. S. (Henry Solomon), Sir, 1853-1936",
            "Wellcome, Henry Solomon, Sir, 1853-1936",
            "Wellcome, Henry, Sir, 1853-1936"
          )
        )
      )
    }
  }
}

object TestData {
  def testDataResource(id: String): String =
    Source.fromResource(s"test-data/$id.json").getLines().mkString("\n")

  val completeConcept = testDataResource("sh88002671")
  val noAltLabels = testDataResource("sh2004005949")
  val deprecatedConcept = testDataResource("sh2009008863")
  val flatLabelConcept = testDataResource("n83217500")
}
