import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DocumentListComponent } from './document-list.component';

@NgModule({
  declarations: [DocumentListComponent],
  imports: [CommonModule, RouterModule],
  exports: [DocumentListComponent]
})
export class DocumentsModule { }
