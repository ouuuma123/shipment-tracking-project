export const SHIPMENT_STATUS = {
    CREATED: 'CREATED',
    PICKED_UP: 'PICKED_UP',
    IN_TRANSIT: 'IN_TRANSIT',
    OUT_FOR_DELIVERY: 'OUT_FOR_DELIVERY',
    DELIVERED: 'DELIVERED',
    FAILED_DELIVERY: 'FAILED_DELIVERY',
    RETURNED: 'RETURNED',
    CANCELLED: 'CANCELLED',
} as const;

export type ShipmentStatus = keyof typeof SHIPMENT_STATUS;

export const STATUS_TRANSITIONS: Readonly<Record<ShipmentStatus, readonly ShipmentStatus[]>> = {
    CREATED: [SHIPMENT_STATUS.PICKED_UP, SHIPMENT_STATUS.CANCELLED],
    PICKED_UP: [SHIPMENT_STATUS.IN_TRANSIT, SHIPMENT_STATUS.CANCELLED],
    IN_TRANSIT: [
        SHIPMENT_STATUS.OUT_FOR_DELIVERY,
        SHIPMENT_STATUS.FAILED_DELIVERY,
        SHIPMENT_STATUS.RETURNED,
        SHIPMENT_STATUS.CANCELLED,
    ],
    OUT_FOR_DELIVERY: [SHIPMENT_STATUS.DELIVERED, SHIPMENT_STATUS.FAILED_DELIVERY],
    FAILED_DELIVERY: [SHIPMENT_STATUS.OUT_FOR_DELIVERY, SHIPMENT_STATUS.RETURNED],
    DELIVERED: [],
    RETURNED: [],
    CANCELLED: [],
};

export interface Shipment {
    id: number;
    trackingNumber: string;
    origin: string;
    destination: string;
    status: ShipmentStatus;
    createdAt: string;
    updatedAt: string;
    currentLocation: string;
    estimatedDelivery?: string;
}

export interface CreateShipmentRequest {
    origin: string;
    destination: string;
    estimatedDelivery?: string;
}

export interface UpdateStatusRequest {
    status: ShipmentStatus;
    currentLocation: string;
}

export interface StatusUpdateMessage {
    shipmentId: number;
    trackingNumber: string;
    status: ShipmentStatus;
    currentLocation: string;
    timestamp: string;
    message: string;
}

export const STATUS_LABELS: Readonly<Record<ShipmentStatus, string>> = {
    [SHIPMENT_STATUS.CREATED]: 'Created',
    [SHIPMENT_STATUS.PICKED_UP]: 'Picked Up',
    [SHIPMENT_STATUS.IN_TRANSIT]: 'In Transit',
    [SHIPMENT_STATUS.OUT_FOR_DELIVERY]: 'Out for Delivery',
    [SHIPMENT_STATUS.DELIVERED]: 'Delivered',
    [SHIPMENT_STATUS.FAILED_DELIVERY]: 'Failed Delivery',
    [SHIPMENT_STATUS.RETURNED]: 'Returned',
    [SHIPMENT_STATUS.CANCELLED]: 'Cancelled',
};

export const STATUS_BADGE_CLASS: Readonly<Record<ShipmentStatus, string>> = {
    [SHIPMENT_STATUS.CREATED]: 'bg-blue-500/10 text-blue-400 ring-blue-500/20',
    [SHIPMENT_STATUS.PICKED_UP]: 'bg-purple-500/10 text-purple-400 ring-purple-500/20',
    [SHIPMENT_STATUS.IN_TRANSIT]: 'bg-cyan-500/10 text-cyan-400 ring-cyan-500/20',
    [SHIPMENT_STATUS.OUT_FOR_DELIVERY]: 'bg-yellow-500/10 text-yellow-400 ring-yellow-500/20',
    [SHIPMENT_STATUS.DELIVERED]: 'bg-green-500/10 text-green-400 ring-green-500/20',
    [SHIPMENT_STATUS.FAILED_DELIVERY]: 'bg-red-500/10 text-red-400 ring-red-500/20',
    [SHIPMENT_STATUS.RETURNED]: 'bg-orange-500/10 text-orange-400 ring-orange-500/20',
    [SHIPMENT_STATUS.CANCELLED]: 'bg-zinc-500/10 text-zinc-400 ring-zinc-500/20',
};
