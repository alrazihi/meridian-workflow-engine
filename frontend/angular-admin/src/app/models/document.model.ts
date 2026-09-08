export interface Document {
  documentId: string;
  status: string;
  type: string;
  priority: string;
  metadata: Record<string, any>;
  createdAt: string;
  contentHash: string;
}
