import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DocumentStoreModule } from './store/document-store.module';

const routes: Routes = [
  { path: '', redirectTo: '/documents', pathMatch: 'full' },
  { path: 'documents', loadChildren: () => import('./features/documents/documents.module').then(m => m.DocumentsModule) },
  { path: 'workflows', loadChildren: () => import('./features/workflows/workflows.module').then(m => m.WorkflowsModule) }
];

@NgModule({
  imports: [RouterModule.forRoot(routes), DocumentStoreModule],
  exports: [RouterModule]
})
export class AppRoutingModule { }
