package query.render

import org.json4s.JsonAST._
import org.scalatest.{Matchers, WordSpec}
import query.lang.QueryParser
import query.loaders.{Expose, ExposeAlways, Loadable}

/**
  * Created by russell on 8/17/16.
  */

trait ParentTrait {
  @Expose
  def exposedFromTrait = "exposedFromTrait"

  @ExposeAlways
  def exposedAlwaysFromTrait = "exposedAlwaysFromTrait"
}

class ObjectToRender extends ParentTrait {
  def unexposed = ???

  @Expose
  def exposed = "exposed"

  @ExposeAlways
  def exposedAlways = "exposedAlways"

  @Expose
  def exposedPrimitiveList = List(1, 2, 3, 4)

  @Expose
  def exposedCompoundList = List(NestedCaseClass("1", "2"), NestedCaseClass("3", "4"), NestedCaseClass("5", "6"))
}

case class NestedCaseClass(@Expose expose: String, @ExposeAlways exposeAlways: String)


class RendererTest extends WordSpec with Matchers {
  implicit val nestedRenderer = Loadable.loadable[NestedCaseClass]
  implicit val renderer = Loadable.loadable[ObjectToRender]
  val obj = new ObjectToRender

  def render(queryString: String): JValue = {
    QueryParser.parse(queryString) match {
      case Right(query) => Renderer.render(obj, query)
      case Left(err) => throw new RuntimeException(err.formattedError)
    }
  }

  "Renderer" should {
    "render basic queries" in {
      render("[exposed]") should be(
        JObject(
          "exposed" -> JString("exposed"),
          "exposedAlwaysFromTrait" -> JString("exposedAlwaysFromTrait"),
          "exposedAlways" -> JString("exposedAlways")))

      render("[exposedAlways]") should be(
        JObject(
          "exposedAlwaysFromTrait" -> JString("exposedAlwaysFromTrait"),
          "exposedAlways" -> JString("exposedAlways"))
      )
    }

    "render iterables" in {
      render("[exposedPrimitiveList*3+1]") should be(
        JObject(
          "exposedPrimitiveList" -> JArray(List(JInt(2), JInt(3),JInt(4))),
          "exposedAlwaysFromTrait" -> JString("exposedAlwaysFromTrait"),
          "exposedAlways" -> JString("exposedAlways"))

      )
    }

  }


}
