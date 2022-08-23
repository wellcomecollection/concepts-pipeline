package weco.concepts.aggregator

import com.sksamuel.elastic4s.requests.update.UpdateRequest
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import weco.concepts.aggregator.testhelpers.UsedConcept

class BulkFormatterTest
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
      val formatter = new BulkFormatter("myindex")
      And("a UsedConcept object")
      val concept =
        UsedConcept(
          authority = "lc-names",
          identifier = "n84165387",
          label = "Pujol, Joseph, 1857-1945",
          canonicalId = "baadbeef"
        )
      When("format is called")
      Then("the result is an update request")
      val formatted: UpdateRequest = formatter.format(concept)
      And(
        "the action upserts a record with the identifier authority and value as the id"
      )
      formatted.docAsUpsert.get shouldBe true
      formatted.id shouldBe "lc-names:n84165387"
      And(
        "the document contains all the information from the used concept"
      )
      formatted.documentFields should contain theSameElementsAs Seq(
        ("authority", "lc-names"),
        ("identifier", "n84165387"),
        ("label", "Pujol, Joseph, 1857-1945"),
        ("canonicalId", "baadbeef")
      )
    }

  }

}
