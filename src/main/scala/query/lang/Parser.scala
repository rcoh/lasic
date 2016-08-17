package query.lang


import scala.util.parsing.combinator.RegexParsers

/**
  * Created by russell on 8/3/16.
  */

case class ParseError(message: String, offset: Int, formattedError: String) {
  override def toString = formattedError
}

object QueryParser {
  def parse(s: String): Either[ParseError, Query] = ParserImpl.parseAll(ParserImpl.query, s) match {
    case ParserImpl.Success(result, _) => Right(result)
    case ParserImpl.NoSuccess(error, next) => Left(createParseError(error, next))
  }

  def createParseError(error: String, next: ParserImpl.Input) = {
    ParseError(error, next.pos.column-1, s"$error\n${next.source}\n${" " * (next.pos.column-1)}^")
  }
}

private object ParserImpl extends RegexParsers {
  case class Subquery(field: String, query: Query)
  val ident = "[a-zA-Z]+".r | failure("Expected an identifier")
  def num: Parser[Int] = ("[0-9]+".r | failure("Expected an integer")) ^^ { _.toInt }

  // [a,b*100+5,c[d]*10]*5+100
  // Query(Map("a" -> Query.NoQuery, "b" -> Query(Map(), Paging(100, 5)), "c" -> Query(Map("d" -> Query.NoQuery), Paging(10))
  def query: Parser[Query] = opt(("[" | failure("Queries must start with `[`")) ~> rep1sep(subquery, ",") <~ ("]" | failure("Expected `]`"))) ~ opt(paging) ^^ { case subqueriesOpt ~ pagingOpt => {
    val subqueries = subqueriesOpt.getOrElse(List())
    val subqueryMap = subqueries.map(q => q.field -> q.query).toMap
    Query(subqueryMap, pagingOpt.getOrElse(Query.DefaultPaging))
  }}

  def subquery: Parser[Subquery]  = ident ~ query ^^ { case (field ~ query) => Subquery(field, query) }

  // TODO: because paging is optional the errors don't propagate that well
  def paging = ((("*" ~> num) ~ opt("+" ~> num)) | failure("Expected paging expression like *20+10"))  ^^ { case (maxOpt ~ offsetOpt) =>
      Paging(maxOpt, offsetOpt.getOrElse(0))
  }


}
