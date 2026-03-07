/**
 * createPost.ts
 * Cloud Function: Create a new post.
 */

import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions/v2/https';
import {
  VALID_SCENES,
  VALID_KINDNESS_TYPES,
  VALID_USER_STATES,
  VALID_EFFECTS,
  Scene,
  KindnessType,
  UserState,
  Effect,
} from './types';
import { normalizeText, validateBody, validateEnum } from './validators';

interface CreatePostRequest {
  body: string;
  scene: string;
  kindnessType: string;
  userState: string;
  effect: string;
}

const COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes

export const createPost = functions.onCall(async (request) => {
  // 1. Authentication check
  if (!request.auth) {
    throw new functions.HttpsError('unauthenticated', 'Authentication required.');
  }
  const uid = request.auth.uid;

  const data = request.data as CreatePostRequest;

  // 2. Input normalization
  const normalizedBody = normalizeText(data.body ?? '');

  // 3. Validation
  // Validate enum fields
  const sceneResult = validateEnum(data.scene, VALID_SCENES, 'scene');
  if (!sceneResult.valid) {
    throw new functions.HttpsError('invalid-argument', sceneResult.error!.message);
  }

  const kindnessTypeResult = validateEnum(data.kindnessType, VALID_KINDNESS_TYPES, 'kindnessType');
  if (!kindnessTypeResult.valid) {
    throw new functions.HttpsError('invalid-argument', kindnessTypeResult.error!.message);
  }

  const userStateResult = validateEnum(data.userState, VALID_USER_STATES, 'userState');
  if (!userStateResult.valid) {
    throw new functions.HttpsError('invalid-argument', userStateResult.error!.message);
  }

  const effectResult = validateEnum(data.effect, VALID_EFFECTS, 'effect');
  if (!effectResult.valid) {
    throw new functions.HttpsError('invalid-argument', effectResult.error!.message);
  }

  // Validate body text
  const bodyResult = validateBody(normalizedBody);
  if (!bodyResult.valid) {
    throw new functions.HttpsError(
      'invalid-argument',
      bodyResult.error!.message,
      { code: bodyResult.error!.code }
    );
  }

  const db = admin.firestore();

  // 4. Cooldown check
  const userRef = db.collection('users').doc(uid);
  const userSnap = await userRef.get();
  if (userSnap.exists) {
    const lastPostAt: admin.firestore.Timestamp | undefined = userSnap.data()?.lastPostAt;
    if (lastPostAt) {
      const lastPostMs = lastPostAt.toMillis();
      const nowMs = Date.now();
      if (nowMs - lastPostMs < COOLDOWN_MS) {
        throw new functions.HttpsError(
          'resource-exhausted',
          'Please wait before posting again.',
          { code: 'COOLDOWN' }
        );
      }
    }
  }

  // 5. Write to Firestore
  const now = admin.firestore.FieldValue.serverTimestamp();
  const postData = {
    uid,
    body: normalizedBody,
    scene: data.scene as Scene,
    kindnessType: data.kindnessType as KindnessType,
    userState: data.userState as UserState,
    effect: data.effect as Effect,
    status: 'visible',
    reactionCounts: {
      notHopeless: 0,
      moved: 0,
      doToo: 0,
    },
    isStock: false,
    createdAt: now,
  };

  const batch = db.batch();
  const postRef = db.collection('posts').doc();
  batch.set(postRef, postData);

  // 6. Update users/{uid}.lastPostAt
  batch.set(userRef, { lastPostAt: now }, { merge: true });

  await batch.commit();

  // 7. Success response
  return { postId: postRef.id };
});
