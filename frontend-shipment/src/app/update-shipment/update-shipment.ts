import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ShipmentService } from '../services/shipment-service';
import { Shipment, ShipmentStatus, STATUS_LABELS, STATUS_TRANSITIONS, StatusUpdateMessage } from '../models/shipment.model';
import { catchError, of } from 'rxjs';
import { WebsocketService } from '../services/websocket-service';

@Component({
  imports: [ReactiveFormsModule, CommonModule],
  selector: 'app-update-shipment',
  styleUrl: './update-shipment.css',
  templateUrl: './update-shipment.html',
})
export class UpdateShipment implements OnInit{
  private destroyRef = inject(DestroyRef);
  private shipmentService = inject(ShipmentService);
  private websocketService = inject(WebsocketService);
  private fb = inject(FormBuilder);

  STATUS_LABELS = STATUS_LABELS;
  statusOptions = signal<ShipmentStatus[]>([]);
  shipments = signal<Shipment[]>([]);
  errorMessage = signal<string>('');
  isLoading = signal<boolean>(false);

  isSubmitting = signal(false);

  updateForm: FormGroup = this.fb.group({
    shipmentId: [null, Validators.required],
    status: [null, Validators.required],
    currentLocation: [''],
  });

  ngOnInit(): void {
    this.loadShipments();
    this.websocketService.getStatusUpdates()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((update) => {
        if (update) {
          this.handleStatusUpdate(update);
        }
      });
  }

  private handleStatusUpdate(update: StatusUpdateMessage): void {
    if (!this.shipments().some(shipment => shipment.id === update.shipmentId)) {
      this.loadShipments();
      return;
    }

    this.shipments.update(shipments =>
      shipments.map(shipment =>
        shipment.id === update.shipmentId
          ? {
              ...shipment,
              status: update.status,
              currentLocation: update.currentLocation,
              updatedAt: update.timestamp,
            }
          : shipment,
      ),
    );
  }

  loadShipments(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.shipmentService.getAllShipments()
    .pipe(catchError((error) => {
      this.errorMessage.set('Failed to load shipments');
      console.error('error:', error);
      return of([]);
    }))
    .subscribe((shipments) => {
      this.shipments.set(shipments);
      this.isLoading.set(false);
    });
  }

  updateShipment(): void {
    if (this.updateForm.invalid || this.isSubmitting()) {
      this.updateForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const { shipmentId, status, currentLocation } = this.updateForm.value; 
    this.shipmentService.updateShipmentStatus(shipmentId, { status, currentLocation})
      .pipe(catchError((error: unknown) => {
        this.errorMessage.set(this.readError(error, 'Failed to update the shipment status'));
        return of(null);
      }))
      .subscribe((result) => {
        if(result !== null) {
          this.statusOptions.set([]);
          this.updateForm.reset({
            shipmentId: null,
            status: null,
            currentLocation: '',
          });
        }
        this.isSubmitting.set(false);
      });
  }

  private readError(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }
    return fallback;
  }

  onSelectShipmentId(): void {
    const shipmentId = this.updateForm.get('shipmentId')?.value;

    if (!shipmentId) {
      this.statusOptions.set([]);
      this.updateForm.patchValue({ status: null, currentLocation: '' });
      return;
    }

    const selectedShipment = this.shipments().find(shipment => shipment.id === shipmentId);
    const nextStatuses = selectedShipment ? [...STATUS_TRANSITIONS[selectedShipment.status]] : [];
    this.statusOptions.set(nextStatuses);
    this.updateForm.patchValue({
      currentLocation: selectedShipment?.currentLocation || '',
      status: nextStatuses[0] ?? null,
    });
  }

  hasError(fieldName: string): boolean {
    const field = this.updateForm.get(fieldName); // récupère le champ du formulaire
    return !!(field?.invalid && field?.touched); // si le champ est invalide et touché
  }

  getErrorMessage(fieldName: string): string {
    const field = this.updateForm.get(fieldName);
    if (field?.hasError('required')) {
      return 'This field is required';
    }
    return '';
  }
}
