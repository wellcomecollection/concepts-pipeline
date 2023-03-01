package weco.concepts.common.json

import ujson.Value

import scala.util.Try

trait JsonOption[T] {
  def apply(node: ujson.Value): Option[T]
}

object JsonOps {
  import scala.language.implicitConversions

  implicit val identityOpt: JsonOption[ujson.Value] = Some(_)
  implicit val stringOpt: JsonOption[String] = _.strOpt
  implicit val doubleOpt: JsonOption[Double] = _.numOpt
  implicit val intOpt: JsonOption[Int] = doubleOpt(_).map(_.toInt)
  implicit val longOpt: JsonOption[Long] = doubleOpt(_).map(_.toLong)
  implicit val boolOpt: JsonOption[Boolean] = _.boolOpt
  implicit def seqOpt[T: JsonOption]: JsonOption[Seq[T]] =
    (node: ujson.Value) =>
      node.arrOpt.map { array =>
        array.toSeq.flatMap { value =>
          value.opt[T]
        }
      }

  implicit def JsonableOption[T](opt: Option[T])(implicit
    f: T => ujson.Value
  ): ujson.Value =
    opt.map(f).getOrElse(ujson.Null)

  implicit class JsonOps[V <: ujson.Value](node: V) {
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

    /** Return the data from the selector as a Seq, regardless of whether the
      * source JSON data is an array.
      *
      *   - A JSON array is be returned as a Seq,
      *   - A missing value or explicit null becomes an empty Seq
      *   - A single value is wrapped in a Seq
      *
      * This allows a caller to consistently treat optional/single/multiple
      * values as an array.
      *
      * This relates to the behaviour of Elasticsearch mappings, where the
      * distinction between a value and a list of values does not really exist.
      */
    def asSeq(selector: String): Seq[ujson.Value] = {
      val selected: Option[Value] = opt[ujson.Value](selector)
      selected match {
        case None                        => Nil
        case Some(found) if found.isNull => Nil
        case Some(found) =>
          found.arrOpt match {
            case Some(arr) => arr.toSeq
            case None      => Seq(found)
          }
      }
    }

    def withoutNulls: ujson.Value = node match {
      case ujson.Obj(items) =>
        ujson.Obj.from(items.filter {
          case (key, value: ujson.Null.type) => false
          case _                             => true
        })
      case _ => node
    }
  }
}
