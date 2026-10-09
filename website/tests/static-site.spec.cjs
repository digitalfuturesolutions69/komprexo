const { test, expect } = require('@playwright/test');
const languages = ['en', 'id', 'es', 'pt-BR', 'hi'];
const routes = ['', 'privacy/', 'terms/', 'premium/', 'support/', 'about/'];
for (const language of languages) {
  for (const route of routes) {
    test(language + '/' + route + ' renders and preserves legal/language navigation', async ({ page }) => {
      const response = await page.goto('/' + language + '/' + route);
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
      expect(new URL(page.url()).pathname).toBe('/hi/' + route);
    });
  }
  for (const viewport of [{ width: 320, height: 569 }, { width: 569, height: 320 }]) {
    test(language + ' responsive 200% ' + viewport.width, async ({ page }) => {
      await page.setViewportSize(viewport);
      for (const route of ['', 'premium/', 'privacy/']) {
        await page.goto('/' + language + '/' + route);
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
  test(language + ' dark theme and keyboard navigation', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.goto('/' + language + '/');
    await page.keyboard.press('Tab');
    await expect(page.locator('.skip')).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(page.locator('main')).toBeInViewport();
    const colors = await page.locator('body').evaluate(e => {
      const s = getComputedStyle(e); return [s.color, getComputedStyle(document.documentElement).backgroundColor];
    });
    expect(colors[0]).not.toBe(colors[1]);
    await page.screenshot({ path: 'test-results/' + language + '-dark.png', fullPage: true });
  });
}
