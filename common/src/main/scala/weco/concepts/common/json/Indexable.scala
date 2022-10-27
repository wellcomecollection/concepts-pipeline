package weco.concepts.common.json

import ujson.Value

trait Indexable[T] {
  def id(t: T): String
  def toDoc(t: T): ujson.Value
  def fromDoc(doc: ujson.Value): Option[T]
}

object Indexable {
  def apply[T](implicit indexable: Indexable[T]): Indexable[T] = indexable

  implicit class IndexableOps[T: Indexable](t: T) {
    def id: String = Indexable[T].id(t)
    def toDoc: Value = Indexable[T].toDoc(t)
  }
}
