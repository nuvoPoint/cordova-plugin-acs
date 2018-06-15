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

    export function transmitAdpuCommand(command: string): Promise<void>;

    export function transmitEscapeCommand(command: string): Promise<void>;
}

export interface StatusMessage {
    code: number;
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