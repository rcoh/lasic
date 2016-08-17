package query.render

import org.json4s.JsonAST.JValue
import query.lang.Query
import query.loaders.Loadable

/**
  * Created by russell on 8/7/16.
  */
trait Renderable {
  def render[A: Loadable](obj: A, query: Query): JValue
}
