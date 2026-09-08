import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { WorkflowListComponent } from './workflow-list.component';

@NgModule({
  declarations: [WorkflowListComponent],
  imports: [CommonModule, RouterModule],
  exports: [WorkflowListComponent]
})
export class WorkflowsModule { }
