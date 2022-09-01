package weco.concepts.common.model

/** A UsedConcept represents a concept in use in Works in the Catalogue. It
  * links a Wellcome canonicalId to an identifier from an external authority.
  *
  * The label is included in all records, but only really required for
  * identifiers with no authority (label-derived), where it will be used as the
  * preferred label later in the pipeline. In authority-derived concepts, it
  * will be overridden by the authority's preferred label, but it is helpful to
  * record it anyway to help understand the resulting records in the aggregated
  * concepts database.
  */
case class UsedConcept(
  identifier: Identifier,
  label: String,
  canonicalId: String
) {
  override def toString: String =
    s"\n$canonicalId\t${identifier.toString.padTo(70, ' ')}$label"
}
