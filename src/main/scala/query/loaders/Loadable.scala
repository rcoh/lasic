package query.loaders

import org.json4s.JsonAST.{JDouble, JInt, JString, JValue}
import query.loaders.Loadable.Fields
import scala.language.experimental.macros

/**
  * Created by russell on 8/4/16.
  */

trait Loadable[A] {
  def load(a: A): Either[Fields, JValue]
}

object Loadable {
  type Fields = Map[String, LoadableField]

  def loadable[A]: Loadable[A] = macro LoadableMacroImpl.loadableImpl[A]

  implicit object StringLoadable extends Loadable[String] {
    override def load(a: String): Either[Fields, JValue] = Right(JString(a))
  }

  implicit object IntLoadable extends Loadable[Int] {
    override def load(a: Int): Either[Fields, JValue] = Right(JInt(a))
  }

  implicit object DoubleLoadable extends Loadable[Double] {
    override def load(a: Double): Either[Fields, JValue] = Right(JDouble(a))
  }

  def toConcreteLoadable[A: Loadable](loadable: A): ConcreteLoadable = {
    new ConcreteLoadable {
      override def load: Either[Fields, JValue] = implicitly[Loadable[A]].load(loadable)
    }
  }

  implicit class LoadableOps[A: Loadable](a: A) {
    def load() = implicitly[Loadable[A]].load(a)
  }
}

trait ConcreteLoadable {
  def load: Either[Fields, JValue]
}

case class LoadableField(fieldMode: FieldMode, loader: () => Either[ConcreteLoadable, Iterable[ConcreteLoadable]])