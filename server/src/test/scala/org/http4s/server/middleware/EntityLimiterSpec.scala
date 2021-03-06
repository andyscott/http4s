package org.http4s
package server
package middleware

import java.nio.charset.StandardCharsets

import fs2._
import fs2.Stream._
import org.http4s.server.middleware.EntityLimiter.EntityTooLarge
import Method._
import Status._

class EntityLimiterSpec extends Http4sSpec {
  import Http4s._

  val s = HttpService {
    case r: Request if r.uri.path == "/echo" => r.decode[String](Response(Ok).withBody)
  }

  val b = chunk(Chunk.bytes("hello".getBytes(StandardCharsets.UTF_8)))

  "EntityLimiter" should {

    "Allow reasonable entities" in {
      EntityLimiter(s, 100).apply(Request(POST, uri("/echo"), body = b))
        .map(_ => -1) must returnValue(-1)
    }

    "Limit the maximum size of an EntityBody" in {
      EntityLimiter(s, 3).apply(Request(POST, uri("/echo"), body = b))
        .map(_ => -1)
        .handle { case EntityTooLarge(i) => i } must returnValue(3)
    }

    "Chain correctly with other HttpServices" in {
      val s2 = HttpService {
        case r: Request if r.uri.path == "/echo2" => r.decode[String](Response(Ok).withBody)
      }

      val st = EntityLimiter(s, 3)
      (st.apply(Request(POST, uri("/echo2"), body = b))
        .map(_ => -1)
        must returnValue(-1))
      (st.apply(Request(POST, uri("/echo"), body = b))
        .map(_ => -1)
        .handle { case EntityTooLarge(i) => i }
        must returnValue(3))
    }
  }

}
