import {lezer} from "@lezer/generator/rollup"

export default {
  input: "./build/vy.grammar",
  output: [{
    format: "es",
    file: "./build/vy.js"
  }, {
    format: "cjs",
    file: "./build/vy.cjs"
  }],
  external: ["@lezer/lr", "@lezer/highlight"],
  plugins: [lezer()]
}