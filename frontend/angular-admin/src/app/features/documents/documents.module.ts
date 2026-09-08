import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DocumentListComponent } from './document-list.component';

@NgModule({
  declarations: [DocumentListComponent],
  imports: [CommonModule, RouterModule, MatTableModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  exports: [DocumentListComponent]
})
export class DocumentsModule { }
