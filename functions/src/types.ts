export const VALID_SCENES = ['commute', 'shop', 'workplace', 'public'] as const;
export const VALID_KINDNESS_TYPES = ['care', 'help', 'integrity', 'courage', 'pro'] as const;
export const VALID_USER_STATES = ['tired', 'rushed', 'down', 'normal'] as const;
export const VALID_EFFECTS = ['relieved', 'lighter', 'inspired', 'survived', 'notHopeless', 'trust'] as const;
export const VALID_REACTION_TYPES = ['notHopeless', 'moved', 'doToo'] as const;
export const VALID_REPORT_REASONS = ['personal_info', 'harassment', 'discrimination', 'sexual', 'other'] as const;

export type Scene = typeof VALID_SCENES[number];
export type KindnessType = typeof VALID_KINDNESS_TYPES[number];
export type UserState = typeof VALID_USER_STATES[number];
export type Effect = typeof VALID_EFFECTS[number];
export type ReactionType = typeof VALID_REACTION_TYPES[number];
export type ReportReason = typeof VALID_REPORT_REASONS[number];
