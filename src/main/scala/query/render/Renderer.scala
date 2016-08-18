package query.render

import org.json4s.JsonAST.{JArray, JObject, JValue}
import query.lang.Query
import query.loaders.{ConcreteLoadable, ExposeAlways, Loadable, LoadableField}

/**
  * Created by russell on 8/7/16.
  */
trait RenderError
case class FieldDoesNotExistError(field: String) extends Exception(s"Field $field did not exist in object.")
case class ObjectNotIndexable(field: String, obj: JValue) extends Exception(s"Requested $field but $obj is not indexable.")

object Renderer {
  def render[A: Loadable](obj: A, query: Query): JValue = {
    val concreteLoadable = Loadable.toConcreteLoadable(obj)
    val loadResult = concreteLoadable.load
    val fieldMap = loadResult match {
      case Right(jValue) if query.fields.nonEmpty => throw ObjectNotIndexable(query.fields.head._1, jValue)
      case Right(jValue) => return jValue
      case Left(fields) => fields
    }

    val requestedFields = query.fields.map { case (fieldName, subQuery) =>
        val field = fieldMap.getOrElse(fieldName, throw FieldDoesNotExistError(fieldName))
        field.loader() match {
          case Left(loadable) => fieldName -> render(loadable, subQuery)
          case Right(loadables) => fieldName -> renderList(loadables, subQuery)
        }
    }

    val alwaysExposed = (fieldMap -- query.fields.keySet).collect {
      case (fieldName, LoadableField(ExposeAlways, loader)) => loader() match {
        case Left(loadable) => fieldName -> render(loadable, Query.NoQuery)
        case Right(loadables) => fieldName -> renderList(loadables, Query.NoQuery)
      }
    }
    val resultingMap = requestedFields ++ alwaysExposed
    JObject(resultingMap.toList: _*)
  }

  private def renderList(loadables: Iterable[ConcreteLoadable], query: Query): JArray = {
    val paged = loadables.drop(query.paging.offset).take(query.paging.max)
    val renderedObjects = paged.map(render(_, query))
    JArray(renderedObjects.toList)
  }
}
