package com.github.rcoh.query.loaders

import scala.annotation.StaticAnnotation

/**
  * Created by russell on 8/4/16.
  */

class Expose extends StaticAnnotation
class ExposeAlways extends StaticAnnotation

sealed trait FieldMode
case object Expose extends FieldMode
case object ExposeAlways extends FieldMode

/*object FieldMode {
  def fromAnnotation(annotation: Annotation)
}*/
