/**
 * reactToPost.ts
 * Cloud Function: React to a post (with optimistic update support).
 */

import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions/v2/https';
import { VALID_REACTION_TYPES, ReactionType } from './types';
import { validateEnum } from './validators';

interface ReactToPostRequest {
  postId: string;
  reactionType: string;
}

export const reactToPost = functions.onCall(async (request) => {
  // 1. Authentication check
  if (!request.auth) {
    throw new functions.HttpsError('unauthenticated', 'Authentication required.');
  }
  const uid = request.auth.uid;

  const data = request.data as ReactToPostRequest;

  if (!data.postId || typeof data.postId !== 'string') {
    throw new functions.HttpsError('invalid-argument', 'postId is required.');
  }

  const reactionTypeResult = validateEnum(data.reactionType, VALID_REACTION_TYPES, 'reactionType');
  if (!reactionTypeResult.valid) {
    throw new functions.HttpsError('invalid-argument', reactionTypeResult.error!.message);
  }

  const newType = data.reactionType as ReactionType;

  // 2. reactionId = {postId}_{uid}
  const reactionId = `${data.postId}_${uid}`;

  const db = admin.firestore();
  const reactionRef = db.collection('reactions').doc(reactionId);
  const postRef = db.collection('posts').doc(data.postId);

  // 3. Firestore transaction
  await db.runTransaction(async (tx) => {
    const [reactionSnap, postSnap] = await Promise.all([
      tx.get(reactionRef),
      tx.get(postRef),
    ]);

    if (!postSnap.exists) {
      throw new functions.HttpsError('not-found', 'Post not found.');
    }

    if (!reactionSnap.exists) {
      // Does not exist: create reaction and increment count
      tx.set(reactionRef, {
        postId: data.postId,
        uid,
        reactionType: newType,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      tx.update(postRef, {
        [`reactionCounts.${newType}`]: admin.firestore.FieldValue.increment(1),
      });
    } else {
      const existingType = reactionSnap.data()?.reactionType as ReactionType;

      if (existingType === newType) {
        // Same type: idempotent, skip
        return;
      }

      // Different type: update type, decrement old count, increment new count
      tx.update(reactionRef, {
        reactionType: newType,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      tx.update(postRef, {
        [`reactionCounts.${existingType}`]: admin.firestore.FieldValue.increment(-1),
        [`reactionCounts.${newType}`]: admin.firestore.FieldValue.increment(1),
      });
    }
  });

  // 4. Success response
  return {};
});
