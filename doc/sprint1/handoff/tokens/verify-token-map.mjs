import { readFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const currentDir = dirname(fileURLToPath(import.meta.url));
const foundationsDir = resolve(currentDir, "../../foundations");
const sourceFiles = ["color-primitives.json", "color-semantics.json", "dimensions.json", "typography.json"];

function countTokens(node) {
  if (!node || typeof node !== "object") return 0;
  if ("$type" in node && "$value" in node) return 1;
  return Object.entries(node)
    .filter(([key]) => !key.startsWith("$"))
    .reduce((count, [, value]) => count + countTokens(value), 0);
}

let sourceTokenCount = 0;
for (const file of sourceFiles) {
  sourceTokenCount += countTokens(JSON.parse(await readFile(join(foundationsDir, file), "utf8")));
}

const manifest = JSON.parse(await readFile(join(currentDir, "token-manifest.json"), "utf8"));
const css = await readFile(join(currentDir, "tokens.css"), "utf8");
const examples = await readFile(join(currentDir, "examples.css"), "utf8");

const failures = [];
if (manifest.tokenCount !== sourceTokenCount) {
  failures.push(`Manifest contains ${manifest.tokenCount} tokens; sources contain ${sourceTokenCount}.`);
}

const cssVariables = manifest.tokens.map((token) => token.cssVariable);
const duplicateVariables = cssVariables.filter((name, index) => cssVariables.indexOf(name) !== index);
if (duplicateVariables.length) failures.push(`Duplicate CSS variables: ${[...new Set(duplicateVariables)].join(", ")}`);

for (const name of [...cssVariables, ...Object.keys(manifest.componentTokens)]) {
  if (!css.includes(`${name}:`)) failures.push(`Missing CSS declaration: ${name}`);
}

const declared = new Set([...cssVariables, ...Object.keys(manifest.componentTokens)]);
const referenced = [...`${css}\n${examples}`.matchAll(/var\((--[^),\s]+)/g)].map((match) => match[1]);
for (const name of referenced) {
  if (!declared.has(name)) failures.push(`Unknown CSS variable reference: ${name}`);
}

const expectedContracts = {
  "--cf-component-action-primary-bg": "var(--cf-color-neutral-950)",
  "--cf-component-focus-ring": "var(--cf-color-neutral-950)",
  "--cf-component-selected-bg": "var(--cf-color-neutral-100)",
  "--cf-component-selected-indicator": "var(--cf-color-neutral-950)",
  "--cf-component-badge-success-bg": "var(--cf-color-bg-success)",
  "--cf-component-badge-success-text": "var(--cf-color-text-success)",
};
for (const [name, value] of Object.entries(expectedContracts)) {
  if (manifest.componentTokens[name] !== value) failures.push(`Unexpected component contract: ${name}`);
}

const result = {
  status: failures.length ? "failed" : "passed",
  sourceTokenCount,
  mappedTokenCount: manifest.tokenCount,
  componentTokenCount: Object.keys(manifest.componentTokens).length,
  checkedExamples: ["Button/Primary", "Badge/Success"],
  failures,
};
console.log(JSON.stringify(result, null, 2));
if (failures.length) process.exitCode = 1;
