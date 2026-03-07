/**
 * validators.test.ts
 * Unit tests for normalizeText and validateBody.
 */

import { normalizeText, validateBody } from '../validators';

describe('normalizeText', () => {
  it('removes zero-width characters (U+200B)', () => {
    const input = 'hello\u200Bworld';
    expect(normalizeText(input)).toBe('helloworld');
  });

  it('removes zero-width non-joiner (U+200C)', () => {
    const input = 'abc\u200Cdef';
    expect(normalizeText(input)).toBe('abcdef');
  });

  it('removes zero-width joiner (U+200D)', () => {
    const input = 'abc\u200Ddef';
    expect(normalizeText(input)).toBe('abcdef');
  });

  it('removes BOM (U+FEFF)', () => {
    const input = '\uFEFFhello';
    expect(normalizeText(input)).toBe('hello');
  });

  it('removes soft hyphen (U+00AD)', () => {
    const input = 'word\u00ADbreak';
    expect(normalizeText(input)).toBe('wordbreak');
  });

  it('converts full-width space (U+3000) to half-width space', () => {
    const input = 'こんにちは\u3000世界';
    expect(normalizeText(input)).toBe('こんにちは 世界');
  });

  it('trims leading and trailing whitespace', () => {
    const input = '  hello world  ';
    expect(normalizeText(input)).toBe('hello world');
  });

  it('handles combination of zero-width, full-width space, and trim', () => {
    const input = '\u3000\u200Bhello\u3000world\u200C  ';
    expect(normalizeText(input)).toBe('hello world');
  });

  it('returns empty string for whitespace-only input', () => {
    expect(normalizeText('   ')).toBe('');
  });

  it('leaves normal text unchanged', () => {
    expect(normalizeText('電車で席を譲りました')).toBe('電車で席を譲りました');
  });
});

describe('validateBody', () => {
  it('accepts normal text within 140 characters', () => {
    const result = validateBody('電車で高齢の方に席を譲りました。とても喜んでくださいました。');
    expect(result.valid).toBe(true);
  });

  it('accepts exactly 140 characters', () => {
    const text = 'あ'.repeat(140);
    const result = validateBody(text);
    expect(result.valid).toBe(true);
  });

  it('rejects text exceeding 140 characters with code TOO_LONG', () => {
    const text = 'あ'.repeat(141);
    const result = validateBody(text);
    expect(result.valid).toBe(false);
    expect(result.error?.code).toBe('TOO_LONG');
  });

  it('rejects email address with code PERSONAL_INFO', () => {
    const result = validateBody('連絡はtest.user@example.comまでどうぞ');
    expect(result.valid).toBe(false);
    expect(result.error?.code).toBe('PERSONAL_INFO');
  });

  it('rejects phone number (hyphen format) with code PERSONAL_INFO', () => {
    const result = validateBody('電話は090-1234-5678にかけてください');
    expect(result.valid).toBe(false);
    expect(result.error?.code).toBe('PERSONAL_INFO');
  });

  it('rejects phone number (space format) with code PERSONAL_INFO', () => {
    const result = validateBody('電話は03 1234 5678にかけてください');
    expect(result.valid).toBe(false);
    expect(result.error?.code).toBe('PERSONAL_INFO');
  });

  it('rejects URL with code PERSONAL_INFO', () => {
    const result = validateBody('詳細はhttps://example.comをご覧ください');
    expect(result.valid).toBe(false);
    expect(result.error?.code).toBe('PERSONAL_INFO');
  });

  it('rejects http URL with code PERSONAL_INFO', () => {
    const result = validateBody('詳細はhttp://example.comをご覧ください');
    expect(result.valid).toBe(false);
    expect(result.error?.code).toBe('PERSONAL_INFO');
  });

  it('rejects SNS ID with code PERSONAL_INFO', () => {
    const result = validateBody('Twitterは@user_name123をフォローしてください');
    expect(result.valid).toBe(false);
    expect(result.error?.code).toBe('PERSONAL_INFO');
  });

  it('rejects postal code with code PERSONAL_INFO', () => {
    const result = validateBody('住所は〒123-4567です');
    expect(result.valid).toBe(false);
    expect(result.error?.code).toBe('PERSONAL_INFO');
  });

  it('rejects address with chome with code PERSONAL_INFO', () => {
    const result = validateBody('場所は2丁目の角を右に曲がります');
    expect(result.valid).toBe(false);
    expect(result.error?.code).toBe('PERSONAL_INFO');
  });

  it('rejects station name suffix (駅) with code SPECIFIC_NOUN', () => {
    const result = validateBody('渋谷駅で困っている方を助けました');
    expect(result.valid).toBe(false);
    expect(result.error?.code).toBe('SPECIFIC_NOUN');
  });

  it('rejects shop name suffix (店) with code SPECIFIC_NOUN', () => {
    const result = validateBody('コンビニ店で財布を拾って届けました');
    expect(result.valid).toBe(false);
    expect(result.error?.code).toBe('SPECIFIC_NOUN');
  });

  it('rejects hospital suffix (病院) with code SPECIFIC_NOUN', () => {
    const result = validateBody('近くの病院まで道を案内しました');
    expect(result.valid).toBe(false);
    expect(result.error?.code).toBe('SPECIFIC_NOUN');
  });

  it('rejects person name with san suffix (さん) with code SPECIFIC_NOUN', () => {
    const result = validateBody('田中さんがドアを開けてくれました');
    expect(result.valid).toBe(false);
    expect(result.error?.code).toBe('SPECIFIC_NOUN');
  });

  it('rejects person name with sama suffix (様) with code SPECIFIC_NOUN', () => {
    const result = validateBody('鈴木様がとても親切にしてくれました');
    expect(result.valid).toBe(false);
    expect(result.error?.code).toBe('SPECIFIC_NOUN');
  });

  it('accepts text without personal info and specific nouns', () => {
    // Note: "おじいさん" contains the suffix "さん" preceded by kana, so it is correctly
    // detected as a specific noun (person reference). Use text with no such patterns.
    const result = validateBody('電車で高齢の方に席を譲ると、とても喜んでいただきました。');
    expect(result.valid).toBe(true);
  });
});
