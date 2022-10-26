package weco.concepts.common.model

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.json.Indexable

class AuthoritativeConceptTest extends AnyFunSpec with Matchers {
  describe("Indexable[AuthoritativeConcept]") {
    import weco.concepts.common.json.Indexable._

    val testConcept = AuthoritativeConcept(
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
    val expectedJson =
      """{
        |  "authority": "lc-subjects",
        |  "identifier": "sh95000541",
        |  "label": "World Wide Web",
        |  "alternativeLabels": [
        |    "W3 (World Wide Web)",
        |    "WWW (World Wide Web)",
        |    "Web (World Wide Web)",
        |    "World Wide Web (Information retrieval system)"
        |  ]
        |}""".stripMargin

    it("serializes correctly") {
      testConcept.toDoc shouldBe ujson.read(expectedJson)
    }

    it("gets the identifier correctly") {
      testConcept.id shouldBe "lc-subjects:sh95000541"
    }

    it("deserializes correctly") {
      Indexable[AuthoritativeConcept].fromDoc(
        ujson.read(expectedJson)
      ) shouldBe Some(testConcept)
    }
  }
}
