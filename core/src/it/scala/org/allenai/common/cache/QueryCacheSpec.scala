package org.allenai.common.cache

import java.io.File
import org.allenai.common.{ GitVersion, Version, cache }
import org.allenai.common.testkit.UnitSpec
import org.scalatest.BeforeAndAfterAll
import spray.json.DefaultJsonProtocol._
import sys.process._

case class Foo(stringVar: String, intVar: Int)

object FooJsonProtocol extends DefaultJsonProtocol {
  implicit val fooFormat = jsonFormat2(Foo)
}

class QueryCaches(redisHostname: String, redisPort: Int) {
  import FooJsonProtocol.__

  val stringQueryCache = new JsonQueryCache[String](redisHostname, redisPort, "test")
  val intQueryCache = new JsonQueryCache[Int](redisHostname, redisPort, "test")
  val seqStringQueryCache = new JsonQueryCache[Seq[String]](redisHostname, redisPort, "test")

  // It's an object I can test
  val fooQueryCache = new JsonQueryCache[Foo](redisHostname, redisPort, "test")

  val stringKey = "stringKey"
  val stringValue = "stringValue"

  val intKey = "intKey"
  val intValue = 32

  val seqStringKey = "seqStringKey"
  val seqStringValue = Seq("a string", "a second string", "third time's the charm")

  val fooKey = "fooKey"
  val fooValue = new Foo("stringerino", 42)

  def getAll(): Seq[Option[Any]] = Seq(
    stringQueryCache.get(stringKey),
    intQueryCache.get(intKey),
    seqStringQueryCache.get(seqStringKey),
    fooQueryCache.get(fooKey)
  )

  def putAll(): Unit = {
    stringQueryCache.put(stringKey, stringValue)
    intQueryCache.put(intKey, intValue)
    seqStringQueryCache.put(seqStringKey, seqStringValue)
    fooQueryCache.put(fooKey, fooValue)
  }

  def delAll(): Unit = {
    stringQueryCache.del(stringKey)
    intQueryCache.del(intKey)
    seqStringQueryCache.del(seqStringKey)
    fooQueryCache.del(fooKey)
  }

  def allThereAndEq(): Boolean = {
    stringQueryCache.get(stringKey).exists(_.equals(stringValue)) &&
      intQueryCache.get(intKey).exists(_.equals(intValue)) &&
      seqStringQueryCache.get(seqStringKey).exists(_.equals(seqStringValue)) &&
      fooQueryCache.get(fooKey).exists(_.equals(fooValue))
  }
}

class QueryCacheSpec extends UnitSpec with BeforeAndAfterAll {

  val redisHostname = "127.0.0.1"
  val redisPort = 6379
  val redisServer = "redis-server".run()
  val queryCaches = new QueryCaches(redisHostname, redisPort)

  "QueryCache" should "return None when items are not in cache" in {
    assert(queryCaches.getAll().forall(_.isEmpty))
  }

  it should "put the items in properly and let us get them back" in {
    queryCaches.putAll()
    assert(queryCaches.allThereAndEq())
  }

  it should "delete the items properly" in {
    queryCaches.delAll()
    assert(queryCaches.getAll().forall(_.isEmpty))
  }

  override def afterAll() {
    Seq("redis-cli", "FLUSHALL").!!
    Seq("redis-cli", "SHUTDOWN").!!
    // delete redis dump file if it exists 
    val f = new File("./dump.rdb")
    if (f.exists) {
      f.delete()
    }
  }
}
