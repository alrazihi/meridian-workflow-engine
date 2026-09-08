import { Component } from '@angular/core';

@Component({
  selector: 'app-workflow-list',
  standalone: true,
  imports: [],
  template: `
    <div class="workflows">
      <h1>Workflows</h1>
      <p>Workflow management interface placeholder.</p>
    </div>
  `,
  styles: [`
    .workflows { padding: 24px; }
  `]
})
export class WorkflowListComponent {}
