declare module 'cordova-plugin-acs' {
    export function connectReader(readerAddress: string): Promise<void>;

    export function authenticate(): Promise<void>;

    export function listenForAdpuResponse(resolve, reject): void;

    export function listenForCardStatusAvailable(resolve, reject): void;

    export function startPolling(): Promise<void>;

    export function stopPolling(): Promise<void>;

    export function startScan(resolve, reject): void;

    export function stopScan(): Promise<void>;

    export function listenForConnectionState(): void;

    export function getCardStatus(): Promise<void>;

    export function requestCardId(): Promise<void>;
}
