declare module 'cordova-plugin-acs' {
  export function connectReader(readerAddress: string): Promise<string>;

  export function disconnectReader(): Promise<string>;

  export function enableNotifications(): Promise<string>;

  export function authenticate(): Promise<string>;

  export function listenForAdpuResponse(resolve, reject): string;

  export function listenForEscapeResponse(resolve, reject): string;

  export function listenForCardStatus(resolve, reject): StatusMessage;

  export function listenForConnectionState(resolve, reject): StatusMessage;

  export function listenForNfcConnectionState(resolve, reject): number;

  export function startScan(resolve, reject): BTDevice;

  export function stopScan(): Promise<string>;

  export function transmitAdpuCommand(command: string): Promise<string>;

  export function transmitEscapeCommand(command: string): Promise<string>;

  export function requestBt(): Promise<string>;

  export function requestBtPermissions(): Promise<string>;

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

  export enum AcsErrorCodes {
    ERR_UNKNOWN = 0,
    ERR_READER_NOT_INITIALIZED = 1,
    ERR_OPERATION_FAILED = 2,
    ERR_OPERATION_TIMED_OUT = 3,
    ERR_BT_IS_OFF = 4,
    ERR_BT_ERROR = 5,
    ERR_SCAN_IN_PROGRESS = 6,
    ERR_SCAN_FAILED = 7,
    ERR_READER_ALREADY_CONNECTED = 8,
    ERR_READER_CONNECTION_IN_PROGRESS = 9,
    ERR_READER_CONNECTION_CANCELLED = 10,
    ERR_READER_TYPE_NOT_SUPPORTED = 11,
  }

  export enum AcsCardStatus {
      CARD_UNKNOWN = 0,
      CARD_ABSENT = 1,
      CARD_PRESENT = 2,
      CARD_POWER_SAVING_MODE = 3,
      CARD_POWERED = 4,
  }
  
  export enum AcsConnectionState {
      CON_UNKNOWN = 0,
      CON_DISCONNECTED = 1,
      CON_CONNECTED = 2,
      CON_CONNECTING = 3,
      CON_DISCONNECTING = 4,
  }      
}
