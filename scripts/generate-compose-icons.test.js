const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { test } = require('node:test');

test('regenerating icons keeps the aspect-ratio correction in the shared builder', () => {
  const fixtureRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'sf-symbols-sizing-'));
  try {
    const scriptsDir = path.join(fixtureRoot, 'scripts');
    fs.mkdirSync(scriptsDir);
    fs.copyFileSync(path.join(__dirname, 'generate-compose-icons.js'), path.join(scriptsDir, 'generate-compose-icons.js'));
    for (const mode of ['monochrome', 'dualtone']) {
      const sourceDir = path.join(fixtureRoot, 'repo_source', 'src', mode, 'icons');
      fs.mkdirSync(sourceDir, { recursive: true });
      fs.writeFileSync(path.join(sourceDir, 'SFTestRectangle.tsx'),
        `const VIEW_BOX = '0 0 40 10';\nconst SVG_CONTENT = '<path d="M0 0L40 0L40 10L0 10Z"/>';\n`);
    }
    execFileSync(process.execPath, [path.join(scriptsDir, 'generate-compose-icons.js')]);

    const relativeBuilder = path.join('sfsymbols', 'src', 'main', 'java', 'com', 'composables', 'sfsymbols', 'SfIconBuilder.kt');
    const normalize = file => fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n').trim();
    assert.equal(normalize(path.join(fixtureRoot, relativeBuilder)), normalize(path.join(__dirname, '..', relativeBuilder)),
      'The generator must preserve the library builder instead of restoring square dimensions.');
  } finally {
    assert.equal(path.dirname(path.resolve(fixtureRoot)), path.resolve(os.tmpdir()));
    fs.rmSync(fixtureRoot, { recursive: true, force: true });
  }
});
