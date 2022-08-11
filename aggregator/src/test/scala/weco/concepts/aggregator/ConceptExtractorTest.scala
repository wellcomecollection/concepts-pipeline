package weco.concepts.aggregator

import org.scalatest.matchers.should.Matchers
import org.scalatest.GivenWhenThen
import org.scalatest.LoneElement.convertToCollectionLoneElementWrapper
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.prop.TableDrivenPropertyChecks
import ujson.ParseException

/*
 * Utility object to generate concepts in the form in which they are
 * presented in the Works API.  This is the form in which they arrive
 * into the ConceptExtractor
 */
object SourceConcept {
  def apply(
    authority: String,
    identifier: String,
    label: String,
    canonicalId: String,
    ontologyType: String
  ): String = {
    s"""
       |{
       |  "id": "$canonicalId",
       |  "identifiers": [
       |    {
       |      "identifierType": {
       |        "id": "$authority",
       |        "label": "This field is ignored",
       |        "type": "IdentifierType"
       |      },
       |      "value": "$identifier",
       |      "type": "Identifier"
       |    }
       |  ],
       |  "label": "$label",
       |  "type": "$ontologyType"
       |}
       |""".stripMargin
  }

}

class ConceptExtractorTest
    extends AnyFeatureSpec
    with Matchers
    with GivenWhenThen
    with TableDrivenPropertyChecks {
  Feature("The Concept Extractor") {
    info("The concept extractor extracts concepts from a single json document")
    info("The resulting content from this process consists of the following")
    info("- the authority")
    info("- the authority's identifier")
    info("- the name used in the original document")
    info("- the wellcome canonical identifier")

    Scenario("A document with no Concepts") {
      Given("a json document with no concepts in it")
      val concepts = ConceptExtractor("""{"hello": "world"}""")
      Then("the concept list is empty")
      concepts shouldBe Nil
    }

    Scenario("A document that is not JSON") {
      Given("an invalid document")
      val notJSON = "<hello>world</hello>"
      Then("an exception is raised")
      a[ParseException] should be thrownBy ConceptExtractor(notJSON)
    }

    Scenario("An unknown Identifier type") {
      info("The type (authority) of an identifier is a controlled vocabulary")
      Given("A document with a concept with an unknown identifier type")
      val doc =
        s"""
          |{
          |"concepts": [
          |${SourceConcept(
            authority = "deadbeef",
            identifier = "hello",
            label = "world",
            canonicalId = "92345678",
            ontologyType = "Concept"
          )}
          |]
          |}
          |""".stripMargin
      Then("an exception is raised")
      a[BadIdentifierTypeException] should be thrownBy ConceptExtractor(doc)
    }

    val t = Table(
      "ontologyType",
      "Concept",
      "Person",
      "Organisation",
      "Meeting",
      "Period"
    )
    forAll(t) { ontologyType =>
      Scenario(s"Extracts the data from a $ontologyType concept") {
        val identifierType = "lc-subjects"
        val identifier = "12345678900"
        val label = "Quirkafleeg"
        val canonicalId = "b7yui912"
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
        concept.identifier.value shouldBe identifierType

        And(s"the concept's canonicalIdentifier is $canonicalId")
        concept.canonicalId shouldBe canonicalId

        And(s"the concept's label is $label")
        concept.label shouldBe label

        And("the concept's alternativeLabels is empty")
        concept.alternativeLabels shouldBe Nil
        pending
      }
    }
  }

  Feature("Extracting multiple Concepts from a document") {
    Scenario("Multiple different concepts") { pending }
    Scenario("Concepts with different labels") {
      pending
    }
  }

  Feature("Unknown Identifier types") {
    info("The type (authority) of an identifier is a controlled vocabulary")
    Scenario("A document with a concept with an unknown identifier type") {
      // Reject or do your best?  Either case, log it.
      pending
    }
  }

  Feature("Ignoring things that are not Concepts") {
    info(
      "A concept in a work is represented by an object containing a sourceIdentifier object"
    )
    info(
      "These objects may also represent other things. If it is not a concept, it should be ignored"
    )
    Scenario("Unknown ontology types") {
      Given("a document with both concepts and non-concept identifiers")

      pending
    }
  }

  Feature("Storing it (this is somewhere else)") {

    // How do we handle upserts, when there may be alternative labels
    // It may have to be fetch and update.
  }
}
