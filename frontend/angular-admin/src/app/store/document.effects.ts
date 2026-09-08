import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { DocumentService } from '../services/document.service';
import * as DocumentActions from './document.actions';
import { catchError, map, mergeMap, of } from 'rxjs';

@Injectable()
export class DocumentEffects {
  loadDocuments$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentActions.loadDocuments),
      mergeMap(() =>
        this.documentService.listDocuments().pipe(
          map((documents) => DocumentActions.loadDocumentsSuccess({ documents })),
          catchError((error) => of(DocumentActions.loadDocumentsFailure({ error: error.message })))
        )
      )
    )
  );

  loadDocument$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentActions.loadDocument),
      mergeMap(({ documentId }) =>
        this.documentService.getDocument(documentId).pipe(
          map((document) => DocumentActions.loadDocumentSuccess({ document })),
          catchError((error) => of(DocumentActions.loadDocumentFailure({ error: error.message })))
        )
      )
    )
  );

  uploadDocument$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentActions.uploadDocument),
      mergeMap(({ file, type, priority, metadata, idempotencyKey }) =>
        this.documentService.uploadDocument(file, type, priority, metadata, idempotencyKey).pipe(
          map((document) => DocumentActions.uploadDocumentSuccess({ document })),
          catchError((error) => of(DocumentActions.uploadDocumentFailure({ error: error.message })))
        )
      )
    )
  );

  constructor(private actions$: Actions, private documentService: DocumentService) {}
}
