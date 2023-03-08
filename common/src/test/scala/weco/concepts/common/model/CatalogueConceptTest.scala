package weco.concepts.common.model

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.json.Indexable

class CatalogueConceptTest extends AnyFunSpec with Matchers {

  import weco.concepts.common.json.Indexable._

  /** Ensure that a round trip from serialised JSON to object and back again
    * would work as expected.
    */
  private def testSymmetricalSerialisation(
    asConcept: CatalogueConcept,
    asJson: String
  ): Unit = {

    it("serialises correctly") {
      asConcept.toDoc shouldBe ujson.read(asJson)
    }

    it("deserializes correctly") {
      Indexable[CatalogueConcept].fromDoc(
        ujson.read(asJson)
      ) shouldBe Some(asConcept)
    }
  }

  describe("Indexable[CatalogueConcept]") {
    info("""
     In the JSON representation of a CatalogueConcept, canonicalId and ontologyType may be lists or single values.
     This relates to the two places it may come from; the way that Elasticsearch handles arrays, and the fact that
     multiple canonicalIds can be assigned to one authoritative id if given different ontologyTypes.
     
     When CatalogueConcepts are extracted from the catalogue, these properties are single values, and that is how 
     they are initially inserted into Elasticsearch.
     
     However, if a later concept with the same Authoritative id but a different canonicalId/ontologyType pair is
     encountered, then the properties in the Elasticsearch record become lists.

     When the documents are read from Elasticsearch, then they are treated as lists.
    """)
    val testConcept = CatalogueConcept(
      identifier = Identifier(
        value = "n84165387",
        identifierType = IdentifierType.LCNames
      ),
      label = "Pujol, Joseph, 1857-1945",
      canonicalId = Seq("baadbeef"),
      ontologyType = Seq("Person")
    )

    it("gets the identifier by combining the authority and id") {
      testConcept.id shouldBe "lc-names:n84165387"
    }

    describe("With a single canonicalId/ontologyType") {
      info("""
           When there is only one canonicalId or ontologyType in a catalogue concept,
           the corresponding properties in the JSON version will be single values
           rather than lists.
        """)
      testSymmetricalSerialisation(
        testConcept,
        """{
          |  "authority": "lc-names",
          |  "identifier": "n84165387",
          |  "label": "Pujol, Joseph, 1857-1945",
          |  "canonicalId": "baadbeef",
          |  "ontologyType": "Person"
          |}""".stripMargin
      )

      it("provides the canonicalId and ontologyType as update parameters") {
        testConcept.toUpdateParams should equal(
          ujson.Obj(
            "canonicalId" -> Seq("baadbeef"),
            "ontologyType" -> Seq("Person")
          )
        )
      }
    }

    describe("With multiple canonicalIds/ontologyTypes") {

      testSymmetricalSerialisation(
        testConcept.copy(
          canonicalId = Seq("baadbeef", "cafef00d"),
          ontologyType = Seq("Agent", "Meeting")
        ),
        """{
          |  "authority": "lc-names",
          |  "identifier": "n84165387",
          |  "label": "Pujol, Joseph, 1857-1945",
          |  "canonicalId": ["baadbeef", "cafef00d"],
          |  "ontologyType": ["Agent", "Meeting"]
          |}""".stripMargin
      )
    }
  }
}
