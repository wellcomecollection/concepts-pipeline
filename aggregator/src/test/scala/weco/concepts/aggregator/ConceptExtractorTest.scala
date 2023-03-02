package weco.concepts.aggregator

import scala.io.Source
import org.scalatest.matchers.should.Matchers
import org.scalatest.GivenWhenThen
import org.scalatest.LoneElement.convertToCollectionLoneElementWrapper
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.prop.TableDrivenPropertyChecks
import ujson.ParseException
import weco.concepts.aggregator.testhelpers.{
  SourceCompoundConcept,
  SourceConcept
}
import weco.concepts.common.model.matchers.CatalogueConceptMatchers

class ConceptExtractorTest
    extends AnyFeatureSpec
    with Matchers
    with GivenWhenThen
    with TableDrivenPropertyChecks
    with CatalogueConceptMatchers {
  Feature("Extracting Concepts from a document") {
    info("The concept extractor extracts concepts from a single json document")
    info("The resulting content from this process consists of the following")
    info("- the authority")
    info("- the authority's identifier")
    info("- the name used in the original document")
    info("- the wellcome canonical identifier")
    info("- the ontology type")

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
      concepts.length shouldBe 12
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

      And(s"the concept's ontologyType is $ontologyType")
      concept.ontologyType shouldBe Seq(ontologyType)

      And(s"the concept's identifierType is $identifierType")
      concept.identifier.identifierType.id shouldBe identifierType

      And(s"the concept's identifier is $identifier")
      concept.identifier.value shouldBe identifier

      And(s"the concept's label is $label")
      concept.label shouldBe label

      And(s"the concept's canonicalIdentifier is $canonicalId")
      concept.canonicalId.loneElement shouldBe canonicalId
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

    Scenario("extract a Concept from within another Concept") {
      info("a document may contain compound concepts")
      info(
        "in which a concept or list of concepts may be nested within a parent concept"
      )
      info(
        "in real examples, Subjects are a kind of Concept that operate this way"
      )
      Given(
        "a document with two concept objects nested within another concept object"
      )
      val json = SourceCompoundConcept(
        authority = "lc-subjects",
        identifier = "sh85046693",
        label = "Eye-sockets--Diseases",
        canonicalId = "z6m7z2uz",
        ontologyType = "Subject",
        concepts = List(
          SourceConcept(
            authority = "lc-subjects",
            identifier = "sh85046691",
            label = "Eye-sockets",
            canonicalId = "cafef00d",
            ontologyType = "Concept"
          ),
          SourceConcept(
            authority = "lc-subjects",
            identifier = "sh99002330",
            label = "Diseases",
            canonicalId = "cafebeef",
            ontologyType = "Concept"
          )
        )
      ).toString
      val concepts = ConceptExtractor(json)
      Then("all three concepts are returned")
      concepts.length shouldBe 3
    }

    Scenario(
      "extract the correct ontologyType for a 'simple compound' concept"
    ) {
      // This is a side effect of the polysemous nature of "type" in the
      // catalogue data.  It is used in the catalogue pipeline to mark the
      // Scala type that the JSON object represents, so that the JSON parser
      // can create objects of that type.
      // Within concept lists (lists of AbstractConcepts), this corresponds to
      // the ontologyType, but elsewhere (subjects) it does not.
      // As a result, the ontologyType of a Subject is found elsewhere.
      info(
        "the type property of a compound concept is not its actual ontologyType"
      )
      info(
        "the ontologyType of a compound can be found in its only constituent concept"
      )
      Given(
        "a subject with one Person subConcept"
      )
      val json = SourceCompoundConcept(
        authority = "lc-names",
        identifier = "n84165387",
        label = "Pujol, Joseph, 1857-1945",
        canonicalId = "baadbeef",
        ontologyType = "Subject",
        concepts = List(
          SourceConcept(
            authority = "lc-names",
            identifier = "n84165387",
            label = "Pujol, Joseph, 1857-1945",
            canonicalId = "baadbeef",
            ontologyType = "Person"
          )
        )
      ).toString
      println(json)
      val concepts = ConceptExtractor(json)
      Then(
        "only one concept is extracted"
      )
      And(
        "the ontologyType of the resulting concept is Person"
      )
      concepts.loneElement.ontologyType.loneElement shouldBe "Person"
    }

    Scenario("extract the correct ontologyType for a 'true compound' concept") {
      info(
        "the type property of a compound concept is not its actual ontologyType"
      )
      info(
        "the ontologyType of a true compound cannot be derived from its subConcepts"
      )
      info(
        "In LoC terms, a true compound is a madsrdf:ComplexSubject, which we consider a Concept"
      )
      Given(
        "a subject with multiple subConcepts"
      )
      val json = SourceCompoundConcept(
        authority = "lc-subjects",
        identifier = "sh85118819",
        label = "Scotland, Description and travel, Early works to 1800.",
        canonicalId = "baadbeef",
        ontologyType = "Subject",
        concepts = List(
          SourceConcept(
            authority = "lc-names",
            identifier = "n79123936",
            label = "Scotland",
            canonicalId = "cafebeef",
            ontologyType = "Place"
          ),
          SourceConcept(
            authority = "lc-subjects",
            identifier = "sj2022050187",
            label = "Description and travel",
            canonicalId = "abadcafe",
            ontologyType = "Concept"
          ),
          SourceConcept(
            authority = "lc-subjects",
            identifier = "sh99001366",
            label = "Early works to 1800",
            canonicalId = "deadbeef",
            ontologyType = "Concept"
          )
        )
      ).toString
      println(json)
      val concepts = ConceptExtractor(json)
      Then(
        "all concepts are extracted"
      )
      And(
        "the ontologyType of the resulting concept is Concept"
      )
      concepts.length shouldBe 4
      concepts.head should have(
        Symbol("label")(
          "Scotland, Description and travel, Early works to 1800."
        ),
        Symbol("canonicalId")(Seq("baadbeef")),
        Symbol("ontologyType")(Seq("Concept"))
      )
    }
  }

  Feature("Different types of Concept") {
    val ontologyTypes = Table(
      "ontologyType",
      "Concept",
      "Person",
      "Organisation",
      "Meeting",
      "Period",
      "Place",
      "Subject",
      "Agent"
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
          Symbol("canonicalId")(Seq(sourceConcept.canonicalId)),
          Symbol("ontologyType")(Seq(ontologyType))
        )
      }
    }

    val identifierTypes = Table(
      "identifierType",
      "lc-subjects",
      "lc-names",
      "nlm-mesh",
      "label-derived",
      "fihrist",
      "viaf"
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
        Given(
          s"a document with a valid concept and a concept with $malformation"
        )
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
        concepts.loneElement.canonicalId.loneElement shouldBe "92345678"
      }
    }
  }
}
