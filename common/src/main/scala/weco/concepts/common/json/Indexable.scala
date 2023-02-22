package weco.concepts.common.json

trait Indexable[T] {
  def id(t: T): String
  def toDoc(t: T): ujson.Value
  def toUpdateParams(t: T): ujson.Value = ujson.Obj()
  def fromDoc(doc: ujson.Value): Option[T]
}

object Indexable {
  def apply[T](implicit indexable: Indexable[T]): Indexable[T] = indexable

  implicit def indexableSeq[T: Indexable]: Indexable[Seq[T]] =
    new Indexable[Seq[T]] {
      def id(t: Seq[T]): String = throw new RuntimeException(
        "Can't identify a sequence, this Indexable must be part of a document"
      )
      def toDoc(t: Seq[T]): ujson.Value = ujson.Arr(t.map(_.toDoc): _*)

      def fromDoc(doc: ujson.Value): Option[Seq[T]] =
        doc.arrOpt.map(_.flatMap(Indexable[T].fromDoc).toSeq)
    }

  implicit class IndexableOps[T: Indexable](t: T) {
    def id: String = Indexable[T].id(t)
    def toDoc: ujson.Value = Indexable[T].toDoc(t)
    def toUpdateParams: ujson.Value =
      Indexable[T].toUpdateParams(t)
  }
}
