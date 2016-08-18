# lasic
Lasic is a small Scala library that allows you to generate JSON serialization on the fly. Rather than having to write separate APIs to return different fields of the same object, `lasic` allows the UI (or anyone else) to request both the fields and pagination they want. Here's a quick example:

```scala
case class User(name: String, bio: String, email: String)
def allUsers: List[User]
```
Suppose users had really prolific bios and you didn't want to send them to the UI when the UI only wanted the user's name. Rather than needing to create a separate endpoint, lasic defines a simple query language to allow the UI to request exactly the fields it wants:
```
[name,email]
```

It also includes control for pagination:
```
// Get the name and email for up to 10 users, starting at the 5th
[name,email]*10+5
```

With these queries, lasic will generate the specific JSON requested by the UI with _almost_ no boilerplate.

## Quick Start ##
Take your class:
```scala
case class User(name: String, email: String, bio: String, friends: List[Friend], internalId: Int)
```
Annotate the fields you want to expose to the UI:
```scala
case class User(@Expose name: String, @ExposeAlways email: String, @Expose bio: String, @Expose friends: List[User], internalId: Int)
```

Generate `Loadable[User]`:
```scala
implicit val userLoadable = Loadable.loadable[User]
```

Render some Json:
```scala
// Get the users name, email, and the name of their first 5 friends
val user: User = User("Alice", "alice@hotmail.com", "My bio", List(User("bob", "bob@geocities.net",  "bob", List())))
val query = QueryParser.parse("[name,email,friends[name]*5]")
Renderer.render(user, query)
/* { ... } */
```

## General Usage ##
In general, you'll want to wire up the rendering layer wherever in your webserver you are converting things to JSON to avoid code duplication. The only requirement is that the argument to 
the render method must be a member of the `Loadable` typeclass (`def foo[A: Loadable](a: A)`)

