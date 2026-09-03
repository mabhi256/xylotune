// @ts-check
const { test, expect } = require('@playwright/test');
const path = require('path');

const APP_URL = 'file://' + path.resolve(__dirname, '..', 'xylotune.html').replace(/\\/g, '/');

test.beforeEach(async ({ page }) => {
  await page.goto(APP_URL);
  await page.waitForSelector('.bar');
});

// Real pixel drag-select of text[start:end) inside `locator`'s first text-bearing
// element, so link tests exercise the same DOM Range + mouseup path a user does -
// synthetic Selection injection would miss real pointerdown/mouseup wiring bugs.
async function dragSelectText(page, locator, start, end) {
  const box = await locator.evaluateHandle((el, [s, e]) => {
    const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
    let total = 0, node, startNode, startOffset, endNode, endOffset;
    while ((node = walker.nextNode())) {
      const len = node.textContent.length;
      if (startNode === undefined && s <= total + len) { startNode = node; startOffset = s - total; }
      if (endNode === undefined && e <= total + len) { endNode = node; endOffset = e - total; }
      total += len;
    }
    const range = document.createRange();
    range.setStart(startNode, startOffset);
    range.setEnd(startNode, startOffset);
    const startRect = range.getClientRects()[0] || range.getBoundingClientRect();
    range.setStart(endNode, endOffset);
    range.setEnd(endNode, endOffset);
    const endRect = range.getClientRects()[0] || range.getBoundingClientRect();
    return { sx: startRect.left, sy: (startRect.top + startRect.bottom) / 2, ex: endRect.left, ey: (endRect.top + endRect.bottom) / 2 };
  }, [start, end]);
  const { sx, sy, ex, ey } = await box.jsonValue();
  await page.mouse.move(sx, sy);
  await page.mouse.down();
  await page.mouse.move((sx + ex) / 2, (sy + ey) / 2);
  await page.mouse.move(ex, ey);
  await page.mouse.up();
}

test('loads with no console errors and no literal HTML-entity text', async ({ page }) => {
  const errors = [];
  page.on('console', (msg) => { if (msg.type() === 'error') errors.push(msg.text()); });
  page.on('pageerror', (err) => errors.push(String(err)));
  await page.goto(APP_URL);
  await page.waitForSelector('.bar');
  expect(errors).toEqual([]);
  // htm (unlike JSX) does not decode named entities in text nodes - a regression
  // here means a literal "&nbsp;" etc. would render as visible text.
  await expect(page.locator('#root')).not.toContainText('&nbsp;');
});

test('help dialog: opens from the info button, lists shortcuts, and closes', async ({ page }) => {
  const dialog = page.locator('dialog');
  await expect(dialog).not.toBeVisible();
  await page.getByRole('button', { name: 'Help and keyboard shortcuts' }).click();
  await expect(dialog).toBeVisible();
  await expect(dialog.locator('dt')).not.toHaveCount(0);
  await dialog.getByRole('button', { name: 'Close' }).click();
  await expect(dialog).not.toBeVisible();
});

test('delete button sits outside the play-buttons row, disabled with no song loaded', async ({ page }) => {
  const del = page.getByRole('button', { name: 'Delete the loaded song' });
  await expect(del).toBeDisabled();
});

test('pad scrolls internally without growing the page (viewport-fixed layout)', async ({ page }) => {
  for (let row = 0; row < 15; row++) {
    for (let i = 0; i < 8; i++) await page.locator('.bar').nth(i).click();
    await page.keyboard.press('Enter');
  }
  const pageScrollHeight = await page.evaluate(() => document.documentElement.scrollHeight);
  const viewportHeight = await page.evaluate(() => window.innerHeight);
  expect(pageScrollHeight).toBeLessThanOrEqual(viewportHeight + 1);
});

test('Tailwind utility classes actually apply (class, not className, still styles via htm+React)', async ({ page }) => {
  const bar = page.locator('.bar').first();
  const bg = await bar.evaluate((el) => getComputedStyle(el).backgroundColor);
  expect(bg).toBe('rgb(214, 60, 48)'); // --c1, the C bar's red
  const radius = await bar.evaluate((el) => getComputedStyle(el).borderRadius);
  expect(radius).toBe('10px');
  const bodyBg = await page.evaluate(() => getComputedStyle(document.body).backgroundColor);
  expect(bodyBg).toBe('rgb(33, 35, 38)');
});

test('clicking bars writes note chips into the pad', async ({ page }) => {
  await page.locator('.bar').nth(0).click();
  await page.locator('.bar').nth(1).click();
  await page.locator('.bar').nth(2).click();
  await expect(page.locator('.chip')).toHaveCount(3);
});

test('Backspace shrinks the gap for 3 presses, then removes the chip on the 4th', async ({ page }) => {
  await page.locator('.bar').first().click();
  await expect(page.locator('.chip')).toHaveCount(1);
  for (let i = 0; i < 3; i++) {
    await page.keyboard.press('Backspace');
    await expect(page.locator('.chip')).toHaveCount(1);
  }
  await page.keyboard.press('Backspace');
  await expect(page.locator('.chip')).toHaveCount(0);
});

test('Enter starts a new line', async ({ page }) => {
  await expect(page.locator('.pad-line, [class*="border-b"]').first()).toBeVisible();
  const before = await page.evaluate(() => document.querySelectorAll('#root .flex.flex-wrap.gap-\\[5px\\]').length);
  await page.locator('.bar').first().click();
  await page.keyboard.press('Enter');
  const after = await page.evaluate(() => document.querySelectorAll('#root .flex.flex-wrap.gap-\\[5px\\]').length);
  expect(after).toBe(before + 1);
});

test('saving and loading a song round-trips its notes and lyric text', async ({ page }) => {
  await page.locator('.bar').nth(0).click();
  await page.locator('.bar').nth(1).click();
  await cycleLyricsTo(page, 'edit');
  await page.locator('.lyric').first().click();
  await page.keyboard.type('Hello there');

  await page.getByRole('button', { name: '💾 Save' }).click();
  const dialog = page.locator('dialog[open]');
  await dialog.locator('input[type="text"]').fill('Lyric Song');
  await dialog.getByRole('button', { name: 'OK' }).click();

  await page.getByRole('button', { name: '✕ Clear' }).click();
  await page.selectOption('select', { label: 'Lyric Song' });
  await expect(page.locator('.chip')).toHaveCount(2);
  await cycleLyricsTo(page, 'on');
  await expect(page.locator('.lyric').first()).toHaveText('Hello there');
});

test('sound toggle switches bar material', async ({ page }) => {
  await page.getByRole('button', { name: '🪵 Wood' }).click();
  await expect(page.locator('body')).toHaveAttribute('data-material', 'wood');
});

test('practice mode: lights the first bar, holds on a wrong strike, advances on the right one, and speeds up after a clean pass', async ({ page }) => {
  const practiceBtn = page.locator('[data-testid="practice-toggle"]');
  await expect(practiceBtn).toBeDisabled(); // nothing to practice yet

  await page.locator('.bar').nth(0).click(); // note 0 (C)
  await page.locator('.bar').nth(1).click(); // note 1 (D)
  await expect(page.locator('.chip')).toHaveCount(2);

  await practiceBtn.click();
  await expect(practiceBtn).toHaveText('⏹ Stop practice');
  await expect(page.locator('.bar.target')).toHaveCount(1);
  await expect(page.locator('.bar').nth(0)).toHaveClass(/target/);
  await expect(page.locator('[data-testid="practice-status"]')).toHaveText('Note 1 of 2 · Speed 100%');

  // editing controls are hidden/disabled while practicing
  await expect(page.getByRole('button', { name: '✕ Clear' })).toBeDisabled();

  // the wrong bar plays but doesn't advance the target
  await page.locator('.bar').nth(1).click();
  await expect(page.locator('[data-testid="practice-status"]')).toHaveText('Note 1 of 2 · Speed 100%');
  await expect(page.locator('.bar').nth(0)).toHaveClass(/target/);

  // the lit bar advances to the next note
  await page.locator('.bar').nth(0).click();
  await expect(page.locator('[data-testid="practice-status"]')).toHaveText('Note 2 of 2 · Speed 100%');
  await expect(page.locator('.bar').nth(1)).toHaveClass(/target/);

  // this pass had a mistake, so finishing it wraps back to the start at the same speed
  await page.locator('.bar').nth(1).click();
  await expect(page.locator('[data-testid="practice-status"]')).toHaveText('Note 1 of 2 · Speed 100%');
  await expect(page.locator('.bar').nth(0)).toHaveClass(/target/);

  // a fully clean pass speeds the next one up
  await page.locator('.bar').nth(0).click();
  await page.locator('.bar').nth(1).click();
  await expect(page.locator('[data-testid="practice-status"]')).toHaveText('Note 1 of 2 · Speed 115%');

  await practiceBtn.click();
  await expect(practiceBtn).toHaveText('🎯 Practice');
  await expect(page.locator('.bar.target')).toHaveCount(0);
  await expect(page.locator('[data-testid="practice-status"]')).toHaveCount(0);
  await expect(page.getByRole('button', { name: '✕ Clear' })).toBeEnabled();
});

test('practice mode: Escape stops practice, same as the button', async ({ page }) => {
  await page.locator('.bar').nth(0).click();
  await page.locator('[data-testid="practice-toggle"]').click();
  await expect(page.locator('.bar.target')).toHaveCount(1);
  await page.keyboard.press('Escape');
  await expect(page.locator('[data-testid="practice-toggle"]')).toHaveText('🎯 Practice');
  await expect(page.locator('.bar.target')).toHaveCount(0);
});

// Regression: adding/removing a dot used to nudge the note's raw sec by +/-0.1 from
// whatever value was already there, so a note with off-grid recorded timing (e.g. 0.37s)
// stayed off-grid forever - the dot count shown always rounds, so this drift was invisible
// in the UI and only showed up as the pad sounding different from what the dots implied.
// Dot edits must instead snap to the exact value their new dot count implies.
test('adding/removing a dot snaps to the grid instead of nudging raw recorded timing (regression)', async ({ page }) => {
  await page.locator('.bar').nth(0).click(); // cursor now sits right after this note
  await page.evaluate(() => window.__xyloTest.pokeSec(0, 0, 0.37)); // off-grid, as if live-recorded

  await page.keyboard.press('Space'); // "add a dot": 0.37s rounds to 4 dots shown, so this should land on 5 dots = 0.5s exactly
  expect(await page.evaluate(() => window.__xyloTest.getSec(0, 0))).toBeCloseTo(0.5, 6);

  await page.keyboard.press('Backspace'); // shrink back a dot: should land on exactly 0.4s, not 0.4-ish
  expect(await page.evaluate(() => window.__xyloTest.getSec(0, 0))).toBeCloseTo(0.4, 6);
});

test('playing notes live one after another captures the real gap between them, and a long pause starts a new line', async ({ page }) => {
  await page.locator('.bar').nth(0).click();
  await page.waitForTimeout(1700); // past PHRASE_BREAK_SEC (1.2s), with generous buffer for timer jitter under load
  const before = await page.evaluate(() => document.querySelectorAll('#root .flex.flex-wrap.gap-\\[5px\\]').length);
  await page.locator('.bar').nth(1).click();
  const after = await page.evaluate(() => document.querySelectorAll('#root .flex.flex-wrap.gap-\\[5px\\]').length);
  expect(after).toBe(before + 1);
});

test('playback highlights notes in sequence and returns to Play when done', async ({ page }) => {
  await page.locator('.bar').nth(0).click();
  await page.locator('.bar').nth(1).click();
  await page.locator('.bar').nth(2).click();
  const playBtn = page.getByRole('button', { name: /Play|Pause|Resume/ });
  await playBtn.click();
  await expect(page.locator('.chip.playing')).toHaveCount(1, { timeout: 3000 });
  await expect(playBtn).toHaveText('▶ Play', { timeout: 10000 });
  await expect(page.locator('.chip.playing')).toHaveCount(0);
});

// Lyrics cycles through 5 states: off -> on -> grouped -> link -> edit -> off.
// Reads the button's actual current state (via aria-label) so repeated calls compose correctly
// regardless of where the cycle currently sits.
async function cycleLyricsTo(page, target) {
  const order = ['off', 'on', 'grouped', 'link', 'edit'];
  const lyricsBtn = page.locator('[data-testid="lyrics-toggle"]');
  const current = (await lyricsBtn.getAttribute('aria-label')).replace('Lyrics mode: ', '');
  const steps = (order.indexOf(target) - order.indexOf(current) + order.length) % order.length;
  for (let i = 0; i < steps; i++) await lyricsBtn.click();
  return lyricsBtn;
}

test('lyric linking: arming a note and drag-selecting a word links them, and it highlights on playback', async ({ page }) => {
  await page.locator('.bar').nth(0).click();
  await page.locator('.bar').nth(1).click();
  await page.locator('.bar').nth(2).click();
  await cycleLyricsTo(page, 'edit');
  const caption = page.locator('.lyric').first();
  await caption.click();
  await page.keyboard.type('Hello world today');

  const lyricsBtn = await cycleLyricsTo(page, 'link');
  await expect(lyricsBtn).toHaveText('🔗 Link words');

  await page.locator('.chip').first().click(); // arm note index 0
  await expect(page.locator('.chip').first()).toHaveClass(/lyric-selecting/);

  await dragSelectText(page, caption, 0, 5); // "Hello"
  const label = page.locator('[data-tag-note="0"]');
  await expect(label).toHaveText('Hello');
  await expect(page.locator('.chip').first()).toHaveClass(/lyric-linked/);
  // the flowing caption still shows the full, unmodified sentence - linking only adds the
  // positioned label under the note, it never removes the word from the line
  await expect(caption).toHaveText('Hello world today');

  // clicking the positioned label un-links it
  await label.click();
  await expect(page.locator('[data-tag-note]')).toHaveCount(0);

  // re-link, then confirm it highlights in sync with playback
  await page.locator('.chip').first().click();
  await dragSelectText(page, caption, 0, 5);
  await page.getByRole('button', { name: /Play|Pause|Resume/ }).click();
  await expect(page.locator('[data-tag-note].playing')).toHaveCount(1, { timeout: 3000 });
});

test('lyric edit mode: typing into an empty caption lands text, and link mode never allows typing', async ({ page }) => {
  const linkBtn = await cycleLyricsTo(page, 'link');
  const caption = page.locator('.lyric').first();
  await caption.click();
  await page.keyboard.type('should not appear');
  await expect(caption).toHaveText('');

  await linkBtn.click(); // link -> edit
  await expect(linkBtn).toHaveText('✏️ Edit text');
  await caption.click();
  await page.keyboard.type('hello world');
  await expect(caption).toHaveText('hello world');
});

test('lyric off state hides the caption entirely, even with existing lyric text', async ({ page }) => {
  await page.locator('.bar').nth(0).click();
  await cycleLyricsTo(page, 'edit');
  await page.locator('.lyric').first().click();
  await page.keyboard.type('Some words');
  await page.getByRole('button', { name: '💾 Save' }).click();
  const dialog = page.locator('dialog[open]');
  await dialog.locator('input[type="text"]').fill('Lyric Song 2');
  await dialog.getByRole('button', { name: 'OK' }).click();

  await cycleLyricsTo(page, 'off');
  await page.getByRole('button', { name: '✕ Clear' }).click();
  await page.selectOption('select', { label: 'Lyric Song 2' });
  // default state is off - nothing shown, even though this line has lyric text
  await expect(page.locator('.lyric')).toHaveCount(0);
  const lyricsBtn = await cycleLyricsTo(page, 'on');
  await expect(lyricsBtn).toHaveText('📖 Lyrics: On');
  await expect(page.locator('.lyric').first()).toBeVisible();
  // cycling the rest of the way around (off -> on -> grouped -> link -> edit -> off) hides it again
  await cycleLyricsTo(page, 'edit');
  await lyricsBtn.click(); // edit -> off
  await expect(page.locator('.lyric')).toHaveCount(0);
});

test('Clear resets the pad to one empty line', async ({ page }) => {
  await page.locator('.bar').nth(0).click();
  await page.locator('.bar').nth(1).click();
  await expect(page.locator('.chip')).not.toHaveCount(0);
  await page.getByRole('button', { name: '✕ Clear' }).click();
  await expect(page.locator('.chip')).toHaveCount(0);
});

// ---------- In-app dialogs (replace window.prompt/confirm/alert - group D) ----------
// Each test also fails the moment a native dialog fires, since that's exactly the
// regression this component replaces.

test('save song: names it via an in-app dialog, not window.prompt', async ({ page }) => {
  page.on('dialog', (d) => { throw new Error(`native dialog fired: ${d.type()} "${d.message()}"`); });

  await page.locator('.bar').first().click();
  await page.getByRole('button', { name: '💾 Save' }).click();

  const dialog = page.locator('dialog[open]');
  await expect(dialog).toBeVisible();
  await expect(dialog).toContainText('Song name:');
  await dialog.locator('input[type="text"]').fill('My Test Song');
  await dialog.getByRole('button', { name: 'OK' }).click();
  await expect(dialog).not.toBeVisible();

  await expect(page.locator('select')).toContainText('My Test Song');
});

test('save song: an empty or whitespace-only name is silently ignored', async ({ page }) => {
  await page.locator('.bar').first().click();
  await page.getByRole('button', { name: '💾 Save' }).click();
  const dialog = page.locator('dialog[open]');
  await dialog.locator('input[type="text"]').fill('   ');
  await dialog.getByRole('button', { name: 'OK' }).click();
  await expect(dialog).not.toBeVisible();
  await expect(page.locator('select')).not.toContainText('My Songs');
});

test('save song: Escape cancels the dialog and saves nothing', async ({ page }) => {
  await page.locator('.bar').first().click();
  await page.getByRole('button', { name: '💾 Save' }).click();
  const dialog = page.locator('dialog[open]');
  await dialog.locator('input[type="text"]').fill('Should Not Save');
  await page.keyboard.press('Escape');
  await expect(dialog).not.toBeVisible();
  await expect(page.locator('select')).not.toContainText('Should Not Save');
});

test('save song: reusing an existing name asks to overwrite via dialog, Cancel leaves it untouched', async ({ page }) => {
  await page.locator('.bar').nth(0).click();
  await page.getByRole('button', { name: '💾 Save' }).click();
  let dialog = page.locator('dialog[open]');
  await dialog.locator('input[type="text"]').fill('Song A');
  await dialog.getByRole('button', { name: 'OK' }).click();
  await expect(dialog).not.toBeVisible();

  // start a fresh, different tune so the save below targets an *other* existing song
  await page.getByRole('button', { name: '✕ Clear' }).click();
  await page.locator('.bar').nth(1).click();
  await page.getByRole('button', { name: '💾 Save' }).click();
  dialog = page.locator('dialog[open]');
  await dialog.locator('input[type="text"]').fill('Song A');
  await dialog.getByRole('button', { name: 'OK' }).click();

  dialog = page.locator('dialog[open]');
  await expect(dialog).toBeVisible();
  await expect(dialog).toContainText('"Song A" already exists. Overwrite it?');
  await dialog.getByRole('button', { name: 'Cancel' }).click();
  await expect(dialog).not.toBeVisible();
  await expect(page.locator('select option', { hasText: 'Song A' })).toHaveCount(1);

  // confirming the same overwrite prompt replaces it, still just one "Song A" entry
  await page.getByRole('button', { name: '💾 Save' }).click();
  dialog = page.locator('dialog[open]');
  await dialog.locator('input[type="text"]').fill('Song A');
  await dialog.getByRole('button', { name: 'OK' }).click();
  dialog = page.locator('dialog[open]');
  await dialog.getByRole('button', { name: 'Overwrite' }).click();
  await expect(dialog).not.toBeVisible();
  await expect(page.locator('select option', { hasText: 'Song A' })).toHaveCount(1);
});

test('delete song: confirms via an in-app dialog, not window.confirm', async ({ page }) => {
  page.on('dialog', (d) => { throw new Error(`native dialog fired: ${d.type()} "${d.message()}"`); });

  await page.locator('.bar').first().click();
  await page.getByRole('button', { name: '💾 Save' }).click();
  const saveDialog = page.locator('dialog[open]');
  await saveDialog.locator('input[type="text"]').fill('Deletable Song');
  await saveDialog.getByRole('button', { name: 'OK' }).click();

  const delBtn = page.getByRole('button', { name: 'Delete the loaded song' });
  await expect(delBtn).toBeEnabled();
  await delBtn.click();

  let confirmDialog = page.locator('dialog[open]');
  await expect(confirmDialog).toBeVisible();
  await expect(confirmDialog).toContainText('Delete "Deletable Song"?');
  await confirmDialog.getByRole('button', { name: 'Cancel' }).click();
  await expect(confirmDialog).not.toBeVisible();
  await expect(delBtn).toBeEnabled(); // cancelled - still there

  await delBtn.click();
  confirmDialog = page.locator('dialog[open]');
  await confirmDialog.getByRole('button', { name: 'Delete' }).click();
  await expect(confirmDialog).not.toBeVisible();
  await expect(delBtn).toBeDisabled();
});

// ---------- Reduction engine (group C): pure and deterministic, so these call it directly
// via window.__xyloTest with synthetic note data instead of routing real audio through
// Basic Pitch - no mic, no network, no ML model needed to check the fitting logic itself.

test('reduction engine: the bars are C5-C6 major (72,74,76,77,79,81,83,84)', async ({ page }) => {
  const scale = await page.evaluate(() => window.__xyloTest.SCALE_MIDI);
  expect(scale).toEqual([72, 74, 76, 77, 79, 81, 83, 84]);
});

test('reduction engine: skyline keeps only the highest note active at each moment', async ({ page }) => {
  const segments = await page.evaluate(() => window.__xyloTest.skylineMelody([
    { midi: 60, start: 0, dur: 2 },   // low note, sounds the whole time
    { midi: 67, start: 0.5, dur: 1 }, // higher note, overlaps the middle of it
  ]));
  expect(segments).toEqual([
    { midi: 60, start: 0, dur: 0.5 },
    { midi: 67, start: 0.5, dur: 1 },
    { midi: 60, start: 1.5, dur: 0.5 },
  ]);
});

test('reduction engine: key detection finds the tonic from pitch classes alone', async ({ page }) => {
  const tonics = await page.evaluate(() => {
    const cMajor = [60, 62, 64, 65, 67, 69, 71].map(midi => ({ midi, dur: 1 }));
    const gMajor = [67, 69, 71, 72, 74, 76, 78].map(midi => ({ midi, dur: 1 })); // G A B C D E F#
    return {
      c: window.__xyloTest.detectTonicPitchClass(cMajor),
      g: window.__xyloTest.detectTonicPitchClass(gMajor),
    };
  });
  expect(tonics).toEqual({ c: 0, g: 7 });
});

test('reduction engine: octave placement picks the shift that keeps the most note-time in range', async ({ page }) => {
  const shift = await page.evaluate(() => {
    // all one octave below the register (C4s); +12 and +24 both land in range, but +12 is closer
    const melody = [{ midi: 60, dur: 1 }];
    return window.__xyloTest.chooseOctaveShift(melody, 0);
  });
  expect(shift).toBe(12);
});

test('reduction engine: a two-octave ascending scale folds into two passes over the eight bars with a perfect fit', async ({ page }) => {
  const result = await page.evaluate(() => {
    const midis = [60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84]; // C4..C6, straight C major
    const rawNotes = midis.map((midi, i) => ({ midi, start: i * 0.5, dur: 0.5 }));
    return window.__xyloTest.reduceToXylophone(rawNotes);
  });
  expect(result.detected.map(n => n.i)).toEqual([0, 1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7]);
  expect(result.fit).toEqual({ total: 15, matched: 15, offRatio: 0 });
});

test('reduction engine: fitMessage is silent on a perfect fit and honest otherwise', async ({ page }) => {
  const messages = await page.evaluate(() => ({
    perfect: window.__xyloTest.fitMessage({ total: 5, matched: 5, offRatio: 0 }),
    none: window.__xyloTest.fitMessage(null),
    minor: window.__xyloTest.fitMessage({ total: 10, matched: 9, offRatio: 0.05 }),
    severe: window.__xyloTest.fitMessage({ total: 10, matched: 5, offRatio: 0.4 }),
  }));
  expect(messages.perfect).toBeNull();
  expect(messages.none).toBeNull();
  expect(messages.minor).toBe('Almost a perfect fit. 1 of 10 note nudged to the nearest bar.');
  expect(messages.severe).toBe("This tune needs notes your xylophone doesn't have — here's the closest version. 5 of 10 notes nudged to the nearest bar.");
});
