# lasic [![Build Status](https://travis-ci.org/rcoh/lasic.svg?branch=master)](https://travis-ci.org/rcoh/lasic)
Lasic is a small Scala library that allows you to generate JSON serialization on the fly. If you're coming from a functional background, lasic may make more sense described as "runtime lenses converted to JSON". Rather than having to write separate APIs to return different fields of the same object, `lasic` allows the UI (or anyone else) to request both the fields and pagination they want. It's also completely type safe, verified at compile time, and doesn't use reflection. Here's a quick example:

```scala
case class User(name: String, bio: String, email: String)
def allUsers: List[User]
```
Suppose users had really prolific bios and you didn't want to send them to the UI when the UI only wanted the user's name. Rather than needing to create a separate endpoint, lasic defines a simple query language to allow the UI to request exactly the fields it wants:
```
// Just the name an email, no bio
[name,email]
```

It also includes control for pagination:
```
// Get the name and email for up to 10 users, starting at the 5th
[name,email]*10+5
```

With these queries, lasic will generate the specific JSON requested by the UI with _almost_ no boilerplate.

## Quick Start ##
Add `lasic` as a dependency:
* For SBT:
```
libraryDependencies += "com.github.rcoh" % "lasic_2.11" % "0.2.0"
```
* For Maven:
```
<dependency>
  <groupId>com.github.rcoh</groupId>
  <artifactId>lasic_2.11</artifactId>
  <version>0.2.0</version>
</dependency>
```


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
implicit val userLoadable = Loadable.byAnnotations[User]
```

Render some Json:
```scala
// Get the users name, email, and the name of their first 5 friends
val user: User = User("Alice", "alice@hotmail.com", "My bio", List(User("bob", "bob@geocities.net",  "bob", List())))
val query = QueryParser.parse("[name,friends[name]*5]")
Renderer.render(user, query)
/*
{
  "name":"Alice",
  "email":"alice@hotmail.com",
  "friends":[{
    "name":"bob",
    "email":"bob@geocities.net"
  }]
}
*/
```
The `email` field appears even though we didn't request it because it's set to `ExposeAlways`. Note how the renderer properly handles the fact that `friends` is a list and correctly extracts fields.

### But wait: there's more!
* For cases where you want all the fields:
```
// As if you put `@ExposeAlways` on each field (not method!!)
implicit val allFieldsLoader = Loadable.allFieldsAlways[User]

// As if you put `@Expose` on each field
implicit val byRequest = Loadable.allFieldsByRequest[User]
```
* Lasic also works for traits, normal classes, and objects. It even works if the annotated fields come from different places:
```
trait Entity {
   @ExposeAlways
   def id: String
}

trait UserEntity extends Entity {
   @Expose
   def email: String
   
   @Expose
   def age: Int
}
```

## General Usage ##
In general, you'll want to wire up the rendering layer wherever in your webserver you are converting things to JSON to avoid code duplication. The only requirement is that the argument to 
the render method must be a member of the `Loadable` typeclass (`def foo[A: Loadable](a: A)`)

## Under the Hood ##
Lasic uses macros to build accesors for fields marked with `@Expose` and `@ExposeAlways`. The macro brings classes into the type class `Loadable[T]`. The macro also verifies that all exposed members of a given class are also `Loadable[T]`. Given a `Loadable[T]` the renderer parses the query string and builds the JSON object.
