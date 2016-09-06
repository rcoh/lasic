package com.github.rcoh.query.lang

object Query {
  val DefaultPaging = Paging(20, 0)
  val NoQuery = Query(Map(), DefaultPaging)
}

case class Paging(max: Int, offset: Int = 0)
case class Query(fields: Map[String, Query], paging: Paging)


