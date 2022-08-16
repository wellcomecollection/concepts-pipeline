package weco.concepts.common.model

/** A UsedConcept represents a concept in use in Works in the Catalogue It links
  * a Wellcome canonicalId to an identifier from an external authority
  */
case class UsedConcept(
  identifier: Identifier,
  label: String,
  canonicalId: String
){
  override def toString():String = s"\n$canonicalId\t${identifier.toString.padTo(70, ' ')}$label"
}
