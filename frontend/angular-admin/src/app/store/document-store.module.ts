import { NgModule } from '@angular/core';
import { StoreModule } from '@ngrx/store';
import { EffectsModule } from '@ngrx/effects';
import { documentReducer } from './document.reducer';
import { DocumentEffects } from './document.effects';

@NgModule({
  imports: [
    StoreModule.forFeature('documents', documentReducer),
    EffectsModule.forFeature([DocumentEffects])
  ]
})
export class DocumentStoreModule {}
