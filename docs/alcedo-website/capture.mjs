import puppeteer from 'puppeteer';
import fs from 'fs';
import path from 'path';

const browser = await puppeteer.launch({ headless: 'new' });
const page = await browser.newPage();
await page.setViewport({ width: 1440, height: 900, deviceScaleFactor: 1 });
await page.goto('http://localhost:5173', { waitUntil: 'networkidle2', timeout: 30000 });
await new Promise(r => setTimeout(r, 2500));

const outDir = './screenshots-preview';
if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

async function shot(name) {
  await new Promise(r => setTimeout(r, 700));
  await page.screenshot({ path: path.join(outDir, `${name}.png`), clip: { x: 0, y: 0, width: 1440, height: 900 } });
  console.log('Captured', name);
}

await shot('01-hero');

// Scroll to each section anchor and capture
for (const [name, sel] of [
  ['02-ai', '#ai'],
  ['03-features', '#features'],
  ['04-films', '#films'],
  ['05-cta', '#download'],
]) {
  await page.evaluate((s) => {
    const el = document.querySelector(s);
    if (el) el.scrollIntoView({ block: 'start' });
  }, sel);
  await shot(name);
}

await browser.close();
console.log('Done');
