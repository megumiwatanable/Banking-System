/** A new key is generated for each user intent and reused only by an explicit HTTP retry. */
export function createIdempotencyKey(): string {
  return crypto.randomUUID();
}
