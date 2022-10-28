package weco.concepts.common.model

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.json.Indexable

class ConceptTest extends AnyFunSpec with Matchers {
  describe("Indexable[Concept]") {
    import weco.concepts.common.json.Indexable._

    val testConcept = Concept(
      label = "World Wide Web",
      identifiers = Seq(
        Identifier(
          value = "sh95000541",
          identifierType = IdentifierType.LCSubjects
        )
      ),
      alternativeLabels = Seq(
        "W3 (World Wide Web)",
        "WWW (World Wide Web)",
        "Web (World Wide Web)",
        "World Wide Web (Information retrieval system)"
      ),
      canonicalId = "123abcde",
      ontologyType = "Concept"
    )
    val expectedJson =
      """{
        |  "canonicalId": "123abcde",
        |  "label": "World Wide Web",
        |  "alternativeLabels": [
        |    "W3 (World Wide Web)",
        |    "WWW (World Wide Web)",
        |    "Web (World Wide Web)",
        |    "World Wide Web (Information retrieval system)"
        |  ],
        |  "identifiers": [
        |    {
        |      "identifier": "sh95000541",
        |      "authority": "lc-subjects"
        |    }
        |  ],
        |  "type": "Concept"
        |}""".stripMargin

    it("serializes correctly") {
      testConcept.toDoc shouldBe ujson.read(expectedJson)
    }

    it("gets the identifier correctly") {
      testConcept.id shouldBe "123abcde"
    }

    it("deserializes correctly") {
      Indexable[Concept].fromDoc(ujson.read(expectedJson)) shouldBe Some(
        testConcept
      )
    }
  }
}