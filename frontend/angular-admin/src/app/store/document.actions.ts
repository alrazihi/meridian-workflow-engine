import { createAction, props } from '@ngrx/store';
import { Document } from '../../models/document.model';

export const loadDocuments = createAction('[Document] Load Documents');
export const loadDocumentsSuccess = createAction('[Document] Load Documents Success', props<{ documents: Document[] }>());
export const loadDocumentsFailure = createAction('[Document] Load Documents Failure', props<{ error: string }>());

export const loadDocument = createAction('[Document] Load Document', props<{ documentId: string }>());
export const loadDocumentSuccess = createAction('[Document] Load Document Success', props<{ document: Document }>());
export const loadDocumentFailure = createAction('[Document] Load Document Failure', props<{ error: string }>());

export const uploadDocument = createAction('[Document] Upload Document', props<{ file: File; type: string; priority: string; metadata: any; idempotencyKey?: string }>());
export const uploadDocumentSuccess = createAction('[Document] Upload Document Success', props<{ document: Document }>());
export const uploadDocumentFailure = createAction('[Document] Upload Document Failure', props<{ error: string }>());
