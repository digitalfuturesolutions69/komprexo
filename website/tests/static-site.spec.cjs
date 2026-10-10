const { test, expect } = require('@playwright/test');
const languages = ['en', 'id', 'es', 'pt-BR', 'hi'];
const routes = ['', 'privacy/', 'terms/', 'premium/', 'support/', 'about/'];
for (const language of languages) {
  test(language + ' adult audience visible on Home and all legal pages', async ({ page }) => {
    const legal = JSON.parse(require('node:fs').readFileSync(require('node:path').join(__dirname, '../../content/legal', language + '.json'), 'utf8'));
    await page.goto(language + '/');
    await expect(page.locator('main .audience p')).toHaveText(legal.about.at(-1)[1]);
    for (const route of ['privacy', 'terms', 'premium', 'support', 'about']) {
      await page.goto(language + '/' + route + '/');
      await expect(page.locator('main section').last()).toContainText(legal[route].at(-1)[1]);
      await expect(page.locator('main section').last()).toContainText('18');
      expect(await page.locator('form, input[type="date"]').count()).toBe(0);
    }
  });
  test(language + ' confirmed identity, draft gates and project-path assets', async ({ page }, testInfo) => {
    const base = new URL(testInfo.project.use.baseURL);
    for (const route of ['privacy', 'terms', 'premium', 'support', 'about']) {
      await page.goto(language + '/' + route + '/');
      await expect(page.locator('main')).toContainText('Digital Future Solutions');
      await expect(page.locator('main')).toContainText('Google Play Console');
      await expect(page.locator('main')).toContainText('Personal');
      await expect(page.locator('.draft')).toContainText('OWNER ACTION REQUIRED');
      await expect(page.locator('footer')).toContainText('Digital Future Solutions');
      for (const href of await page.locator('link[href], img[src], header a, footer a').evaluateAll(nodes =>
        nodes.map(n => n.href || n.src).filter(url => url.startsWith(location.origin)))) {
        expect(new URL(href).pathname.startsWith(base.pathname)).toBe(true);
        expect((await page.request.get(href)).status()).toBe(200);
      }
    }
    // The English root alias must work under both hosting layouts too.
    await page.goto('privacy/');
    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    await page.locator('.languages a[hreflang="id"]').click();
    expect(new URL(page.url()).pathname).toBe(base.pathname + 'id/privacy/');
  });
  for (const route of routes) {
    test(language + '/' + route + ' renders and preserves legal/language navigation', async ({ page }, testInfo) => {
      const response = await page.goto(language + '/' + route);
      expect(response.status()).toBe(200);
      await expect(page.locator('html')).toHaveAttribute('lang', language);
      await expect(page.locator('main h1')).toBeVisible();
      await expect(page.locator('main h1')).toHaveCount(1);
      await expect(page.locator('.languages a')).toHaveCount(5);
      await expect(page.locator('footer a[href$="/privacy/"]')).toHaveCount(1);
      await expect(page.locator('footer a[href$="/terms/"]')).toHaveCount(1);
      await expect(page.locator('a[href="mailto:komprexo.support@gmail.com"]').first()).toBeVisible();
      expect(await page.locator('form, script[src], iframe').count()).toBe(0);
      await page.locator('.languages a[hreflang="hi"]').click();
      await expect(page.locator('html')).toHaveAttribute('lang', 'hi');
      expect(new URL(page.url()).pathname).toBe(new URL(testInfo.project.use.baseURL).pathname + 'hi/' + route);
    });
  }
  for (const viewport of [{ width: 320, height: 569 }, { width: 569, height: 320 }]) {
    test(language + ' responsive 200% ' + viewport.width, async ({ page }) => {
      await page.setViewportSize(viewport);
      for (const route of ['', 'premium/', 'privacy/']) {
        await page.goto(language + '/' + route);
        await page.addStyleTag({ content: 'html { font-size: 200%; }' });
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
        const boxes = await page.locator('header nav a, .languages a').evaluateAll(elements =>
          elements.map(e => { const b = e.getBoundingClientRect(); return { x: b.x, y: b.y, w: b.width, h: b.height }; }));
        for (let i = 0; i < boxes.length; i++) {
          const a = boxes[i];
          expect(a.h).toBeGreaterThanOrEqual(48);
          expect(a.x).toBeGreaterThanOrEqual(0);
          expect(a.x + a.w).toBeLessThanOrEqual(viewport.width + 1);
          for (let j = i + 1; j < boxes.length; j++) {
            const b = boxes[j];
            const overlap = Math.min(a.x + a.w, b.x + b.w) - Math.max(a.x, b.x) > 1 &&
              Math.min(a.y + a.h, b.y + b.h) - Math.max(a.y, b.y) > 1;
            expect(overlap).toBe(false);
          }
        }
      }
    });
  }
  test(language + ' dark theme and keyboard navigation', async ({ page }, testInfo) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.goto(language + '/');
    await page.keyboard.press('Tab');
    await expect(page.locator('.skip')).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(page.locator('main')).toBeInViewport();
    const colors = await page.locator('body').evaluate(e => {
      const s = getComputedStyle(e); return [s.color, getComputedStyle(document.documentElement).backgroundColor];
    });
    expect(colors[0]).not.toBe(colors[1]);
    await page.screenshot({ path: testInfo.outputPath(language + '-dark.png'), fullPage: true });
  });
}

// A policy shown online must have the same substantive paragraphs as the offline source.
const fs = require('node:fs');
const path = require('node:path');
for (const language of languages) {
  test(language + ' legal draft paragraphs match offline policy source', async ({ page }) => {
    const legal = JSON.parse(fs.readFileSync(path.join(__dirname, '../../content/legal', language + '.json'), 'utf8'));
    for (const route of ['privacy', 'terms', 'premium', 'support', 'about']) {
      await page.goto(language + '/' + route + '/');
      await expect(page.locator('main .draft')).toHaveText(legal.draft);
      expect(await page.locator('main section h2').allTextContents()).toEqual(legal[route].map(s => s[0]));
      expect(await page.locator('main section p').allTextContents()).toEqual(legal[route].map(s => s[1]));
    }
  });
}

// Hosting disclosures distinguish planned provider from unverified practices.
for (const language of languages) {
  test(language + ' planned hosting disclosure and privacy link', async ({ page }) => {
    await page.goto(language + '/privacy/');
    await expect(page.locator('main')).toContainText('Rumahweb');
    await expect(page.locator('main')).toContainText('IP');
    await expect(page.locator('main')).not.toContainText('GitHub Pages');
    await expect(page.locator('main .draft')).toContainText('OWNER ACTION REQUIRED');
  });
}

// A static policy visit must not quietly add third-party collection requests.
for (const language of languages) {
  test(language + ' all static pages request only project-local resources', async ({ page }, testInfo) => {
    const base = new URL(testInfo.project.use.baseURL);
    const unexpected = [];
    page.on('request', request => {
      const url = new URL(request.url());
      if (url.origin !== base.origin || !url.pathname.startsWith(base.pathname)) unexpected.push(request.url());
    });
    for (const route of ['', 'privacy/', 'terms/', 'premium/', 'support/', 'about/']) {
      await page.goto(language + '/' + route, { waitUntil: 'networkidle' });
      expect(await page.locator('script, iframe, form').count()).toBe(0);
    }
    expect(unexpected).toEqual([]);
  });
}

// The custom host publishes the English aliases at its root. Relative links
// must also retain the historical GitHub Pages project prefix.
test('Rumahweb root aliases exclude Pages configuration', async ({ page, request }, testInfo) => {
  const base = new URL(testInfo.project.use.baseURL);
  const cname = await request.get(new URL('CNAME', base).href);
  expect(cname.status()).toBe(404);
  for (const route of ['', 'privacy/', 'terms/', 'premium/', 'support/', 'about/']) {
    const response = await page.goto(route);
    expect(response.ok()).toBeTruthy();
    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    await expect(page.locator('footer a[href="mailto:komprexo.support@gmail.com"]')).toBeVisible();
    expect(await page.locator('a[href="mailto:support@komprexo.digitalfuturesolutions.my.id"]').count()).toBe(0);
    if (route) await expect(page.locator('main .draft')).toContainText('OWNER ACTION REQUIRED');
    for (const selector of ['link[rel="stylesheet"]', 'link[rel="icon"]']) {
      const url = new URL(await page.locator(selector).getAttribute('href'), page.url());
      expect(url.pathname.startsWith(base.pathname)).toBeTruthy();
      expect((await request.get(url.href)).ok()).toBeTruthy();
    }
    for (const link of await page.locator('header a, nav.languages a, footer a').all()) {
      const href = await link.getAttribute('href');
      if (href.startsWith('mailto:')) continue;
      const url = new URL(href, page.url());
      expect(url.origin).toBe(base.origin);
      expect(url.pathname.startsWith(base.pathname)).toBeTruthy();
      expect((await request.get(url.href)).ok()).toBeTruthy();
    }
  }
});
