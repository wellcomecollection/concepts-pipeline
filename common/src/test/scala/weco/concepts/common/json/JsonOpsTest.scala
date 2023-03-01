package weco.concepts.common.json

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class JsonOpsTest extends AnyFunSpec with Matchers {
  import weco.concepts.common.json.JsonOps._
  describe("optSeq") {
    it(
      "returns a seq of JSON values if the source data is an Arr"
    ) {
      val node: ujson.Value = ujson.Obj("foo" -> Seq("bar", "baz"))
      node.optSeq("foo").get should equal(
        Seq(ujson.Str("bar"), ujson.Str("baz"))
      )
    }

    it("returns None if the selector does not match") {
      val node: ujson.Value = ujson.Obj("foo" -> "bar")
      node.optSeq("bar") shouldBe None
    }

    it("returns None if the selector matches a value that is not an Arr") {
      val node: ujson.Value = ujson.Obj("foo" -> "bar")
      node.optSeq("foo") shouldBe None
    }

    it("returns a seq of JSON values if the source data is a single value") {
      val node: ujson.Value = ujson.Obj("foo" -> "bar")
      node.optSeq("foo").get should equal(
        Seq(ujson.Str("bar"))
      )
    }
  }

  describe("asSeq") {

    it(
      "returns a seq of JSON values if the source data is an Arr"
    ) {
      val node: ujson.Value = ujson.Obj("foo" -> Seq("bar", "baz"))
      node.asSeq("foo") should equal(
        Seq(ujson.Str("bar"), ujson.Str("baz"))
      )
    }

    it("returns Nil if the selector does not match") {
      val node: ujson.Value = ujson.Obj("foo" -> "bar")
      node.asSeq("bar") shouldBe Nil
    }

    it("returns Nil if the selector matches a null value") {
      val node: ujson.Value = ujson.read("""{"foo":null}""")
      node.asSeq("foo") shouldBe Nil
    }

    it("returns a single value wrapped in a Seq") {
      val node: ujson.Value = ujson.Obj("foo" -> "bar")
      node.asSeq("foo") should equal(
        Seq(ujson.Str("bar"))
      )
    }

  }
}
