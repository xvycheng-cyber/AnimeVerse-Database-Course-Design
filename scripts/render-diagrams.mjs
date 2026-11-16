import fs from 'node:fs/promises';
import { existsSync } from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(scriptDir, '..');
const diagramDir = path.join(repoRoot, 'docs', 'diagrams');
const require = createRequire(import.meta.url);

let playwright;
try {
    playwright = require('playwright');
} catch {
    const modulePath = process.env.PLAYWRIGHT_MODULE;
    if (!modulePath) {
        throw new Error(
            'Playwright is not resolvable. Set PLAYWRIGHT_MODULE to the playwright package directory.'
        );
    }
    playwright = require(modulePath);
}

const diagrams = [
    '01-global-er',
    '02-production-er',
    '03-user-interaction-er'
];

const browserCandidates = [
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE,
    playwright.chromium.executablePath(),
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe'
].filter(Boolean);
const executablePath = browserCandidates.find((candidate) => existsSync(candidate));
if (!executablePath) {
    throw new Error('No Chromium-based browser executable was found.');
}
const browser = await playwright.chromium.launch({
    headless: true,
    executablePath
});
try {
    for (const name of diagrams) {
        const sourcePath = path.join(diagramDir, `${name}.mmd`);
        const svgPath = path.join(diagramDir, `${name}.svg`);
        const pngPath = path.join(diagramDir, `${name}.png`);
        const source = await fs.readFile(sourcePath, 'utf8');

        const page = await browser.newPage({
            viewport: { width: 1800, height: 1200 },
            deviceScaleFactor: 2
        });
        await page.setContent(
            '<!doctype html><html><head><meta charset="utf-8"></head><body></body></html>',
            { waitUntil: 'domcontentloaded' }
        );
        await page.addScriptTag({
            url: 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js'
        });
        await page.waitForFunction(() => Boolean(window.mermaid));

        const rendered = await page.evaluate(async (mmdSource) => {
            window.mermaid.initialize({
                startOnLoad: false,
                theme: 'base',
                securityLevel: 'loose',
                er: {
                    useMaxWidth: false,
                    diagramPadding: 24
                },
                themeVariables: {
                    background: '#ffffff',
                    primaryColor: '#eef6ff',
                    primaryBorderColor: '#2563eb',
                    primaryTextColor: '#172554',
                    lineColor: '#475569',
                    secondaryColor: '#f8fafc',
                    tertiaryColor: '#f1f5f9',
                    fontFamily: 'Segoe UI, Microsoft YaHei, sans-serif',
                    fontSize: '15px'
                }
            });
            const result = await window.mermaid.render(`diagram-${Date.now()}`, mmdSource);
            document.body.style.margin = '0';
            document.body.style.padding = '24px';
            document.body.style.background = '#ffffff';
            document.body.innerHTML = result.svg;
            const svg = document.querySelector('svg');
            svg.style.maxWidth = 'none';
            svg.style.height = 'auto';
            svg.style.background = '#ffffff';
            return {
                svg: svg.outerHTML,
                width: Math.ceil(svg.getBoundingClientRect().width),
                height: Math.ceil(svg.getBoundingClientRect().height)
            };
        }, source);

        await fs.writeFile(svgPath, `${rendered.svg}\n`, 'utf8');
        const svgElement = await page.$('svg');
        if (!svgElement) {
            throw new Error(`Mermaid did not produce an SVG for ${name}.`);
        }
        await svgElement.screenshot({ path: pngPath });
        console.log(`${name}: ${rendered.width}x${rendered.height}, SVG/PNG written`);
        await page.close();
    }
} finally {
    await browser.close();
}
