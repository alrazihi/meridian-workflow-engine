import { createReducer, on } from '@ngrx/store';
import { Document } from '../models/document.model';
import * as DocumentActions from './document.actions';

export interface DocumentState {
  documents: Document[];
  selectedDocument: Document | null;
  loading: boolean;
  error: string | null;
}

export const initialState: DocumentState = {
  documents: [],
  selectedDocument: null,
  loading: false,
  error: null
};

export const documentReducer = createReducer(
  initialState,
  on(DocumentActions.loadDocuments, (state) => ({ ...state, loading: true, error: null })),
  on(DocumentActions.loadDocumentsSuccess, (state, { documents }) => ({ ...state, documents, loading: false })),
  on(DocumentActions.loadDocumentsFailure, (state, { error }) => ({ ...state, error, loading: false })),
  on(DocumentActions.loadDocument, (state) => ({ ...state, loading: true, error: null })),
  on(DocumentActions.loadDocumentSuccess, (state, { document }) => ({ ...state, selectedDocument: document, loading: false })),
  on(DocumentActions.loadDocumentFailure, (state, { error }) => ({ ...state, error, loading: false })),
  on(DocumentActions.uploadDocument, (state) => ({ ...state, loading: true, error: null })),
  on(DocumentActions.uploadDocumentSuccess, (state, { document }) => ({ ...state, documents: [...state.documents, document], loading: false })),
  on(DocumentActions.uploadDocumentFailure, (state, { error }) => ({ ...state, error, loading: false }))
);
