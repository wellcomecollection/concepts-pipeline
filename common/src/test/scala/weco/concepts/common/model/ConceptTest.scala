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
      ontologyType = "Concept",
      sameAs = Seq("4567fghi")
    )
    val expectedJson =
      """{
        |  "query": {
        |    "id": "123abcde",
        |    "identifiers": [
        |      {
        |        "value": "sh95000541",
        |        "identifierType": "lc-subjects"
        |      }
        |    ],
        |    "label": "World Wide Web",
        |    "alternativeLabels": [
        |      "W3 (World Wide Web)",
        |      "WWW (World Wide Web)",
        |      "Web (World Wide Web)",
        |      "World Wide Web (Information retrieval system)"
        |    ],
        |    "type": "Concept"
        |  },
        |  "display": {
        |    "id": "123abcde",
        |    "label": "World Wide Web",
        |    "alternativeLabels": [
        |      "W3 (World Wide Web)",
        |      "WWW (World Wide Web)",
        |      "Web (World Wide Web)",
        |      "World Wide Web (Information retrieval system)"
        |    ],
        |    "identifiers": [
        |      {
        |        "value": "sh95000541",
        |        "identifierType": {
        |          "id": "lc-subjects",
        |          "label": "Library of Congress Subject Headings (LCSH)",
        |          "type": "IdentifierType"
        |        },
        |        "type": "Identifier"
        |      }
        |    ],
        |    "type": "Concept",
        |    "sameAs": ["4567fghi"]
        |  }
        |}""".stripMargin

    it("serializes correctly") {
      testConcept.toDoc shouldBe ujson.read(expectedJson)
    }

    it("gets the identifier correctly") {
      testConcept.id shouldBe "123abcde"
    }

    it("does not support deserialization") {
      a[NotImplementedError] should be thrownBy Indexable[Concept].fromDoc(
        ujson.read(expectedJson)
      )
    }
  }
}
