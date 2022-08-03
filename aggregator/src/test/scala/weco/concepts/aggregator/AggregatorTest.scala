package weco.concepts.aggregator

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class AggregatorTest extends AnyFunSpec with Matchers {
  it("greets the user") {
    List("hello", "world").mkString(" ") shouldBe "hello world"
  }
}
