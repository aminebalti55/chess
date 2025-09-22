import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MovesPanelComponent } from './moves-panel.component';

describe('MovesPanelComponent', () => {
  let component: MovesPanelComponent;
  let fixture: ComponentFixture<MovesPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MovesPanelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MovesPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
