package com.github.rcoh.query.render

import org.json4s.JsonAST._
import org.scalatest.{Matchers, WordSpec}
import com.github.rcoh.query.lang.QueryParser
import com.github.rcoh.query.loaders.{Expose, ExposeAlways, Loadable}


/**
  * Created by russell on 8/17/16.
  */

case class User(@Expose name: String, @ExposeAlways email: String, @Expose bio: String, @Expose friends: List[User], internalId: Int)
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

    /*"render the example from the readme" in {
      implicit val userLoader = Loadable.loadable[User]
      val user: User = User("Alice", "alice@hotmail.com", "My bio", List(User("bob", "bob@geocities.net",  "bob", List(), 2)), 1)
      val query = QueryParser.parse("[name,email,friends[name]*5]").right.get
      //println(pretty(render((Renderer.render(user, query)))))
    }*/

  }


}
