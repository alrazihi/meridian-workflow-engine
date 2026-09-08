import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-workflow-list',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="workflows">
      <h1>Workflows</h1>
      @if (loading) {
        <mat-spinner></mat-spinner>
      } @else {
        <p>Workflow management interface.</p>
      }
    </div>
  `,
  styles: [`
    .workflows { padding: 24px; }
  `]
})
export class WorkflowListComponent implements OnInit {
  loading = true;

  ngOnInit(): void {
    this.loading = false;
  }
}
