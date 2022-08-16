package weco.concepts.aggregator

import scala.io.Source
import org.scalatest.matchers.should.Matchers
import org.scalatest.GivenWhenThen
import org.scalatest.LoneElement.convertToCollectionLoneElementWrapper
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.prop.TableDrivenPropertyChecks
import ujson.ParseException
import weco.concepts.aggregator.testhelpers.SourceConcept

class ConceptExtractorTest
    extends AnyFeatureSpec
    with Matchers
    with GivenWhenThen
    with TableDrivenPropertyChecks {
  Feature("Extracting Concepts from a document") {
    info("The concept extractor extracts concepts from a single json document")
    info("The resulting content from this process consists of the following")
    info("- the authority")
    info("- the authority's identifier")
    info("- the name used in the original document")
    info("- the wellcome canonical identifier")

    Scenario("a real example") {
      // Choose an actual document from the API output and
      // prove that it works.
      Given("a Work from the catalogue API")
      val json = Source.fromResource("uk4kymkq.json").getLines().mkString("\n")
      val concepts = ConceptExtractor(json)
      // The more precisely defined tests here show what should be
      // extracted.  This is just to demonstrate that it can successfully
      // find the concepts in a real document from the catalogue api.
      Then("all the concepts in the work are returned")
      concepts.length shouldBe 6
    }

    Scenario(s"extract the data from a concept") {
      val identifierType = "lc-subjects"
      val identifier = "12345678900"
      val label = "Quirkafleeg"
      val canonicalId = "b7yui912"
      val ontologyType = "Concept"
      Given(s"a document containing a concept of type $ontologyType")
      And(s"an identifierType of $identifierType")
      And(s"an identifier of $identifier")
      And(s"a label $label")
      And(s"a canonical identifier $canonicalId")
      val json =
        s"""{
           |"concepts":[
            ${SourceConcept(
            authority = identifierType,
            identifier = identifier,
            label = label,
            canonicalId = canonicalId,
            ontologyType = ontologyType
          )}
           |]
           |}""".stripMargin

      Then("there is one resulting concept")
      val concept = ConceptExtractor(json).loneElement

      And(s"the concept's identifierType is $identifierType")
      concept.identifier.identifierType.id shouldBe identifierType

      And(s"the concept's identifier is $identifier")
      concept.identifier.value shouldBe identifier

      And(s"the concept's label is $label")
      concept.label shouldBe label

      And(s"the concept's canonicalIdentifier is $canonicalId")
      concept.canonicalId shouldBe canonicalId
    }

    val ontologyTypes = Table(
      "ontologyType",
      "Concept",
      "Person",
      "Organisation",
      "Meeting",
      "Period"
    )
    forAll(ontologyTypes) { ontologyType =>
      Scenario(s"extract a concept of type $ontologyType ") {
        Given(s"a document containing a concept of type $ontologyType")
        val sourceConcept = SourceConcept(
          ontologyType = ontologyType
        )
        val json =
          s"""{
             |"concepts":[
            $sourceConcept
             |]
             |}""".stripMargin

        Then("that concept is extracted")
        ConceptExtractor(json).loneElement should have(
          Symbol("label")(sourceConcept.label),
          Symbol("canonicalId")(sourceConcept.canonicalId)
        )
      }
    }

    val identifierTypes = Table(
      "identifierType",
      "lc-subjects",
      "lc-names",
      "nlm-mesh",
      "label-derived"
    )
    forAll(identifierTypes) { identifierType =>
      Scenario(
        s"extract a concept with an identifier in the $identifierType scheme "
      ) {
        Given(
          s"a document containing a concept with an identifier of type $identifierType"
        )
        val sourceConcept = SourceConcept(
          authority = identifierType
        )
        val json =
          s"""{
             |"concepts":[
            $sourceConcept
             |]
             |}""".stripMargin

        Then("that concept is extracted")
        val concept = ConceptExtractor(json).loneElement
        concept should have(
          Symbol("label")(sourceConcept.label),
          Symbol("canonicalId")(sourceConcept.canonicalId)
        )
        concept.identifier.identifierType.id shouldBe identifierType
      }
    }

    Scenario("extract multiple different concepts throughout the document") {
      info("Concepts may be found in various places in a document")
      info("The extractor can extract them wherever they might be")
      Given("a document containing concepts in various places")
      // as a property of the top-level object
      // in a list
      // in an object in a list
      // in an object in an object
      val json =
        s"""{
         |"thing": ${SourceConcept()},
         |"things":[${SourceConcept()}, ${SourceConcept()}, {"wossname": ${SourceConcept()}}],
         |"thingy":{
         |  "wotsit": {
         |    "thingummy": ${SourceConcept()},
         |    "wotsit": ${SourceConcept()},
         |    "stuff":[${SourceConcept()}, ${SourceConcept()}]
         |  }
         |}
         |}""".stripMargin
      Then("all the concepts are returned")
      val concepts = ConceptExtractor(json)
      concepts.length shouldBe 8
    }

    Scenario("extract concepts with different labels but same id") {
      Given("a document with two concept objects")
      And("both concept objects have the same id, but different labels")
      val concept1 = SourceConcept(
        authority = "lc-names",
        identifier = "n79007443",
        label = "Isaac Newton",
        canonicalId = "b7yui912",
        ontologyType = "Person"
      )
      val concept2 = concept1.copy(label = "Zack Neutron")
      val json =
        s"""{
           |"concepts":[$concept1, $concept2]
           |}""".stripMargin
      val concepts = ConceptExtractor(json)
      Then("one Concept is returned")
      And("the label is that of the first concept")
      concepts.loneElement.label shouldBe "Isaac Newton"
    }

    Scenario("extract a source Concept with multiple identifiers") {
      // If a source concept has multiple identifiers, then this
      // results in multiple concepts in the output.
      Given("a document with one concept object")
      And("the concept object has two identifiers")
      val identifierType1 = "lc-subjects"
      val identifier1 = "sh85120937"
      val identifierType2 = "lc-names"
      val identifier2 = "no2017146789"
      val json =
        s"""
           |{
           |  "id": "z6m7z2uz",
           |  "identifiers": [
           |    {
           |      "identifierType": {
           |        "id": "$identifierType1",
           |        "label": "This field is ignored",
           |        "type": "IdentifierType"
           |      },
           |      "value": "$identifier1",
           |      "type": "Identifier"
           |    },
           |    {
           |      "identifierType": {
           |        "id": "$identifierType2",
           |        "label": "This field is ignored",
           |        "type": "IdentifierType"
           |      },
           |      "value": "$identifier2",
           |      "type": "Identifier"
           |    }
           |  ],
           |  "label": "William Shakespeare",
           |  "type": "Person"
           |}
           |""".stripMargin
      val concepts = ConceptExtractor(json)
      Then("two Concepts are returned")
      And("the first Concept contains the canonicalid and the first identifier")
      concepts.head.canonicalId shouldBe "z6m7z2uz"
      concepts.head.identifier.identifierType.id shouldBe identifierType1
      concepts.head.identifier.value shouldBe identifier1
      And(
        "the second Concept contains the canonicalid and the second identifier"
      )
      concepts(1).canonicalId shouldBe "z6m7z2uz"
      concepts(1).identifier.identifierType.id shouldBe identifierType2
      concepts(1).identifier.value shouldBe identifier2
    }
  }
  Feature("Handling bad input") {

    Scenario("a document with no Concepts") {
      Given("a json document with no concepts in it")
      val concepts = ConceptExtractor("""{"hello": "world"}""")
      Then("the concept list is empty")
      concepts shouldBe Nil
    }

    Scenario("a document that is not JSON") {
      Given("an invalid document")
      val notJSON = "<hello>world</hello>"
      Then("an exception is raised")
      a[ParseException] should be thrownBy ConceptExtractor(notJSON)
    }

    Scenario(s"ignore concept-like objects") {
      Given(s"a document containing a concept-shaped object of type 'Banana'")
      val sourceConcept = SourceConcept(
        ontologyType = "Banana"
      )
      val json =
        s"""{
           |"concepts":[
            $sourceConcept
           |]
           |}""".stripMargin
      val concepts = ConceptExtractor(json)
      Then("the concept list is empty")
      concepts shouldBe Nil
    }

    val malformations = Table(
      ("malformation", "badJson"),
      (
        "an unknown identifier type",
        SourceConcept(
          authority = "deadbeef",
          identifier = "hello",
          label = "world",
          canonicalId = "92345678",
          ontologyType = "Concept"
        )
      ),
      (
        "no identifier property",
        """{
          |  "label": "Oh No! I have no identifiers",
          |  "type": "Concept",
          |  "id": "baadf00d"
          |}""".stripMargin
      ),
      (
        "no canonical id",
        """{
          |  "label": "Oh No! I have no canonicalid",
          |"identifiers": [
          |    {
          |      "identifierType": {
          |        "id": "lc-names",
          |        "label": "This field is ignored",
          |        "type": "IdentifierType"
          |      },
          |      "value": "deadbeef",
          |      "type": "Identifier"
          |    }
          |  ],
          |  "type": "Concept"
          |}""".stripMargin
      ),
      (
        "no label",
        """{
          |"identifiers": [
          |    {
          |      "identifierType": {
          |        "id": "lc-names",
          |        "label": "This field is ignored",
          |        "type": "IdentifierType"
          |      },
          |      "value": "abadcafe",
          |      "type": "Identifier"
          |    }
          |  ],
          |  "id": "nolabel",
          |  "type": "Concept"
          |}""".stripMargin
      ),
      (
        "a malformed identifier",
        """{
          |  "label": "Oh No! my identifiers are dodgy",
          |"identifiers": [
          |    {
          |      "value": "deadbeef",
          |      "type": "Identifier"
          |    }
          |  ],
          |  "type": "Concept"
          |}""".stripMargin
      )
    )
    forAll(malformations) { (malformation, badJson) =>
      Scenario(s"encountering a malformed concept - $malformation") {
        Given(s"a document with a valid concept and a concept with $malformation")
        val jsonString =
          s"""
             |{
             |"concepts": [
             |$badJson,
             |${SourceConcept(
              authority = "lc-subjects",
              identifier = "hello",
              label = "world",
              canonicalId = "92345678",
              ontologyType = "Concept"
            )}
             |]
             |}
             |""".stripMargin
        val concepts = ConceptExtractor(jsonString)
        Then("the good concept is included")
        And("the malformed concept is excluded")
        concepts.loneElement.canonicalId shouldBe "92345678"
      }
    }
  }
}
