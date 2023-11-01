package vyxal.gen

private[vyxal] object GenTools:
  val templateRegex = raw"\{\{(\w+?)\}\}".r
  def template(input: String, map: Map[String, Object]): String =
    val replacements = templateRegex
      .findAllMatchIn(input)
      .map(m => m -> map.get(m.group(1)) // fuck you
      )
      .filter(_._2.isDefined)
      .map(p => p._1 -> p._2.get)
    replacements
      .scanLeft(input)((s, m) =>
        s.patch(
          m._1.start + (s.size - input.size),
          m._2.toString,
          m._1.matched.size,
        )
      )
      .toList
      .last
  end template
end GenTools
