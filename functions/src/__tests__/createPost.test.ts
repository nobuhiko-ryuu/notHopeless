/**
 * createPost.test.ts
 * Unit tests for createPost Cloud Function (validation logic with mocked Firestore).
 *
 * We test the validation/business logic by directly calling the validators
 * and simulating what createPost does, rather than invoking the full Cloud
 * Function, which would require a real Firebase project.
 */

import { normalizeText, validateBody, validateEnum } from '../validators';
import {
  VALID_SCENES,
  VALID_KINDNESS_TYPES,
  VALID_USER_STATES,
  VALID_EFFECTS,
} from '../types';

// --------------------------------------------------------------------------
// Helpers that mirror the logic inside createPost
// --------------------------------------------------------------------------

interface CreatePostInput {
  uid?: string;
  body?: unknown;
  scene?: unknown;
  kindnessType?: unknown;
  userState?: unknown;
  effect?: unknown;
}

type ValidationOutcome =
  | { ok: true; normalizedBody: string }
  | { ok: false; code: string; message: string };

function runCreatePostValidation(
  authUid: string | null,
  data: CreatePostInput
): ValidationOutcome {
  // 1. Authentication check
  if (!authUid) {
    return { ok: false, code: 'unauthenticated', message: 'Authentication required.' };
  }

  // 2. Input normalization
  const normalizedBody = normalizeText(String(data.body ?? ''));

  // 3. Enum validation
  const sceneResult = validateEnum(data.scene, VALID_SCENES, 'scene');
  if (!sceneResult.valid) {
    return { ok: false, code: 'invalid-argument', message: sceneResult.error!.message };
  }

  const kindnessTypeResult = validateEnum(data.kindnessType, VALID_KINDNESS_TYPES, 'kindnessType');
  if (!kindnessTypeResult.valid) {
    return { ok: false, code: 'invalid-argument', message: kindnessTypeResult.error!.message };
  }

  const userStateResult = validateEnum(data.userState, VALID_USER_STATES, 'userState');
  if (!userStateResult.valid) {
    return { ok: false, code: 'invalid-argument', message: userStateResult.error!.message };
  }

  const effectResult = validateEnum(data.effect, VALID_EFFECTS, 'effect');
  if (!effectResult.valid) {
    return { ok: false, code: 'invalid-argument', message: effectResult.error!.message };
  }

  // 4. Body validation
  const bodyResult = validateBody(normalizedBody);
  if (!bodyResult.valid) {
    return {
      ok: false,
      code: 'invalid-argument',
      message: bodyResult.error!.message,
    };
  }

  return { ok: true, normalizedBody };
}

// --------------------------------------------------------------------------
// Valid base input for reuse across tests
// --------------------------------------------------------------------------

const VALID_INPUT: CreatePostInput = {
  body: '電車で高齢の方に席を譲りました。',
  scene: 'commute',
  kindnessType: 'care',
  userState: 'normal',
  effect: 'relieved',
};

// --------------------------------------------------------------------------
// Tests
// --------------------------------------------------------------------------

describe('createPost validation', () => {
  describe('Authentication', () => {
    it('returns unauthenticated error when uid is null', () => {
      const result = runCreatePostValidation(null, VALID_INPUT);
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe('unauthenticated');
      }
    });

    it('passes authentication when uid is provided', () => {
      const result = runCreatePostValidation('user-123', VALID_INPUT);
      expect(result.ok).toBe(true);
    });
  });

  describe('Enum validation', () => {
    it('rejects invalid scene value', () => {
      const result = runCreatePostValidation('user-123', {
        ...VALID_INPUT,
        scene: 'invalid_scene',
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe('invalid-argument');
        expect(result.message).toContain('scene');
      }
    });

    it('rejects invalid kindnessType value', () => {
      const result = runCreatePostValidation('user-123', {
        ...VALID_INPUT,
        kindnessType: 'unknown_type',
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe('invalid-argument');
        expect(result.message).toContain('kindnessType');
      }
    });

    it('rejects invalid userState value', () => {
      const result = runCreatePostValidation('user-123', {
        ...VALID_INPUT,
        userState: 'happy',
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe('invalid-argument');
        expect(result.message).toContain('userState');
      }
    });

    it('rejects invalid effect value', () => {
      const result = runCreatePostValidation('user-123', {
        ...VALID_INPUT,
        effect: 'bad_effect',
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe('invalid-argument');
        expect(result.message).toContain('effect');
      }
    });

    it('accepts all valid scene values', () => {
      for (const scene of VALID_SCENES) {
        const result = runCreatePostValidation('user-123', { ...VALID_INPUT, scene });
        expect(result.ok).toBe(true);
      }
    });

    it('accepts all valid kindnessType values', () => {
      for (const kindnessType of VALID_KINDNESS_TYPES) {
        const result = runCreatePostValidation('user-123', { ...VALID_INPUT, kindnessType });
        expect(result.ok).toBe(true);
      }
    });

    it('accepts all valid userState values', () => {
      for (const userState of VALID_USER_STATES) {
        const result = runCreatePostValidation('user-123', { ...VALID_INPUT, userState });
        expect(result.ok).toBe(true);
      }
    });

    it('accepts all valid effect values', () => {
      for (const effect of VALID_EFFECTS) {
        const result = runCreatePostValidation('user-123', { ...VALID_INPUT, effect });
        expect(result.ok).toBe(true);
      }
    });
  });

  describe('Body validation', () => {
    it('rejects body exceeding 140 characters with invalid-argument', () => {
      const longBody = 'あ'.repeat(141);
      const result = runCreatePostValidation('user-123', { ...VALID_INPUT, body: longBody });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe('invalid-argument');
        expect(result.message).toContain('140');
      }
    });

    it('rejects body containing email address with invalid-argument (PERSONAL_INFO)', () => {
      const result = runCreatePostValidation('user-123', {
        ...VALID_INPUT,
        body: 'メールはtest@example.comです',
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe('invalid-argument');
        expect(result.message).toContain('personal information');
      }
    });

    it('rejects body containing phone number with invalid-argument (PERSONAL_INFO)', () => {
      const result = runCreatePostValidation('user-123', {
        ...VALID_INPUT,
        body: '電話は090-1234-5678にかけてください',
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe('invalid-argument');
        expect(result.message).toContain('personal information');
      }
    });

    it('rejects body containing URL with invalid-argument (PERSONAL_INFO)', () => {
      const result = runCreatePostValidation('user-123', {
        ...VALID_INPUT,
        body: 'https://example.comをご覧ください',
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe('invalid-argument');
        expect(result.message).toContain('personal information');
      }
    });

    it('normalizes body before validation (zero-width characters stripped)', () => {
      // After stripping zero-width chars, body becomes valid text within 140 chars
      const bodyWithZeroWidth = '親切にしました\u200B';
      const result = runCreatePostValidation('user-123', {
        ...VALID_INPUT,
        body: bodyWithZeroWidth,
      });
      expect(result.ok).toBe(true);
      if (result.ok) {
        expect(result.normalizedBody).toBe('親切にしました');
      }
    });

    it('normalizes full-width spaces before validation', () => {
      const bodyWithFullWidth = '\u3000親切にしました\u3000';
      const result = runCreatePostValidation('user-123', {
        ...VALID_INPUT,
        body: bodyWithFullWidth,
      });
      expect(result.ok).toBe(true);
      if (result.ok) {
        expect(result.normalizedBody).toBe('親切にしました');
      }
    });
  });
});
