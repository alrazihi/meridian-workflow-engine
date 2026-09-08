import { Component } from '@angular/core';

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [],
  template: `
    <div class="documents">
      <h1>Documents</h1>
      <p>Document management interface placeholder.</p>
    </div>
  `,
  styles: [`
    .documents { padding: 24px; }
  `]
})
export class DocumentListComponent {}
