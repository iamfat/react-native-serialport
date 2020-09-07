declare type SerialPortEvent = 'open' | 'close' | 'error' | 'data';
export default class SerialPort {
    private deviceId;
    private baudRate;
    private eventListeners;
    private nativeSubscription;
    constructor(deviceId: string, baudRate: number);
    write(chunk: ArrayBuffer): Promise<number>;
    close(): any;
    open(): void;
    destroy(): void;
    on(event: SerialPortEvent, cb: Function): this;
    off(event: SerialPortEvent, cb: Function): this;
}
export {};
