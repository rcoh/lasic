package com.github.rcoh.query.loaders

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
      case Expose => q"_root_.com.github.rcoh.query.loaders.Expose"
      case ExposeAlways => q"_root_.com.github.rcoh.query.loaders.ExposeAlways"
    }

    val instanceType = weakTypeOf[A]
    val aSymbol = atag.tpe.typeSymbol
    val caseClassAnnotations: Map[String, List[Annotation]] = if (aSymbol.isClass && aSymbol.asClass.isCaseClass) {
      val constructorSymbols = aSymbol.asClass.primaryConstructor.typeSignature.paramLists.head
      constructorSymbols.map(s => s.name.toString -> s.annotations).toMap
    } else { Map() }
    val validMembers = instanceType.members.flatMap { member =>
      val annotationsFromCase = caseClassAnnotations.getOrElse(member.name.toString, List())
      val fieldModes = (member.annotations ++ annotationsFromCase).flatMap(annotationMapper(c)(_))
      fieldModes match {
        case mode :: Nil => Some(member -> mode)
        case Nil => None
        case _ => throw new IllegalArgumentException(s"Expected only 1 matching annotation but instead found $fieldModes")
      }
    }

    val loadableField = q"_root_.com.github.rcoh.query.loaders.LoadableField"
    val jValue = q"_root_.org.json4s.JsonAST.JValue"
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

        if (accessor.typeSignature.resultType <:< typeOf[Iterable[Any]]) {
          q"""${accessor.name.toString} -> $loadableField($fieldMode, () => Right(a.$accessor.map(loadable => Loadable.toConcreteLoadable(loadable))))"""
        } else {
          q"""${accessor.name.toString} -> $loadableField($fieldMode, () => Left(Loadable.toConcreteLoadable(a.$accessor)))"""
        }
    }.toList

    val fieldList = q"""Map($fields: _*)"""
    c.Expr[Loadable[A]](
      q"""
        new Loadable[$aSymbol] {
          def load(a: $aSymbol): Either[Map[String, _root_.com.github.rcoh.query.loaders.LoadableField], _root_.org.json4s.JsonAST.JValue] = Left($fieldList)
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