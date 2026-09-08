import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Store } from '@ngrx/store';
import * as DocumentActions from '../../store/document.actions';
import { Document } from '../../models/document.model';

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="documents">
      <h1>Documents</h1>
      @if (loading) {
        <mat-spinner></mat-spinner>
      } @else {
        <table mat-table [dataSource]="documents">
          <ng-container matColumnDef="documentId">
            <th mat-header-cell *matHeaderCellDef> ID </th>
            <td mat-cell *matCellDef="let document"> {{document.documentId}} </td>
          </ng-container>
          <ng-container matColumnDef="type">
            <th mat-header-cell *matHeaderCellDef> Type </th>
            <td mat-cell *matCellDef="let document"> {{document.type}} </td>
          </ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef> Status </th>
            <td mat-cell *matCellDef="let document"> {{document.status}} </td>
          </ng-container>
          <ng-container matColumnDef="priority">
            <th mat-header-cell *matHeaderCellDef> Priority </th>
            <td mat-cell *matCellDef="let document"> {{document.priority}} </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
        @if (documents.length === 0) {
          <p>No documents found.</p>
        }
      }
    </div>
  `,
  styles: [`
    .documents { padding: 24px; }
    table { width: 100%; }
  `]
})
export class DocumentListComponent implements OnInit {
  displayedColumns: string[] = ['documentId', 'type', 'status', 'priority'];
  documents: Document[] = [];
  loading = true;

  constructor(private store: Store) {}

  ngOnInit(): void {
    this.store.dispatch(DocumentActions.loadDocuments());
    this.store.select(state => (state as any).documents?.documents ?? []).subscribe(data => {
      this.documents = data;
      this.loading = false;
    });
  }
}
