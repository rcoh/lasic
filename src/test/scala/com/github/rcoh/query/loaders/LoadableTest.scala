package com.github.rcoh.query.loaders

import org.json4s.JValue
import org.json4s.JsonAST.{JDouble, JInt, JObject, JString}
import org.scalatest.{Matchers, WordSpec}

/**
  * Created by russell on 8/7/16.
  */

class LoadableClass {
  @ExposeAlways
  val always = "always"

  @ExposeAlways
  val alwaysIterable = List("always")

  @Expose
  val byRequest = "byRequest"
  @Expose
  val byRequestIterable = "byRequestIterable"

  @Expose
  def method = "method"

  def unexposed = ???

  @Expose
  def exposedList = List(1,2,3)
}

case class LoadableCaseClass(@Expose exposeCase: String)
case class RecursiveList(@Expose y: Int, @Expose x: List[RecursiveList])
case class Recursive(@Expose r: Recursive)

class LoadableTest extends WordSpec with Matchers {
  import Loadable._
  "Loadable" should {
    "Provide default implementations for built-in types" in {
      5.load should be (Right(JInt(5)))
      "123".load should be(Right(JString("123")))
      5.5.load should be(Right(JDouble(5.5)))
    }

    "Provide loadable via macro for classes" in {
      implicit val loadableLoader = Loadable.byAnnotations[LoadableClass]
    }

    "Support loading fields and methods" in {
      implicit val loadableLoader = Loadable.byAnnotations[LoadableClass]
      val instance = new LoadableClass()
      val loader = instance.load().left.get
      loader("always").fieldMode should be(ExposeAlways)
      loader("always").loader().left.get.load should be(Right(JString("always")))
      loader.keys should not contain("unexposed")

      loader("method").loader().left.get.load should be(Right(JString("method")))
    }

    "Support case classes" in {
      val instance = LoadableCaseClass("exposeCase")
      implicit val caseClassLoader = Loadable.byAnnotations[LoadableCaseClass]
      val loader = instance.load().left.get
      loader("exposeCase").fieldMode should be(Expose)
      loader("exposeCase").loader().left.get.load should be(Right(JString("exposeCase")))
    }

    "Support recursive types" in {
      implicit val recursiveLoader = Loadable.byAnnotations[RecursiveList]
      val r = RecursiveList(5, List(RecursiveList(10, List())))
      val loader = r.load().left.get
      loader("y").loader().left.get.load should be(Right(JInt(5)))
      val loaderList = loader("x").loader().right.get.toList
      loaderList should have length(1)
      loaderList.head.load.left.get("y").loader().left.get.load should be(Right(JInt(10)))

      implicit val recursiveLoader2 = Loadable.byAnnotations[Recursive]
    }

    "Support allFieldsAlways" in {
      case class Record(f1: Int, f2: String, f3: List[String])
      implicit val recordLoader = Loadable.allFieldsAlways[Record]
      val r = Record(1, "2", List("1", "2", "3"))
      val loader = r.load().left.get
      loader.keySet should be(Set("f1", "f2", "f3"))
      loader("f1").fieldMode should be(ExposeAlways)
    }

    "Support allFieldsByRequest" in {
      case class Record(f1: Int, f2: String, f3: List[String])
      implicit val recordLoader = Loadable.allFieldsByRequest[Record]
      val r = Record(1, "2", List("1", "2", "3"))
      val loader = r.load().left.get
      loader.keySet should be(Set("f1", "f2", "f3"))
      loader("f1").fieldMode should be(Expose)
    }

    "JSON should be loadable" in {
      val json: JValue = JObject("f1" -> JString("f1"))
      json.load().left.get("f1").loader().left.get.load should be(Right(JString("f1")))
    }




  }

}
