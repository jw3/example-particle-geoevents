package com.github.jw3.geo

import java.util.UUID

package object test {
  def random(): String = UUID.randomUUID.toString.take(8)
}
