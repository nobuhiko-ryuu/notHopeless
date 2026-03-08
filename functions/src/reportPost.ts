/**
 * reportPost.ts
 * Cloud Function: Report a post.
 */

import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions/v2/https';
import { VALID_REPORT_REASONS, ReportReason } from './types';
import { normalizeText, validateEnum } from './validators';

interface ReportPostRequest {
  postId: string;
  reason: string;
  comment?: string;
}

export const reportPost = functions.onCall(async (request) => {
  // 1. Authentication check
  if (!request.auth) {
    throw new functions.HttpsError('unauthenticated', 'Authentication required.');
  }
  const uid = request.auth.uid;

  const data = request.data as ReportPostRequest;

  if (!data.postId || typeof data.postId !== 'string') {
    throw new functions.HttpsError('invalid-argument', 'postId is required.');
  }

  const reasonResult = validateEnum(data.reason, VALID_REPORT_REASONS, 'reason');
  if (!reasonResult.valid) {
    throw new functions.HttpsError('invalid-argument', reasonResult.error!.message);
  }

  const db = admin.firestore();

  // 2. Check post existence
  const postSnap = await db.collection('posts').doc(data.postId).get();
  if (!postSnap.exists) {
    throw new functions.HttpsError('not-found', 'Post not found.');
  }

  // 3. Write report
  await db.collection('reports').add({
    postId: data.postId,
    reporterId: uid,
    reason: data.reason as ReportReason,
    comment: data.comment != null ? normalizeText(String(data.comment)).slice(0, 200) : null,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  // 4. Success response
  return {};
});
