package weco.concepts.common.model

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.json.Indexable

class CatalogueConceptTest extends AnyFunSpec with Matchers {
  describe("Indexable[CatalogueConcept]") {
    import weco.concepts.common.json.Indexable._

    val testConcept = CatalogueConcept(
      identifier = Identifier(
        value = "n84165387",
        identifierType = IdentifierType.LCNames
      ),
      label = "Pujol, Joseph, 1857-1945",
      canonicalId = "baadbeef",
      ontologyType = "Person"
    )
    val expectedJson =
      """{
        |  "authority": "lc-names",
        |  "identifier": "n84165387",
        |  "label": "Pujol, Joseph, 1857-1945",
        |  "canonicalId":"baadbeef",
        |  "ontologyType": "Person"
        |}""".stripMargin

    it("serializes correctly") {
      testConcept.toDoc shouldBe ujson.read(expectedJson)
    }

    it("gets the identifier correctly") {
      testConcept.id shouldBe "lc-names:n84165387"
    }

    it("deserializes correctly") {
      Indexable[CatalogueConcept].fromDoc(
        ujson.read(expectedJson)
      ) shouldBe Some(testConcept)
    }
  }
}
