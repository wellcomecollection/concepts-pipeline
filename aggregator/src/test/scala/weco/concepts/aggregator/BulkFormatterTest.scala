package weco.concepts.aggregator

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

  Feature("Formatting Concepts for Elasticsearch") {
    Scenario("a single concept") {
      Given("a formatter for the index 'myindex'")
      val formatter = new BulkFormatter("myindex")
      And("a UsedConcept object")
      When("format is called")
      formatter.format(UsedConcept())
      Then("the result is a two-line string")
      1 + 1 shouldBe 4
      pending
      And("")
      // load the two jsons and look at them.

    }

  }

}
