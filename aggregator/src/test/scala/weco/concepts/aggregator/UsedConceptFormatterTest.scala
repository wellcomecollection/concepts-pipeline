package weco.concepts.aggregator

import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import weco.concepts.aggregator.testhelpers.UsedConcept

class UsedConceptFormatterTest
    extends AnyFeatureSpec
    with Matchers
    with GivenWhenThen
    with TableDrivenPropertyChecks {

  Feature("Formatting UsedConcepts for the Elasticsearch bulk API") {
    info("Records inserted by this process represent the pairing of a")
    info("Wellcome canonical id with an authority id, as found in a Work.")
    info("The unique identifier for a record is the authority id")
    info("This is for two reasons:")
    info(
      "- A canonical id may refer to more than one authority id, but not the other way around"
    )
    info("  (see 'extract a source Concept with multiple identifiers')")
    info(
      "- this database will be read by an application that knows the authority identifiers"
    )
    Scenario("a single concept") {
      Given("a formatter for the index 'myindex'")
      val formatter = UsedConceptFormatter.format("myindex") _
      And("a UsedConcept object")
      val concept =
        UsedConcept(
          authority = "lc-names",
          identifier = "n84165387",
          label = "Pujol, Joseph, 1857-1945",
          canonicalId = "baadbeef"
        )
      When("format is called")
      val formatted: String = formatter(concept).get
      Then("the result is two lines of NDJSON")
      val lines = formatted.linesIterator.toList
      lines.length shouldBe 2
      val action = ujson.read(lines.head)
      val document = ujson.read(lines(1))
      And(
        "the action upserts a record with the identifier authority and value as the id"
      )
      action
        .obj("update")
        .obj("_id")
        .value shouldBe "lc-names:n84165387"
      document("doc_as_upsert").value shouldBe true
      And(
        "the document contains all the information from the used concept"
      )
      val expectedDocument = ujson.read("""{
        "authority": "lc-names",
        "identifier": "n84165387",
        "label": "Pujol, Joseph, 1857-1945",
        "canonicalId":"baadbeef"
      }""")
      document("doc") shouldBe expectedDocument
    }

  }

}
