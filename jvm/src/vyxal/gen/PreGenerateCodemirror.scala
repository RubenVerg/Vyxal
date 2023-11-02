package vyxal.gen

import vyxal.parsing.Lexer
import vyxal.Elements

private[vyxal] object PreGenerateCodemirror:
  val codepage = Lexer.Codepage
    .filter(_ != '\n')
    .map(c => "\\u" + Integer.toHexString(c | 0x10000).substring(1))
    .mkString
  val digraphable = codepage.filterNot(Lexer.SyntaxDigraphs.contains(_))
  val elements = Elements.elements.keys
    .filterNot(s => !"∆øÞk#".contains(s(0)))
    .map(_.charAt(0))
    .map(c => "\\u" + Integer.toHexString(c | 0x10000).substring(1))
    .mkString

  def generate(input: String): String =
    GenTools.template(
      input,
      Map(
        "codepage" -> codepage,
        "digraphable" -> digraphable,
        "elementChar" -> elements,
        "oneModChar" -> Lexer.MonadicModifiers,
        "twoModChar" -> Lexer.DyadicModifiers,
        "threeModChar" -> Lexer.TriadicModifiers,
        "fourModChar" -> Lexer.TetradicModifiers,
        "specialModChar" -> "ᵜ",
      ),
    )
end PreGenerateCodemirror
