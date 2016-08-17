package query.loaders

import scala.reflect.macros.blackbox.Context
import language.experimental.macros

/**
  * Created by russell on 8/4/16.
  */


object LoadableMacroImpl {
  /**
    * 1. Traverse the fields of A
    * 2. Extract fields that are annotated with matching macros
    * 3. Return a `Loadable[A]` that provides a listing of those fields
    * @param c
    * @tparam A
    * @return
    */
  def loadableImpl[A](c: Context)(implicit atag: c.WeakTypeTag[A]): c.Expr[Loadable[A]] = {
    import c.universe._
    implicit val liftableFieldMode = Liftable[FieldMode] {
      case Expose => q"_root_.query.loaders.Expose"
      case ExposeAlways => q"_root_.query.loaders.ExposeAlways"
    }

    val instanceType = atag.tpe
    val aSymbol = atag.tpe.typeSymbol
    val validMembers = instanceType.members.flatMap { member =>
      val fieldModes = member.annotations.flatMap(annotationMapper(c)(_))
      fieldModes match {
        case mode :: Nil => Some(member -> mode)
        case Nil => None
        case _ => throw new IllegalArgumentException(s"Expected only 1 matching annotation but instead found $fieldModes")
      }
    }

    // Want to end up with:
    // Either[Iterable(String, LoadableField), JValue]
    val fields = validMembers.map { case (symbol, fieldMode) =>
        val term = symbol.asTerm
        if (term.isMethod && term.asMethod.paramLists.nonEmpty) {
          throw new IllegalArgumentException("Method must not have arguments")
        }

        // A bit of massaging to get a symbol that will work for fields and methods
        val accessor: Symbol = if (symbol.isMethod) {
          symbol.asMethod
        } else if (symbol.isTerm) {
          symbol.asTerm.getter
        } else {
          throw new RuntimeException(s"$symbol was not a supported type for loading")
        }

        if (term.typeSignature <:< typeOf[Iterable[_]]) {
          q"""${accessor.name.toString} -> LoadableField($fieldMode, () => Right(a.$accessor.map(loadable => Loadable.toConcreteLoadable(loadable))))"""
        } else {
          q"""${accessor.name.toString} -> LoadableField($fieldMode, () => Left(Loadable.toConcreteLoadable(a.$accessor)))"""
        }
    }.toList

    val imports = q"import org.json4s.JsonAST.JValue"
    val fieldList = q"""Map($fields: _*)"""
    c.Expr[Loadable[A]](
      q"""
        $imports
        new Loadable[$aSymbol] {
          def load(a: $aSymbol): Either[Map[String, LoadableField], JValue] = Left($fieldList)
      }
      """)
  }

  def annotationMapper(c: Context)(annotation: c.universe.Annotation): Option[FieldMode] = {
    import c.universe._
    annotation.tree.tpe match {
      case x if x == typeOf[Expose] => Some(Expose)
      case x if x == typeOf[ExposeAlways] => Some(ExposeAlways)
      case _ => None
    }
  }
}
