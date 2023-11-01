package vyxal.gen

import vyxal.parsing.Lexer
import vyxal.Elements
import vyxal.Modifiers

private[vyxal] object PreGenerateCodemirror:
  val codepage = Lexer.Codepage
    .filter(_ != '\n')
    .map(c => "\\u" + Integer.toHexString(c | 0x10000).substring(1))
    .mkString
  val charactersThatArentAllowedInHashDigraphs = "[]$!=#>@{".toCharArray()
  val codepageExcludingCharactersThatArentAllowedInHashDigraphs =
    codepage.filterNot(charactersThatArentAllowedInHashDigraphs.contains(_))
  val elements = Elements.elements.keys
    .filterNot(_.startsWith("#"))
    .map(_.charAt(0))
    .map(c => "\\u" + Integer.toHexString(c | 0x10000).substring(1))
    .mkString
  val modifiersByArity =
    Modifiers.modifiers.groupBy(_._2.arity).view.mapValues(_.keys.mkString)

  def generate(input: String): String =
    GenTools.template(
      input,
      Map(
        "codepage" -> codepage,
        "codepageExcludingCharactersThatArentAllowedInHashDigraphs" ->
          codepageExcludingCharactersThatArentAllowedInHashDigraphs,
        "elementChar" -> elements,
        "oneModChar" -> modifiersByArity.get(1).get,
        "twoModChar" -> modifiersByArity.get(2).get,
        "threeModChar" -> modifiersByArity.get(3).get,
        "fourModChar" -> modifiersByArity.get(4).get,
        "specialModChar" -> "",
      ),
    )
end PreGenerateCodemirror
