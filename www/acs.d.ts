import { Observable } from "rxjs";

declare module 'cordova-plugin-acs' {
    export function connectReader(readerAddress: string): Promise<void>;

    export function authenticate(): Promise<void>;

    export function listenForAdpuResponse(): Promise<void>;

    export function listenForCardStatusAvailable(): Promise<void>;

    export function startPolling(): Promise<void>;

    export function stopPolling(): Promise<void>;

    export function startScan(): Observable<string>;

    export function stopScan(): Promise<void>;

    export function getConnectionState(): Promise<void>;

    export function getCardStatus(): Promise<void>;

    export function requestCardId(): Promise<void>;
}
