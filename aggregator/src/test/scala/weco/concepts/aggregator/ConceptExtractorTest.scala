package weco.concepts.aggregator

import org.scalatest.matchers.should.Matchers
import org.scalatest.GivenWhenThen
import org.scalatest.LoneElement.convertToCollectionLoneElementWrapper
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.prop.TableDrivenPropertyChecks
import ujson.ParseException
//import weco.concepts.aggregator.SourceConcept.{aCanonicalId, aLabel, aType, anAuthority, anExternalId}
import weco.concepts.common.model._

import scala.util.Random

trait ValueGenerators {
  private val keys = IdentifierType.typeMap.keys.toList

  def anAuthority: String =
    keys(Random.nextInt(keys.length))

  private val terms = List(
    "Lorem",
    "Ipsum",
    "Dolor",
    "Sit",
    "Amet",
    "Consectetur"
  )

  def aLabel: String =
    terms(Random.nextInt(terms.length))

  def aType: String =
    ConceptExtractor.conceptTypes(
      Random.nextInt(ConceptExtractor.conceptTypes.length)
    )

  def anExternalId: String =
    "EXT_" + Random.alphanumeric.take(10).mkString

  def aCanonicalId: String =
    Random.alphanumeric.take(8).mkString
}

/*
 * Utility object to generate concepts in the form in which they are
 * presented in the Works API.  This is the form in which they arrive
 * into the ConceptExtractor
 */
case class SourceConcept(
  authority: String,
  identifier: String,
  label: String,
  canonicalId: String,
  ontologyType: String
) {
  override def toString: String =
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

object SourceConcept extends ValueGenerators {
  def apply(
    authority: String = anAuthority,
    identifier: String = anExternalId,
    label: String = aLabel,
    canonicalId: String = aCanonicalId,
    ontologyType: String = aType
  ): SourceConcept = new SourceConcept(
    authority,
    identifier,
    label,
    canonicalId,
    ontologyType
  )

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

    Scenario("A malformed concept") {
      Given("a document with an invalid concept")
      val jsonString = s"""
                          |{
                          |"concepts": [
                          |${SourceConcept(
                           authority = "deadbeef",
                           identifier = "hello",
                           label = "world",
                           canonicalId = "92345678",
                           ontologyType = "Concept"
                         )},
                          |
                          |]
                          |}
                          |""".stripMargin
      Then("that concept is excluded")
      ConceptExtractor(jsonString)
      pending
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
        concept.identifier.value shouldBe identifier

        And(s"the concept's canonicalIdentifier is $canonicalId")
        concept.canonicalId shouldBe canonicalId

        And(s"the concept's label is $label")
        concept.label shouldBe label
      }
    }
  }

  Scenario("Multiple different concepts throughout the document") {
    info("Concepts may be found in various places in a document")
    info("The extractor is agnostic as to where they might be")
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
    println(json)
    Then("all the concepts are returned")
    val concepts = ConceptExtractor(json)
    concepts.length shouldBe 8
  }

  Scenario("Concepts with different labels but same id") {
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

  Scenario("A real example") {
    // Choose an actual document from the API output and
    // prove that it works.
    pending
  }

  Scenario("Source Concept with multiple identifiers") {
    // If a source concept has multiple identifiers, then this
    // results in multiple concepts in the output.
    pending
  }

  Feature("Storing it (this is somewhere else)") {

    // How do we handle upserts, when there may be alternative labels
    // It may have to be fetch and update, or will it be
  }
}
