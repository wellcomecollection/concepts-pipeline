package test.scala.weco.concepts.recorder

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class RecorderTest extends AnyFunSpec with Matchers {
  it("greets the user") {
    List("hello", "world").mkString(" ") shouldBe "hello world"
  }
}
