import {parser} from "./vy.grammar"
import {foldNodeProp, foldInside, indentNodeProp, LRLanguage, LanguageSupport} from "@codemirror/language"
import {styleTags, tags as t} from "@lezer/highlight"

export const SBCSLanguage = LRLanguage.define({
    parser: parser.configure({
        props: [
            styleTags({
                ElementChar: t.function(t.variableName),
                String: t.string,
                Number: t.number,
                SingleCharString: t.string,
                TwoCharString: t.string,
                TwoCharNumber: t.number,
                OneModChar: t.modifier,
                TwoModChar: t.modifier,
                ThreeModChar: t.modifier,
                FourModChar: t.modifier,
                Digraph: t.operator,
                SugarTrigraph: t.operator,
                SyntaxTrigraph: t.operator,
                StructureOpen: t.controlKeyword,
                StructureSingleClose: t.bracket,
                StructureDoubleClose: t.bracket,
                StructureAllClose: t.bracket,
                ListOpen: t.squareBracket,
                ListClose: t.squareBracket,
                VariableName: t.variableName,
                Lambda: t.definition(t.function(t.variableName)),
                VariableGet: t.operator,
                VariableAssign: t.definitionKeyword,
                VariableAugAssign: t.definitionOperator,
                VariableUnpack: t.operator,
                ContextIndex: t.keyword
            })
        ]
    })
})

export function SBCS() {
    return new LanguageSupport(SBCSLanguage)
}