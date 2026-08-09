import { readFile, writeFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const currentDir = dirname(fileURLToPath(import.meta.url));
const foundationsDir = resolve(currentDir, "../../foundations");

const sources = [
  { file: "color-primitives.json", group: "colorPrimitive" },
  { file: "color-semantics.json", group: "colorSemantic" },
  { file: "dimensions.json", group: "dimension" },
  { file: "typography.json", group: "typography" },
];

const componentTokens = {
  "--cf-component-action-primary-bg": "var(--cf-color-neutral-950)",
  "--cf-component-action-primary-bg-hover": "var(--cf-color-neutral-900)",
  "--cf-component-action-primary-bg-pressed": "var(--cf-color-neutral-700)",
  "--cf-component-action-primary-text": "var(--cf-color-text-inverse)",
  "--cf-component-focus-ring": "var(--cf-color-neutral-950)",
  "--cf-component-selected-bg": "var(--cf-color-neutral-100)",
  "--cf-component-selected-indicator": "var(--cf-color-neutral-950)",
  "--cf-component-button-radius": "var(--cf-radius-md)",
  "--cf-component-button-height": "var(--cf-size-touch-target)",
  "--cf-component-button-padding-inline": "var(--cf-spacing-md)",
  "--cf-component-button-gap": "var(--cf-spacing-xs)",
  "--cf-component-badge-success-bg": "var(--cf-color-bg-success)",
  "--cf-component-badge-success-text": "var(--cf-color-text-success)",
  "--cf-component-badge-success-icon": "var(--cf-color-icon-success)",
  "--cf-component-badge-radius": "var(--cf-radius-full)",
  "--cf-component-badge-padding-block": "var(--cf-spacing-2xs)",
  "--cf-component-badge-padding-inline": "var(--cf-spacing-xs)",
};

function walkTokens(node, path = [], result = []) {
  if (!node || typeof node !== "object") return result;
  if ("$type" in node && "$value" in node) {
    result.push({ path: path.join("/"), token: node });
    return result;
  }
  for (const [key, value] of Object.entries(node)) {
    if (!key.startsWith("$")) walkTokens(value, [...path, key], result);
  }
  return result;
}

function cssVariableName(entry) {
  const syntax = entry.token.$extensions?.["com.figma.codeSyntax"]?.WEB;
  const match = syntax?.match(/^var\((--[^)]+)\)$/);
  if (match) return match[1];
  if (entry.group === "typography" && entry.path.startsWith("weight/")) {
    return `--cf-font-weight-${entry.path.split("/").at(-1)}`;
  }
  throw new Error(`Missing or invalid WEB Code Syntax: ${entry.file}#${entry.path}`);
}

function rawValue(entry) {
  const value = entry.token.$value;
  if (entry.token.$type === "color") return value.hex;
  if (entry.token.$type === "string") {
    if (entry.path === "family/sans") {
      return '"Microsoft YaHei", "PingFang SC", sans-serif';
    }
    return JSON.stringify(value);
  }
  if (entry.token.$type === "number") {
    return entry.path.startsWith("weight/") ? String(value) : `${value}px`;
  }
  throw new Error(`Unsupported token type: ${entry.token.$type}`);
}

function tsKey(path) {
  return path.replace(/[^a-zA-Z0-9]+(.)/g, (_, next) => next.toUpperCase());
}

function tsTokenPath(token) {
  if (token.group === "colorPrimitive") return `primitive/color/${token.designPath}`;
  if (token.group === "colorSemantic") return token.designPath;
  if (token.group === "dimension") return token.designPath;
  if (token.group === "typography") {
    return token.designPath.startsWith("font-size/") ? token.designPath : `font/${token.designPath}`;
  }
  throw new Error(`Unknown token group: ${token.group}`);
}

const entries = [];
for (const source of sources) {
  const json = JSON.parse(await readFile(join(foundationsDir, source.file), "utf8"));
  for (const entry of walkTokens(json)) {
    entries.push({ ...entry, ...source });
  }
}

const primitiveCssByPath = new Map(
  entries
    .filter((entry) => entry.group === "colorPrimitive")
    .map((entry) => [entry.path, cssVariableName(entry)]),
);

const manifest = entries.map((entry) => {
  const extensions = entry.token.$extensions ?? {};
  const alias = extensions["com.figma.aliasData"]?.targetVariableName ?? null;
  const cssValue = alias
    ? `var(${primitiveCssByPath.get(alias) ?? (() => { throw new Error(`Unknown alias: ${alias}`); })()})`
    : rawValue(entry);
  const figmaValue = entry.token.$value?.hex ?? entry.token.$value;
  return {
    sourceFile: entry.file,
    group: entry.group,
    designPath: entry.path,
    cssVariable: cssVariableName(entry),
    cssValue,
    figmaValue,
    alias,
    figmaVariableId: extensions["com.figma.variableId"] ?? null,
    scopes: extensions["com.figma.scopes"] ?? [],
  };
});

const sections = [
  ["Color primitives", manifest.filter((token) => token.group === "colorPrimitive")],
  ["Semantic colors", manifest.filter((token) => token.group === "colorSemantic")],
  ["Dimensions", manifest.filter((token) => token.group === "dimension")],
  ["Typography", manifest.filter((token) => token.group === "typography")],
];

const cssLines = [
  "/* GENERATED FILE — edit Figma exports or generate-tokens.mjs, then regenerate. */",
  ":root {",
];
for (const [title, tokens] of sections) {
  cssLines.push(`  /* ${title} */`);
  for (const token of tokens) cssLines.push(`  ${token.cssVariable}: ${token.cssValue};`);
  cssLines.push("");
}
cssLines.push("  /* Project component contracts — approved by US-S1-UI-02. */");
for (const [name, value] of Object.entries(componentTokens)) {
  cssLines.push(`  ${name}: ${value};`);
}
cssLines.push("}", "");

const tokenRefs = Object.fromEntries(
  manifest.map((token) => [tsKey(tsTokenPath(token)), `var(${token.cssVariable})`]),
);
const componentRefs = Object.fromEntries(
  Object.keys(componentTokens).map((name) => [tsKey(name.replace(/^--cf-component-/, "")), `var(${name})`]),
);

const tsLines = [
  "// GENERATED FILE — edit Figma exports or generate-tokens.mjs, then regenerate.",
  "export const cupFlowToken = " + JSON.stringify(tokenRefs, null, 2) + " as const;",
  "",
  "export const cupFlowComponentToken = " + JSON.stringify(componentRefs, null, 2) + " as const;",
  "",
  "export type CupFlowTokenName = keyof typeof cupFlowToken;",
  "export type CupFlowComponentTokenName = keyof typeof cupFlowComponentToken;",
  "",
];

await Promise.all([
  writeFile(join(currentDir, "tokens.css"), cssLines.join("\n")),
  writeFile(join(currentDir, "tokens.ts"), tsLines.join("\n")),
  writeFile(join(currentDir, "token-manifest.json"), `${JSON.stringify({ generatedFrom: sources.map((source) => source.file), tokenCount: manifest.length, tokens: manifest, componentTokens }, null, 2)}\n`),
]);

console.log(JSON.stringify({ generated: ["tokens.css", "tokens.ts", "token-manifest.json"], sourceTokenCount: manifest.length, componentTokenCount: Object.keys(componentTokens).length }, null, 2));
