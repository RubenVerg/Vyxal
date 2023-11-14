package vyxal.gen

import vyxal.{Element, Elements, Modifiers, SugarMap}
import vyxal.parsing.Lexer

/** For generating elements.txt and trigraphs.txt. See build.sc */
private object GenerateDocs:
  def generate(): (String, String, String) =
    (elements(), trigraphs(), elementTable())

  def elements(): String =
    val sb = StringBuilder()
    Elements.elements.values.toSeq
      .sortBy { elem =>
        // Have to use tuple in case of digraphs
        (
          Lexer.Codepage.indexOf(elem.symbol.charAt(0)) +
            (if "#∆øÞ".contains(elem.symbol.charAt(0)) then 400 else 0),
          Lexer.Codepage.indexOf(elem.symbol.substring(1)),
        )
      }
      .foreach {
        case Element(
              symbol,
              name,
              keywords,
              arity,
              vectorises,
              overloads,
              impl,
            ) =>
          sb ++=
            s"$symbol ($name) (${if vectorises then "" else "non-"}vectorising)\n"

          SugarMap.trigraphs
            .collect { case (tri, s) if s == symbol => tri }
            .foreach { tri => sb ++= s"Trigraph: $tri\n" }

          sb ++= s"Keywords:${keywords.mkString(" ", ", ", "")}\n"
          overloads.foreach { overload => sb ++= s"- $overload\n" }
          sb ++= "---------------------\n"
      }

    Modifiers.modifiers.foreach {
      case (name, info) =>
        sb ++= s"$name\n"
        sb ++= s"Keywords:${info.keywords.mkString(" ", ", ", "")}\n"
        sb ++= s"Description: ${info.description}\n"
        SugarMap.trigraphs
          .collect { case (tri, s) if s == name => tri }
          .foreach { tri => sb ++= s"Trigraph: $tri\n" }
        sb ++= "----------------------\n"
    }

    sb.toString
  end elements

  def trigraphs(): String =
    SugarMap.trigraphs
      .map { case (key, value) => s"$key -> $value" }
      .mkString("", "\n", "\n")

  def elementTable(): String =
    val header =
      "| Symbol | Trigraph |  Name | Keywords | Arity | Vectorises | Overloads |"
    val divider = "| --- | --- | --- | --- | --- | --- | --- |"
    val contents = StringBuilder()
    val formatOverload = (overload: String) =>
      val (args, description) = overload.splitAt(overload.indexOf("->"))
      val newDesc = description
        .replace("|", "\\|")
        .replace("->", "")
        .stripLeading()
        .stripTrailing()
      if args.stripTrailing() == "" then s"`$newDesc`"
      else
        s"`${args.stripTrailing().replace("|", "\\|").replace("->", "")}` => `$newDesc`"
    Elements.elements.values.toSeq
      .sortBy { elem =>
        // Have to use tuple in case of digraphs
        (
          Lexer.Codepage.indexOf(elem.symbol.charAt(0)) +
            (if "#∆øÞ".contains(elem.symbol.charAt(0)) then 400 else 0),
          Lexer.Codepage.indexOf(elem.symbol.substring(1)),
        )
      }
      .foreach(elem =>
        if !elem.symbol.startsWith("#|") then
          var trigraph = ""
          SugarMap.trigraphs
            .collect { case (tri, s) if s == elem.symbol => tri }
            .foreach { tri => trigraph = tri }
          var overloads = elem.overloads
          contents ++=
            s"| `${"\\".repeat(if elem.symbol == "`" then 1 else 0) +
                elem.symbol.replace("|", "\\|")}` | ${trigraph} | ${elem.name
                .replace("|", "/")} | ${elem.keywords
                .map("`" + _ + "`")
                .mkString(", ")} | ${elem.arity.getOrElse("NA")} | ${if elem.vectorises then ":white_check_mark:"
              else ":x:"} | ${formatOverload(overloads.head)}\n"
          overloads = overloads.tail
          while overloads.nonEmpty do
            contents ++= s"| | | | | | | ${formatOverload(overloads.head)}\n"
            overloads = overloads.tail
      )

    val elements = header + "\n" + divider + "\n" + contents.toString

    contents.setLength(0)

    val modifierHeader =
      "| Symbol | Trigraph | Name | Keywords | Arity | Description |"
    val modiDivider = "| --- | --- | --- | --- | --- | --- |"

    Modifiers.modifiers.keys
      .zip(Modifiers.modifiers.values)
      .toSeq
      .sortBy((modi, _) =>
        (
          Lexer.Codepage.indexOf(modi.charAt(0)) +
            (if "#∆øÞ".contains(modi.charAt(0)) then 400 else 0),
          Lexer.Codepage.indexOf(modi.substring(1)),
        )
      )
      .foreach(modi =>
        var trigraph = ""
        SugarMap.trigraphs
          .collect { case (tri, s) if s == modi._1 => tri }
          .foreach { tri => trigraph = tri }
        contents ++=
          s"| `${"\\".repeat(if modi._1 == "`" then 1 else 0) +
              modi._1.replace("|", "\\|")}` | ${trigraph} | ${modi._1
              .replace("|", "/")} | ${modi._2.keywords
              .map("`" + _ + "`")
              .mkString(", ")} | ${modi._2.arity} | ${modi._2.description
              .replace("|", "\\|")} |\n"
      )

    val modifiers = modifierHeader + "\n" + modiDivider + "\n" +
      contents.toString

    s"""
       |# Element Table
       |
       |## Elements
       |
       |$elements
       |
       |## Modifiers
       |
       |$modifiers
       |""".stripMargin

  end elementTable
end GenerateDocs
