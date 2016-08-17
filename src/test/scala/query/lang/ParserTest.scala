package query.lang

import org.scalatest.{Matchers, WordSpec}

/**
  * Created by russell on 8/3/16.
  */
class ParserTest extends WordSpec with Matchers {
  import Query._
  "QueryParser" should {
    "parse trivial queries" in {
      parse("[a]") should be(Query(Map("a" -> NoQuery), DefaultPaging))
      parse("[a]*5") should be(Query(Map("a" -> NoQuery), Paging(5)))
      parse("[a]*5+10") should be(Query(Map("a" -> NoQuery), Paging(5, 10)))
    }

    "parse complex queries" in {
      parse("[a,b*100+5,c[d,e]*10]*5+100") should be(
        Query(
          Map(
            "a" -> Query.NoQuery,
            "b" -> Query(Map(), Paging(100, 5)),
            "c" -> Query(Map("d" -> Query.NoQuery, "e" -> Query.NoQuery), Paging(10))
          ),
          Paging(5, 100)
        )
      )
    }

    "ignore whitespace" in {
      parse("[ a,b*    100+5 ,c[d,e]*    10]*5+100") should be(
        Query(
          Map(
            "a" -> Query.NoQuery,
            "b" -> Query(Map(), Paging(100, 5)),
            "c" -> Query(Map("d" -> Query.NoQuery, "e" -> Query.NoQuery), Paging(10))
          ),
          Paging(5, 100)
        )
      )
    }

    "give good error messages for missing `]`" in {
      noParse("[a,b").message should be("Expected `]`")
      noParse("[a,b").offset should be(4)
      noParse("[a,b").formattedError should be(
        """Expected `]`
          |[a,b
          |    ^""".stripMargin
      )
    }

    "give good error messages when a paging is invalid" in {
      noParse("[a*x]").message should be("Expected an integer")
      noParse("[a*x]").offset should be(3)
    }

    "handle multi-character field names" in {
      parse("[exposed]") should be (Query(Map("exposed" -> NoQuery), DefaultPaging))
    }

  }

  def parse(s: String) = QueryParser.parse(s).right.get
  def noParse(s: String) = QueryParser.parse(s).left.get

}
