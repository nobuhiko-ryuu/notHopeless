/**
 * validators.ts
 * Input normalization and validation for notHopeless Cloud Functions.
 */

export interface ValidationError {
  code: string;
  message: string;
}

export interface ValidationResult {
  valid: boolean;
  error?: ValidationError;
}

/**
 * Patterns for personal information detection (reject).
 */
const PERSONAL_INFO_PATTERNS: RegExp[] = [
  /\d{2,4}[-\s]\d{2,4}[-\s]\d{4}/,          // Phone number
  /[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}/,  // Email
  /https?:\/\//,                              // URL
  /@[A-Za-z0-9_]+/,                          // SNS ID
  /〒\d{3}-\d{4}/,                           // Postal code
  /[0-9]+丁目/,                              // Address
];

/**
 * Suffixes that indicate specific nouns (reject when preceded by kanji/kana).
 */
const SPECIFIC_NOUN_SUFFIXES = ['駅', '店', '会社', '学校', '病院', '公園', 'さん', 'くん', 'ちゃん', '様'];

/**
 * Regex to detect specific noun patterns: kanji/kana followed by a specific suffix.
 * Unicode ranges:
 *   \u3040-\u309F  Hiragana
 *   \u30A0-\u30FF  Katakana
 *   \u4E00-\u9FFF  CJK Unified Ideographs (kanji)
 *   \u3400-\u4DBF  CJK Extension A
 */
const buildSpecificNounRegex = (): RegExp => {
  const kanjiKana = '[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF\\u3400-\\u4DBF]';
  const suffixAlternatives = SPECIFIC_NOUN_SUFFIXES.map((s) => s).join('|');
  return new RegExp(`${kanjiKana}+(${suffixAlternatives})`);
};

const SPECIFIC_NOUN_PATTERN = buildSpecificNounRegex();

/**
 * Normalize input text:
 * 1. Remove zero-width characters
 * 2. Convert full-width spaces to half-width spaces
 * 3. Trim leading/trailing whitespace
 */
export function normalizeText(text: string): string {
  // Remove zero-width characters (U+200B, U+200C, U+200D, U+FEFF, U+00AD, U+2060)
  let normalized = text.replace(/[\u200B\u200C\u200D\uFEFF\u00AD\u2060]/g, '');

  // Convert full-width spaces (U+3000) to half-width spaces
  normalized = normalized.replace(/\u3000/g, ' ');

  // Trim leading and trailing whitespace
  normalized = normalized.trim();

  return normalized;
}

/**
 * Validate the body text of a post.
 * Returns a ValidationResult with error code and message on failure.
 */
export function validateBody(body: string): ValidationResult {
  // Check length (max 140 characters)
  if (body.length > 140) {
    return {
      valid: false,
      error: {
        code: 'TOO_LONG',
        message: `Body must be 140 characters or fewer. Got ${body.length}.`,
      },
    };
  }

  // Check for personal information patterns
  for (const pattern of PERSONAL_INFO_PATTERNS) {
    if (pattern.test(body)) {
      return {
        valid: false,
        error: {
          code: 'PERSONAL_INFO',
          message: 'Body contains personal information.',
        },
      };
    }
  }

  // Check for specific noun patterns
  if (SPECIFIC_NOUN_PATTERN.test(body)) {
    return {
      valid: false,
      error: {
        code: 'SPECIFIC_NOUN',
        message: 'Body contains a specific noun (place name, person name, etc.).',
      },
    };
  }

  return { valid: true };
}

/**
 * Check if a value is in an allowed set.
 */
export function validateEnum<T extends string>(
  value: unknown,
  allowed: readonly T[],
  fieldName: string
): ValidationResult {
  if (typeof value !== 'string' || !(allowed as readonly string[]).includes(value)) {
    return {
      valid: false,
      error: {
        code: 'INVALID_ENUM',
        message: `Invalid value for ${fieldName}: "${String(value)}". Allowed: ${allowed.join(', ')}.`,
      },
    };
  }
  return { valid: true };
}
