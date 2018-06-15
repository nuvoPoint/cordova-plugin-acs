declare module 'cordova-plugin-acs' {
    export function connectReader(readerAddress: string): Promise<void>;

    export function disconnectReader(): Promise<void>;

    export function enableNotifications(): Promise<void>;

    export function authenticate(): Promise<void>;

    export function listenForAdpuResponse(resolve, reject): String;

    export function listenForEscapeResponse(resolve, reject): String;

    export function listenForCardStatus(resolve, reject): StatusMessage;

    export function listenForConnectionState(resolve, reject): StatusMessage;

    export function startScan(resolve, reject): BTDevice;

    export function stopScan(): Promise<void>;

    export function transmitAdpuCommand(): Promise<void>;

    export function transmitEscapeCommand(): Promise<void>;
}

export interface StatusMessage {
    code: int;
    message: String;
}

export interface BTDevice {
    mDevice: {
      mAddress: string;
    };
    mScanRecord: {
      mDeviceName: string
    };
  }