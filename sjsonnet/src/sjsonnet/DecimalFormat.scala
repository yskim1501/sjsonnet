package sjsonnet

/**
  * Minimal re-implementation of java.text.DecimalFormat, for Scala.js
  * compatibility (the existing shim for java.text scala-java-locales isn't
  * fully compliant, and adds tons of unnecessary code that bloats the JS bundle)
  */
object DecimalFormat {

  def trailingZeroes(n: Long) = {
    var count = 0
    var current = n
    var done = false
    while(!done && current > 0){
      if (current % 10 == 0) count += 1
      else done = true
      current /= 10
    }
    count
  }
  def leftPad(n: Long, targetWidth: Int): String = {
    val sign = if (n < 0) "-" else ""
    val absN = math.abs(n)
    val nWidth = if (absN == 0) 1 else Math.log10(absN).toInt + 1
    sign + "0" * (targetWidth - nWidth) + absN
  }
  def rightPad(n0: Long, minWidth: Int, maxWidth: Int): String = {
    if (n0 == 0 && minWidth == 0) ""
    else {
      val n = (n0 / Math.pow(10, trailingZeroes(n0))).toInt
      assert(n == math.abs(n))
      val nWidth = if (n == 0) 1 else Math.log10(n).toInt + 1
      (n + "0" * (minWidth - nWidth)).take(maxWidth)
    }
  }
  def format(pattern: String, number: Double): String = {
    val (wholeStr, fracStrOpt, expStrOpt) = pattern.split("\\.", -1).map(_.split('E')) match{
      case Array(Array(wholeStr: String)) =>
        (wholeStr, None, None)
      case Array(Array(wholeStr: String, expStr: String)) =>
        (wholeStr, None, Some(expStr))
      case Array(Array(wholeStr: String), Array(fracStr: String)) =>
        (wholeStr, Some(fracStr), None)
      case Array(Array(wholeStr: String), Array(fracStr: String, expStr: String)) =>
        (wholeStr, Some(fracStr), Some(expStr))
    }
    val wholeLength = wholeStr.length
    val fracLengthOpt = fracStrOpt.map(fracStr => (fracStr.count(_ == '0'), fracStr.count(_ == '#')))
    val expLengthOpt = expStrOpt.map(_.length)


    expLengthOpt match{
      case Some(expLength) =>
        val roundLog10 = Math.ceil(Math.log10(Math.abs(number))).toInt
        val expNum = roundLog10 - wholeLength
        val scaled = number / math.pow(10, expNum)
        val prefix = leftPad(scaled.toInt, wholeLength)
        val expFrag = leftPad(expNum, expLength)
        val fracFrag = fracLengthOpt.map{case (zeroes, hashes) =>
          if (zeroes == 0 && hashes == 0) ""
          else {
            val divided = (number / Math.pow(10, expNum - zeroes - hashes))
            val scaledFrac = divided % Math.pow(10, zeroes + hashes)
            rightPad(Math.abs(Math.round(scaledFrac).toInt), zeroes, zeroes + hashes)
          }
        }

        fracFrag match{
          case None  => prefix + "E" + expFrag
          case Some("") => if (fracLengthOpt.contains((0, 0))) prefix + ".E" + expFrag else prefix + "E" + expFrag
          case Some(frac) => prefix + "." + frac + "E" + expFrag
        }
      case None =>
        val prefix = leftPad(number.toInt, wholeLength)
        val fracFrag = fracLengthOpt.map { case (zeroes, hashes) =>
          val fracNum = Math.round(number * math.pow(10, zeroes + hashes)).toLong % math.pow(10, zeroes + hashes).toLong
          rightPad(fracNum, zeroes, zeroes + hashes)
        }
        fracFrag match{
          case None  => prefix
          case Some("") => if (fracLengthOpt.contains((0, 0))) prefix + "." else prefix
          case Some(frac) => prefix + "." + frac
        }

    }

  }

}