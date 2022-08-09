package weco.concepts.common.json

import scala.util.Try

trait JsonOption[T] {
  def apply(node: ujson.Value): Option[T]
}

object JsonOps {
  implicit val identityOpt: JsonOption[ujson.Value] = Some(_)
  implicit val stringOpt: JsonOption[String] = _.strOpt
  implicit val doubleOpt: JsonOption[Double] = _.numOpt
  implicit val intOpt: JsonOption[Int] = doubleOpt(_).map(_.toInt)
  implicit def seqOpt[T: JsonOption]: JsonOption[Seq[T]] =
    (node: ujson.Value) =>
      node.arrOpt.map { array =>
        array.toSeq.flatMap { value =>
          value.opt[T]
        }
      }

  implicit class JsonOps(node: ujson.Value) {
    def opt[T: JsonOption](selector: String): Option[T] =
      Try(node(selector)).toOption
        .flatMap(implicitly[JsonOption[T]].apply)

    def opt[T: JsonOption]: Option[T] = implicitly[JsonOption[T]].apply(node)

    def opt[T: JsonOption](selectors: String*): Option[T] =
      selectors.tail
        .foldLeft(opt[ujson.Value](selectors.head)) { (nextNode, nextPath) =>
          nextNode.flatMap(_.opt[ujson.Value](nextPath))
        }
        .flatMap(_.opt[T])

    def optSeq(selector: String): Option[Seq[ujson.Value]] =
      opt[Seq[ujson.Value]](selector)
  }
}
